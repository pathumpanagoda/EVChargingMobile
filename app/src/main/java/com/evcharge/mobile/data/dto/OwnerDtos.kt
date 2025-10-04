package com.evcharge.mobile.data.dto

/**
 * Data classes for EV Owner API requests and responses
 */

/**
 * Owner profile DTO
 */
data class OwnerProfile(
    val nic: String,
    val name: String,
    val email: String,
    val phone: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Owner update request DTO
 */
data class OwnerUpdateRequest(
    val name: String,
    val email: String,
    val phone: String
)

/**
 * Owner deactivate request DTO
 */
data class OwnerDeactivateRequest(
    val reason: String? = null
)

/**
 * Owner response DTO
 */
data class OwnerResponse(
    val success: Boolean,
    val message: String,
    val data: OwnerProfile? = null
)

/**
 * Owner list response DTO
 */
data class OwnerListResponse(
    val success: Boolean,
    val message: String,
    val data: List<OwnerProfile>? = null,
    val total: Int = 0
)
