package com.evcharge.mobile.data.api

import com.evcharge.mobile.data.dto.Availability
import com.evcharge.mobile.data.dto.Slot
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class StationApi(private val http: okhttp3.OkHttpClient = ApiClient.client()) {
    fun getNearby(lat: Double, lng: Double, radiusKm: Double): String {
        val req = ApiClient.requestBuilder("/api/station/nearby?latitude=$lat&longitude=$lng&maxDistanceKm=$radiusKm").get().build()
        http.newCall(req).execute().use { r -> if (!r.isSuccessful) throw IllegalStateException(r.body?.string().orEmpty()); return r.body?.string().orEmpty() }
    }
    fun getAvailability(stationId: String, dateIso: String? = null): Availability {
        val q = if (dateIso.isNullOrBlank()) "" else "&date=$dateIso"
        val req: Request = ApiClient.requestBuilder("/api/station/availability?stationId=$stationId$q").get().build()
        http.newCall(req).execute().use { r ->
            val raw = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IllegalStateException(raw)
            val root = JSONObject(raw); val data = if (root.has("data")) root.getJSONObject("data") else root
            val st = data.optString("stationId", stationId); val date = data.optString("date", "")
            val ok = data.optBoolean("isAvailable", false); val arr = data.optJSONArray("nextSlots") ?: JSONArray()
            val slots = buildList { for (i in 0 until arr.length()) { val s = arr.getJSONObject(i); add(Slot(s.optString("start"), s.optString("end"))) } }
            return Availability(st, date, ok, slots)
        }
    }
}