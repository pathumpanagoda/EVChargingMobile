package com.evcharge.mobile.data.dto

import android.os.Parcel
import android.os.Parcelable

/**
 * Data classes for booking API requests and responses
 */

/**
 * Booking status enum
 */
enum class BookingStatus : Parcelable {
    PENDING,
    APPROVED,
    COMPLETED,
    CANCELLED;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BookingStatus> {
        override fun createFromParcel(parcel: Parcel): BookingStatus {
            return valueOf(parcel.readString() ?: "PENDING")
        }

        override fun newArray(size: Int): Array<BookingStatus?> {
            return arrayOfNulls(size)
        }
    }
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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readParcelable(BookingStatus::class.java.classLoader) ?: BookingStatus.PENDING,
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(ownerNic)
        parcel.writeString(stationId)
        parcel.writeString(stationName)
        parcel.writeLong(startTime)
        parcel.writeLong(endTime)
        parcel.writeParcelable(status, flags)
        parcel.writeLong(createdAt)
        parcel.writeLong(updatedAt)
        parcel.writeString(qrCode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking {
            return Booking(parcel)
        }

        override fun newArray(size: Int): Array<Booking?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Booking create request DTO - matches backend BookingRequest
 */
data class BookingCreateRequest(
    val stationId: String,
    val reservationDateTime: String  // ISO 8601 format for backend
)

/**
 * Booking update request DTO - matches backend BookingUpdateRequest
 */
data class BookingUpdateRequest(
    val reservationDateTime: String  // ISO 8601 format for backend
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
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0
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
