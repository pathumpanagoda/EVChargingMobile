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
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val owner = OwnerProfile(
                        nic = data.optString("NIC"),  // Backend returns NIC (capital)
                        name = data.optString("Name"),  // Backend returns Name (capital)
                        email = data.optString("Email"),  // Backend returns Email (capital)
                        phone = data.optString("Phone"),  // Backend returns Phone (capital)
                        active = data.optBoolean("IsActive", true),  // Backend returns IsActive (capital)
                        createdAt = data.optLong("CreatedAt", System.currentTimeMillis()),  // Backend returns CreatedAt (capital)
                        updatedAt = data.optLong("UpdatedAt", System.currentTimeMillis())  // Backend returns UpdatedAt (capital)
                    )
                    Result.Success(owner)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Failed to get owner profile")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
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
                    val owner = OwnerProfile(
                        nic = data.optString("NIC"),  // Backend returns NIC (capital)
                        name = data.optString("Name"),  // Backend returns Name (capital)
                        email = data.optString("Email"),  // Backend returns Email (capital)
                        phone = data.optString("Phone"),  // Backend returns Phone (capital)
                        active = data.optBoolean("IsActive", true),  // Backend returns IsActive (capital)
                        createdAt = data.optLong("CreatedAt", System.currentTimeMillis()),  // Backend returns CreatedAt (capital)
                        updatedAt = data.optLong("UpdatedAt", System.currentTimeMillis())  // Backend returns UpdatedAt (capital)
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
