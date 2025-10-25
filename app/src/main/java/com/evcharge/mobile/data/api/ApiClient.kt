package com.evcharge.mobile.data.api

import android.util.Log
import com.evcharge.mobile.BuildConfig
import com.evcharge.mobile.common.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for API communication using OkHttp
 */
class ApiClient(private val prefs: Prefs) {
    
    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = BuildConfig.BASE_URL
        private const val TIMEOUT_SECONDS = 60L  // Increased timeout for mobile networks
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val WRITE_TIMEOUT_SECONDS = 30L
    }
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(prefs))
            .addInterceptor(LoggingInterceptor())
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)  // Retry on connection failure
            .build()
    }
    
    /**
     * Make a POST request
     */
    suspend fun post(path: String, body: JSONObject): JSONObject {
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .post(requestBody)
            .build()
        
        return executeRequestAsync(request)
    }
    
    /**
     * Make a GET request
     */
    suspend fun get(path: String): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .get()
            .build()
        
        return executeRequestAsync(request)
    }
    
    /**
     * Make a PUT request
     */
    suspend fun put(path: String, body: JSONObject): JSONObject {
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .put(requestBody)
            .build()
        
        return executeRequestAsync(request)
    }
    
    /**
     * Make a DELETE request
     */
    suspend fun delete(path: String): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .delete()
            .build()
        
        return executeRequestAsync(request)
    }
    
    /**
     * Test connection to the server
     */
    suspend fun testConnection(): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL/health")  // Try a simple health endpoint
            .get()
            .build()
        
        return try {
            executeRequestAsync(request)
        } catch (e: Exception) {
            // If health endpoint doesn't exist, try the base URL
            val baseRequest = Request.Builder()
                .url(BASE_URL)
                .get()
                .build()
            executeRequestAsync(baseRequest)
        }
    }
    
    /**
     * Execute HTTP request asynchronously and return JSON response
     */
    private suspend fun executeRequestAsync(request: Request): JSONObject {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            executeRequest(request)
        }
    }
    
    /**
     * Execute HTTP request and return JSON response
     */
    private fun executeRequest(request: Request): JSONObject {
        return try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Making request to: ${request.url}")
                Log.d(TAG, "Request method: ${request.method}")
                Log.d(TAG, "Request headers: ${request.headers}")
            }
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: $responseBody")
            }
            
            when (response.code) {
                200, 201 -> {
                    try {
                        JSONObject(responseBody)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse JSON response", e)
                        JSONObject().apply {
                            put("success", false)
                            put("message", "Invalid response format")
                        }
                    }
                }
                400 -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Bad request. Please check your input.")
                        put("error", "BAD_REQUEST")
                    }
                }
                401 -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Unauthorized. Please login again.")
                        put("error", "UNAUTHORIZED")
                    }
                }
                403 -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Forbidden. You don't have permission to perform this action.")
                        put("error", "FORBIDDEN")
                    }
                }
                404 -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Resource not found.")
                        put("error", "NOT_FOUND")
                    }
                }
                409 -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Conflict. Resource already exists or is in use.")
                        put("error", "CONFLICT")
                    }
                }
                422 -> {
                    try {
                        val errorJson = JSONObject(responseBody)
                        JSONObject().apply {
                            put("success", false)
                            put("message", errorJson.optString("message", "Validation error"))
                            put("error", "VALIDATION_ERROR")
                            put("details", errorJson.optJSONObject("details"))
                        }
                    } catch (e: Exception) {
                        JSONObject().apply {
                            put("success", false)
                            put("message", "Validation error")
                            put("error", "VALIDATION_ERROR")
                        }
                    }
                }
                500 -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Internal server error. Please try again later.")
                        put("error", "INTERNAL_ERROR")
                    }
                }
                else -> {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Unexpected error occurred. Code: ${response.code}")
                        put("error", "UNKNOWN_ERROR")
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Socket timeout error", e)
            JSONObject().apply {
                put("success", false)
                put("message", "Connection timeout. The server at $BASE_URL is not responding. Please check if the server is running and accessible.")
                put("error", "TIMEOUT_ERROR")
                put("details", "SocketTimeoutException: ${e.message}")
                put("server_url", BASE_URL)
                put("suggestion", "Ensure both devices are on the same Wi-Fi network and the server is running")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Log.e(TAG, "IOException details: ${e.message}")
            Log.e(TAG, "IOException cause: ${e.cause}")
            
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    "Connection timeout. Please check if the server is running and accessible at $BASE_URL"
                }
                e.message?.contains("refused", ignoreCase = true) == true -> {
                    "Connection refused. The server may not be running or accessible."
                }
                e.message?.contains("unreachable", ignoreCase = true) == true -> {
                    "Network unreachable. Please check your Wi-Fi connection and server IP address."
                }
                else -> {
                    "Network error: ${e.message}. Please check your connection and ensure the server is running."
                }
            }
            
            JSONObject().apply {
                put("success", false)
                put("message", errorMessage)
                put("error", "NETWORK_ERROR")
                put("details", e.message)
                put("server_url", BASE_URL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Exception stack trace: ${e.stackTraceToString()}")
            JSONObject().apply {
                put("success", false)
                put("message", "An unexpected error occurred: ${e.javaClass.simpleName} - ${e.message}")
                put("error", "UNKNOWN_ERROR")
            }
        }
    }
    
    /**
     * Authentication interceptor to add JWT token
     */
    private class AuthInterceptor(private val prefs: Prefs) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val token = prefs.getToken()
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AuthInterceptor - Token length: ${token.length}")
                Log.d(TAG, "AuthInterceptor - Token preview: ${token.take(20)}...")
            }
            
            val newRequest = if (token.isNotEmpty()) {
                val requestWithAuth = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "AuthInterceptor - Added Authorization header")
                }
                
                requestWithAuth
            } else {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "AuthInterceptor - No token available")
                }
                originalRequest
            }
            
            return chain.proceed(newRequest)
        }
    }
    
    /**
     * Logging interceptor for debugging
     */
    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Request: ${request.method} ${request.url}")
                request.headers.forEach { (name, value) ->
                    Log.d(TAG, "Header: $name: $value")
                }
            }
            
            val response = chain.proceed(request)
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Response: ${response.code} ${response.message}")
                response.headers.forEach { (name, value) ->
                    Log.d(TAG, "Response Header: $name: $value")
                }
            }
            
            return response
        }
    }
}
