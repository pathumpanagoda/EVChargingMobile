package com.evcharge.mobile.ui.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingUpdateRequest
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import android.widget.Spinner
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for modifying an existing booking
 */
class BookingModifyActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    private lateinit var booking: Booking
    private var stations: List<Station> = emptyList()
    
    // UI Components
    private lateinit var spinnerStation: Spinner
    private lateinit var tilStartDate: TextInputLayout
    private lateinit var etStartDate: TextInputEditText
    private lateinit var tilStartTime: TextInputLayout
    private lateinit var etStartTime: TextInputEditText
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var loadingView: LoadingView
    
    // Date/Time handling
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_modify)
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadBookingData()
        loadStations()
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val bookingApi = BookingApi(apiClient)
        val stationApi = StationApi(apiClient)
        bookingRepository = BookingRepository(bookingApi)
        stationRepository = StationRepository(stationApi)
        
        // Get booking from intent
        booking = intent.getSerializableExtra("booking") as Booking
        
        // Initialize UI components
        spinnerStation = findViewById(R.id.spinner_station)
        tilStartDate = findViewById(R.id.til_start_date)
        etStartDate = findViewById(R.id.et_start_date)
        tilStartTime = findViewById(R.id.til_start_time)
        etStartTime = findViewById(R.id.et_start_time)
        btnUpdate = findViewById(R.id.btn_update)
        btnCancel = findViewById(R.id.btn_cancel)
        loadingView = findViewById(R.id.loading_view)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Modify Booking"
        
        // Check if booking can be modified
        if (booking.status.name != "PENDING") {
            Toasts.showError(this, "Only pending bookings can be modified")
            finish()
            return
        }
        
        // Set current booking details
        etStartDate.setText(dateFormat.format(Date(booking.startTime)))
        etStartTime.setText(timeFormat.format(Date(booking.startTime)))
        
        // Initialize date/time
        selectedDate.timeInMillis = booking.startTime
        selectedTime.timeInMillis = booking.startTime
    }
    
    private fun setupClickListeners() {
        // Date picker
        etStartDate.setOnClickListener {
            showDatePicker()
        }
        
        // Time picker
        etStartTime.setOnClickListener {
            showTimePicker()
        }
        
        // Update button
        btnUpdate.setOnClickListener {
            updateBooking()
        }
        
        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun loadBookingData() {
        // Pre-populate with current booking data
        etStartDate.setText(dateFormat.format(Date(booking.startTime)))
        etStartTime.setText(timeFormat.format(Date(booking.startTime)))
    }
    
    private fun loadStations() {
        loadingView.show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = stationRepository.getAllStations()
                
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    
                    if (result.isSuccess()) {
                        stations = result.getDataOrNull() ?: emptyList()
                        setupStationSpinner()
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingModifyActivity, "Failed to load stations: ${error?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    Toasts.showError(this@BookingModifyActivity, "Error loading stations: ${e.message}")
                }
            }
        }
    }
    
    private fun setupStationSpinner() {
        val stationNames = stations.map { "${it.name} - ${it.address}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stationNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStation.adapter = adapter
        
        // Select current station
        val currentStationIndex = stations.indexOfFirst { it.id == booking.stationId }
        if (currentStationIndex >= 0) {
            spinnerStation.setSelection(currentStationIndex)
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                etStartDate.setText(dateFormat.format(selectedDate.time))
                validateDateTimeSelection()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set minimum date to 12 hours from now (to allow 12-hour rule)
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.HOUR_OF_DAY, 12)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        
        // Set maximum date to 7 days from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.DAY_OF_MONTH, 7)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        
        datePickerDialog.show()
    }
    
    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                etStartTime.setText(timeFormat.format(selectedTime.time))
                validateDateTimeSelection()
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            true
        )
        
        timePickerDialog.show()
    }
    
    private fun validateDateTimeSelection() {
        val now = Calendar.getInstance()
        val selectedDateTime = Calendar.getInstance().apply {
            set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Check if time is in the future
        if (selectedDateTime.timeInMillis <= now.timeInMillis) {
            tilStartTime.error = "Please select a future time"
            btnUpdate.isEnabled = false
            return
        }
        
        // Check 12-hour rule (cannot modify within 12 hours of start time)
        val twelveHoursFromStartTime = booking.startTime - (12 * 60 * 60 * 1000)
        val currentTime = System.currentTimeMillis()
        
        if (currentTime >= twelveHoursFromStartTime) {
            tilStartTime.error = "Cannot modify booking within 12 hours of reservation time"
            btnUpdate.isEnabled = false
            return
        }
        
        // Check 7-day rule
        val sevenDaysFromNow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 7)
        }
        
        if (selectedDateTime.timeInMillis > sevenDaysFromNow.timeInMillis) {
            tilStartDate.error = "Reservation must be within 7 days"
            btnUpdate.isEnabled = false
            return
        }
        
        // Clear errors
        tilStartDate.error = null
        tilStartTime.error = null
        btnUpdate.isEnabled = true
    }
    
    private fun updateBooking() {
        if (!validateInput()) return
        
        loadingView.show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create combined date/time
                val selectedDateTime = Calendar.getInstance().apply {
                    set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val reservationDateTime = iso8601Format.format(selectedDateTime.time)
                
                val request = BookingUpdateRequest(
                    reservationDateTime = reservationDateTime
                )
                
                val result = bookingRepository.updateBooking(booking.id, request)
                
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    
                    if (result.isSuccess()) {
                        Toasts.showSuccess(this@BookingModifyActivity, "Booking updated successfully")
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val error = result.getErrorOrNull()
                        Toasts.showError(this@BookingModifyActivity, "Failed to update booking: ${error?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingView.hide()
                    Toasts.showError(this@BookingModifyActivity, "Error updating booking: ${e.message}")
                }
            }
        }
    }
    
    private fun validateInput(): Boolean {
        var isValid = true
        
        if (etStartDate.text.toString().isEmpty()) {
            tilStartDate.error = "Please select a date"
            isValid = false
        }
        
        if (etStartTime.text.toString().isEmpty()) {
            tilStartTime.error = "Please select a time"
            isValid = false
        }
        
        return isValid
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
