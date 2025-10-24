package com.evcharge.mobile.data.dto

/**
 * Data classes for charging station API requests and responses
 */

/**
 * Station status enum
 */
enum class StationStatus {
    AVAILABLE,
    OCCUPIED,
    MAINTENANCE,
    OFFLINE
}

/**
 * Station DTO
 */
data class Station(
    val id: String,
    val customId: String? = null,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: StationStatus,
    val maxCapacity: Int = 1,
    val currentOccupancy: Int = 0,
    val chargingRate: Double = 0.0, // kW
    val pricePerHour: Double = 0.0,
    val amenities: List<String> = emptyList(),
    val schedule: List<StationScheduleItem> = emptyList()
)

/**
 * Station schedule item DTO
 */
data class StationScheduleItem(
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val startTime: String, // HH:mm format
    val endTime: String, // HH:mm format
    val isAvailable: Boolean = true
)

/**
 * Station response DTO
 */
data class StationResponse(
    val success: Boolean,
    val message: String,
    val data: Station? = null
)

/**
 * Station list response DTO
 */
data class StationListResponse(
    val success: Boolean,
    val message: String,
    val data: List<Station>? = null,
    val total: Int = 0
)

/**
 * Nearby stations request DTO
 */
data class NearbyStationsRequest(
    val latitude: Double,
    val longitude: Double,
    val radius: Double = 10.0 // km
)

/**
 * Nearby stations response DTO
 */
data class NearbyStationsResponse(
    val success: Boolean,
    val message: String,
    val data: List<Station>? = null,
    val total: Int = 0
)

/**
 * Station availability check request DTO
 */
data class StationAvailabilityRequest(
    val stationId: String,
    val startTime: Long,
    val endTime: Long
)

/**
 * Station availability response DTO
 */
data class StationAvailabilityResponse(
    val success: Boolean,
    val message: String,
    val isAvailable: Boolean = false,
    val conflictingBookings: List<Booking>? = null
)
