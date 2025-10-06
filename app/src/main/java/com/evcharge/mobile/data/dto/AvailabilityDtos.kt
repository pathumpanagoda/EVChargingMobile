package com.evcharge.mobile.data.dto
data class Slot(val start: String, val end: String)
data class Availability(
    val stationId: String,
    val date: String,
    val isAvailable: Boolean,
    val nextSlots: List<Slot> = emptyList()
)