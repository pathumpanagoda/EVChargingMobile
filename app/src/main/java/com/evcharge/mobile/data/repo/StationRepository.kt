package com.evcharge.mobile.data.repo

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.*

/**
 * Repository for charging station operations
 */
class StationRepository(private val stationApi: StationApi) {
    
    /**
     * Get all stations
     */
    suspend fun getAllStations(): Result<List<Station>> {
        return stationApi.getAllStations()
    }
    
    /**
     * Get station by ID
     */
    suspend fun getStation(stationId: String): Result<Station> {
        return stationApi.getStation(stationId)
    }
    
    /**
     * Get nearby stations
     */
    suspend fun getNearbyStations(latitude: Double, longitude: Double, radius: Double = 10.0): Result<List<Station>> {
        return stationApi.getNearbyStations(latitude, longitude, radius)
    }
    
    /**
     * Get available time slots for a station
     */
    suspend fun getAvailableTimeSlots(stationId: String, startDate: String, days: Int = 7): Result<StationAvailabilityResponse> {
        return stationApi.getAvailableTimeSlots(stationId, startDate, days)
    }
    
    /**
     * Check station availability
     */
    suspend fun checkStationAvailability(stationId: String, startTime: Long, endTime: Long): Result<StationAvailabilityResponse> {
        return stationApi.checkStationAvailability(stationId, startTime, endTime)
    }
    
    /**
     * Get available stations (status = AVAILABLE)
     */
    suspend fun getAvailableStations(): Result<List<Station>> {
        return try {
            val result = getAllStations()
            if (result.isSuccess()) {
                val allStations = result.getDataOrNull() ?: emptyList()
                val availableStations = allStations.filter { it.status == StationStatus.AVAILABLE }
                Result.Success(availableStations)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get stations within radius of location
     */
    suspend fun getStationsInRadius(latitude: Double, longitude: Double, radiusKm: Double): Result<List<Station>> {
        return try {
            val result = getNearbyStations(latitude, longitude, radiusKm)
            if (result.isSuccess()) {
                val nearbyStations = result.getDataOrNull() ?: emptyList()
                val stationsInRadius = nearbyStations.filter { station ->
                    val distance = calculateDistance(latitude, longitude, station.latitude, station.longitude)
                    distance <= radiusKm
                }
                Result.Success(stationsInRadius)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Get stations with specific amenities
     */
    suspend fun getStationsWithAmenities(amenities: List<String>): Result<List<Station>> {
        return try {
            val result = getAllStations()
            if (result.isSuccess()) {
                val allStations = result.getDataOrNull() ?: emptyList()
                val filteredStations = allStations.filter { station ->
                    amenities.all { amenity -> station.amenities.contains(amenity) }
                }
                Result.Success(filteredStations)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get stations by status
     */
    suspend fun getStationsByStatus(status: StationStatus): Result<List<Station>> {
        return try {
            val result = getAllStations()
            if (result.isSuccess()) {
                val allStations = result.getDataOrNull() ?: emptyList()
                val filteredStations = allStations.filter { it.status == status }
                Result.Success(filteredStations)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
