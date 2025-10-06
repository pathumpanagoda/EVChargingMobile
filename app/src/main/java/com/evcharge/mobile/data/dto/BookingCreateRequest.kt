package com.evcharge.mobile.data.dto
data class BookingCreateRequest(
    val stationId: String,
    val reservationDateTime: String // ISO-8601 UTC
)