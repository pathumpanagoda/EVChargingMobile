package com.evcharge.mobile.data.dto

data class RegisterRequest(
    val nic: String,
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val address: String? = null
)
