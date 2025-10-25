package com.evcharge.mobile.data.dto

/**
 * Data class representing a time slot for booking
 */
data class TimeSlot(
    val time: String,                    // Time in HH:mm format (e.g., "08:00")
    val hour: Int,                       // Hour as integer (0-23)
    val available: Boolean,              // Whether this slot is available
    val approvedBookings: Int,           // Number of approved bookings
    val pendingBookings: Int,           // Number of pending bookings
    val totalSlots: Int,                 // Total number of slots
    val isSelected: Boolean = false     // Whether this slot is currently selected
) {
    /**
     * Get the number of available slots
     */
    fun getAvailableSlots(): Int {
        return totalSlots - (approvedBookings + pendingBookings)
    }
    
    /**
     * Get the availability text to display
     */
    fun getAvailabilityText(): String {
        return if (available) {
            "${getAvailableSlots()} left"
        } else {
            "Full"
        }
    }
    
    /**
     * Check if the slot is fully booked
     */
    fun isFullyBooked(): Boolean {
        return !available || getAvailableSlots() <= 0
    }
}
