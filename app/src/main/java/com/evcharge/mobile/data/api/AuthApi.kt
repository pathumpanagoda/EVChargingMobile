package com.evcharge.mobile.data.api

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.data.dto.LoginRequest
import com.evcharge.mobile.data.dto.LoginResponse
import com.evcharge.mobile.data.dto.RegisterRequest
import com.evcharge.mobile.data.dto.RegisterResponse
import org.json.JSONObject

/**
 * Authentication API service
 */
class AuthApi(private val apiClient: ApiClient) {
    
    /**
     * Login user
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val body = JSONObject().apply {
                put("Username", request.usernameOrNic)
                put("Password", request.password)
            }
            
            val response = apiClient.post("/api/auth/login", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val loginResponse = LoginResponse(
                        token = data.optString("token"),
                        expiresAt = data.optString("expiresAt"),
                        role = data.optString("role"),
                        userId = data.optString("userId"), // Backend returns 'userId' (NIC for EVOwner, user ID for system users)
                        nic = if (data.optString("role") == "EVOwner") data.optString("userId") else null, // For EVOwner, userId is the NIC
                        message = data.optString("message")
                    )
                    Result.Success(loginResponse)
                } else {
                    Result.Error(Exception("Invalid response format"))
                }
            } else {
                val message = response.optString("message", "Login failed")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Register new owner
     */
    suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val body = JSONObject().apply {
                put("NIC", request.nic)
                put("Name", request.name)
                put("Email", request.email)
                put("Phone", request.phone)
                put("Password", request.password)
            }
            
            val response = apiClient.post("/api/auth/register", body)
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                val loginData = if (data != null) {
                    LoginResponse(
                        token = data.optString("token"),
                        expiresAt = data.optString("expiresAt"),
                        role = data.optString("role"),
                        userId = data.optString("userId"), // Backend returns 'userId' (NIC for EVOwner, user ID for system users)
                        nic = if (data.optString("role") == "EVOwner") data.optString("userId") else null, // For EVOwner, userId is the NIC
                        message = data.optString("message")
                    )
                } else null
                
                val registerResponse = RegisterResponse(
                    success = true,
                    message = response.optString("message", "Registration successful"),
                    data = loginData
                )
                Result.Success(registerResponse)
            } else {
                val message = response.optString("message", "Registration failed")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Check if account is deactivated
     */
    suspend fun checkAccountStatus(nic: String): Result<Boolean> {
        return try {
            val response = apiClient.get("/api/evowner/$nic")
            
            if (response.optBoolean("success", false)) {
                val data = response.optJSONObject("data")
                val isActive = data?.optBoolean("active", true) ?: true
                Result.Success(isActive)
            } else {
                val message = response.optString("message", "Failed to check account status")
                Result.Error(Exception(message))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
