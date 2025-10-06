package com.evcharge.mobile.data.api

import com.evcharge.mobile.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TO = 30L
    private val client by lazy {
        OkHttpClient.Builder()
            .callTimeout(TO, TimeUnit.SECONDS)
            .connectTimeout(TO, TimeUnit.SECONDS)
            .readTimeout(TO, TimeUnit.SECONDS)
            .writeTimeout(TO, TimeUnit.SECONDS)
            .build()
    }
    fun client(): OkHttpClient = client
    fun endpoint(path: String) = if (path.startsWith("/")) BuildConfig.BASE_URL + path else BuildConfig.BASE_URL + "/" + path
    fun requestBuilder(path: String) = Request.Builder().url(endpoint(path))
}