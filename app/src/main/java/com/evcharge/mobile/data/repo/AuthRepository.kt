package com.evcharge.mobile.data.repo

import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Result
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.AuthApi
import com.evcharge.mobile.data.db.OwnerDao
import com.evcharge.mobile.data.dto.LoginRequest
import com.evcharge.mobile.data.dto.LoginResponse
import com.evcharge.mobile.data.dto.OwnerProfile
import com.evcharge.mobile.data.dto.RegisterRequest
import com.evcharge.mobile.data.dto.RegisterResponse

/**
 * Repository for authentication operations
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val ownerDao: OwnerDao,
    private val prefs: Prefs
) {
    
    /**
     * Login user
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val result = authApi.login(request)
            
            if (result.isSuccess()) {
                val loginResponse = result.getDataOrNull()
                if (loginResponse != null) {
                    // Save authentication data to preferences
                    // For EVOwner, userId is the NIC; for system users, userId is the user ID
                    val nicToSave = if (loginResponse.role == "EVOwner") loginResponse.userId else loginResponse.nic ?: ""
                    prefs.saveAuthData(
                        loginResponse.token,
                        loginResponse.role,
                        nicToSave
                    )
                    
                    // If EVOwner, save to local database
                    if (loginResponse.role == "EVOwner" && loginResponse.userId.isNotEmpty()) {
                        saveOwnerToLocal(loginResponse.userId, loginResponse)
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Login user with selected role (for dual role login)
     */
    suspend fun loginWithRole(request: LoginRequest, selectedRole: String): Result<LoginResponse> {
        return try {
            val result = authApi.login(request)
            
            if (result.isSuccess()) {
                val loginResponse = result.getDataOrNull()
                if (loginResponse != null) {
                    // Save authentication data with selected role
                    val nicToSave = if (loginResponse.role == "EVOwner") loginResponse.userId else loginResponse.nic ?: ""
                    prefs.saveAuthDataWithRole(
                        loginResponse.token,
                        loginResponse.role,
                        nicToSave,
                        selectedRole
                    )
                    
                    // If EVOwner, save to local database
                    if (loginResponse.role == "EVOwner" && loginResponse.userId.isNotEmpty()) {
                        saveOwnerToLocal(loginResponse.userId, loginResponse)
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Register new owner
     */
    suspend fun registerOwner(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val result = authApi.register(request)
            
            if (result.isSuccess()) {
                val registerResponse = result.getDataOrNull()
                if (registerResponse?.data != null) {
                    val loginData = registerResponse.data
                    
                    // Save authentication data to preferences
                    // For EVOwner registration, userId is the NIC
                    val nicToSave = if (loginData.role == "EVOwner") loginData.userId else loginData.nic ?: ""
                    prefs.saveAuthData(
                        loginData.token,
                        loginData.role,
                        nicToSave
                    )
                    
                    // Save owner to local database
                    if (loginData.role == "EVOwner" && loginData.userId.isNotEmpty()) {
                        val ownerProfile = OwnerProfile(
                            nic = request.nic,
                            name = request.name,
                            email = request.email,
                            phone = request.phone,
                            active = true
                        )
                        ownerDao.insert(ownerProfile)
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Check if account is deactivated
     */
    suspend fun checkAccountStatus(nic: String): Result<Boolean> {
        return authApi.checkAccountStatus(nic)
    }
    
    /**
     * Logout user
     */
    fun logout() {
        prefs.clear()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = prefs.isLoggedIn()
    
    /**
     * Get current user role
     */
    fun getCurrentRole(): String = prefs.getRole()
    
    /**
     * Get current user NIC
     */
    fun getCurrentNIC(): String = prefs.getNIC()
    
    /**
     * Check if current user is owner
     */
    fun isOwner(): Boolean = prefs.isOwner()
    
    /**
     * Check if current user is operator
     */
    fun isOperator(): Boolean = prefs.isOperator()
    
    /**
     * Check if current user is station operator
     */
    fun isStationOperator(): Boolean = prefs.isStationOperator()
    
    /**
     * Check if current user is backoffice admin
     */
    fun isBackoffice(): Boolean = prefs.isBackoffice()
    
    /**
     * Get current user ID (NIC for EVOwner, user ID for system users)
     */
    fun getCurrentUserId(): String = prefs.getNIC() // This stores the userId from backend
    
    /**
     * Check if current user is EVOwner with dual access
     */
    fun isEVOwnerWithDualAccess(): Boolean = prefs.isEVOwnerWithDualAccess()
    
    /**
     * Check if user is currently logged in as Owner (for dual role)
     */
    fun isLoggedInAsOwner(): Boolean = prefs.isLoggedInAsOwner()
    
    /**
     * Check if user is currently logged in as Operator (for dual role)
     */
    fun isLoggedInAsOperator(): Boolean = prefs.isLoggedInAsOperator()
    
    /**
     * Get effective role (selected role for EVOwners, actual role for others)
     */
    fun getEffectiveRole(): String = prefs.getEffectiveRole()
    
    /**
     * Switch role for EVOwner (between Owner and Operator)
     */
    fun switchRole(newRole: String) {
        if (isEVOwnerWithDualAccess()) {
            prefs.saveAuthDataWithRole(
                prefs.getToken(),
                prefs.getRole(),
                prefs.getNIC(),
                newRole
            )
        }
    }
    
    /**
     * Get owner from local database
     */
    fun getLocalOwner(nic: String): OwnerProfile? = ownerDao.getByNic(nic)
    
    /**
     * Save owner to local database after login
     */
    private suspend fun saveOwnerToLocal(nic: String, loginResponse: LoginResponse) {
        try {
            // Check if owner already exists in local DB
            val existingOwner = ownerDao.getByNic(nic)
            if (existingOwner == null) {
                // Create a basic owner profile for local storage
                // Full profile will be fetched from server when needed
                val ownerProfile = OwnerProfile(
                    nic = nic,
                    name = "", // Will be updated when profile is fetched
                    email = "",
                    phone = "",
                    active = true
                )
                ownerDao.insert(ownerProfile)
            }
        } catch (e: Exception) {
            // Log error but don't fail login
            android.util.Log.e("AuthRepository", "Failed to save owner to local DB", e)
        }
    }
}
