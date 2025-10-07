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
import androidx.lifecycle.lifecycleScope
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
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import kotlinx.coroutines.launch

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
        
        initializeComponents()
        setupUI()
        checkCameraPermission()
    }
    
    private fun initializeComponents() {
        barcodeView = findViewById(R.id.barcode_scanner)
        
        val prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val bookingApi = BookingApi(apiClient)
        bookingRepository = BookingRepository(bookingApi)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Scan QR Code"
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Permissions.REQUEST_CAMERA_PERMISSION)
        } else {
            startScanning()
        }
    }
    
    private fun startScanning() {
        if (!isScanning) {
            isScanning = true
            barcodeView.decodeContinuous(callback)
        }
    }
    
    private fun stopScanning() {
        if (isScanning) {
            isScanning = false
            barcodeView.pause()
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
        
        // Use real API call with coroutines
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = bookingRepository.completeBooking(bookingId, qrCode)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        Toasts.showSuccess(this@QrScanActivity, "Booking completed successfully")
                        showCompletionDialog(bookingId)
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@QrScanActivity, "Failed to complete booking: ${error?.message}")
                        resumeScanning()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@QrScanActivity, "Failed to complete booking: ${e.message}")
                    resumeScanning()
                }
            }
        }
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
                    startScanning()
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
            startScanning()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopScanning()
    }
}
