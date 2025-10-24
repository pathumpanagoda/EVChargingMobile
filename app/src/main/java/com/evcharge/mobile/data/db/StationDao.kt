package com.evcharge.mobile.data.db

import android.content.ContentValues
import android.database.Cursor
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.dto.StationStatus
import com.evcharge.mobile.common.Datex

/**
 * Data Access Object for Station operations using SQLite
 */
class StationDao(private val dbHelper: UserDbHelper) {
    
    /**
     * Insert a new station
     */
    fun insert(station: Station): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_STATION_ID, station.id)
            put(UserDbHelper.COLUMN_STATION_CUSTOM_ID, station.customId)
            put(UserDbHelper.COLUMN_NAME, station.name)
            put(UserDbHelper.COLUMN_STATION_ADDRESS, station.address)
            put(UserDbHelper.COLUMN_LATITUDE, station.latitude)
            put(UserDbHelper.COLUMN_LONGITUDE, station.longitude)
            put(UserDbHelper.COLUMN_STATION_STATUS, station.status.name)
            put(UserDbHelper.COLUMN_MAX_CAPACITY, station.maxCapacity)
            put(UserDbHelper.COLUMN_CURRENT_OCCUPANCY, station.currentOccupancy)
            put(UserDbHelper.COLUMN_CHARGING_RATE, station.chargingRate)
            put(UserDbHelper.COLUMN_PRICE_PER_HOUR, station.pricePerHour)
            put(UserDbHelper.COLUMN_CREATED_AT, Datex.now())
            put(UserDbHelper.COLUMN_UPDATED_AT, Datex.now())
        }
        
        return try {
            val result = db.insert(UserDbHelper.TABLE_STATIONS, null, values)
            result != -1L
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update an existing station
     */
    fun update(station: Station): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_STATION_CUSTOM_ID, station.customId)
            put(UserDbHelper.COLUMN_NAME, station.name)
            put(UserDbHelper.COLUMN_STATION_ADDRESS, station.address)
            put(UserDbHelper.COLUMN_LATITUDE, station.latitude)
            put(UserDbHelper.COLUMN_LONGITUDE, station.longitude)
            put(UserDbHelper.COLUMN_STATION_STATUS, station.status.name)
            put(UserDbHelper.COLUMN_MAX_CAPACITY, station.maxCapacity)
            put(UserDbHelper.COLUMN_CURRENT_OCCUPANCY, station.currentOccupancy)
            put(UserDbHelper.COLUMN_CHARGING_RATE, station.chargingRate)
            put(UserDbHelper.COLUMN_PRICE_PER_HOUR, station.pricePerHour)
            put(UserDbHelper.COLUMN_UPDATED_AT, Datex.now())
        }
        
        return try {
            val result = db.update(
                UserDbHelper.TABLE_STATIONS,
                values,
                "${UserDbHelper.COLUMN_STATION_ID} = ?",
                arrayOf(station.id)
            )
            result > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get station by ID
     */
    fun getById(stationId: String): Station? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_STATIONS,
            null,
            "${UserDbHelper.COLUMN_STATION_ID} = ?",
            arrayOf(stationId),
            null,
            null,
            null
        )
        
        return try {
            if (cursor.moveToFirst()) {
                cursorToStation(cursor)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get all stations
     */
    fun getAll(): List<Station> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_STATIONS,
            null,
            null,
            null,
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val stations = mutableListOf<Station>()
        return try {
            while (cursor.moveToNext()) {
                cursorToStation(cursor)?.let { stations.add(it) }
            }
            stations
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get stations by status
     */
    fun getByStatus(status: StationStatus): List<Station> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_STATIONS,
            null,
            "${UserDbHelper.COLUMN_STATION_STATUS} = ?",
            arrayOf(status.name),
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val stations = mutableListOf<Station>()
        return try {
            while (cursor.moveToNext()) {
                cursorToStation(cursor)?.let { stations.add(it) }
            }
            stations
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get nearby stations (within radius)
     */
    fun getNearby(latitude: Double, longitude: Double, radiusKm: Double = 10.0): List<Station> {
        val db = dbHelper.readableDatabase
        // Simple distance calculation (not precise, but good enough for local storage)
        val cursor = db.rawQuery("""
            SELECT * FROM ${UserDbHelper.TABLE_STATIONS} 
            WHERE (
                ((${UserDbHelper.COLUMN_LATITUDE} - ?) * (${UserDbHelper.COLUMN_LATITUDE} - ?)) + 
                ((${UserDbHelper.COLUMN_LONGITUDE} - ?) * (${UserDbHelper.COLUMN_LONGITUDE} - ?))
            ) <= ?
            ORDER BY (
                ((${UserDbHelper.COLUMN_LATITUDE} - ?) * (${UserDbHelper.COLUMN_LATITUDE} - ?)) + 
                ((${UserDbHelper.COLUMN_LONGITUDE} - ?) * (${UserDbHelper.COLUMN_LONGITUDE} - ?))
            )
        """.trimIndent(), arrayOf(
            latitude.toString(), latitude.toString(),
            longitude.toString(), longitude.toString(),
            (radiusKm * radiusKm).toString(),
            latitude.toString(), latitude.toString(),
            longitude.toString(), longitude.toString()
        ))
        
        val stations = mutableListOf<Station>()
        return try {
            while (cursor.moveToNext()) {
                cursorToStation(cursor)?.let { stations.add(it) }
            }
            stations
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Delete station by ID
     */
    fun delete(stationId: String): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            val result = db.delete(
                UserDbHelper.TABLE_STATIONS,
                "${UserDbHelper.COLUMN_STATION_ID} = ?",
                arrayOf(stationId)
            )
            result > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get station count
     */
    fun getCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${UserDbHelper.TABLE_STATIONS}", null)
        return try {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Convert cursor to Station
     */
    private fun cursorToStation(cursor: Cursor): Station? {
        return try {
            val statusString = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATION_STATUS))
            val status = when (statusString.uppercase()) {
                "AVAILABLE" -> StationStatus.AVAILABLE
                "OCCUPIED" -> StationStatus.OCCUPIED
                "MAINTENANCE" -> StationStatus.MAINTENANCE
                "OFFLINE" -> StationStatus.OFFLINE
                else -> StationStatus.OFFLINE
            }
            
            Station(
                id = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATION_ID)),
                customId = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATION_CUSTOM_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_NAME)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATION_ADDRESS)),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_LATITUDE)),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_LONGITUDE)),
                status = status,
                maxCapacity = cursor.getInt(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_MAX_CAPACITY)),
                currentOccupancy = cursor.getInt(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_CURRENT_OCCUPANCY)),
                chargingRate = cursor.getDouble(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_CHARGING_RATE)),
                pricePerHour = cursor.getDouble(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_PRICE_PER_HOUR)),
                amenities = emptyList(), // Not stored in local DB
                schedule = emptyList() // Not stored in local DB
            )
        } catch (e: Exception) {
            null
        }
    }
}

