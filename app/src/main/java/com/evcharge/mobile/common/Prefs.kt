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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save authentication data
     */
    fun saveAuthData(token: String, role: String, nic: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_ROLE, role)
            putString(KEY_NIC, nic)
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
        }
    }
    
    /**
     * Get JWT token
     */
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
    
    /**
     * Get user role
     */
    fun getRole(): String {
        val storedRole = prefs.getString(KEY_ROLE, "") ?: ""
        
        // If role is not stored, try to extract from JWT token
        if (storedRole.isEmpty()) {
            val token = getToken()
            if (token.isNotEmpty()) {
                val extractedRole = JwtUtils.extractRole(token)
                if (!extractedRole.isNullOrEmpty()) {
                    // Save the extracted role for future use
                    prefs.edit().putString(KEY_ROLE, extractedRole).apply()
                    return extractedRole
                }
            }
        }
        
        return storedRole
    }
    
    /**
     * Get user NIC
     */
    fun getNIC(): String {
        val storedNIC = prefs.getString(KEY_NIC, "") ?: ""
        
        // If NIC is not stored, try to extract from JWT token
        if (storedNIC.isEmpty()) {
            val token = getToken()
            if (token.isNotEmpty()) {
                val extractedNIC = JwtUtils.extractNIC(token)
                if (!extractedNIC.isNullOrEmpty()) {
                    // Save the extracted NIC for future use
                    prefs.edit().putString(KEY_NIC, extractedNIC).apply()
                    return extractedNIC
                }
            }
        }
        
        return storedNIC
    }
    
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
}
