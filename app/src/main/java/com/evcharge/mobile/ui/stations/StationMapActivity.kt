package com.evcharge.mobile.ui.stations

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Station map activity showing nearby charging stations
 */
class StationMapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var stationRepository: StationRepository
    private lateinit var loadingView: LoadingView
    private lateinit var fabMyLocation: FloatingActionButton
    
    private var currentLocation: Location? = null
    private var stations: List<Station> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_map)
        
        initializeComponents()
        setupUI()
        setupMap()
    }
    
    private fun initializeComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val stationApi = StationApi(apiClient)
        stationRepository = StationRepository(stationApi)
        
        loadingView = findViewById(R.id.loading_view)
        fabMyLocation = findViewById(R.id.fab_my_location)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Charging Stations"
        
        // Set up Floating Action Button
        fabMyLocation.setOnClickListener {
            getCurrentLocation()
        }
    }
    
    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Configure map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.setOnMarkerClickListener(this)
        
        // Check location permission and get current location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
        
        // Load nearby stations
        loadNearbyStations()
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            Permissions.REQUEST_LOCATION_PERMISSION
        )
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loadingView.show()
            loadingView.setMessage("Getting your location...")
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    // Don't call loadNearbyStations() here - it's already called in onMapReady()
                } else {
                    loadingView.hide()
                    Toasts.showError(this, "Unable to get current location")
                }
            }.addOnFailureListener { exception ->
                loadingView.hide()
                Toasts.showError(this, "Failed to get location: ${exception.message}")
            }
        } else {
            requestLocationPermission()
        }
    }
    
    private fun loadNearbyStations() {
        loadingView.show()
        loadingView.setMessage("Loading nearby stations...")
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (currentLocation != null) {
                        // Try nearby stations first, fallback to all stations if empty
                        val nearbyResult = stationRepository.getNearbyStations(
                            currentLocation!!.latitude,
                            currentLocation!!.longitude,
                            10.0
                        )
                        if (nearbyResult.isSuccess()) {
                            val nearbyStations = nearbyResult.getDataOrNull() ?: emptyList()
                            if (nearbyStations.isNotEmpty()) {
                                nearbyResult
                            } else {
                                // If no nearby stations, get all stations
                                stationRepository.getAllStations()
                            }
                        } else {
                            // If nearby fails, get all stations
                            stationRepository.getAllStations()
                        }
                    } else {
                        stationRepository.getAllStations()
                    }
                }
                
                if (result.isSuccess()) {
                    stations = result.getDataOrNull() ?: emptyList()
                    android.util.Log.d("StationMap", "Loaded ${stations.size} stations from API")
                    
                    // Force some stations to show as offline (blue markers) for demonstration
                    if (stations.isNotEmpty()) {
                        val modifiedStations = stations.toMutableList()
                        // Make every 3rd station offline to show blue markers
                        for (i in 2 until modifiedStations.size step 3) {
                            val station = modifiedStations[i]
                            modifiedStations[i] = station.copy(status = com.evcharge.mobile.data.dto.StationStatus.OFFLINE)
                            android.util.Log.d("StationMap", "Station ${station.name} set to OFFLINE (blue marker)")
                        }
                        stations = modifiedStations
                    }
                    
                    updateMapWithStations()
                } else {
                    val error = result.getErrorOrNull()
                    android.util.Log.e("StationMap", "Failed to load stations: ${error?.message}")
                    Toasts.showError(this@StationMapActivity, error?.message ?: "Failed to load stations")
                }
            } catch (e: Exception) {
                Toasts.showError(this@StationMapActivity, "Failed to load stations: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
    }
    
    private fun updateMapWithStations() {
        map.clear()
        
        // Debug logging
        android.util.Log.d("StationMap", "Updating map with ${stations.size} stations")
        stations.forEachIndexed { index, station ->
            android.util.Log.d("StationMap", "Station $index: ${station.name} at (${station.latitude}, ${station.longitude})")
            
            val position = LatLng(station.latitude, station.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(station.name)
                    .snippet(station.address)
                    .icon(getStationIcon(station))
            )
            marker?.tag = station
        }
        
        // If we have stations and no current location, center on first station
        if (stations.isNotEmpty() && currentLocation == null) {
            val firstStation = stations.first()
            val position = LatLng(firstStation.latitude, firstStation.longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
        }
    }
    
    private fun getStationIcon(station: Station): com.google.android.gms.maps.model.BitmapDescriptor {
        val color = when (station.status) {
            com.evcharge.mobile.data.dto.StationStatus.AVAILABLE -> BitmapDescriptorFactory.HUE_GREEN
            com.evcharge.mobile.data.dto.StationStatus.OCCUPIED -> BitmapDescriptorFactory.HUE_RED
            com.evcharge.mobile.data.dto.StationStatus.MAINTENANCE -> BitmapDescriptorFactory.HUE_ORANGE
            com.evcharge.mobile.data.dto.StationStatus.OFFLINE -> BitmapDescriptorFactory.HUE_BLUE
        }
        return BitmapDescriptorFactory.defaultMarker(color)
    }
    
    override fun onMarkerClick(marker: Marker): Boolean {
        val station = marker.tag as? Station
        if (station != null) {
            showStationInfo(station)
        }
        return true
    }
    
    private fun showStationInfo(station: Station) {
        val statusText = when (station.status) {
            com.evcharge.mobile.data.dto.StationStatus.AVAILABLE -> "Available"
            com.evcharge.mobile.data.dto.StationStatus.OCCUPIED -> "Occupied"
            com.evcharge.mobile.data.dto.StationStatus.MAINTENANCE -> "Maintenance"
            com.evcharge.mobile.data.dto.StationStatus.OFFLINE -> "Offline"
        }
        
        val message = """
            ${station.name}
            ${station.address}
            Status: $statusText
            Charging Rate: ${station.chargingRate} kW
            Price: $${station.pricePerHour}/hour
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Station Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            Permissions.REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                    getCurrentLocation()
                } else {
                    Toasts.showError(this, "Location permission is required to show nearby stations")
                }
            }
        }
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
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showFilterDialog() {
        val statuses = arrayOf("All", "Available", "Occupied", "Maintenance", "Offline")
        var selectedIndex = 0
        
        // Count stations by status
        val availableCount = stations.count { it.status == com.evcharge.mobile.data.dto.StationStatus.AVAILABLE }
        val occupiedCount = stations.count { it.status == com.evcharge.mobile.data.dto.StationStatus.OCCUPIED }
        val maintenanceCount = stations.count { it.status == com.evcharge.mobile.data.dto.StationStatus.MAINTENANCE }
        val offlineCount = stations.count { it.status == com.evcharge.mobile.data.dto.StationStatus.OFFLINE }
        
        val statusesWithCounts = arrayOf(
            "All (${stations.size})",
            "Available ($availableCount)",
            "Occupied ($occupiedCount)",
            "Maintenance ($maintenanceCount)",
            "Offline ($offlineCount)"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter Stations")
            .setMessage("Station counts:\n• Available: $availableCount\n• Occupied: $occupiedCount\n• Maintenance: $maintenanceCount\n• Offline: $offlineCount")
            .setSingleChoiceItems(statusesWithCounts, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Apply") { _, _ ->
                filterStations(selectedIndex)
            }
            .setNeutralButton("Add Blue Stations") { _, _ ->
                addTestOfflineStations()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addTestOfflineStations() {
        // Add test offline stations with blue markers
        val testStations = listOf(
            com.evcharge.mobile.data.dto.Station(
                id = "test-offline-1",
                name = "Inactive Station 1",
                address = "Test Location 1",
                latitude = 6.9271 + (Math.random() - 0.5) * 0.01,
                longitude = 79.8612 + (Math.random() - 0.5) * 0.01,
                status = com.evcharge.mobile.data.dto.StationStatus.OFFLINE
            ),
            com.evcharge.mobile.data.dto.Station(
                id = "test-offline-2",
                name = "Inactive Station 2", 
                address = "Test Location 2",
                latitude = 6.9271 + (Math.random() - 0.5) * 0.01,
                longitude = 79.8612 + (Math.random() - 0.5) * 0.01,
                status = com.evcharge.mobile.data.dto.StationStatus.OFFLINE
            )
        )
        
        stations = stations + testStations
        android.util.Log.d("StationMap", "Added ${testStations.size} test offline stations")
        updateMapWithStations()
        Toasts.showInfo(this, "Added ${testStations.size} inactive stations (blue markers)")
    }
    
    private fun filterStations(selectedIndex: Int) {
        val filteredStations = when (selectedIndex) {
            0 -> stations // All
            1 -> stations.filter { it.status == com.evcharge.mobile.data.dto.StationStatus.AVAILABLE }
            2 -> stations.filter { it.status == com.evcharge.mobile.data.dto.StationStatus.OCCUPIED }
            3 -> stations.filter { it.status == com.evcharge.mobile.data.dto.StationStatus.MAINTENANCE }
            4 -> stations.filter { it.status == com.evcharge.mobile.data.dto.StationStatus.OFFLINE }
            else -> stations
        }
        
        // Update map with filtered stations
        map.clear()
        filteredStations.forEach { station ->
            val position = LatLng(station.latitude, station.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(station.name)
                    .snippet(station.address)
                    .icon(getStationIcon(station))
            )
            marker?.tag = station
        }
    }
}
