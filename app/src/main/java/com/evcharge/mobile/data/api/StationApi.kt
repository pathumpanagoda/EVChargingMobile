package com.evcharge.mobile.data.api

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.data.dto.*
import org.json.JSONObject

/**
 * Charging Station API service
 */
class StationApi(private val apiClient: ApiClient) {
    
    /**
     * Get all stations
     */
    suspend fun getAllStations(): Result<List<Station>> {
        return try {
            // Backend returns paginated response, request large page size to get all
            val response = apiClient.get("/api/ChargingStation?page=1&pageSize=100")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                val items = data?.optJSONArray("items")
                
                if (items != null) {
                    val stations = mutableListOf<Station>()
                    for (i in 0 until items.length()) {
                        val stationData = items.getJSONObject(i)
                        val station = parseStation(stationData)
                        stations.add(station)
                    }
                    Result.Success(stations)
                } else {
                    Result.Success(emptyList())
                }
            } else {
                val message = response.optString("message", "Failed to get stations")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get station by ID
     */
    suspend fun getStation(stationId: String): Result<Station> {
        return try {
            val response = apiClient.get("/api/ChargingStation/$stationId")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val station = parseStation(data)
                    Result.Success(station)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to get station")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get nearby stations
     */
    suspend fun getNearbyStations(latitude: Double, longitude: Double, radius: Double = 10.0): Result<List<Station>> {
        return try {
            val response = apiClient.get("/api/ChargingStation/nearby?latitude=$latitude&longitude=$longitude&maxDistanceKm=$radius&limit=50")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONArray("data")
                if (data != null) {
                    val stations = mutableListOf<Station>()
                    for (i in 0 until data.length()) {
                        val stationData = data.getJSONObject(i)
                        val station = parseStation(stationData)
                        stations.add(station)
                    }
                    Result.Success(stations)
                } else {
                    Result.Success(emptyList())
                }
            } else {
                val message = response.optString("message", "Failed to get nearby stations")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Check station availability
     */
    suspend fun checkStationAvailability(stationId: String, startTime: Long, endTime: Long): Result<StationAvailabilityResponse> {
        return try {
            val body = JSONObject().apply {
                put("stationId", stationId)
                put("startTime", startTime)
                put("endTime", endTime)
            }
            
            val response = apiClient.post("/api/ChargingStation/availability", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                val isAvailable = data?.optBoolean("isAvailable", false) ?: false
                
                val conflictingBookings = mutableListOf<Booking>()
                val bookingsData = data?.optJSONArray("conflictingBookings")
                if (bookingsData != null) {
                    for (i in 0 until bookingsData.length()) {
                        val bookingData = bookingsData.getJSONObject(i)
                        val booking = parseBooking(bookingData)
                        conflictingBookings.add(booking)
                    }
                }
                
                val availabilityResponse = StationAvailabilityResponse(
                    success = true,
                    message = response.optString("message", "Availability checked"),
                    isAvailable = isAvailable,
                    conflictingBookings = conflictingBookings
                )
                Result.Success(availabilityResponse)
            } else {
                val message = response.optString("message", "Failed to check availability")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Parse station from JSON
     */
    private fun parseStation(data: JSONObject): Station {
        // Debug logging
        android.util.Log.d("StationApi", "Parsing station: ${data.toString()}")
        
        // Parse location (nested in backend model)
        val locationData = data.optJSONObject("location")
        val address = locationData?.optString("address") ?: ""
        val latitude = locationData?.optDouble("latitude") ?: 0.0
        val longitude = locationData?.optDouble("longitude") ?: 0.0
        
        android.util.Log.d("StationApi", "Parsed location: $address at ($latitude, $longitude)")
        
        // Parse custom ID - Backend returns customId (lowercase in JSON)
        val customId = data.optString("customId")
        android.util.Log.d("StationApi", "Custom ID from API: customId='${data.optString("customId")}', final='$customId'")
        
        // Debug full API response to see what fields are available
        android.util.Log.d("StationApi", "Full API response keys: ${data.keys().asSequence().toList()}")
        android.util.Log.d("StationApi", "Full API response: ${data.toString()}")
        
        // Parse station capacity and determine status - using C# property names from backend
        val totalSlots = data.optInt("TotalSlots", 1)
        val availableSlots = data.optInt("AvailableSlots", 1)
        val isActive = data.optBoolean("IsActive", true)
        
        // Debug logging for station status determination
        android.util.Log.d("StationApi", "Station ${data.optString("name", "Unknown")}: isActive=$isActive, totalSlots=$totalSlots, availableSlots=$availableSlots")
        
        // Determine status based on availability and active state
        val status = when {
            !isActive -> {
                android.util.Log.d("StationApi", "Station marked as OFFLINE due to isActive=false")
                StationStatus.OFFLINE
            }
            availableSlots == 0 -> {
                android.util.Log.d("StationApi", "Station marked as OCCUPIED due to no available slots")
                StationStatus.OCCUPIED
            }
            availableSlots < totalSlots -> {
                android.util.Log.d("StationApi", "Station marked as OCCUPIED due to partial occupancy")
                StationStatus.OCCUPIED
            }
            else -> {
                android.util.Log.d("StationApi", "Station marked as AVAILABLE")
                StationStatus.AVAILABLE
            }
        }
        
        android.util.Log.d("StationApi", "Final status for ${data.optString("name", "Unknown")}: $status")
        
        // Parse amenities (if available)
        val amenities = mutableListOf<String>()
        val amenitiesData = data.optJSONArray("amenities")
        if (amenitiesData != null) {
            for (i in 0 until amenitiesData.length()) {
                amenities.add(amenitiesData.getString(i))
            }
        }
        
        // Add station type as an amenity
        val stationType = data.optString("type", "AC")
        if (stationType.isNotEmpty()) {
            amenities.add("Type: $stationType")
        }
        
        // Parse schedule (simplified from backend DailySchedule)
        val schedule = mutableListOf<StationScheduleItem>()
        val scheduleData = data.optJSONArray("schedule")
        if (scheduleData != null) {
            for (i in 0 until scheduleData.length()) {
                val scheduleItemData = scheduleData.getJSONObject(i)
                val scheduleItem = StationScheduleItem(
                    dayOfWeek = i + 1, // Use index as day of week
                    startTime = scheduleItemData.optString("open", "00:00"),
                    endTime = scheduleItemData.optString("close", "23:59"),
                    isAvailable = scheduleItemData.optInt("slotsAvailable", 0) > 0
                )
                schedule.add(scheduleItem)
            }
        }
        
        return Station(
            id = data.optString("id"),
            customId = customId, // Use the debugged customId variable
            name = data.optString("name", "Unknown Station"),
            address = address,
            latitude = latitude,
            longitude = longitude,
            status = status,
            maxCapacity = totalSlots,
            currentOccupancy = totalSlots - availableSlots,
            chargingRate = 50.0, // Default charging rate (kW)
            pricePerHour = 5.0, // Default price per hour
            amenities = amenities,
            schedule = schedule
        )
    }
    
    /**
     * Parse booking from JSON (for conflicting bookings)
     */
    private fun parseBooking(data: JSONObject): Booking {
        val statusString = data.optString("status", "PENDING")
        val status = when (statusString.uppercase()) {
            "PENDING" -> BookingStatus.PENDING
            "APPROVED" -> BookingStatus.APPROVED
            "COMPLETED" -> BookingStatus.COMPLETED
            "CANCELLED" -> BookingStatus.CANCELLED
            else -> BookingStatus.PENDING
        }
        
        return Booking(
            id = data.optString("id"),
            ownerNic = data.optString("ownerNic"),
            stationId = data.optString("stationId"),
            stationName = data.optString("stationName"),
            startTime = data.optLong("startTime"),
            endTime = data.optLong("endTime"),
            status = status,
            createdAt = data.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = data.optLong("updatedAt", System.currentTimeMillis()),
            qrCode = data.optString("qrCode")
        )
    }
}
