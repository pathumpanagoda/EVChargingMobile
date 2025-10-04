package com.evcharge.mobile.common

import android.content.Context
import android.widget.Toast

/**
 * Toast utility functions
 */
object Toasts {
    
    /**
     * Show short toast message
     */
    fun showShort(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show long toast message
     */
    fun showLong(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Show success message
     */
    fun showSuccess(context: Context, message: String) {
        showShort(context, "✓ $message")
    }
    
    /**
     * Show error message
     */
    fun showError(context: Context, message: String) {
        showLong(context, "✗ $message")
    }
    
    /**
     * Show warning message
     */
    fun showWarning(context: Context, message: String) {
        showShort(context, "⚠ $message")
    }
    
    /**
     * Show info message
     */
    fun showInfo(context: Context, message: String) {
        showShort(context, "ℹ $message")
    }
    
    /**
     * Show network error
     */
    fun showNetworkError(context: Context) {
        showError(context, "Network error. Please check your connection.")
    }
    
    /**
     * Show validation error
     */
    fun showValidationError(context: Context, message: String) {
        showError(context, "Validation error: $message")
    }
    
    /**
     * Show server error
     */
    fun showServerError(context: Context, message: String) {
        showError(context, "Server error: $message")
    }
    
    /**
     * Show loading message
     */
    fun showLoading(context: Context, message: String = "Loading...") {
        showShort(context, message)
    }
}
