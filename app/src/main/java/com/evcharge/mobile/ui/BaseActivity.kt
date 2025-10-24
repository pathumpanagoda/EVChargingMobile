package com.evcharge.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.SessionManager
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.ui.auth.AuthActivity
import android.content.Intent

/**
 * Base activity that handles session management and automatic logout
 */
abstract class BaseActivity : AppCompatActivity() {
    
    protected lateinit var prefs: Prefs
    protected lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize session management
        prefs = Prefs(this)
        sessionManager = SessionManager(this)
        
        // Check if user is logged in and session is valid
        if (!prefs.isLoggedIn() || !prefs.hasValidSession()) {
            // User not logged in or session expired, redirect to login
            redirectToLogin()
            return
        }
        
        // Start session monitoring
        sessionManager.startSessionMonitoring()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check session validity on resume
        if (!sessionManager.isSessionValid()) {
            redirectToLogin()
            return
        }
        
        // Extend session on user activity
        sessionManager.extendSession()
    }
    
    override fun onPause() {
        super.onPause()
        // Session monitoring continues in background
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop session monitoring when activity is destroyed
        sessionManager.stopSessionMonitoring()
    }
    
    /**
     * Redirect to login activity
     */
    protected fun redirectToLogin() {
        prefs.clear()
        sessionManager.stopSessionMonitoring()
        
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Handle session timeout
     */
    protected fun handleSessionTimeout() {
        Toasts.showInfo(this, "Your session has expired. Please login again.")
        redirectToLogin()
    }
    
    /**
     * Check if user is logged in
     */
    protected fun isUserLoggedIn(): Boolean {
        return prefs.isLoggedIn() && prefs.hasValidSession()
    }
}

