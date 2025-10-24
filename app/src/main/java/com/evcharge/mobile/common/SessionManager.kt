package com.evcharge.mobile.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.evcharge.mobile.ui.auth.AuthActivity

/**
 * Session manager for handling session timeout and automatic logout
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val SESSION_CHECK_INTERVAL_MS = 60 * 1000L // Check every minute
        private const val SESSION_WARNING_TIME_MS = 5 * 60 * 1000L // Warn 5 minutes before timeout
    }
    
    private val prefs = Prefs(context)
    private val handler = Handler(Looper.getMainLooper())
    private var sessionCheckRunnable: Runnable? = null
    private var isSessionCheckActive = false
    
    /**
     * Start session monitoring
     */
    fun startSessionMonitoring() {
        if (isSessionCheckActive) return
        
        isSessionCheckActive = true
        sessionCheckRunnable = object : Runnable {
            override fun run() {
                checkSession()
                handler.postDelayed(this, SESSION_CHECK_INTERVAL_MS)
            }
        }
        handler.post(sessionCheckRunnable!!)
    }
    
    /**
     * Stop session monitoring
     */
    fun stopSessionMonitoring() {
        isSessionCheckActive = false
        sessionCheckRunnable?.let { handler.removeCallbacks(it) }
        sessionCheckRunnable = null
    }
    
    /**
     * Check session validity and handle timeout
     */
    private fun checkSession() {
        if (!prefs.hasValidSession()) {
            // Session expired, logout user
            logoutUser()
        } else {
            // Check if session is about to expire
            val remainingTime = prefs.getSessionRemainingTime()
            if (remainingTime <= SESSION_WARNING_TIME_MS && remainingTime > 0) {
                // Show warning about session timeout
                showSessionWarning(remainingTime)
            }
        }
    }
    
    /**
     * Show session timeout warning
     */
    private fun showSessionWarning(remainingTimeMs: Long) {
        val remainingMinutes = remainingTimeMs / (60 * 1000)
        Toasts.showWarning(
            context,
            "Your session will expire in $remainingMinutes minutes. Please save your work."
        )
    }
    
    /**
     * Logout user and redirect to login
     */
    private fun logoutUser() {
        prefs.clear()
        stopSessionMonitoring()
        
        // Redirect to login activity
        val intent = Intent(context, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        
        // Show logout message
        Toasts.showInfo(
            context,
            "Your session has expired. Please login again."
        )
    }
    
    /**
     * Extend session (call this on user activity)
     */
    fun extendSession() {
        if (prefs.isLoggedIn()) {
            prefs.extendSession()
        }
    }
    
    /**
     * Check if session is valid
     */
    fun isSessionValid(): Boolean {
        return prefs.hasValidSession()
    }
    
    /**
     * Get remaining session time in minutes
     */
    fun getRemainingSessionTimeMinutes(): Long {
        return prefs.getSessionRemainingTime() / (60 * 1000)
    }
}
