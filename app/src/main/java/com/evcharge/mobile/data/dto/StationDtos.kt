package com.evcharge.mobile.data.dto

data class Station(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: StationStatus,
    val capacity: Int,
    val availableSlots: Int,
    val pricePerHour: Double,
    val operatingHours: String,
    val amenities: List<String> = emptyList(),
    val chargingRate: String? = null
)

enum class StationStatus {
    ACTIVE,
    MAINTENANCE,
    OFFLINE,
    FULL,
    AVAILABLE,
    OCCUPIED
}

data class StationScheduleItem(
    val dayOfWeek: String,
    val openTime: String,
    val closeTime: String,
    val isAvailable: Boolean
)
