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
import androidx.appcompat.app.AlertDialog
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
import com.evcharge.mobile.data.dto.BookingCreateRequest
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
 * Booking form activity for creating new bookings
 */
class BookingFormActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    
    // UI Components
    private lateinit var etStation: AutoCompleteTextView
    private lateinit var etReservationDate: TextInputEditText
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var progressTimeSlots: ProgressBar
    private lateinit var btnCreate: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var loadingView: LoadingView
    
    // Debug components
    private lateinit var debugInfo: View
    private lateinit var tvDebugSelectedSlot: TextView
    private lateinit var tvDebugAvailableSlots: TextView
    private lateinit var tvDebugStation: TextView
    private lateinit var tvDebugDate: TextView
    
    // Data
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
            setupClickListeners()
            loadStations()
        } catch (e: Exception) {
            Toasts.showError(this, "Booking form initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun initializeComponents() {
        try {
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
            btnCreate = findViewById(R.id.btn_create)
            btnCancel = findViewById(R.id.btn_cancel)
            loadingView = findViewById(R.id.loading_view)
            
            // Debug components
            debugInfo = findViewById(R.id.debug_info)
            tvDebugSelectedSlot = findViewById(R.id.tv_debug_selected_slot)
            tvDebugAvailableSlots = findViewById(R.id.tv_debug_available_slots)
            tvDebugStation = findViewById(R.id.tv_debug_station)
            tvDebugDate = findViewById(R.id.tv_debug_date)
            
            // Show debug info in development builds
            if (BuildConfig.DEBUG) {
                debugInfo.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Component initialization failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupUI() {
        try {
            // Set up toolbar with error handling for missing toolbar
            try {
                setSupportActionBar(findViewById(R.id.toolbar))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Create New Booking"
            } catch (e: Exception) {
                // Handle missing toolbar gracefully
                Toasts.showWarning(this, "Toolbar setup skipped: ${e.message}")
            }
            
            // Set up time slots RecyclerView
            setupTimeSlotsRecyclerView()
            
            // Set default date to today
            val calendar = Calendar.getInstance()
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            etReservationDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time))
            
            updateDebugInfo()
        } catch (e: Exception) {
            Toasts.showError(this, "UI setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupClickListeners() {
        try {
            // Station selection is handled in setupStationSpinner()
            
            // Date picker
            etReservationDate.setOnClickListener { 
                try {
                    showDatePicker()
                } catch (e: Exception) {
                    Toasts.showError(this, "Date picker failed: ${e.message}")
                }
            }
            
            // Cancel button
            btnCancel.setOnClickListener {
                finish()
            }
            
            // Create button
            btnCreate.setOnClickListener {
                try {
                    createBooking()
                } catch (e: Exception) {
                    Toasts.showError(this, "Create booking failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Click listeners setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun loadStations() {
        loadingView.show()
        loadingView.setMessage("Loading stations...")
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Changed to getAllStations() to show all stations regardless of status
                val result = stationRepository.getAllStations()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        stations = result.getDataOrNull() ?: emptyList()
                        if (stations.isEmpty()) {
                            Toasts.showWarning(this@BookingFormActivity, "No stations available")
                        } else {
                            setupStationSpinner()
                            Toasts.showInfo(this@BookingFormActivity, "Loaded ${stations.size} stations")
                        }
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingFormActivity, error?.message ?: "Failed to load stations")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@BookingFormActivity, "Failed to load stations: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
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
        
        // Override toString to display station name only
        etStation.setOnItemClickListener { _, _, position, _ ->
            try {
                if (position < stations.size) {
                    selectedStationId = stations[position].id
                    val selectedStation = stations[position]
                    
                    // Set only the station name in the text field
                    etStation.setText(selectedStation.name, false)
                    
                    // Show station details
                    val statusText = when (selectedStation.status) {
                        com.evcharge.mobile.data.dto.StationStatus.AVAILABLE -> "Available"
                        com.evcharge.mobile.data.dto.StationStatus.OCCUPIED -> "Occupied"
                        com.evcharge.mobile.data.dto.StationStatus.MAINTENANCE -> "Under Maintenance"
                        com.evcharge.mobile.data.dto.StationStatus.OFFLINE -> "Offline"
                    }
                    
                    Toasts.showInfo(this, "Selected: ${selectedStation.name} ($statusText)")
                    
                    // Load time slots for the selected station and date
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
    
    private fun updateDebugInfo() {
        try {
            if (BuildConfig.DEBUG) {
                tvDebugSelectedSlot.text = "Selected time slot: ${selectedTimeSlot?.time ?: "None"}"
                tvDebugAvailableSlots.text = "Available slots: ${availableTimeSlots.size}"
                tvDebugStation.text = "Station: ${selectedStationId.ifEmpty { "None" }}"
                tvDebugDate.text = "Date: $selectedDate"
                
                // Additional debug info for date validation
                if (selectedDate.isNotEmpty() && selectedTimeSlot != null) {
                    try {
                        val selectedDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$selectedDate ${selectedTimeSlot!!.time}")
                        if (selectedDateTime != null) {
                            val now = System.currentTimeMillis()
                            val twelveHoursFromNow = now + (12 * 60 * 60 * 1000)
                            val isValid = selectedDateTime.time >= twelveHoursFromNow
                            
                            val debugDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(selectedDateTime)
                            val debugNow = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now))
                            
                            tvDebugDate.text = "Date: $selectedDate\nParsed: $debugDate\nNow: $debugNow\nValid: $isValid"
                        }
                    } catch (e: Exception) {
                        tvDebugDate.text = "Date: $selectedDate\nParse Error: ${e.message}"
                    }
                }
            }
        } catch (e: Exception) {
            // Debug info update failed, but don't crash the app
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newCalendar.time)
                etReservationDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(newCalendar.time))
                
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
                // Generate time slots (6 AM to 10 PM, 1-hour slots)
                val timeSlots = mutableListOf<TimeSlot>()
                for (hour in 6..22) {
                    val timeString = String.format("%02d:00", hour)
                    timeSlots.add(
                        TimeSlot(
                            time = timeString,
                            hour = hour,
                            available = true, // Default to available
                            approvedBookings = 0,
                            pendingBookings = 0,
                            totalSlots = 4 // Default assumption
                        )
                    )
                }
                
                // TODO: Replace with actual API call to get availability
                // val availabilityResult = bookingRepository.getAvailableSlots(stationId, date)
                // if (availabilityResult.isSuccess()) {
                //     val availabilityData = availabilityResult.getDataOrNull()
                //     // Process availability data and update timeSlots
                // }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    availableTimeSlots = timeSlots
                    timeSlotAdapter?.updateTimeSlots(availableTimeSlots)
                    progressTimeSlots.visibility = View.GONE
                    rvTimeSlots.visibility = View.VISIBLE
                    updateDebugInfo()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    progressTimeSlots.visibility = View.GONE
                    rvTimeSlots.visibility = View.VISIBLE
                    Toasts.showError(this@BookingFormActivity, "Failed to load time slots: ${e.message}")
                }
            }
        }
    }
    
    private fun createBooking() {
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
            
            val selectedTimeMillis = selectedDateTime.time
            val now = System.currentTimeMillis()
            val twelveHoursFromNow = now + (12 * 60 * 60 * 1000)
            
            // Debug logging
            val debugDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(selectedDateTime)
            val debugNow = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now))
            val debugTwelveHours = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(twelveHoursFromNow))
            
            if (BuildConfig.DEBUG) {
                Toasts.showInfo(this, "Debug: Selected: $debugDate, Now: $debugNow, 12h from now: $debugTwelveHours")
            }
            
            if (selectedTimeMillis < twelveHoursFromNow) {
                val hoursDifference = (selectedTimeMillis - now) / (1000 * 60 * 60)
                Toasts.showValidationError(this, "Bookings must be made at least 12 hours in advance. Selected time is only ${hoursDifference}h from now.")
                return
            }
        } catch (e: Exception) {
            Toasts.showValidationError(this, "Invalid date/time format: ${e.message}")
            return
        }
        
        // Create booking
        submitBooking()
    }
    
    
    private fun submitBooking() {
        loadingView.show()
        loadingView.setMessage("Creating booking...")
        
        // Create start and end times for 1-hour booking
        // Parse the date and time as if they were in UTC (matching web app behavior)
        val dateTimeString = "$selectedDate ${selectedTimeSlot!!.time}"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Parse as UTC, not local time
        
        val selectedDateTime = dateFormat.parse(dateTimeString)
        val startTime = selectedDateTime!!.time
        val endTime = startTime + (60 * 60 * 1000) // 1 hour later
        
        val request = BookingCreateRequest(selectedStationId, startTime, endTime)
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = bookingRepository.createBooking(request)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        val booking = result.getDataOrNull()
                        if (booking != null) {
                            Toasts.showSuccess(this@BookingFormActivity, "Booking created successfully")
                            
                            // Open booking detail
                            val intent = Intent(this@BookingFormActivity, BookingDetailActivity::class.java)
                            intent.putExtra("booking_id", booking.id)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingFormActivity, error?.message ?: "Failed to create booking")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@BookingFormActivity, "Failed to create booking: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
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
