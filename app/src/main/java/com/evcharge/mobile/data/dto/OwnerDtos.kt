package com.evcharge.mobile.data.dto

data class OwnerProfile(
    val nic: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

data class OwnerUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)
