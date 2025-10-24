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
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_SELECTED_ROLE = "selected_role" // For dual role login
        private const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
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
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Save authentication data with selected role (for dual role login)
     */
    fun saveAuthDataWithRole(token: String, role: String, nic: String, selectedRole: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_ROLE, role)
            putString(KEY_NIC, nic)
            putString(KEY_SELECTED_ROLE, selectedRole)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
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
    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""
    
    /**
     * Get user NIC
     */
    fun getNIC(): String = prefs.getString(KEY_NIC, "") ?: ""
    
    /**
     * Get selected role (for dual role login)
     */
    fun getSelectedRole(): String = prefs.getString(KEY_SELECTED_ROLE, "") ?: ""
    
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
        if (!isLoggedIn()) return false
        
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        
        // Check if session has expired
        return (currentTime - loginTime) < SESSION_TIMEOUT_MS
    }
    
    /**
     * Get session remaining time in milliseconds
     */
    fun getSessionRemainingTime(): Long {
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - loginTime
        
        return maxOf(0L, SESSION_TIMEOUT_MS - elapsed)
    }
    
    /**
     * Extend session by updating login time
     */
    fun extendSession() {
        if (isLoggedIn()) {
            prefs.edit().putLong(KEY_LOGIN_TIME, System.currentTimeMillis()).apply()
        }
    }
    
    /**
     * Check if user is owner
     */
    fun isOwner(): Boolean = getRole() == "EVOwner"
    
    /**
     * Check if user is station operator
     */
    fun isStationOperator(): Boolean = getRole() == "StationOperator"
    
    /**
     * Check if user is backoffice admin
     */
    fun isBackoffice(): Boolean = getRole() == "Backoffice"
    
    /**
     * Check if user is operator (StationOperator or Backoffice)
     */
    fun isOperator(): Boolean = isStationOperator() || isBackoffice()
    
    /**
     * Check if current user is EVOwner with dual access
     */
    fun isEVOwnerWithDualAccess(): Boolean = getRole() == "EVOwner"
    
    /**
     * Check if user is currently logged in as Owner (for dual role)
     */
    fun isLoggedInAsOwner(): Boolean = getSelectedRole() == "Owner"
    
    /**
     * Check if user is currently logged in as Operator (for dual role)
     */
    fun isLoggedInAsOperator(): Boolean = getSelectedRole() == "Operator"
    
    /**
     * Get effective role (selected role for EVOwners, actual role for others)
     */
    fun getEffectiveRole(): String {
        return if (isEVOwnerWithDualAccess() && getSelectedRole().isNotEmpty()) {
            getSelectedRole()
        } else {
            getRole()
        }
    }
}
