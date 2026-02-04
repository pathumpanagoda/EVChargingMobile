package com.evcharge.mobile.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Permissions
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.dto.BookingCompleteRequest
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.ui.dashboard.OperatorHomeActivity
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.evcharge.mobile.common.Prefs

/**
 * QR Code scanning activity for operators
 */
class QrScanActivity : AppCompatActivity() {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var bookingRepository: BookingRepository
    private var isScanning = false
    private var isTorchOn = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)
        
        try {
            initializeComponents()
            setupUI()
            checkCameraPermission()
        } catch (e: Exception) {
            Toasts.showError(this, "QR Scanner initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun initializeComponents() {
        try {
            barcodeView = findViewById(R.id.barcode_scanner)
            
            val prefs = Prefs(this)
            val apiClient = ApiClient(prefs)
            val bookingApi = BookingApi(apiClient)
            bookingRepository = BookingRepository(bookingApi)
        } catch (e: Exception) {
            Toasts.showError(this, "Component initialization failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupUI() {
        try {
            // Set up toolbar with error handling for action bar conflict
            try {
                setSupportActionBar(findViewById(R.id.toolbar))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Scan QR Code"
            } catch (e: Exception) {
                // Handle action bar conflict gracefully
                Toasts.showWarning(this, "Toolbar setup skipped: ${e.message}")
            }
        } catch (e: Exception) {
            Toasts.showError(this, "UI setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show rationale and request permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera permission is needed to scan QR codes for booking completion.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Permissions.REQUEST_CAMERA_PERMISSION)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        Toasts.showError(this, "Camera permission is required to scan QR codes")
                        finish()
                    }
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Permissions.REQUEST_CAMERA_PERMISSION)
            }
        } else {
            initializeCamera()
        }
    }
    
    private fun initializeCamera() {
        try {
            // Initialize the camera with proper error handling
            barcodeView.initializeFromIntent(intent)
            startScanning()
        } catch (e: Exception) {
            Toasts.showError(this, "Camera initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun startScanning() {
        if (!isScanning) {
            try {
                isScanning = true
                barcodeView.resume() // Resume the camera
                barcodeView.decodeContinuous(callback)
            } catch (e: Exception) {
                Toasts.showError(this, "Failed to start camera: ${e.message}")
                isScanning = false
            }
        }
    }
    
    private fun stopScanning() {
        if (isScanning) {
            try {
                isScanning = false
                barcodeView.pause()
            } catch (e: Exception) {
                Toasts.showWarning(this, "Error stopping camera: ${e.message}")
            }
        }
    }
    
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (isScanning) {
                stopScanning()
                processQRCode(result.text)
            }
        }
        
        override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
            // Handle possible result points if needed
        }
    }
    
    private fun processQRCode(qrData: String) {
        try {
            // Parse QR code data (assuming format: BOOKING:{id}:{hash})
            val parts = qrData.split(":")
            if (parts.size >= 3 && parts[0] == "BOOKING") {
                val bookingId = parts[1]
                val qrHash = parts[2]
                
                completeBooking(bookingId, qrData)
            } else {
                Toasts.showError(this, "Invalid QR code format")
                resumeScanning()
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Failed to process QR code: ${e.message}")
            resumeScanning()
        }
    }
    
    private fun completeBooking(bookingId: String, qrCode: String) {
        val request = BookingCompleteRequest(bookingId, qrCode)
        
        // Show loading
        Toasts.showLoading(this, "Processing booking...")
        
        // In a real app, you would use coroutines here
        // For now, we'll simulate the API call
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                // Simulate successful completion
                Toasts.showSuccess(this, "Booking completed successfully")
                
                // Show completion dialog
                showCompletionDialog(bookingId)
            } catch (e: Exception) {
                Toasts.showError(this, "Failed to complete booking: ${e.message}")
                resumeScanning()
            }
        }, 2000)
    }
    
    private fun showCompletionDialog(bookingId: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Booking Completed")
            .setMessage("Booking $bookingId has been completed successfully.")
            .setPositiveButton("OK") { _, _ ->
                // Return to operator home
                val intent = Intent(this, OperatorHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun resumeScanning() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startScanning()
        }, 1000)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            Permissions.REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeCamera()
                } else {
                    Toasts.showError(this, "Camera permission is required to scan QR codes")
                    finish()
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_qr_scan, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_toggle_flash -> {
                toggleFlash()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleFlash() {
        if (isTorchOn) {
            barcodeView.setTorchOff()
            isTorchOn = false
        } else {
            barcodeView.setTorchOn()
            isTorchOn = true
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (!isScanning) {
                initializeCamera()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopScanning()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            barcodeView.pause()
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
}
