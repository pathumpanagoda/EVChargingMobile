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
                        nic = data.optString("nic"),
                        name = data.optString("name"),
                        email = data.optString("email"),
                        phone = data.optString("phone"),
                        active = data.optBoolean("active", true),
                        createdAt = data.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = data.optLong("updatedAt", System.currentTimeMillis())
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
                put("name", request.name)
                put("email", request.email)
                put("phone", request.phone)
            }
            
            val response = apiClient.put("/api/evowner/$nic", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val owner = OwnerProfile(
                        nic = data.optString("nic"),
                        name = data.optString("name"),
                        email = data.optString("email"),
                        phone = data.optString("phone"),
                        active = data.optBoolean("active", true),
                        createdAt = data.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = data.optLong("updatedAt", System.currentTimeMillis())
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
