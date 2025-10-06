package com.evcharge.mobile.data.model
import java.util.Locale
enum class BookingStatus { Pending, Approved, Cancelled, Completed, Unknown }
fun String?.toBookingStatus(): BookingStatus = when (this?.lowercase(Locale.getDefault())) {
    "pending" -> BookingStatus.Pending
    "approved" -> BookingStatus.Approved
    "cancelled" -> BookingStatus.Cancelled
    "completed" -> BookingStatus.Completed
    else -> BookingStatus.Unknown
}
