package com.evcharge.mobile.ui.bookings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Datex
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingStatus
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.qr.QrCodeActivity
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Booking detail activity
 */
class BookingDetailActivity : AppCompatActivity() {
    
    companion object {
        private const val MODIFY_BOOKING_REQUEST_CODE = 1001
    }
    
    private lateinit var prefs: Prefs
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    
    // UI Components
    private lateinit var tvStationName: MaterialTextView
    private lateinit var tvAddress: MaterialTextView
    private lateinit var tvStartTime: MaterialTextView
    private lateinit var tvEndTime: MaterialTextView
    private lateinit var tvDuration: MaterialTextView
    private lateinit var tvStatus: MaterialTextView
    private lateinit var tvCreatedAt: MaterialTextView
    private lateinit var btnModify: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnShowQr: MaterialButton
    private lateinit var loadingView: LoadingView
    
    private var booking: Booking? = null
    private var bookingId: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadBooking()
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val bookingApi = BookingApi(apiClient)
        val stationApi = StationApi(apiClient)
        bookingRepository = BookingRepository(bookingApi)
        stationRepository = StationRepository(stationApi)
        
        // Get booking ID from intent
        bookingId = intent.getStringExtra("booking_id") ?: ""
        
        // Initialize UI components
        tvStationName = findViewById(R.id.tv_station_name)
        tvAddress = findViewById(R.id.tv_address)
        tvStartTime = findViewById(R.id.tv_start_time)
        tvEndTime = findViewById(R.id.tv_end_time)
        tvDuration = findViewById(R.id.tv_duration)
        tvStatus = findViewById(R.id.tv_status)
        tvCreatedAt = findViewById(R.id.tv_created_at)
        btnModify = findViewById(R.id.btn_modify)
        btnCancel = findViewById(R.id.btn_cancel)
        btnShowQr = findViewById(R.id.btn_show_qr)
        loadingView = findViewById(R.id.loading_view)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Booking Details"
    }
    
    private fun setupClickListeners() {
        android.util.Log.d("BookingDetail", "Setting up click listeners")
        
        // Modify button
        btnModify.setOnClickListener {
            android.util.Log.d("BookingDetail", "Modify button clicked")
            modifyBooking()
        }
        
        // Cancel button
        btnCancel.setOnClickListener {
            android.util.Log.d("BookingDetail", "Cancel button clicked")
            showCancelConfirmation()
        }
        
        // Show QR button
        btnShowQr.setOnClickListener {
            android.util.Log.d("BookingDetail", "Show QR button clicked")
            showQRCode()
        }
        
        android.util.Log.d("BookingDetail", "Click listeners set up successfully")
    }
    
    private fun loadBooking() {
        if (bookingId.isEmpty()) {
            Toasts.showError(this, "Invalid booking ID")
            finish()
            return
        }
        
        loadingView.show()
        loadingView.setMessage("Loading booking details...")
        
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Get owner's bookings and find the specific booking
                val result = bookingRepository.getOwnerBookings(ownerNic, false) // Include history
                
                if (result.isSuccess()) {
                    val bookings = result.getDataOrNull() ?: emptyList()
                    val foundBooking = bookings.find { it.id == bookingId }
                    
                    if (foundBooking != null) {
                        // Enhance booking with station details
                        val enhancedBooking = enhanceBookingWithStationDetails(foundBooking)
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            booking = enhancedBooking
                            updateUI()
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toasts.showError(this@BookingDetailActivity, "Booking not found")
                            finish()
                        }
                    }
                } else {
                    val error = result.getErrorOrNull()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toasts.showError(this@BookingDetailActivity, error?.message ?: "Failed to load booking")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@BookingDetailActivity, "Failed to load booking: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    /**
     * Enhance booking data with station details
     */
    private suspend fun enhanceBookingWithStationDetails(booking: Booking): Booking {
        return try {
            if (booking.stationId.isNotEmpty()) {
                val stationResult = stationRepository.getStation(booking.stationId)
                if (stationResult.isSuccess()) {
                    val station = stationResult.getDataOrNull()
                    if (station != null) {
                        // Create enhanced booking with station details
                        booking.copy(
                            stationName = station.name,
                            stationId = station.id
                        )
                    } else {
                        // Station not found, use fallback name
                        booking.copy(
                            stationName = "Station ${booking.stationId.take(8)}..."
                        )
                    }
                } else {
                    // Station API failed, use fallback name
                    booking.copy(
                        stationName = "Station ${booking.stationId.take(8)}..."
                    )
                }
            } else {
                booking
            }
        } catch (e: Exception) {
            android.util.Log.w("BookingDetailActivity", "Failed to load station details: ${e.message}")
            // Network error, use fallback name
            booking.copy(
                stationName = "Station ${booking.stationId.take(8)}..."
            )
        }
    }
    
    private fun updateUI() {
        val booking = this.booking ?: return
        
        tvStationName.text = booking.stationName ?: "Unknown Station"
        tvAddress.text = "Station ID: ${booking.stationId}" // Display station ID for now
        tvStartTime.text = Datex.formatToDisplay(booking.startTime)
        tvEndTime.text = Datex.formatToDisplay(booking.endTime)
        tvDuration.text = formatDuration(booking.startTime, booking.endTime)
        tvStatus.text = booking.status.name
        tvCreatedAt.text = Datex.formatToDisplay(booking.createdAt)
        
        // Set status color
        val statusColor = when (booking.status) {
            BookingStatus.PENDING -> R.color.status_pending
            BookingStatus.APPROVED -> R.color.status_approved
            BookingStatus.COMPLETED -> R.color.status_completed
            BookingStatus.CANCELLED -> R.color.status_cancelled
        }
        tvStatus.setTextColor(getColor(statusColor))
        
        // Update button visibility
        updateButtonVisibility()
    }
    
    private fun updateButtonVisibility() {
        val booking = this.booking ?: return
        
        android.util.Log.d("BookingDetail", "Updating button visibility for status: ${booking.status.name}")
        
        when (booking.status) {
            BookingStatus.PENDING -> {
                android.util.Log.d("BookingDetail", "Setting PENDING buttons - Modify: VISIBLE, Cancel: VISIBLE, QR: GONE")
                btnModify.visibility = android.view.View.VISIBLE
                btnCancel.visibility = android.view.View.VISIBLE
                btnShowQr.visibility = android.view.View.GONE
            }
            BookingStatus.APPROVED -> {
                android.util.Log.d("BookingDetail", "Setting APPROVED buttons - Modify: GONE, Cancel: VISIBLE, QR: VISIBLE")
                btnModify.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.VISIBLE
                btnShowQr.visibility = android.view.View.VISIBLE
            }
            BookingStatus.COMPLETED -> {
                android.util.Log.d("BookingDetail", "Setting COMPLETED buttons - All GONE")
                btnModify.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.GONE
                btnShowQr.visibility = android.view.View.GONE
            }
            BookingStatus.CANCELLED -> {
                android.util.Log.d("BookingDetail", "Setting CANCELLED buttons - All GONE")
                btnModify.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.GONE
                btnShowQr.visibility = android.view.View.GONE
            }
        }
        
        android.util.Log.d("BookingDetail", "Button visibility - Modify: ${btnModify.visibility}, Cancel: ${btnCancel.visibility}, QR: ${btnShowQr.visibility}")
    }
    
    private fun formatDuration(startTime: Long, endTime: Long): String {
        val duration = endTime - startTime
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
        return "${hours}h ${minutes}m"
    }
    
    private fun showCancelConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Yes") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun cancelBooking() {
        loadingView.show()
        loadingView.setMessage("Cancelling booking...")
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = bookingRepository.cancelBooking(bookingId)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        Toasts.showSuccess(this@BookingDetailActivity, "Booking cancelled successfully")
                        finish()
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingDetailActivity, error?.message ?: "Failed to cancel booking")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@BookingDetailActivity, "Failed to cancel booking: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    private fun modifyBooking() {
        val booking = this.booking ?: return
        
        android.util.Log.d("BookingDetail", "Modify button clicked")
        android.util.Log.d("BookingDetail", "Booking status: ${booking.status.name}")
        android.util.Log.d("BookingDetail", "Booking start time: ${booking.startTime}")
        android.util.Log.d("BookingDetail", "Current time: ${System.currentTimeMillis()}")
        
        // Check if booking can be modified (only pending bookings)
        if (booking.status.name != "PENDING") {
            android.util.Log.d("BookingDetail", "Booking is not PENDING, status: ${booking.status.name}")
            Toasts.showError(this, "Only pending bookings can be modified")
            return
        }
        
        // Check if booking can be modified (12-hour rule)
        // The rule should be: cannot modify within 12 hours of the START TIME, not creation time
        val twelveHoursFromStartTime = booking.startTime - (12 * 60 * 60 * 1000)
        val currentTime = System.currentTimeMillis()
        
        android.util.Log.d("BookingDetail", "Current time: $currentTime")
        android.util.Log.d("BookingDetail", "Booking start time: ${booking.startTime}")
        android.util.Log.d("BookingDetail", "12 hours before start time: $twelveHoursFromStartTime")
        
        if (currentTime >= twelveHoursFromStartTime) {
            android.util.Log.d("BookingDetail", "Current time is within 12 hours of start time, cannot modify")
            Toasts.showError(this, "Cannot modify booking within 12 hours of reservation time")
            return
        }
        
        android.util.Log.d("BookingDetail", "Opening BookingModifyActivity")
        val intent = Intent(this, BookingModifyActivity::class.java)
        intent.putExtra("booking", booking)
        startActivityForResult(intent, MODIFY_BOOKING_REQUEST_CODE)
    }
    
    private fun showQRCode() {
        val booking = this.booking ?: return
        
        if (booking.qrCode != null) {
            val intent = Intent(this, QrCodeActivity::class.java)
            intent.putExtra("qr_data", booking.qrCode)
            startActivity(intent)
        } else {
            Toasts.showError(this, "QR code not available")
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_booking_detail, menu)
        return true
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == MODIFY_BOOKING_REQUEST_CODE && resultCode == RESULT_OK) {
            // Refresh booking details after successful modification
            loadBooking()
            Toasts.showSuccess(this, "Booking updated successfully")
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                loadBooking()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
