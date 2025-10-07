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
                    // Save authentication data to preferences first
                    prefs.saveAuthData(
                        loginResponse.token,
                        loginResponse.role,
                        loginResponse.nic ?: ""
                    )
                    
                    // Extract name from JWT token and update preferences
                    val name = prefs.extractNameFromToken()
                    if (name.isNotEmpty()) {
                        prefs.saveAuthData(
                            loginResponse.token,
                            loginResponse.role,
                            loginResponse.nic ?: "",
                            name
                        )
                    }
                    
                    // If owner, save to local database
                    if (loginResponse.role == "Owner" && loginResponse.nic != null) {
                        saveOwnerToLocal(loginResponse.nic, loginResponse)
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
                    
                    // Save authentication data to preferences first
                    prefs.saveAuthData(
                        loginData.token,
                        loginData.role,
                        loginData.nic ?: ""
                    )
                    
                    // Extract name from JWT token and update preferences
                    val name = prefs.extractNameFromToken()
                    if (name.isNotEmpty()) {
                        prefs.saveAuthData(
                            loginData.token,
                            loginData.role,
                            loginData.nic ?: "",
                            name
                        )
                    }
                    
                    // Save owner to local database
                    if (loginData.nic != null) {
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
