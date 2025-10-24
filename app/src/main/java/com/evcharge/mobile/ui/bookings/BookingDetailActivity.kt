package com.evcharge.mobile.ui.bookings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        
        // Display station name and ID
        val stationDisplayName = if (!booking.stationName.isNullOrEmpty()) {
            "${booking.stationName} (ID: ${booking.stationId.take(8)}...)"
        } else {
            "Station ID: ${booking.stationId.take(8)}..."
        }
        tvStationName.text = stationDisplayName
        
        // Fetch station details for address
        fetchStationDetails(booking.stationId)
        
        tvStartTime.text = Datex.formatToDisplay(booking.startTime)
        tvEndTime.text = Datex.formatToDisplay(booking.endTime)
        tvDuration.text = formatDuration(booking.startTime, booking.endTime)
        tvStatus.text = booking.status.name
        tvCreatedAt.text = Datex.formatToDisplay(booking.createdAt)
        
        // Set status color and background
        val (textColor, backgroundColor) = when (booking.status) {
            BookingStatus.PENDING -> Pair(R.color.status_pending, R.color.status_pending_bg)
            BookingStatus.APPROVED -> Pair(R.color.status_approved, R.color.status_approved_bg)
            BookingStatus.COMPLETED -> Pair(R.color.status_completed, R.color.status_completed_bg)
            BookingStatus.CANCELLED -> Pair(R.color.status_cancelled, R.color.status_cancelled_bg)
        }
        tvStatus.setTextColor(getColor(textColor))
        tvStatus.setBackgroundColor(getColor(backgroundColor))
        
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
                        } else {
                            tvAddress.text = "Address not available"
                        }
                    } else {
                        tvAddress.text = "Address not available"
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    tvAddress.text = "Address not available"
                }
            }
        }
    }
    
    private fun updateButtonVisibility() {
        val booking = this.booking ?: return
        
        when (booking.status) {
            BookingStatus.PENDING -> {
                btnModify.visibility = android.view.View.VISIBLE
                btnCancel.visibility = android.view.View.VISIBLE
                btnShowQr.visibility = android.view.View.GONE
            }
            BookingStatus.APPROVED -> {
                btnModify.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.VISIBLE
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
    
    private fun showModifyOptions() {
        val booking = this.booking ?: return
        
        // Check if booking can be modified (12-hour rule)
        val twelveHoursFromNow = System.currentTimeMillis() + (12 * 60 * 60 * 1000)
        if (booking.startTime <= twelveHoursFromNow) {
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
                showStationChangeDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
    
    private fun showTimeModificationDialog() {
        val booking = this.booking ?: return
        
        // Create a custom dialog for time selection
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_modification, null)
        val datePicker = dialogView.findViewById<android.widget.DatePicker>(R.id.date_picker)
        val timePicker = dialogView.findViewById<android.widget.TimePicker>(R.id.time_picker)
        
        // Set current booking time as default
        val currentDate = java.util.Calendar.getInstance()
        currentDate.timeInMillis = booking.startTime
        
        datePicker.init(
            currentDate.get(java.util.Calendar.YEAR),
            currentDate.get(java.util.Calendar.MONTH),
            currentDate.get(java.util.Calendar.DAY_OF_MONTH),
            null
        )
        
        timePicker.hour = currentDate.get(java.util.Calendar.HOUR_OF_DAY)
        timePicker.minute = currentDate.get(java.util.Calendar.MINUTE)
        timePicker.setIs24HourView(true)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Modify Booking Time")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val selectedDate = java.util.Calendar.getInstance()
                selectedDate.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                selectedDate.set(java.util.Calendar.HOUR_OF_DAY, timePicker.hour)
                selectedDate.set(java.util.Calendar.MINUTE, timePicker.minute)
                selectedDate.set(java.util.Calendar.SECOND, 0)
                selectedDate.set(java.util.Calendar.MILLISECOND, 0)
                
                val newStartTime = selectedDate.timeInMillis
                val duration = booking.endTime - booking.startTime
                val newEndTime = newStartTime + duration
                
                updateBookingTime(newStartTime, newEndTime)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showStationChangeDialog() {
        val booking = this.booking ?: return
        
        // Load available stations for selection
        loadingView.show()
        loadingView.setMessage("Loading available stations...")
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    stationRepository.getAllStations()
                }
                
                if (result.isSuccess()) {
                    val stations = result.getDataOrNull() ?: emptyList()
                    val availableStations = stations.filter { it.status == com.evcharge.mobile.data.dto.StationStatus.AVAILABLE }
                    
                    withContext(Dispatchers.Main) {
                        loadingView.hide()
                        showStationSelectionDialog(availableStations)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingView.hide()
                        Toasts.showError(this@BookingDetailActivity, "Failed to load stations")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    Toasts.showError(this@BookingDetailActivity, "Error loading stations: ${e.message}")
                }
            }
        }
    }
    
    private fun showStationSelectionDialog(stations: List<com.evcharge.mobile.data.dto.Station>) {
        if (stations.isEmpty()) {
            Toasts.showWarning(this, "No available stations found")
            return
        }
        
        val stationNames = stations.map { "${it.name} - ${it.address}" }.toTypedArray()
        var selectedIndex = 0
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select New Station")
            .setSingleChoiceItems(stationNames, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Update") { _, _ ->
                val selectedStation = stations[selectedIndex]
                updateBookingStation(selectedStation.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateBookingTime(newStartTime: Long, newEndTime: Long) {
        val booking = this.booking ?: return
        
        loadingView.show()
        loadingView.setMessage("Updating booking time...")
        
        lifecycleScope.launch {
            try {
                val updateRequest = com.evcharge.mobile.data.dto.BookingUpdateRequest(
                    startTime = newStartTime,
                    endTime = newEndTime
                )
                
                val result = withContext(Dispatchers.IO) {
                    bookingRepository.updateBooking(booking.id, updateRequest)
                }
                
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    
                    if (result.isSuccess()) {
                        Toasts.showSuccess(this@BookingDetailActivity, "Booking time updated successfully")
                        loadBooking() // Refresh the booking details
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingDetailActivity, error?.message ?: "Failed to update booking time")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    Toasts.showError(this@BookingDetailActivity, "Error updating booking: ${e.message}")
                }
            }
        }
    }
    
    private fun updateBookingStation(newStationId: String) {
        val booking = this.booking ?: return
        
        loadingView.show()
        loadingView.setMessage("Updating booking station...")
        
        lifecycleScope.launch {
            try {
                val updateRequest = com.evcharge.mobile.data.dto.BookingUpdateRequest(
                    stationId = newStationId
                )
                
                val result = withContext(Dispatchers.IO) {
                    bookingRepository.updateBooking(booking.id, updateRequest)
                }
                
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    
                    if (result.isSuccess()) {
                        Toasts.showSuccess(this@BookingDetailActivity, "Booking station updated successfully")
                        loadBooking() // Refresh the booking details
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingDetailActivity, error?.message ?: "Failed to update booking station")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    Toasts.showError(this@BookingDetailActivity, "Error updating booking: ${e.message}")
                }
            }
        }
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
