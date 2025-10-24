package com.evcharge.mobile.data.db

import android.content.ContentValues
import android.database.Cursor
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingStatus
import com.evcharge.mobile.common.Datex

/**
 * Data Access Object for Booking operations using SQLite
 */
class BookingDao(private val dbHelper: UserDbHelper) {
    
    /**
     * Insert a new booking
     */
    fun insert(booking: Booking): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_BOOKING_ID, booking.id)
            put(UserDbHelper.COLUMN_OWNER_NIC, booking.ownerNic)
            put(UserDbHelper.COLUMN_STATION_ID, booking.stationId)
            put(UserDbHelper.COLUMN_STATION_NAME, booking.stationName)
            put(UserDbHelper.COLUMN_START_TIME, booking.startTime)
            put(UserDbHelper.COLUMN_END_TIME, booking.endTime)
            put(UserDbHelper.COLUMN_STATUS, booking.status.name)
            put(UserDbHelper.COLUMN_QR_CODE, booking.qrCode)
            put(UserDbHelper.COLUMN_CREATED_AT, booking.createdAt)
            put(UserDbHelper.COLUMN_UPDATED_AT, booking.updatedAt)
        }
        
        return try {
            val result = db.insert(UserDbHelper.TABLE_BOOKINGS, null, values)
            result != -1L
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update an existing booking
     */
    fun update(booking: Booking): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_OWNER_NIC, booking.ownerNic)
            put(UserDbHelper.COLUMN_STATION_ID, booking.stationId)
            put(UserDbHelper.COLUMN_STATION_NAME, booking.stationName)
            put(UserDbHelper.COLUMN_START_TIME, booking.startTime)
            put(UserDbHelper.COLUMN_END_TIME, booking.endTime)
            put(UserDbHelper.COLUMN_STATUS, booking.status.name)
            put(UserDbHelper.COLUMN_QR_CODE, booking.qrCode)
            put(UserDbHelper.COLUMN_UPDATED_AT, Datex.now())
        }
        
        return try {
            val result = db.update(
                UserDbHelper.TABLE_BOOKINGS,
                values,
                "${UserDbHelper.COLUMN_BOOKING_ID} = ?",
                arrayOf(booking.id)
            )
            result > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get booking by ID
     */
    fun getById(bookingId: String): Booking? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_BOOKINGS,
            null,
            "${UserDbHelper.COLUMN_BOOKING_ID} = ?",
            arrayOf(bookingId),
            null,
            null,
            null
        )
        
        return try {
            if (cursor.moveToFirst()) {
                cursorToBooking(cursor)
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
     * Get bookings by owner NIC
     */
    fun getByOwnerNic(ownerNic: String): List<Booking> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_BOOKINGS,
            null,
            "${UserDbHelper.COLUMN_OWNER_NIC} = ?",
            arrayOf(ownerNic),
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val bookings = mutableListOf<Booking>()
        return try {
            while (cursor.moveToNext()) {
                cursorToBooking(cursor)?.let { bookings.add(it) }
            }
            bookings
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get bookings by status
     */
    fun getByStatus(status: BookingStatus): List<Booking> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_BOOKINGS,
            null,
            "${UserDbHelper.COLUMN_STATUS} = ?",
            arrayOf(status.name),
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val bookings = mutableListOf<Booking>()
        return try {
            while (cursor.moveToNext()) {
                cursorToBooking(cursor)?.let { bookings.add(it) }
            }
            bookings
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get all bookings
     */
    fun getAll(): List<Booking> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_BOOKINGS,
            null,
            null,
            null,
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val bookings = mutableListOf<Booking>()
        return try {
            while (cursor.moveToNext()) {
                cursorToBooking(cursor)?.let { bookings.add(it) }
            }
            bookings
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Delete booking by ID
     */
    fun delete(bookingId: String): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            val result = db.delete(
                UserDbHelper.TABLE_BOOKINGS,
                "${UserDbHelper.COLUMN_BOOKING_ID} = ?",
                arrayOf(bookingId)
            )
            result > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get booking count
     */
    fun getCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${UserDbHelper.TABLE_BOOKINGS}", null)
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
     * Convert cursor to Booking
     */
    private fun cursorToBooking(cursor: Cursor): Booking? {
        return try {
            val statusString = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATUS))
            val status = when (statusString.uppercase()) {
                "PENDING" -> BookingStatus.PENDING
                "APPROVED" -> BookingStatus.APPROVED
                "COMPLETED" -> BookingStatus.COMPLETED
                "CANCELLED" -> BookingStatus.CANCELLED
                else -> BookingStatus.PENDING
            }
            
            Booking(
                id = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_BOOKING_ID)),
                ownerNic = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_OWNER_NIC)),
                stationId = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATION_ID)),
                stationName = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_STATION_NAME)),
                startTime = cursor.getLong(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_START_TIME)),
                endTime = cursor.getLong(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_END_TIME)),
                status = status,
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_UPDATED_AT)),
                qrCode = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_QR_CODE))
            )
        } catch (e: Exception) {
            null
        }
    }
}

