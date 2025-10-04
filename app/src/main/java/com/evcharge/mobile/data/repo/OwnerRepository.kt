package com.evcharge.mobile.data.repo

import com.evcharge.mobile.common.Result
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.OwnerApi
import com.evcharge.mobile.data.db.OwnerDao
import com.evcharge.mobile.data.dto.OwnerProfile
import com.evcharge.mobile.data.dto.OwnerUpdateRequest

/**
 * Repository for EV Owner operations
 */
class OwnerRepository(
    private val ownerApi: OwnerApi,
    private val ownerDao: OwnerDao
) {
    
    /**
     * Get owner profile from server
     */
    suspend fun getOwner(nic: String): Result<OwnerProfile> {
        return try {
            val result = ownerApi.getOwner(nic)
            
            // If successful, update local database
            if (result.isSuccess()) {
                val owner = result.getDataOrNull()
                if (owner != null) {
                    ownerDao.update(owner)
                }
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get owner profile from local database
     */
    fun getLocalOwner(nic: String): OwnerProfile? = ownerDao.getByNic(nic)
    
    /**
     * Update owner profile
     */
    suspend fun updateOwner(nic: String, request: OwnerUpdateRequest): Result<OwnerProfile> {
        return try {
            val result = ownerApi.updateOwner(nic, request)
            
            // If successful, update local database
            if (result.isSuccess()) {
                val owner = result.getDataOrNull()
                if (owner != null) {
                    ownerDao.update(owner)
                }
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Deactivate owner account
     */
    suspend fun deactivateOwner(nic: String, reason: String? = null): Result<Boolean> {
        return try {
            val result = ownerApi.deactivateOwner(nic, reason)
            
            // If successful, update local database
            if (result.isSuccess()) {
                ownerDao.deactivate(nic)
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get all local owners
     */
    fun getAllLocalOwners(): List<OwnerProfile> = ownerDao.getAll()
    
    /**
     * Get active local owners
     */
    fun getActiveLocalOwners(): List<OwnerProfile> = ownerDao.getActiveOwners()
    
    /**
     * Check if owner exists locally
     */
    fun ownerExists(nic: String): Boolean = ownerDao.exists(nic)
    
    /**
     * Get owner count
     */
    fun getOwnerCount(): Int = ownerDao.getCount()
    
    /**
     * Delete owner from local database
     */
    fun deleteLocalOwner(nic: String): Boolean = ownerDao.delete(nic)
}
