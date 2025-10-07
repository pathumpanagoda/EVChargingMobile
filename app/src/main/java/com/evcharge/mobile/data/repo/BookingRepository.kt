package com.evcharge.mobile.data.repo

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.dto.*

/**
 * Repository for booking operations
 */
class BookingRepository(private val bookingApi: BookingApi) {
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(request: BookingCreateRequest): Result<Booking> {
        return bookingApi.createBooking(request)
    }
    
    /**
     * Get booking by ID
     */
    suspend fun getBooking(bookingId: String): Result<Booking> {
        return bookingApi.getBooking(bookingId)
    }
    
    /**
     * Get bookings for owner
     */
    suspend fun getOwnerBookings(ownerNic: String, includeHistory: Boolean = true): Result<List<Booking>> {
        return bookingApi.getOwnerBookings(ownerNic, includeHistory)
    }
    
    /**
     * Update booking
     */
    suspend fun updateBooking(bookingId: String, request: BookingUpdateRequest): Result<Booking> {
        return bookingApi.updateBooking(bookingId, request)
    }
    
    /**
     * Cancel booking
     */
    suspend fun cancelBooking(bookingId: String): Result<Boolean> {
        return bookingApi.cancelBooking(bookingId)
    }
    
    /**
     * Get dashboard stats for owner
     */
    suspend fun getDashboardStats(ownerNic: String): Result<DashboardStats> {
        return bookingApi.getDashboardStats(ownerNic)
    }
    
    /**
     * Complete booking (for operator)
     */
    suspend fun completeBooking(bookingId: String, qrCode: String): Result<BookingCompleteResponse> {
        return bookingApi.completeBooking(bookingId, qrCode)
    }
    
    /**
     * Get upcoming bookings for owner (exclude history)
     */
    suspend fun getUpcomingBookings(ownerNic: String): Result<List<Booking>> {
        return getOwnerBookings(ownerNic, false)
    }
    
    /**
     * Get booking history for owner (include history)
     */
    suspend fun getBookingHistory(ownerNic: String): Result<List<Booking>> {
        return getOwnerBookings(ownerNic, true)
    }
    
    /**
     * Get all bookings for owner (both upcoming and history)
     */
    suspend fun getAllBookings(ownerNic: String): Result<List<Booking>> {
        return getOwnerBookings(ownerNic, true)
    }
}
