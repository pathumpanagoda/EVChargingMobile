package com.evcharge.mobile.ui.stations

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Permissions
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import kotlinx.coroutines.launch

/**
 * Dummy map activity showing nearby charging stations as a list
 * This replaces the Google Maps implementation to avoid API key issues
 */
class DummyMapActivity : AppCompatActivity() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var stationRepository: StationRepository
    private lateinit var loadingView: LoadingView
    private lateinit var recyclerView: RecyclerView
    private lateinit var stationAdapter: StationListAdapter
    
    private var currentLocation: Location? = null
    private var stations: List<Station> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dummy_map)
        
        initializeComponents()
        setupUI()
        setupRecyclerView()
        loadNearbyStations()
    }
    
    private fun initializeComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val stationApi = StationApi(apiClient)
        stationRepository = StationRepository(stationApi)
        
        loadingView = findViewById(R.id.loading_view)
        recyclerView = findViewById(R.id.recycler_view)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Charging Stations"
    }
    
    private fun setupRecyclerView() {
        stationAdapter = StationListAdapter { station ->
            showStationInfo(station)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DummyMapActivity)
            adapter = stationAdapter
        }
    }
    
    private fun loadNearbyStations() {
        loadingView.show()
        
        lifecycleScope.launch {
            try {
                val result = stationRepository.getNearbyStations(
                    latitude = currentLocation?.latitude ?: 37.4219983,
                    longitude = currentLocation?.longitude ?: -122.084,
                    radius = 10.0
                )
                
                if (result.isSuccess()) {
                    val data = result.getDataOrNull()
                    if (data != null) {
                        stations = data
                        stationAdapter.updateStations(stations)
                        Toasts.showSuccess(this@DummyMapActivity, "Found ${stations.size} stations nearby")
                    } else {
                        Toasts.showError(this@DummyMapActivity, "No stations found")
                    }
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@DummyMapActivity, error?.message ?: "Failed to load stations")
                }
            } catch (e: Exception) {
                Toasts.showError(this@DummyMapActivity, "Error: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
    }
    
    private fun showStationInfo(station: Station) {
        val message = buildString {
            appendLine("Station: ${station.name}")
            appendLine("Address: ${station.address}")
            appendLine("Status: ${station.status}")
            appendLine("Available Ports: ${station.maxCapacity - station.currentOccupancy}/${station.maxCapacity}")
            appendLine("Charging Rate: ${station.chargingRate}kW")
            appendLine("Price: $${station.pricePerHour}/hour")
        }
        
        Toasts.showInfo(this, message)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_station_map, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                loadNearbyStations()
                true
            }
            R.id.action_my_location -> {
                getCurrentLocation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    Toasts.showSuccess(this, "Location updated")
                    loadNearbyStations()
                } else {
                    Toasts.showError(this, "Unable to get current location")
                }
            }
        } else {
            requestLocationPermission()
        }
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            Permissions.REQUEST_LOCATION_PERMISSION
        )
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Permissions.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toasts.showError(this, "Location permission denied")
            }
        }
    }
}
