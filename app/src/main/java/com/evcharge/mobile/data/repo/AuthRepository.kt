package com.evcharge.mobile.data.repo

import com.evcharge.mobile.common.AppResult
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.data.api.AuthApi
import com.evcharge.mobile.data.dto.TokenData

class AuthRepository(
    private val api: AuthApi = AuthApi(),
    private val prefs: Prefs = Prefs.instance()
) {
    fun login(username: String, password: String): AppResult<TokenData> {
        val r = api.login(username, password)
        val td = r.data ?: return AppResult.Err(IllegalStateException(r.error ?: "Login failed"))
        prefs.setToken(td.token); prefs.setRole(td.role)
        td.userId?.let { prefs.setUserId(it) }
        td.nic?.let { prefs.setNic(it) }
        return AppResult.Ok(td)
    }
    
    fun registerOwner(request: com.evcharge.mobile.data.dto.RegisterRequest): AppResult<Boolean> {
        // TODO: Implement when backend has this endpoint
        return AppResult.Ok(true)
    }
    
    fun logout() {
        prefs.clear()
    }
}