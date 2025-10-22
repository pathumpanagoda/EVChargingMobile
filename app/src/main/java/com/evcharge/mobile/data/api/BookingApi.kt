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
            // Convert Unix timestamp to ISO 8601 format for backend
            val startDateTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date(request.startTime))
            
            val body = JSONObject().apply {
                put("stationId", request.stationId)
                put("reservationDateTime", startDateTime)
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
    suspend fun getOwnerBookings(ownerNic: String, upcoming: Boolean? = null): Result<List<Booking>> {
        return try {
            var path = "/api/booking/owner/$ownerNic"
            if (upcoming != null) {
                // Backend expects includeHistory parameter
                // upcoming=true means includeHistory=false (only future bookings)
                // upcoming=false means includeHistory=true (all bookings including past)
                val includeHistory = !upcoming
                path += "?includeHistory=$includeHistory"
            }
            
            android.util.Log.d("BookingApi", "Getting owner bookings - Path: $path, NIC: $ownerNic, Upcoming: $upcoming")
            
            val response = apiClient.get(path)
            
            android.util.Log.d("BookingApi", "Response success: ${response.optBoolean("success", false)}")
            android.util.Log.d("BookingApi", "Response message: ${response.optString("message", "N/A")}")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONArray("data")
                if (data != null) {
                    val bookings = mutableListOf<Booking>()
                    for (i in 0 until data.length()) {
                        val bookingData = data.getJSONObject(i)
                        val booking = parseBooking(bookingData)
                        bookings.add(booking)
                    }
                    android.util.Log.d("BookingApi", "Successfully loaded ${bookings.size} bookings")
                    Result.Success(bookings)
                } else {
                    android.util.Log.d("BookingApi", "No data array in response, returning empty list")
                    Result.Success(emptyList())
                }
            } else {
                val message = response.optString("message", "Failed to get bookings")
                android.util.Log.e("BookingApi", "API returned error: $message")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            android.util.Log.e("BookingApi", "Exception getting bookings: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Update booking
     */
    suspend fun updateBooking(bookingId: String, request: BookingUpdateRequest): Result<Booking> {
        return try {
            val body = JSONObject().apply {
                if (request.stationId != null) put("stationId", request.stationId)
                if (request.startTime != null) put("startTime", request.startTime)
                if (request.endTime != null) put("endTime", request.endTime)
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
                        pendingReservations = data.optInt("pendingReservations", 0),
                        approvedFutureReservations = data.optInt("approvedFutureReservations", 0),
                        totalBookings = data.optInt("totalBookings", 0)
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
        val statusString = data.optString("status", "PENDING")
        val status = when (statusString.uppercase()) {
            "PENDING" -> BookingStatus.PENDING
            "APPROVED" -> BookingStatus.APPROVED
            "COMPLETED" -> BookingStatus.COMPLETED
            "CANCELLED" -> BookingStatus.CANCELLED
            else -> BookingStatus.PENDING
        }
        
        // Parse ISO 8601 dates from backend
        val reservationDateTime = parseIso8601(data.optString("reservationDateTime"))
        val createdAt = parseIso8601(data.optString("createdAt")) ?: System.currentTimeMillis()
        val updatedAt = parseIso8601(data.optString("updatedAt")) ?: System.currentTimeMillis()
        
        // Calculate end time (backend doesn't provide it, assume 2 hours from start)
        val endTime = reservationDateTime?.let { it + (2 * 60 * 60 * 1000) } ?: 0L
        
        return Booking(
            id = data.optString("id"),
            ownerNic = data.optString("evOwnerNIC", data.optString("ownerNic")), // Backend uses evOwnerNIC
            stationId = data.optString("stationId"),
            stationName = data.optString("stationName"),
            startTime = reservationDateTime ?: 0L,
            endTime = endTime,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            qrCode = data.optString("qrPayload", data.optString("qrCode")) // Backend uses qrPayload
        )
    }
    
    /**
     * Parse ISO 8601 date string to timestamp
     */
    private fun parseIso8601(dateString: String): Long? {
        if (dateString.isEmpty()) return null
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time
        } catch (e: Exception) {
            android.util.Log.e("BookingApi", "Failed to parse ISO 8601 date: $dateString", e)
            null
        }
    }
}
