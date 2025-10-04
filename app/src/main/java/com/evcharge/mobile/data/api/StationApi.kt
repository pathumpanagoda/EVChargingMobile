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
            val response = apiClient.get("/api/station")
            
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
            val response = apiClient.get("/api/station/$stationId")
            
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
            val response = apiClient.get("/api/station/nearby?lat=$latitude&lng=$longitude&radius=$radius")
            
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
            
            val response = apiClient.post("/api/station/availability", body)
            
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
        val statusString = data.optString("status", "AVAILABLE")
        val status = when (statusString.uppercase()) {
            "AVAILABLE" -> StationStatus.AVAILABLE
            "OCCUPIED" -> StationStatus.OCCUPIED
            "MAINTENANCE" -> StationStatus.MAINTENANCE
            "OFFLINE" -> StationStatus.OFFLINE
            else -> StationStatus.AVAILABLE
        }
        
        val amenities = mutableListOf<String>()
        val amenitiesData = data.optJSONArray("amenities")
        if (amenitiesData != null) {
            for (i in 0 until amenitiesData.length()) {
                amenities.add(amenitiesData.getString(i))
            }
        }
        
        val schedule = mutableListOf<StationScheduleItem>()
        val scheduleData = data.optJSONArray("schedule")
        if (scheduleData != null) {
            for (i in 0 until scheduleData.length()) {
                val scheduleItemData = scheduleData.getJSONObject(i)
                val scheduleItem = StationScheduleItem(
                    dayOfWeek = scheduleItemData.optInt("dayOfWeek", 1),
                    startTime = scheduleItemData.optString("startTime", "00:00"),
                    endTime = scheduleItemData.optString("endTime", "23:59"),
                    isAvailable = scheduleItemData.optBoolean("isAvailable", true)
                )
                schedule.add(scheduleItem)
            }
        }
        
        return Station(
            id = data.optString("id"),
            name = data.optString("name"),
            address = data.optString("address"),
            latitude = data.optDouble("latitude", 0.0),
            longitude = data.optDouble("longitude", 0.0),
            status = status,
            maxCapacity = data.optInt("maxCapacity", 1),
            currentOccupancy = data.optInt("currentOccupancy", 0),
            chargingRate = data.optDouble("chargingRate", 0.0),
            pricePerHour = data.optDouble("pricePerHour", 0.0),
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
