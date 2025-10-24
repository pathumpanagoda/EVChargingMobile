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
        const val TABLE_BOOKINGS = "bookings"
        const val TABLE_STATIONS = "stations"
        
        // Owner column names
        const val COLUMN_NIC = "nic"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ACTIVE = "active"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        
        // Booking column names
        const val COLUMN_BOOKING_ID = "booking_id"
        const val COLUMN_OWNER_NIC = "owner_nic"
        const val COLUMN_STATION_ID = "station_id"
        const val COLUMN_STATION_NAME = "station_name"
        const val COLUMN_START_TIME = "start_time"
        const val COLUMN_END_TIME = "end_time"
        const val COLUMN_STATUS = "status"
        const val COLUMN_QR_CODE = "qr_code"
        
        // Station column names
        const val COLUMN_STATION_CUSTOM_ID = "custom_id"
        const val COLUMN_STATION_ADDRESS = "address"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_STATION_STATUS = "station_status"
        const val COLUMN_MAX_CAPACITY = "max_capacity"
        const val COLUMN_CURRENT_OCCUPANCY = "current_occupancy"
        const val COLUMN_CHARGING_RATE = "charging_rate"
        const val COLUMN_PRICE_PER_HOUR = "price_per_hour"
        
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
        
        private const val CREATE_TABLE_BOOKINGS = """
            CREATE TABLE $TABLE_BOOKINGS (
                $COLUMN_BOOKING_ID TEXT PRIMARY KEY,
                $COLUMN_OWNER_NIC TEXT NOT NULL,
                $COLUMN_STATION_ID TEXT NOT NULL,
                $COLUMN_STATION_NAME TEXT,
                $COLUMN_START_TIME INTEGER NOT NULL,
                $COLUMN_END_TIME INTEGER NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_QR_CODE TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_OWNER_NIC) REFERENCES $TABLE_OWNERS($COLUMN_NIC),
                FOREIGN KEY ($COLUMN_STATION_ID) REFERENCES $TABLE_STATIONS($COLUMN_STATION_ID)
            )
        """
        
        private const val CREATE_TABLE_STATIONS = """
            CREATE TABLE $TABLE_STATIONS (
                $COLUMN_STATION_ID TEXT PRIMARY KEY,
                $COLUMN_STATION_CUSTOM_ID TEXT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_STATION_ADDRESS TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_STATION_STATUS TEXT NOT NULL,
                $COLUMN_MAX_CAPACITY INTEGER NOT NULL DEFAULT 1,
                $COLUMN_CURRENT_OCCUPANCY INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CHARGING_RATE REAL NOT NULL DEFAULT 0.0,
                $COLUMN_PRICE_PER_HOUR REAL NOT NULL DEFAULT 0.0,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
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
                // Add new tables for version 2
                db.execSQL(CREATE_TABLE_STATIONS)
                db.execSQL(CREATE_TABLE_BOOKINGS)
            }
            2 -> {
                // Future migrations can be added here
                // Example: db.execSQL("ALTER TABLE $TABLE_OWNERS ADD COLUMN new_column TEXT")
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
        db.delete(TABLE_OWNERS, null, null)
        // Note: Don't close database here - let SQLiteOpenHelper manage connections
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
    
    /**
     * Test database connection
     */
    fun testConnection(): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT 1", null)
            val result = cursor.moveToFirst()
            cursor.close()
            result
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all table names in the database
     */
    fun getAllTableNames(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
            null
        )
        
        val tables = mutableListOf<String>()
        return try {
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            tables
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get table info (columns) for a specific table
     */
    fun getTableInfo(tableName: String): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        
        val columns = mutableListOf<String>()
        return try {
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(1) // Column name is at index 1
                columns.add(columnName)
            }
            columns
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
}
