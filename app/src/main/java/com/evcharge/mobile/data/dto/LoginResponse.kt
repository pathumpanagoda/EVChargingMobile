package com.evcharge.mobile.data.dto

data class TokenData(
    val token: String,
    val role: String,
    val userId: String? = null,
    val nic: String? = null,
    val expiresAt: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val data: TokenData? = null,
    val error: String? = null,
    val message: String? = null
)