package com.evcharge.mobile.data.dto

import java.io.Serializable

/**
 * Data classes for booking API requests and responses
 */

/**
 * Booking status enum
 */
enum class BookingStatus : Serializable {
    PENDING,
    APPROVED,
    COMPLETED,
    CANCELLED
}

/**
 * Booking DTO
 */
data class Booking(
    val id: String,
    val ownerNic: String,
    val stationId: String,
    val stationName: String? = null,
    val startTime: Long,
    val endTime: Long,
    val status: BookingStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val qrCode: String? = null
) : Serializable

/**
 * Booking create request DTO
 */
data class BookingCreateRequest(
    val stationId: String,
    val startTime: Long,
    val endTime: Long
)

/**
 * Booking update request DTO
 */
data class BookingUpdateRequest(
    val stationId: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null
)

/**
 * Booking response DTO
 */
data class BookingResponse(
    val success: Boolean,
    val message: String,
    val data: Booking? = null
)

/**
 * Booking list response DTO
 */
data class BookingListResponse(
    val success: Boolean,
    val message: String,
    val data: List<Booking>? = null,
    val total: Int = 0
)

/**
 * Dashboard stats DTO
 */
data class DashboardStats(
    val pendingReservations: Int = 0,
    val approvedFutureReservations: Int = 0,
    val totalBookings: Int = 0
)

/**
 * Dashboard response DTO
 */
data class DashboardResponse(
    val success: Boolean,
    val message: String,
    val data: DashboardStats? = null
)

/**
 * Booking complete request DTO (for operator)
 */
data class BookingCompleteRequest(
    val bookingId: String,
    val qrCode: String
)

/**
 * Booking complete response DTO
 */
data class BookingCompleteResponse(
    val success: Boolean,
    val message: String,
    val data: Booking? = null
)

/**
 * Slot availability response DTO
 */
data class SlotAvailabilityResponse(
    val isAvailable: Boolean,
    val message: String,
    val booking: Booking? = null
)
