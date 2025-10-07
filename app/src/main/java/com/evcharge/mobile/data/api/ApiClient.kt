package com.evcharge.mobile.data.api

import android.util.Log
import com.evcharge.mobile.BuildConfig
import com.evcharge.mobile.common.Prefs
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for API communication using OkHttp
 */
class ApiClient(private val prefs: Prefs) {
    
    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = BuildConfig.BASE_URL
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(prefs))
            .addInterceptor(LoggingInterceptor())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Make a POST request
     */
    fun post(path: String, body: JSONObject): JSONObject {
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .post(requestBody)
            .build()
        
        return executeRequest(request)
    }
    
    /**
     * Make a GET request
     */
    fun get(path: String): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .get()
            .build()
        
        return executeRequest(request)
    }
    
    /**
     * Make a PUT request
     */
    fun put(path: String, body: JSONObject): JSONObject{
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .put(requestBody)
            .build()
        
        return executeRequest(request)
    }
    
    /**
     * Make a DELETE request
     */
    fun delete(path: String): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .delete()
            .build()
        
        return executeRequest(request)
    }
    
    /**
     * Execute HTTP request and return JSON response
     */
    private fun executeRequest(request: Request): JSONObject {
        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
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
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            JSONObject().apply {
                put("success", false)
                put("message", "Network error. Please check your connection.")
                put("error", "NETWORK_ERROR")
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
            
            val newRequest = if (token.isNotEmpty()) {
                originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
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
