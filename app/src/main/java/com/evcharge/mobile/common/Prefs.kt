package com.evcharge.mobile.common

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * SharedPreferences helper for managing user session data
 */
class Prefs(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_NIC = "nic"
        private const val KEY_NAME = "name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save authentication data
     */
    fun saveAuthData(token: String, role: String, nic: String, name: String = "") {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_ROLE, role)
            putString(KEY_NIC, nic)
            putString(KEY_NAME, name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * Get authentication data as JSON
     */
    fun getAuthData(): JSONObject {
        return JSONObject().apply {
            put("token", getToken())
            put("role", getRole())
            put("nic", getNIC())
            put("name", getName())
        }
    }
    
    /**
     * Get JWT token
     */
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
    
    /**
     * Get user role
     */
    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""
    
    /**
     * Get user NIC
     */
    fun getNIC(): String = prefs.getString(KEY_NIC, "") ?: ""
    
    /**
     * Get user name
     */
    fun getName(): String = prefs.getString(KEY_NAME, "") ?: ""
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    
    /**
     * Clear all authentication data
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if user has valid session
     */
    fun hasValidSession(): Boolean {
        return isLoggedIn() && getToken().isNotEmpty() && getRole().isNotEmpty()
    }
    
    /**
     * Check if user is owner
     */
    fun isOwner(): Boolean = getRole() == "EVOwner"
    
    /**
     * Check if user is operator
     */
    fun isOperator(): Boolean = getRole() == "Operator"
    
    /**
     * Extract name from JWT token
     */
    fun extractNameFromToken(): String {
        val token = getToken()
        if (token.isEmpty()) return ""
        
        try {
            // JWT tokens have 3 parts separated by dots
            val parts = token.split(".")
            if (parts.size != 3) return ""
            
            // Decode the payload (second part)
            val payload = parts[1]
            val decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val payloadJson = String(decodedBytes)
            val jsonObject = JSONObject(payloadJson)
            
            // Extract name from the token
            return jsonObject.optString("name", "")
        } catch (e: Exception) {
            return ""
        }
    }
}
