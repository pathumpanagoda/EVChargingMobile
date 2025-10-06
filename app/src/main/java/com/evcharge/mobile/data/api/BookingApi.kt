package com.evcharge.mobile.data.api

import com.evcharge.mobile.common.AppResult
import com.evcharge.mobile.data.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import org.json.JSONObject

class BookingApi(private val http: okhttp3.OkHttpClient = ApiClient.client()) {
    private val json = "application/json; charset=utf-8".toMediaType()
    
    suspend fun createBooking(request: BookingCreateRequest): AppResult<Booking> {
        return try {
            val body = JSONObject().apply {
                put("stationId", request.stationId)
                put("reservationDateTime", request.reservationDateTime)
            }
            
            val req = ApiClient.requestBuilder("/api/booking")
                .post(RequestBody.create(json, body.toString()))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    val data = obj.optJSONObject("data")
                    if (data != null) {
                        val booking = parseBooking(data)
                        AppResult.Ok(booking)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to create booking")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun getBooking(bookingId: String): AppResult<Booking> {
        return try {
            val req = ApiClient.requestBuilder("/api/booking/$bookingId")
                .get()
                .addHeader("Accept", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    val data = obj.optJSONObject("data")
                    if (data != null) {
                        val booking = parseBooking(data)
                        AppResult.Ok(booking)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to get booking")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun getOwnerBookings(ownerNic: String, upcoming: Boolean? = null): AppResult<List<Booking>> {
        return try {
            val url = if (upcoming != null) {
                "/api/booking/owner/$ownerNic?upcoming=$upcoming"
            } else {
                "/api/booking/owner/$ownerNic"
            }
            
            val req = ApiClient.requestBuilder(url)
                .get()
                .addHeader("Accept", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    val data = obj.optJSONArray("data")
                    if (data != null) {
                        val bookings = mutableListOf<Booking>()
                        for (i in 0 until data.length()) {
                            val booking = parseBooking(data.getJSONObject(i))
                            bookings.add(booking)
                        }
                        AppResult.Ok(bookings)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to get bookings")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun updateBooking(bookingId: String, request: BookingUpdateRequest): AppResult<Booking> {
        return try {
            val body = JSONObject().apply {
                request.status?.let { put("status", it) }
                request.notes?.let { put("notes", it) }
            }
            
            val req = ApiClient.requestBuilder("/api/booking/$bookingId")
                .put(RequestBody.create(json, body.toString()))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    val data = obj.optJSONObject("data")
                    if (data != null) {
                        val booking = parseBooking(data)
                        AppResult.Ok(booking)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to update booking")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun cancelBooking(bookingId: String): AppResult<Boolean> {
        return try {
            val req = ApiClient.requestBuilder("/api/booking/$bookingId/cancel")
                .delete()
                .addHeader("Accept", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    AppResult.Ok(true)
                } else {
                    val message = obj.optString("message", "Failed to cancel booking")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun getDashboardStats(ownerNic: String): AppResult<DashboardStats> {
        return try {
            val req = ApiClient.requestBuilder("/api/booking/stats/$ownerNic")
                .get()
                .addHeader("Accept", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    val data = obj.optJSONObject("data")
                    if (data != null) {
                        val stats = DashboardStats(
                            totalBookings = data.optInt("totalBookings", 0),
                            pendingBookings = data.optInt("pendingBookings", 0),
                            completedBookings = data.optInt("completedBookings", 0),
                            cancelledBookings = data.optInt("cancelledBookings", 0)
                        )
                        AppResult.Ok(stats)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to get stats")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun completeBooking(bookingId: String, qrCode: String): AppResult<BookingCompleteResponse> {
        return try {
            val body = JSONObject().apply {
                put("qrCode", qrCode)
            }
            
            val req = ApiClient.requestBuilder("/api/booking/$bookingId/complete")
                .post(RequestBody.create(json, body.toString()))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return AppResult.Err(Exception(raw))
                }
                
                val obj = JSONObject(raw)
                if (obj.optBoolean("success", false)) {
                    val data = obj.optJSONObject("data")
                    if (data != null) {
                        val response = BookingCompleteResponse(
                            success = true,
                            message = data.optString("message", "Booking completed"),
                            booking = parseBooking(data.optJSONObject("booking"))
                        )
                        AppResult.Ok(response)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to complete booking")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    private fun parseBooking(data: JSONObject): Booking {
        return Booking(
            id = data.optString("id", ""),
            stationId = data.optString("stationId", ""),
            ownerNic = data.optString("ownerNic", ""),
            reservationDateTime = data.optString("reservationDateTime", ""),
            status = data.optString("status", ""),
            createdAt = data.optString("createdAt", ""),
            updatedAt = data.optString("updatedAt", "")
        )
    }
}