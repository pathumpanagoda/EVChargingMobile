package com.evcharge.mobile.data.api

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.data.dto.*
import org.json.JSONObject

/**
 * Booking API service
 */
class BookingApi(private val apiClient: ApiClient) {
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(request: BookingCreateRequest): Result<Booking> {
        return try {
            val body = JSONObject().apply {
                put("StationId", request.stationId)  // Backend expects StationId (capital S)
                put("ReservationDateTime", request.reservationDateTime)  // Backend expects ReservationDateTime (capital R)
            }
            
            val response = apiClient.post("/api/booking", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val booking = parseBooking(data)
                    Result.Success(booking)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to create booking")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get booking by ID
     */
    suspend fun getBooking(bookingId: String): Result<Booking> {
        return try {
            val response = apiClient.get("/api/booking/$bookingId")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val booking = parseBooking(data)
                    Result.Success(booking)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to get booking")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get bookings for owner
     */
    suspend fun getOwnerBookings(ownerNic: String, includeHistory: Boolean = true): Result<List<Booking>> {
        return try {
            val path = "/api/booking/owner/$ownerNic?includeHistory=$includeHistory"
            
            val response = apiClient.get(path)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONArray("data")
                if (data != null) {
                    val bookings = mutableListOf<Booking>()
                    for (i in 0 until data.length()) {
                        val bookingData = data.getJSONObject(i)
                        val booking = parseBooking(bookingData)
                        bookings.add(booking)
                    }
                    Result.Success(bookings)
                } else {
                    Result.Success(emptyList())
                }
            } else {
                val message = response.optString("message", "Failed to get bookings")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Update booking
     */
    suspend fun updateBooking(bookingId: String, request: BookingUpdateRequest): Result<Booking> {
        return try {
            val body = JSONObject().apply {
                put("ReservationDateTime", request.reservationDateTime)  // Backend expects ReservationDateTime (capital R)
            }
            
            val response = apiClient.put("/api/booking/$bookingId", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val booking = parseBooking(data)
                    Result.Success(booking)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to update booking")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Cancel booking
     */
    suspend fun cancelBooking(bookingId: String): Result<Boolean> {
        return try {
            val response = apiClient.delete("/api/booking/$bookingId")
            
            if (response.optBoolean("success", false)) {
                Result.Success(true)
            } else {
                val message = response.optString("message", "Failed to cancel booking")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get dashboard stats for owner
     */
    suspend fun getDashboardStats(ownerNic: String): Result<DashboardStats> {
        return try {
            val response = apiClient.get("/api/booking/dashboard/$ownerNic")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val stats = DashboardStats(
                        pendingCount = data.optInt("pendingReservations", 0),
                        approvedCount = data.optInt("approvedFutureReservations", 0),
                        completedCount = 0, // Backend doesn't provide this separately
                        cancelledCount = 0  // Backend doesn't provide this separately
                    )
                    Result.Success(stats)
                } else {
                    Result.Success(DashboardStats())
                }
            } else {
                val message = response.optString("message", "Failed to get dashboard stats")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Complete booking (for operator)
     */
    suspend fun completeBooking(bookingId: String, qrCode: String): Result<BookingCompleteResponse> {
        return try {
            val body = JSONObject().apply {
                put("bookingId", bookingId)
                put("qrCode", qrCode)
            }
            
            val response = apiClient.post("/api/booking/complete", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                val booking = if (data != null) parseBooking(data) else null
                
                val completeResponse = BookingCompleteResponse(
                    success = true,
                    message = response.optString("message", "Booking completed successfully"),
                    data = booking
                )
                Result.Success(completeResponse)
            } else {
                val message = response.optString("message", "Failed to complete booking")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Parse booking from JSON
     */
    private fun parseBooking(data: JSONObject): Booking {
        val statusString = data.optString("status", "Pending")
        val status = when (statusString.uppercase()) {
            "PENDING" -> BookingStatus.PENDING
            "APPROVED" -> BookingStatus.APPROVED
            "COMPLETED" -> BookingStatus.COMPLETED
            "CANCELLED" -> BookingStatus.CANCELLED
            else -> BookingStatus.PENDING
        }
        
        // Parse dates from backend format
        val reservationDateTime = data.optString("reservationDateTime", "")
        val startTime = parseIso8601(reservationDateTime) ?: System.currentTimeMillis()
        
        // For end time, we'll use start time + 2 hours as default (backend doesn't store end time separately)
        val endTime = startTime + (2 * 60 * 60 * 1000) // 2 hours in milliseconds
        
        return Booking(
            id = data.optString("id"),
            ownerNic = data.optString("evOwnerNIC"),
            stationId = data.optString("stationId"),
            stationName = data.optString("stationName"),
            startTime = startTime,
            endTime = endTime,
            status = status,
            createdAt = parseIso8601(data.optString("createdAt")) ?: System.currentTimeMillis(),
            updatedAt = parseIso8601(data.optString("updatedAt")) ?: System.currentTimeMillis(),
            qrCode = data.optString("qrPayload")
        )
    }

    
    /**
     * Parse ISO 8601 date string to timestamp
     */
    private fun parseIso8601(dateString: String): Long? {
        if (dateString.isEmpty()) return null
        
        // Try multiple date formats
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // 2024-12-19T10:30:00.000Z
            "yyyy-MM-dd'T'HH:mm:ss'Z'",     // 2024-12-19T10:30:00Z
            "yyyy-MM-dd'T'HH:mm:ss",        // 2024-12-19T10:30:00
            "yyyy-MM-dd HH:mm:ss",          // 2024-12-19 10:30:00
            "yyyy-MM-dd'T'HH:mm:ss.SSS",    // 2024-12-19T10:30:00.000
        )
        
        for (formatPattern in formats) {
            try {
                val format = java.text.SimpleDateFormat(formatPattern, java.util.Locale.US)
                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val parsed = format.parse(dateString)
                if (parsed != null) {
                    android.util.Log.d("BookingApi", "Successfully parsed date: $dateString with format: $formatPattern")
                    return parsed.time
                }
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }
        
        android.util.Log.e("BookingApi", "Failed to parse date with any format: $dateString")
        return null
    }
}
