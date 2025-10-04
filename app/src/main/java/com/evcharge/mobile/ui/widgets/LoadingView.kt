package com.evcharge.mobile.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.evcharge.mobile.R

/**
 * Custom loading view widget
 */
class LoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val progressBar: ProgressBar
    private val messageText: TextView
    
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_loading_view, this, true)
        progressBar = findViewById(R.id.progress_bar)
        messageText = findViewById(R.id.message_text)
    }
    
    /**
     * Set loading message
     */
    fun setMessage(message: String) {
        messageText.text = message
    }
    
    /**
     * Show loading view
     */
    fun show() {
        visibility = VISIBLE
    }
    
    /**
     * Hide loading view
     */
    fun hide() {
        visibility = GONE
    }
    
    /**
     * Check if loading view is visible
     */
    fun isShowing(): Boolean = visibility == VISIBLE
}
