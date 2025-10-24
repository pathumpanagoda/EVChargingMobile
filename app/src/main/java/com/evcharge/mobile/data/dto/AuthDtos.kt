package com.evcharge.mobile.data.dto

/**
 * Data classes for authentication API requests and responses
 */

/**
 * Login request DTO
 */
data class LoginRequest(
    val usernameOrNic: String,
    val password: String
)

/**
 * Register request DTO
 */
data class RegisterRequest(
    val nic: String,
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)

/**
 * Login response DTO - matches backend AuthResponse structure
 */
data class LoginResponse(
    val token: String,
    val expiresAt: String? = null,
    val role: String,
    val userId: String, // This is the NIC for EVOwner or user ID for system users
    val nic: String? = null, // Additional NIC field for EVOwner
    val message: String? = null
)

/**
 * Register response DTO
 */
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: LoginResponse? = null
)

/**
 * Auth error response DTO
 */
data class AuthErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null
)
