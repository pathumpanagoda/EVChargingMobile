package com.evcharge.mobile.ui.bookings

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evcharge.mobile.BuildConfig
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Datex
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.common.Validators
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingUpdateRequest
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.dto.TimeSlot
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.adapters.TimeSlotAdapter
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

/**
 * Modify booking activity for updating existing bookings
 * Uses the same UI as BookingFormActivity for consistency
 */
class ModifyBookingActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    
    // UI Components
    private lateinit var etStation: AutoCompleteTextView
    private lateinit var etReservationDate: TextInputEditText
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var progressTimeSlots: ProgressBar
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var loadingView: LoadingView
    
    // Debug components
    private lateinit var debugInfo: View
    private lateinit var tvDebugSelectedSlot: TextView
    private lateinit var tvDebugAvailableSlots: TextView
    private lateinit var tvDebugStation: TextView
    private lateinit var tvDebugDate: TextView
    
    // Data
    private var booking: Booking? = null
    private var stations: List<Station> = emptyList()
    private var selectedStationId: String = ""
    private var selectedDate: String = ""
    private var selectedTimeSlot: TimeSlot? = null
    private var timeSlotAdapter: TimeSlotAdapter? = null
    private var availableTimeSlots: List<TimeSlot> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_form)
        
        try {
            initializeComponents()
            setupUI()
            loadBookingData()
        } catch (e: Exception) {
            Toasts.showError(this, "Activity setup failed: ${e.message}")
            finish()
        }
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val bookingApi = BookingApi(apiClient)
        val stationApi = StationApi(apiClient)
        bookingRepository = BookingRepository(bookingApi)
        stationRepository = StationRepository(stationApi)
        
        // Initialize UI components
        etStation = findViewById(R.id.et_station)
        etReservationDate = findViewById(R.id.et_reservation_date)
        rvTimeSlots = findViewById(R.id.rv_time_slots)
        progressTimeSlots = findViewById(R.id.progress_time_slots)
        btnUpdate = findViewById(R.id.btn_create)
        btnCancel = findViewById(R.id.btn_cancel)
        loadingView = findViewById(R.id.loading_view)
        
        // Debug components
        debugInfo = findViewById(R.id.debug_info)
        tvDebugSelectedSlot = findViewById(R.id.tv_debug_selected_slot)
        tvDebugAvailableSlots = findViewById(R.id.tv_debug_available_slots)
        tvDebugStation = findViewById(R.id.tv_debug_station)
        tvDebugDate = findViewById(R.id.tv_debug_date)
        
        // Change button text for modify
        btnUpdate.text = "Update Booking"
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Modify Booking"
        
        // Set up click listeners
        setupClickListeners()
        setupTimeSlotsRecyclerView()
    }
    
    private fun setupClickListeners() {
        try {
            // Date picker
            etReservationDate.setOnClickListener {
                showDatePicker()
            }
            
            // Update button
            btnUpdate.setOnClickListener {
                updateBooking()
            }
            
            // Cancel button
            btnCancel.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Click listeners setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun loadBookingData() {
        val bookingId = intent.getStringExtra("booking_id")
        if (bookingId.isNullOrEmpty()) {
            Toasts.showError(this, "Invalid booking ID")
            finish()
            return
        }
        
        loadingView.show()
        loadingView.setMessage("Loading booking details...")
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Load booking details
                val bookingResult = bookingRepository.getBooking(bookingId)
                val stationsResult = stationRepository.getAllStations()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (bookingResult.isSuccess() && stationsResult.isSuccess()) {
                        booking = bookingResult.getDataOrNull()
                        val allStations = stationsResult.getDataOrNull() ?: emptyList()
                        
                        // Filter out deactivated stations
                        stations = allStations.filter { station ->
                            station.status != com.evcharge.mobile.data.dto.StationStatus.OFFLINE
                        }
                        
                        if (booking != null && stations.isNotEmpty()) {
                            populateFormWithBookingData()
                            setupStationSpinner()
                            Toasts.showInfo(this@ModifyBookingActivity, "Booking loaded successfully")
                        } else {
                            Toasts.showError(this@ModifyBookingActivity, "Failed to load booking or stations")
                            finish()
                        }
                    } else {
                        val error = bookingResult.getErrorOrNull() ?: stationsResult.getErrorOrNull()
                        Toasts.showError(this@ModifyBookingActivity, error?.message ?: "Failed to load data")
                        finish()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@ModifyBookingActivity, "Failed to load booking: ${e.message}")
                    finish()
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    private fun populateFormWithBookingData() {
        val booking = this.booking ?: return
        
        // Set station
        selectedStationId = booking.stationId
        val station = stations.find { it.id == booking.stationId }
        if (station != null) {
            etStation.setText(station.name, false)
        }
        
        // Set date
        val startDate = Date(booking.startTime)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate)
        etReservationDate.setText(dateFormat.format(startDate))
        
        // Set time slot
        val startTime = Date(booking.startTime)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(startTime)
        
        // Load time slots for the selected date
        loadTimeSlots(selectedStationId, selectedDate)
        
        updateDebugInfo()
    }
    
    private fun setupStationSpinner() {
        // Use custom adapter for better station display
        val adapter = StationAdapter(this, stations)
        etStation.setAdapter(adapter)
        
        // Enable filtering/searching
        etStation.threshold = 1
        
        // Show the dropdown on click
        etStation.setOnClickListener { 
            etStation.showDropDown() 
        }
        
        // Handle text input for filtering
        etStation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etStation.showDropDown()
            }
        }
        
        // Handle station selection
        etStation.setOnItemClickListener { _, _, position, _ ->
            try {
                if (position < stations.size) {
                    selectedStationId = stations[position].id
                    val selectedStation = stations[position]
                    
                    // Set only the station name in the text field
                    etStation.setText(selectedStation.name, false)
                    
                    // Load time slots for the new station
                    if (selectedDate.isNotEmpty()) {
                        loadTimeSlots(selectedStationId, selectedDate)
                    }
                    
                    updateDebugInfo()
                }
            } catch (e: Exception) {
                Toasts.showError(this, "Station selection failed: ${e.message}")
            }
        }
    }
    
    private fun setupTimeSlotsRecyclerView() {
        try {
            // Set up grid layout manager (3 columns)
            val layoutManager = GridLayoutManager(this, 3)
            rvTimeSlots.layoutManager = layoutManager
            
            // Initialize adapter
            timeSlotAdapter = TimeSlotAdapter(
                timeSlots = availableTimeSlots,
                onSlotSelected = { timeSlot ->
                    selectedTimeSlot = timeSlot
                    updateDebugInfo()
                    Toasts.showInfo(this, "Selected time slot: ${timeSlot.time}")
                }
            )
            rvTimeSlots.adapter = timeSlotAdapter
        } catch (e: Exception) {
            Toasts.showError(this, "Time slots setup failed: ${e.message}")
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Set initial date to current booking date
        booking?.let {
            calendar.timeInMillis = it.startTime
        }
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                selectedDate = dateString.format(selectedCalendar.time)
                etReservationDate.setText(dateFormat.format(selectedCalendar.time))
                
                // Load time slots for the selected date
                if (selectedStationId.isNotEmpty()) {
                    loadTimeSlots(selectedStationId, selectedDate)
                }
                updateDebugInfo()
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
        
        // For testing purposes, allow future dates (remove this in production)
        if (BuildConfig.DEBUG) {
            val futureDate = Calendar.getInstance()
            futureDate.add(Calendar.YEAR, 1) // Allow dates up to 1 year in the future for testing
            datePickerDialog.datePicker.maxDate = futureDate.timeInMillis
        }
        
        datePickerDialog.show()
    }
    
    private fun loadTimeSlots(stationId: String, date: String) {
        progressTimeSlots.visibility = View.VISIBLE
        rvTimeSlots.visibility = View.GONE
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Get real availability data from API
                val availabilityResult = bookingRepository.getAvailableSlots(stationId, date)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (availabilityResult.isSuccess()) {
                        val timeSlots = availabilityResult.getDataOrNull() ?: emptyList()
                        availableTimeSlots = timeSlots
                        timeSlotAdapter?.updateTimeSlots(timeSlots)
                        progressTimeSlots.visibility = View.GONE
                        rvTimeSlots.visibility = View.VISIBLE
                        updateDebugInfo()
                        
                        android.util.Log.d("ModifyBooking", "Loaded ${timeSlots.size} time slots from API")
                        Toasts.showInfo(this@ModifyBookingActivity, "Loaded ${timeSlots.size} time slots")
                    } else {
                        val error = availabilityResult.getErrorOrNull()
                        android.util.Log.e("ModifyBooking", "Failed to load time slots: ${error?.message}")
                        Toasts.showError(this@ModifyBookingActivity, "Failed to load time slots: ${error?.message}")
                        
                        // Fallback to empty list
                        availableTimeSlots = emptyList()
                        timeSlotAdapter?.updateTimeSlots(emptyList())
                        progressTimeSlots.visibility = View.GONE
                        rvTimeSlots.visibility = View.VISIBLE
                        updateDebugInfo()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ModifyBooking", "Exception loading time slots", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    progressTimeSlots.visibility = View.GONE
                    rvTimeSlots.visibility = View.VISIBLE
                    Toasts.showError(this@ModifyBookingActivity, "Failed to load time slots: ${e.message}")
                    
                    // Fallback to empty list
                    availableTimeSlots = emptyList()
                    timeSlotAdapter?.updateTimeSlots(emptyList())
                    updateDebugInfo()
                }
            }
        }
    }
    
    private fun updateBooking() {
        // Validate input
        if (selectedStationId.isEmpty()) {
            Toasts.showValidationError(this, "Please select a station")
            return
        }
        
        if (selectedDate.isEmpty()) {
            Toasts.showValidationError(this, "Please select a reservation date")
            return
        }
        
        if (selectedTimeSlot == null) {
            Toasts.showValidationError(this, "Please select a time slot")
            return
        }
        
        // Validate booking rules
        try {
            // Parse the date and time, treating it as local time
            val dateTimeString = "$selectedDate ${selectedTimeSlot!!.time}"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getDefault() // Use local timezone
            
            val selectedDateTime = dateFormat.parse(dateTimeString)
            if (selectedDateTime == null) {
                Toasts.showValidationError(this, "Invalid date/time selection")
                return
            }
            
            val currentTime = System.currentTimeMillis()
            val timeDifference = selectedDateTime.time - currentTime
            val hoursDifference = timeDifference / (1000 * 60 * 60)
            
            // Check 12-hour advance rule
            if (hoursDifference < 12) {
                Toasts.showValidationError(this, "Bookings must be made at least 12 hours in advance. Selected time is ${hoursDifference} hours from now.")
                return
            }
            
            // Check 7-day limit
            val sevenDaysFromNow = currentTime + (7 * 24 * 60 * 60 * 1000)
            if (selectedDateTime.time > sevenDaysFromNow) {
                Toasts.showValidationError(this, "Reservations can only be made up to 7 days in advance")
                return
            }
            
            // Proceed with booking update
            submitBookingUpdate()
            
        } catch (e: Exception) {
            Toasts.showError(this, "Date parsing failed: ${e.message}")
        }
    }
    
    private fun submitBookingUpdate() {
        loadingView.show()
        loadingView.setMessage("Updating booking...")
        
        // Create start and end times for 1-hour booking
        // Parse the date and time as if they were in UTC (matching web app behavior)
        val dateTimeString = "$selectedDate ${selectedTimeSlot!!.time}"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Parse as UTC, not local time
        
        val selectedDateTime = dateFormat.parse(dateTimeString)
        val startTime = selectedDateTime!!.time
        val endTime = startTime + (60 * 60 * 1000) // 1 hour later
        
        val request = BookingUpdateRequest(
            startTime = startTime
        )
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = bookingRepository.updateBooking(booking!!.id, request)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        val updatedBooking = result.getDataOrNull()
                        if (updatedBooking != null) {
                            Toasts.showSuccess(this@ModifyBookingActivity, "Booking updated successfully")
                            
                            // Return to booking detail with updated data
                            val intent = Intent().apply {
                                putExtra("updated_booking", updatedBooking)
                            }
                            setResult(RESULT_OK, intent)
                            finish()
                        } else {
                            Toasts.showError(this@ModifyBookingActivity, "Failed to update booking")
                        }
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@ModifyBookingActivity, error?.message ?: "Failed to update booking")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@ModifyBookingActivity, "Update booking failed: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    private fun updateDebugInfo() {
        try {
            tvDebugSelectedSlot.text = "Selected time slot: ${selectedTimeSlot?.time ?: "None"}"
            tvDebugAvailableSlots.text = "Available slots: ${availableTimeSlots.size}"
            tvDebugStation.text = "Station: $selectedStationId"
            tvDebugDate.text = "Date: $selectedDate"
        } catch (e: Exception) {
            android.util.Log.e("ModifyBooking", "Debug info update failed", e)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_booking_form, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
