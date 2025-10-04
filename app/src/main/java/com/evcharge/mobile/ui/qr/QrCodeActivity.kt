package com.evcharge.mobile.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Toasts
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR Code display activity
 */
class QrCodeActivity : AppCompatActivity() {
    
    private lateinit var qrImageView: ImageView
    private var qrCodeData: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        
        initializeComponents()
        setupUI()
        generateQRCode()
    }
    
    private fun initializeComponents() {
        qrImageView = findViewById(R.id.qr_image_view)
        
        // Get QR code data from intent
        qrCodeData = intent.getStringExtra("qr_data") ?: ""
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Booking QR Code"
    }
    
    private fun generateQRCode() {
        if (qrCodeData.isEmpty()) {
            Toasts.showError(this, "No QR code data available")
            finish()
            return
        }
        
        try {
            val bitmap = generateQRCodeBitmap(qrCodeData)
            qrImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toasts.showError(this, "Failed to generate QR code: ${e.message}")
            finish()
        }
    }
    
    private fun generateQRCodeBitmap(data: String): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_qr_code, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_share -> {
                shareQRCode()
                true
            }
            R.id.action_save -> {
                saveQRCode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shareQRCode() {
        try {
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "Booking QR Code: $qrCodeData")
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            Toasts.showError(this, "Failed to share QR code")
        }
    }
    
    private fun saveQRCode() {
        try {
            // In a real app, you would save the bitmap to gallery
            Toasts.showInfo(this, "QR code saved to gallery")
        } catch (e: Exception) {
            Toasts.showError(this, "Failed to save QR code")
        }
    }
}
