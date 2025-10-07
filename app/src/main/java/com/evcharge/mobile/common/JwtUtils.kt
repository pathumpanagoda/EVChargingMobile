package com.evcharge.mobile.common

import android.util.Base64
import android.util.Log
import com.evcharge.mobile.BuildConfig
import org.json.JSONObject

/**
 * Utility class for JWT token operations
 */
object JwtUtils {
    
    private const val TAG = "JwtUtils"
    
    /**
     * Extract NIC from JWT token
     */
    fun extractNIC(token: String): String? {
        return try {
            if (token.isEmpty()) {
                Log.w(TAG, "Token is empty")
                return null
            }
            
            // Split JWT token into parts
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.w(TAG, "Invalid JWT token format")
                return null
            }
            
            // Decode the payload (second part)
            val payload = parts[1]
            
            // Add padding if needed
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            
            // Decode base64
            val decodedBytes = Base64.decode(paddedPayload, Base64.URL_SAFE)
            val payloadJson = String(decodedBytes)
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "JWT Payload: $payloadJson")
            }
            
            // Parse JSON and extract NIC
            val jsonObject = JSONObject(payloadJson)
            val nic = jsonObject.optString("nic", null)
            
            if (nic.isNullOrEmpty()) {
                Log.w(TAG, "NIC not found in JWT token")
                return null
            }
            
            Log.d(TAG, "Extracted NIC from JWT: $nic")
            nic
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting NIC from JWT token: ${e.message}", e)
            null
        }
    }
    
    /**
     * Extract role from JWT token
     */
    fun extractRole(token: String): String? {
        return try {
            if (token.isEmpty()) {
                return null
            }
            
            val parts = token.split(".")
            if (parts.size != 3) {
                return null
            }
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            
            val decodedBytes = Base64.decode(paddedPayload, Base64.URL_SAFE)
            val payloadJson = String(decodedBytes)
            val jsonObject = JSONObject(payloadJson)
            
            jsonObject.optString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role", null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting role from JWT token: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if JWT token is expired
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            if (token.isEmpty()) {
                return true
            }
            
            val parts = token.split(".")
            if (parts.size != 3) {
                return true
            }
            
            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> payload + "=="
                3 -> payload + "="
                else -> payload
            }
            
            val decodedBytes = Base64.decode(paddedPayload, Base64.URL_SAFE)
            val payloadJson = String(decodedBytes)
            val jsonObject = JSONObject(payloadJson)
            
            val exp = jsonObject.optLong("exp", 0)
            if (exp == 0L) {
                return true
            }
            
            val currentTime = System.currentTimeMillis() / 1000
            exp < currentTime
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking token expiration: ${e.message}", e)
            true
        }
    }
}
