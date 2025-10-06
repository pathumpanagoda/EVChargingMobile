package com.evcharge.mobile.data.dto

data class Booking(
    val id: String,
    val stationId: String,
    val ownerNic: String,
    val reservationDateTime: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val stationName: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val name: String? = null,
    val qrCode: String? = null
)

data class BookingUpdateRequest(
    val status: String? = null,
    val notes: String? = null
)

data class DashboardStats(
    val totalBookings: Int,
    val pendingBookings: Int,
    val completedBookings: Int,
    val cancelledBookings: Int,
    val pendingCount: Int = pendingBookings,
    val approvedCount: Int = 0
)

data class BookingCompleteResponse(
    val success: Boolean,
    val message: String,
    val booking: Booking? = null
)
