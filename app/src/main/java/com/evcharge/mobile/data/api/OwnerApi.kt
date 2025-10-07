package com.evcharge.mobile.data.api

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.data.dto.OwnerProfile
import com.evcharge.mobile.data.dto.OwnerUpdateRequest
import org.json.JSONObject

/**
 * EV Owner API service
 */
class OwnerApi(private val apiClient: ApiClient) {
    
    /**
     * Get owner profile by NIC
     */
    suspend fun getOwner(nic: String): Result<OwnerProfile> {
        return try {
            val response = apiClient.get("/api/evowner/$nic")
            
            android.util.Log.d("OwnerApi", "Response: $response")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                android.util.Log.d("OwnerApi", "Data object: $data")
                
                if (data != null) {
                    // Log all available keys to debug the response format
                    val keys = data.keys()
                    android.util.Log.d("OwnerApi", "Available keys in response: ${keys.asSequence().toList()}")
                    
                    // Parse fields using lowercase field names (backend format)
                    val owner = OwnerProfile(
                        nic = data.optString("nic", ""),  // Backend uses lowercase
                        name = data.optString("name", ""),  // Backend uses lowercase
                        email = data.optString("email", ""),  // Backend uses lowercase
                        phone = data.optString("phone", ""),  // Backend uses lowercase
                        active = data.optBoolean("isActive", true),  // Backend uses lowercase
                        createdAt = parseIso8601(data.optString("createdAt", "")) ?: System.currentTimeMillis(),  // Parse ISO 8601 date
                        updatedAt = parseIso8601(data.optString("updatedAt", "")) ?: System.currentTimeMillis()  // Parse ISO 8601 date
                    )
                    android.util.Log.d("OwnerApi", "Parsed owner: $owner")
                    Result.Success(owner)
                } else {
                    android.util.Log.w("OwnerApi", "No data object in response")
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to get owner profile")
                android.util.Log.e("OwnerApi", "API error: $message")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnerApi", "Exception: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Update owner profile
     */
    suspend fun updateOwner(nic: String, request: OwnerUpdateRequest): Result<OwnerProfile> {
        return try {
            val body = JSONObject().apply {
                put("NIC", nic)  // Backend expects NIC field
                put("Name", request.name)  // Backend expects Name (capital N)
                put("Email", request.email)  // Backend expects Email (capital E)
                put("Phone", request.phone)  // Backend expects Phone (capital P)
                // Password is optional for updates, so we don't include it
            }
            
            val response = apiClient.put("/api/evowner/$nic", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    // Parse fields using lowercase field names (backend format)
                    val owner = OwnerProfile(
                        nic = data.optString("nic", ""),  // Backend uses lowercase
                        name = data.optString("name", ""),  // Backend uses lowercase
                        email = data.optString("email", ""),  // Backend uses lowercase
                        phone = data.optString("phone", ""),  // Backend uses lowercase
                        active = data.optBoolean("isActive", true),  // Backend uses lowercase
                        createdAt = parseIso8601(data.optString("createdAt", "")) ?: System.currentTimeMillis(),  // Parse ISO 8601 date
                        updatedAt = parseIso8601(data.optString("updatedAt", "")) ?: System.currentTimeMillis()  // Parse ISO 8601 date
                    )
                    Result.Success(owner)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to update owner profile")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Parse ISO 8601 date string to timestamp
     */
    private fun parseIso8601(dateString: String): Long? {
        return try {
            if (dateString.isEmpty()) return null
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time
        } catch (e: Exception) {
            android.util.Log.w("OwnerApi", "Failed to parse date: $dateString", e)
            null
        }
    }
    
    /**
     * Deactivate owner account
     */
    suspend fun deactivateOwner(nic: String, reason: String? = null): Result<Boolean> {
        return try {
            val body = JSONObject().apply {
                if (reason != null) {
                    put("reason", reason)
                }
            }
            
            val response = apiClient.post("/api/evowner/$nic/deactivate", body)
            
            if (response.optBoolean("success", false)) {
                Result.Success(true)
            } else {
                val message = response.optString("message", "Failed to deactivate account")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
