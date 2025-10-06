package com.evcharge.mobile.data.api

import com.evcharge.mobile.common.AppResult
import com.evcharge.mobile.data.dto.OwnerProfile
import com.evcharge.mobile.data.dto.OwnerUpdateRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import org.json.JSONObject

class OwnerApi(private val http: okhttp3.OkHttpClient = ApiClient.client()) {
    private val json = "application/json; charset=utf-8".toMediaType()
    
    suspend fun getOwner(nic: String): AppResult<OwnerProfile> {
        return try {
            val req = ApiClient.requestBuilder("/api/evowner/$nic")
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
                        val owner = parseOwner(data)
                        AppResult.Ok(owner)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to get owner")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun updateOwner(nic: String, request: OwnerUpdateRequest): AppResult<OwnerProfile> {
        return try {
            val body = JSONObject().apply {
                request.name?.let { put("name", it) }
                request.email?.let { put("email", it) }
                request.phone?.let { put("phone", it) }
                request.address?.let { put("address", it) }
            }
            
            val req = ApiClient.requestBuilder("/api/evowner/$nic")
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
                        val owner = parseOwner(data)
                        AppResult.Ok(owner)
                    } else {
                        AppResult.Err(Exception("Invalid response format"))
                    }
                } else {
                    val message = obj.optString("message", "Failed to update owner")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    suspend fun deactivateOwner(nic: String, reason: String? = null): AppResult<Boolean> {
        return try {
            val body = JSONObject().apply {
                reason?.let { put("reason", it) }
            }
            
            val req = ApiClient.requestBuilder("/api/evowner/$nic/deactivate")
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
                    AppResult.Ok(true)
                } else {
                    val message = obj.optString("message", "Failed to deactivate owner")
                    AppResult.Err(Exception(message))
                }
            }
        } catch (e: Exception) {
            AppResult.Err(e)
        }
    }
    
    private fun parseOwner(data: JSONObject): OwnerProfile {
        return OwnerProfile(
            nic = data.optString("nic", ""),
            name = data.optString("name", ""),
            email = data.optString("email", ""),
            phone = data.optString("phone", null),
            address = data.optString("address", null),
            isActive = data.optBoolean("isActive", true),
            createdAt = data.optString("createdAt", ""),
            updatedAt = data.optString("updatedAt", "")
        )
    }
}