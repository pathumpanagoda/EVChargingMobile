package com.evcharge.mobile.data.db

import android.content.ContentValues
import android.database.Cursor
import com.evcharge.mobile.data.dto.OwnerProfile
import com.evcharge.mobile.common.Datex

/**
 * Data Access Object for Owner operations using SQLite
 */
class OwnerDao(private val dbHelper: UserDbHelper) {
    
    /**
     * Insert a new owner profile
     */
    fun insert(owner: OwnerProfile): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_NIC, owner.nic)
            put(UserDbHelper.COLUMN_NAME, owner.name)
            put(UserDbHelper.COLUMN_EMAIL, owner.email)
            put(UserDbHelper.COLUMN_PHONE, owner.phone)
            put(UserDbHelper.COLUMN_ACTIVE, if (owner.active) 1 else 0)
            put(UserDbHelper.COLUMN_CREATED_AT, owner.createdAt)
            put(UserDbHelper.COLUMN_UPDATED_AT, owner.updatedAt)
        }
        
        return try {
            val result = db.insert(UserDbHelper.TABLE_OWNERS, null, values)
            result != -1L
        } catch (e: Exception) {
            false
        }
        // Note: Don't close database here - let SQLiteOpenHelper manage connections
    }
    
    /**
     * Update an existing owner profile
     */
    fun update(owner: OwnerProfile): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_NAME, owner.name)
            put(UserDbHelper.COLUMN_EMAIL, owner.email)
            put(UserDbHelper.COLUMN_PHONE, owner.phone)
            put(UserDbHelper.COLUMN_ACTIVE, if (owner.active) 1 else 0)
            put(UserDbHelper.COLUMN_UPDATED_AT, Datex.now())
        }
        
        return try {
            val result = db.update(
                UserDbHelper.TABLE_OWNERS,
                values,
                "${UserDbHelper.COLUMN_NIC} = ?",
                arrayOf(owner.nic)
            )
            result > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Deactivate an owner (set active = 0)
     */
    fun deactivate(nic: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(UserDbHelper.COLUMN_ACTIVE, 0)
            put(UserDbHelper.COLUMN_UPDATED_AT, Datex.now())
        }
        
        return try {
            val result = db.update(
                UserDbHelper.TABLE_OWNERS,
                values,
                "${UserDbHelper.COLUMN_NIC} = ?",
                arrayOf(nic)
            )
            result > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get owner by NIC
     */
    fun getByNic(nic: String): OwnerProfile? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_OWNERS,
            null,
            "${UserDbHelper.COLUMN_NIC} = ?",
            arrayOf(nic),
            null,
            null,
            null
        )
        
        return try {
            if (cursor.moveToFirst()) {
                cursorToOwner(cursor)
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
     * Get all owners
     */
    fun getAll(): List<OwnerProfile> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_OWNERS,
            null,
            null,
            null,
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val owners = mutableListOf<OwnerProfile>()
        return try {
            while (cursor.moveToNext()) {
                cursorToOwner(cursor)?.let { owners.add(it) }
            }
            owners
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Get active owners only
     */
    fun getActiveOwners(): List<OwnerProfile> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_OWNERS,
            null,
            "${UserDbHelper.COLUMN_ACTIVE} = ?",
            arrayOf("1"),
            null,
            null,
            "${UserDbHelper.COLUMN_CREATED_AT} DESC"
        )
        
        val owners = mutableListOf<OwnerProfile>()
        return try {
            while (cursor.moveToNext()) {
                cursorToOwner(cursor)?.let { owners.add(it) }
            }
            owners
        } catch (e: Exception) {
            emptyList()
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Check if owner exists
     */
    fun exists(nic: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            UserDbHelper.TABLE_OWNERS,
            arrayOf(UserDbHelper.COLUMN_NIC),
            "${UserDbHelper.COLUMN_NIC} = ?",
            arrayOf(nic),
            null,
            null,
            null,
            "1"
        )
        
        return try {
            cursor.count > 0
        } finally {
            cursor.close()
        }
    }
    
    /**
     * Delete owner by NIC
     */
    fun delete(nic: String): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            val result = db.delete(
                UserDbHelper.TABLE_OWNERS,
                "${UserDbHelper.COLUMN_NIC} = ?",
                arrayOf(nic)
            )
            result > 0
        } catch (e: Exception) {
            false
        } finally {
        }
    }
    
    /**
     * Get owner count
     */
    fun getCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${UserDbHelper.TABLE_OWNERS}", null)
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
     * Convert cursor to OwnerProfile
     */
    private fun cursorToOwner(cursor: Cursor): OwnerProfile? {
        return try {
            OwnerProfile(
                nic = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_NIC)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_PHONE)),
                active = cursor.getInt(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_ACTIVE)) == 1,
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(UserDbHelper.COLUMN_UPDATED_AT))
            )
        } catch (e: Exception) {
            null
        }
    }
}
