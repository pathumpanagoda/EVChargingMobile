package com.evcharge.mobile.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.evcharge.mobile.data.dto.OwnerProfile

/**
 * SQLite database helper for EV Charging Mobile app
 */
class UserDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    
    companion object {
        private const val DATABASE_NAME = "ev_charging.db"
        private const val DATABASE_VERSION = 2
        
        // Table names
        const val TABLE_OWNERS = "owners"
        const val TABLE_STATIONS = "stations"
        const val TABLE_BOOKINGS = "bookings"
        
        // Owner table column names
        const val COLUMN_NIC = "nic"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ACTIVE = "active"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        
        // Station table column names
        const val COLUMN_STATION_ID = "station_id"
        const val COLUMN_STATION_CUSTOM_ID = "station_custom_id"
        const val COLUMN_STATION_ADDRESS = "station_address"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_STATION_STATUS = "station_status"
        const val COLUMN_MAX_CAPACITY = "max_capacity"
        const val COLUMN_CURRENT_OCCUPANCY = "current_occupancy"
        const val COLUMN_CHARGING_RATE = "charging_rate"
        const val COLUMN_PRICE_PER_HOUR = "price_per_hour"
        
        // Booking table column names
        const val COLUMN_BOOKING_ID = "booking_id"
        const val COLUMN_EV_OWNER_NIC = "ev_owner_nic"
        const val COLUMN_RESERVATION_DATETIME = "reservation_datetime"
        const val COLUMN_DURATION_HOURS = "duration_hours"
        const val COLUMN_STATUS = "status"
        const val COLUMN_QR_CODE = "qr_code"
        const val COLUMN_CREATED_AT_BOOKING = "created_at_booking"
        
        // Create table SQL
        private const val CREATE_TABLE_OWNERS = """
            CREATE TABLE $TABLE_OWNERS (
                $COLUMN_NIC TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_ACTIVE INTEGER NOT NULL DEFAULT 1,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
        """
        
        private const val CREATE_TABLE_STATIONS = """
            CREATE TABLE $TABLE_STATIONS (
                $COLUMN_STATION_ID TEXT PRIMARY KEY,
                $COLUMN_STATION_CUSTOM_ID TEXT NOT NULL,
                $COLUMN_STATION_ADDRESS TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_STATION_STATUS TEXT NOT NULL DEFAULT 'active',
                $COLUMN_MAX_CAPACITY INTEGER NOT NULL DEFAULT 1,
                $COLUMN_CURRENT_OCCUPANCY INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CHARGING_RATE REAL NOT NULL DEFAULT 0.0,
                $COLUMN_PRICE_PER_HOUR REAL NOT NULL DEFAULT 0.0
            )
        """
        
        private const val CREATE_TABLE_BOOKINGS = """
            CREATE TABLE $TABLE_BOOKINGS (
                $COLUMN_BOOKING_ID TEXT PRIMARY KEY,
                $COLUMN_EV_OWNER_NIC TEXT NOT NULL,
                $COLUMN_STATION_ID TEXT NOT NULL,
                $COLUMN_RESERVATION_DATETIME INTEGER NOT NULL,
                $COLUMN_DURATION_HOURS REAL NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL DEFAULT 'pending',
                $COLUMN_QR_CODE TEXT,
                $COLUMN_CREATED_AT_BOOKING INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_EV_OWNER_NIC) REFERENCES $TABLE_OWNERS($COLUMN_NIC),
                FOREIGN KEY ($COLUMN_STATION_ID) REFERENCES $TABLE_STATIONS($COLUMN_STATION_ID)
            )
        """
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_OWNERS)
        db.execSQL(CREATE_TABLE_STATIONS)
        db.execSQL(CREATE_TABLE_BOOKINGS)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database migrations here
        when (oldVersion) {
            1 -> {
                // Drop existing tables and recreate with new schema
                db.execSQL("DROP TABLE IF EXISTS $TABLE_OWNERS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_STATIONS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
                
                // Create new tables with updated schema
                db.execSQL(CREATE_TABLE_OWNERS)
                db.execSQL(CREATE_TABLE_STATIONS)
                db.execSQL(CREATE_TABLE_BOOKINGS)
            }
        }
    }
    
    /**
     * Get writable database instance
     */
    fun getWritableDb(): SQLiteDatabase = writableDatabase
    
    /**
     * Get readable database instance
     */
    fun getReadableDb(): SQLiteDatabase = readableDatabase
    
    /**
     * Clear all data (for testing purposes)
     */
    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_BOOKINGS, null, null)
        db.delete(TABLE_STATIONS, null, null)
        db.delete(TABLE_OWNERS, null, null)
        db.close()
    }
    
    /**
     * Check if table exists
     */
    fun tableExists(tableName: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
    
    /**
     * Get table row count
     */
    fun getRowCount(tableName: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
}
