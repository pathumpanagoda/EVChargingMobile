package com.evcharge.mobile.data.repo

import com.evcharge.mobile.common.AppResult
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.Availability

class StationRepository(private val api: StationApi = StationApi()) {
    fun nearby(lat: Double, lng: Double, radiusKm: Double): AppResult<String> =
        try { AppResult.Ok(api.getNearby(lat,lng,radiusKm)) } catch (e:Throwable){ AppResult.Err(e) }

    fun checkStationAvailability(stationId: String, dateIso: String? = null): AppResult<Availability> =
        try { AppResult.Ok(api.getAvailability(stationId, dateIso)) } catch (e:Throwable){ AppResult.Err(e) }
    
    fun getAvailableStations(): AppResult<List<Any>> {
        // TODO: Implement when backend has this endpoint
        return AppResult.Ok(emptyList())
    }
    
    fun getNearbyStations(lat: Double, lng: Double, radiusKm: Double): AppResult<String> =
        nearby(lat, lng, radiusKm)
    
    fun getAllStations(): AppResult<String> {
        // TODO: Implement when backend has this endpoint
        return AppResult.Ok("[]")
    }
}