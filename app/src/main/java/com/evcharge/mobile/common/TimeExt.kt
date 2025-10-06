package com.evcharge.mobile.common

import java.text.SimpleDateFormat
import java.util.*

object TimeExt {
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    fun millisToIsoUTC(millis: Long): String = iso.format(Date(millis))
    fun isoToMillisOrNull(s: String?): Long? = try { s?.let { iso.parse(it)?.time } } catch (_: Exception) { null }
}