package com.evcharge.mobile.net

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object HealthCheck {
    fun isUp(client: OkHttpClient, baseUrl: String): Boolean {
        fun ping(path: String): Boolean {
            val url = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) + path else baseUrl + path
            val req = Request.Builder().url(url).get().build()
            return try {
                client.newCall(req).execute().use { it.isSuccessful }.also {
                    Log.d("HealthCheck", "GET $url -> $it")
                }
            } catch (e: Exception) {
                Log.e("HealthCheck", "GET $url failed", e)
                false
            }
        }

        // Primary: /health (newly added on backend)
        if (ping("/health")) return true
        // Fallbacks (public endpoints from audit)
        if (ping("/WeatherForecast")) return true
        if (ping("/api/chargingstation")) return true
        return false
    }

    fun defaultClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
}

