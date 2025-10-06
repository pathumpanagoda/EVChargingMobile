package com.evcharge.mobile.ui.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.evcharge.mobile.data.dto.BookingCreateRequest
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.common.TimeExt
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import android.widget.Spinner
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Booking form activity for creating new bookings
 */
class BookingFormActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    
    // UI Components
    private lateinit var spinnerStation: Spinner
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var btnCreate: MaterialButton
    private lateinit var loadingView: LoadingView
    
    // Data
    private var stations: List<Station> = emptyList()
    private var selectedStationId: String = ""
    private var startDateTime: Long = 0
    private var endDateTime: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_form)
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadStations()
    }
    
    private fun initializeComponents() {
        prefs = Prefs.instance()
        bookingRepository = BookingRepository()
        stationRepository = StationRepository()
        
        // Initialize UI components
        spinnerStation = findViewById(R.id.spinner_station)
        etStartDate = findViewById(R.id.et_start_date)
        etStartTime = findViewById(R.id.et_start_time)
        etEndDate = findViewById(R.id.et_end_date)
        etEndTime = findViewById(R.id.et_end_time)
        btnCreate = findViewById(R.id.btn_create)
        loadingView = findViewById(R.id.loading_view)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "New Booking"
        
        // Set default times (1 hour from now)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 1)
        startDateTime = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 2)
        endDateTime = calendar.timeInMillis
        
        updateDateTimeFields()
    }
    
    private fun setupClickListeners() {
        // Station selection
        spinnerStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position <= stations.size) {
                    selectedStationId = stations[position - 1].id
                } else {
                    selectedStationId = ""
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStationId = ""
            }
        }
        
        // Date and time pickers
        etStartDate.setOnClickListener { showStartDatePicker() }
        etStartTime.setOnClickListener { showStartTimePicker() }
        etEndDate.setOnClickListener { showEndDatePicker() }
        etEndTime.setOnClickListener { showEndTimePicker() }
        
        // Create button
        btnCreate.setOnClickListener {
            createBooking()
        }
    }
    
    private fun loadStations() {
        loadingView.show()
        loadingView.setMessage("Loading stations...")
        
        lifecycleScope.launch {
            try {
                val result = stationRepository.getAvailableStations()
                
                if (result.isSuccess()) {
                    stations = emptyList() // TODO: Fix when backend returns proper Station objects
                    setupStationSpinner()
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@BookingFormActivity, error?.message ?: "Failed to load stations")
                }
            } catch (e: Exception) {
                Toasts.showError(this@BookingFormActivity, "Failed to load stations: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
    }
    
    private fun setupStationSpinner() {
        val stationNames = mutableListOf("Select a station")
        stationNames.addAll(stations.map { it.name })
        
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stationNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStation.adapter = adapter
    }
    
    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDateTime
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                newCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                newCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                startDateTime = newCalendar.timeInMillis
                updateDateTimeFields()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showStartTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDateTime
        
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.timeInMillis = startDateTime
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                startDateTime = newCalendar.timeInMillis
                updateDateTimeFields()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endDateTime
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                newCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                newCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                endDateTime = newCalendar.timeInMillis
                updateDateTimeFields()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showEndTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endDateTime
        
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.timeInMillis = endDateTime
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                endDateTime = newCalendar.timeInMillis
                updateDateTimeFields()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun updateDateTimeFields() {
        val startCalendar = Calendar.getInstance()
        startCalendar.timeInMillis = startDateTime
        
        val endCalendar = Calendar.getInstance()
        endCalendar.timeInMillis = endDateTime
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        etStartDate.setText(dateFormat.format(startCalendar.time))
        etStartTime.setText(timeFormat.format(startCalendar.time))
        etEndDate.setText(dateFormat.format(endCalendar.time))
        etEndTime.setText(timeFormat.format(endCalendar.time))
    }
    
    private fun createBooking() {
        // Validate input
        if (selectedStationId.isEmpty()) {
            Toasts.showValidationError(this, "Please select a station")
            return
        }
        
        if (!Validators.isValidBookingDuration(startDateTime, endDateTime)) {
            Toasts.showValidationError(this, Validators.getBookingDurationErrorMessage())
            return
        }
        
        if (!Validators.canCreateBooking(startDateTime)) {
            Toasts.showValidationError(this, Validators.getBookingCreationErrorMessage())
            return
        }
        
        // Show booking summary
        showBookingSummary()
    }
    
    private fun showBookingSummary() {
        val selectedStation = stations.find { it.id == selectedStationId }
        val stationName = selectedStation?.name ?: "Unknown Station"
        
        val startTimeStr = Datex.formatToDisplay(startDateTime)
        val endTimeStr = Datex.formatToDisplay(endDateTime)
        val duration = (endDateTime - startDateTime) / (1000 * 60 * 60) // hours
        
        val message = """
            Station: $stationName
            Start: $startTimeStr
            End: $endTimeStr
            Duration: ${duration}h
            
            ${Validators.getBookingCreationErrorMessage()}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Booking Summary")
            .setMessage(message)
            .setPositiveButton("Create Booking") { _, _ ->
                submitBooking()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun submitBooking() {
        loadingView.show()
        loadingView.setMessage("Creating booking...")
        
        val iso = TimeExt.millisToIsoUTC(startDateTime)
        val request = BookingCreateRequest(
            stationId = selectedStationId,
            reservationDateTime = iso
        )
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.create(selectedStationId, iso)
                
                if (result.isSuccess()) {
                    val booking = result.getDataOrNull()
                    if (booking != null) {
                        Toasts.showSuccess(this@BookingFormActivity, "Booking created successfully")
                        
                        // Open booking detail
                        val intent = Intent(this@BookingFormActivity, BookingDetailActivity::class.java)
                        intent.putExtra("booking_id", "temp_id") // TODO: Get actual booking ID
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@BookingFormActivity, error?.message ?: "Failed to create booking")
                }
            } catch (e: Exception) {
                Toasts.showError(this@BookingFormActivity, "Failed to create booking: ${e.message}")
            } finally {
                loadingView.hide()
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
