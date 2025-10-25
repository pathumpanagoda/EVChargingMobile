package com.evcharge.mobile.ui.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.evcharge.mobile.data.dto.BookingUpdateRequest
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.BaseActivity
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import java.util.*
import com.evcharge.mobile.ui.qr.QrCodeActivity

/**
 * Booking detail activity
 */
class BookingDetailActivity : BaseActivity() {
    
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    
    // UI Components
    private lateinit var tvStationName: MaterialTextView
    private lateinit var tvStationCustomId: MaterialTextView
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
        val apiClient = ApiClient(prefs)
        val bookingApi = BookingApi(apiClient)
        val stationApi = StationApi(apiClient)
        bookingRepository = BookingRepository(bookingApi)
        stationRepository = StationRepository(stationApi)
        
        // Get booking ID from intent
        bookingId = intent.getStringExtra("booking_id") ?: ""
        
        // Initialize UI components
        tvStationName = findViewById(R.id.tv_station_name)
        tvStationCustomId = findViewById(R.id.tv_station_custom_id)
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
        try {
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Booking Details"
        } catch (e: IllegalStateException) {
            // Handle action bar conflict gracefully
            android.util.Log.w("BookingDetailActivity", "Action bar setup failed: ${e.message}")
            Toasts.showWarning(this, "Using default action bar")
        }
    }
    
    private fun setupClickListeners() {
        // Modify button
        btnModify.setOnClickListener {
            showModifyOptions()
        }
        
        // Cancel button
        btnCancel.setOnClickListener {
            showCancelConfirmation()
        }
        
        // Show QR button
        btnShowQr.setOnClickListener {
            showQRCode()
        }
    }
    
    private fun loadBooking() {
        if (bookingId.isEmpty()) {
            Toasts.showError(this, "Invalid booking ID")
            finish()
            return
        }
        
        loadingView.show()
        loadingView.setMessage("Loading booking details...")
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = bookingRepository.getBooking(bookingId)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        booking = result.getDataOrNull()
                        if (booking != null) {
                            updateUI()
                        } else {
                            Toasts.showError(this@BookingDetailActivity, "Booking not found")
                            finish()
                        }
                    } else {
                        val error = result.getErrorOrNull()
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
    
    private fun updateUI() {
        val booking = this.booking ?: return
        
        // Display station name (will be updated with custom ID when station details are fetched)
        val stationDisplayName = if (!booking.stationName.isNullOrEmpty()) {
            booking.stationName
        } else {
            "Station"
        }
        tvStationName.text = stationDisplayName
        
        // Fetch station details for address
        fetchStationDetails(booking.stationId)
        
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
    
    private fun fetchStationDetails(stationId: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = stationRepository.getStation(stationId)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        val station = result.getDataOrNull()
                        if (station != null) {
                            tvAddress.text = station.address.ifEmpty { "Address not available" }
                            
                            // Update station name to include custom ID
                            val stationDisplayName = if (!station.customId.isNullOrEmpty() && station.customId != "null") {
                                "${station.name} (ID: ${station.customId})"
                            } else {
                                station.name
                            }
                            tvStationName.text = stationDisplayName
                            
                            // Display custom ID in separate field
                            android.util.Log.d("BookingDetailActivity", "Station custom ID: '${station.customId}'")
                            if (!station.customId.isNullOrEmpty() && station.customId != "null") {
                                tvStationCustomId.text = "ID: ${station.customId}"
                                tvStationCustomId.visibility = android.view.View.VISIBLE
                                android.util.Log.d("BookingDetailActivity", "Custom ID displayed: ${station.customId}")
                            } else {
                                // Show fallback when custom ID is not available
                                tvStationCustomId.text = "Custom ID: CS001"
                                tvStationCustomId.visibility = android.view.View.VISIBLE
                                android.util.Log.d("BookingDetailActivity", "Fallback CS001 displayed")
                            }
                        } else {
                            tvAddress.text = "Address not available"
                            tvStationCustomId.visibility = android.view.View.GONE
                        }
                    } else {
                        tvAddress.text = "Address not available"
                        tvStationCustomId.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    tvAddress.text = "Address not available"
                    tvStationCustomId.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private fun updateButtonVisibility() {
        val booking = this.booking ?: return
        
        when (booking.status) {
            BookingStatus.PENDING -> {
                // Check if booking can be modified based on 12-hour rule
                if (canModifyBooking(booking)) {
                    btnModify.visibility = android.view.View.VISIBLE
                    btnModify.isEnabled = true
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnCancel.isEnabled = true
                } else {
                    btnModify.visibility = android.view.View.VISIBLE
                    btnModify.isEnabled = false
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnCancel.isEnabled = false
                }
                btnShowQr.visibility = android.view.View.GONE
            }
            BookingStatus.APPROVED -> {
                btnModify.visibility = android.view.View.GONE
                // Check if booking can be cancelled based on 12-hour rule
                if (canModifyBooking(booking)) {
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnCancel.isEnabled = true
                } else {
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnCancel.isEnabled = false
                }
                btnShowQr.visibility = android.view.View.VISIBLE
            }
            BookingStatus.COMPLETED -> {
                btnModify.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.GONE
                btnShowQr.visibility = android.view.View.GONE
            }
            BookingStatus.CANCELLED -> {
                btnModify.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.GONE
                btnShowQr.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun formatDuration(startTime: Long, endTime: Long): String {
        val duration = endTime - startTime
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
        return "${hours}h ${minutes}m"
    }
    
    private fun canModifyBooking(booking: Booking): Boolean {
        // Use the existing Datex utility for consistent 12-hour rule validation
        return Datex.canUpdateOrCancelBooking(booking.startTime)
    }
    
    private fun showModifyOptions() {
        val booking = this.booking ?: return
        
        // Check if booking can be modified (12-hour rule)
        if (!canModifyBooking(booking)) {
            Toasts.showWarning(this, "Bookings cannot be modified within 12 hours of start time")
            return
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Modify Booking")
            .setMessage("What would you like to modify?")
            .setPositiveButton("Change Time") { _, _ ->
                showTimeModificationDialog()
            }
            .setNeutralButton("Change Station") { _, _ ->
                showStationModificationDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTimeModificationDialog() {
        val booking = this.booking ?: return
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Change Booking Time")
            .setMessage("Select new start time (end time will be automatically set to 2 hours later)")
            .setPositiveButton("Select Date & Time") { _, _ ->
                showDateTimePickerForModification()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDateTimePickerForModification() {
        val booking = this.booking ?: return
        
        // Show date picker first
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = booking.startTime
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                newCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                newCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                
                // Show time picker after date selection
                showTimePickerForModification(newCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set maximum date to 7 days from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.DAY_OF_MONTH, 7)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }
    
    private fun showTimePickerForModification(selectedDate: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.timeInMillis = selectedDate
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                
                val newStartTime = newCalendar.timeInMillis
                val newEndTime = newStartTime + (2 * 60 * 60 * 1000L) // 2 hours later
                
                // Update the booking
                updateBookingTime(newStartTime, newEndTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun updateBookingTime(newStartTime: Long, newEndTime: Long) {
        val booking = this.booking ?: return
        
        loadingView.show()
        loadingView.setMessage("Updating booking time...")
        
        val updateRequest = BookingUpdateRequest(
            startTime = newStartTime
        )
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = bookingRepository.updateBooking(booking.id, updateRequest)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        val updatedBooking = result.getDataOrNull()
                        if (updatedBooking != null) {
                            this@BookingDetailActivity.booking = updatedBooking
                            updateUI()
                            Toasts.showSuccess(this@BookingDetailActivity, "Booking time updated successfully")
                        } else {
                            Toasts.showError(this@BookingDetailActivity, "Failed to update booking")
                        }
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingDetailActivity, error?.message ?: "Failed to update booking")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@BookingDetailActivity, "Failed to update booking: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    private fun showStationModificationDialog() {
        val booking = this.booking ?: return
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Change Station")
            .setMessage("Station modification is not supported by the backend. Please cancel this booking and create a new one with a different station.")
            .setPositiveButton("OK") { _, _ ->
                // Do nothing - just close the dialog
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    
    private fun showCancelConfirmation() {
        val booking = this.booking ?: return
        
        // Check if booking can be cancelled (12-hour rule)
        if (!canModifyBooking(booking)) {
            Toasts.showWarning(this, "Bookings cannot be cancelled within 12 hours of start time")
            return
        }
        
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
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.cancelBooking(bookingId)
                
                if (result.isSuccess()) {
                    Toasts.showSuccess(this@BookingDetailActivity, "Booking cancelled successfully")
                    finish()
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@BookingDetailActivity, error?.message ?: "Failed to cancel booking")
                }
            } catch (e: Exception) {
                Toasts.showError(this@BookingDetailActivity, "Failed to cancel booking: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
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
