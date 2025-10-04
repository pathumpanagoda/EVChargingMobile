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
        private const val DATABASE_VERSION = 1
        
        // Table names
        const val TABLE_OWNERS = "owners"
        
        // Column names
        const val COLUMN_NIC = "nic"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ACTIVE = "active"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        
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
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_OWNERS)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database migrations here
        when (oldVersion) {
            1 -> {
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
