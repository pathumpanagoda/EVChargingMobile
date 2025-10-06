package com.evcharge.mobile.data.api

import com.evcharge.mobile.data.dto.LoginResponse
import com.evcharge.mobile.data.dto.TokenData
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import org.json.JSONObject

class AuthApi(private val http: okhttp3.OkHttpClient = ApiClient.client()) {
    private val json = "application/json; charset=utf-8".toMediaType()

    fun login(username: String, password: String): LoginResponse {
        val body = JSONObject().put("username", username).put("password", password).toString()
        val req = ApiClient.requestBuilder("/api/auth/login")
            .post(RequestBody.create(json, body))
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val obj = runCatching { JSONObject(raw) }.getOrNull()

            if (!resp.isSuccessful) {
                val err = obj?.optString("error") ?: obj?.optString("message") ?: raw
                return LoginResponse(false, null, err, err)
            }
            val data = when {
                obj == null -> null
                obj.has("data") -> obj.getJSONObject("data")
                else -> obj
            }
            val token = data?.optString("token").orEmpty()
            val role = data?.optString("role").orEmpty()
            val userId = data?.optString("userId", null)
            val nic = data?.optString("nic", null)
            val expiresAt = data?.optString("expiresAt", null)
            if (token.isBlank() || role.isBlank()) {
                val err = obj?.optString("error") ?: "Invalid login response"
                return LoginResponse(false, null, err, err)
            }
            return LoginResponse(true, TokenData(token, role, userId, nic, expiresAt), null, null)
        }
    }
}