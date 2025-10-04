package com.evcharge.mobile.common

import java.text.SimpleDateFormat
import java.util.*

/**
 * Date and time utility functions
 */
object Datex {
    
    private const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val TIME_FORMAT = "HH:mm"
    private const val DISPLAY_DATE_TIME_FORMAT = "MMM dd, yyyy HH:mm"
    private const val DISPLAY_DATE_FORMAT = "MMM dd, yyyy"
    private const val DISPLAY_TIME_FORMAT = "HH:mm"
    
    private val dateTimeFormatter = SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault())
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    private val timeFormatter = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
    private val displayDateTimeFormatter = SimpleDateFormat(DISPLAY_DATE_TIME_FORMAT, Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault())
    private val displayTimeFormatter = SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault())
    
    /**
     * Get current timestamp in milliseconds
     */
    fun now(): Long = System.currentTimeMillis()
    
    /**
     * Get current date as string in API format
     */
    fun nowAsString(): String = dateTimeFormatter.format(Date())
    
    /**
     * Format timestamp to API date-time string
     */
    fun formatToApi(timestamp: Long): String = dateTimeFormatter.format(Date(timestamp))
    
    /**
     * Format timestamp to display date-time string
     */
    fun formatToDisplay(timestamp: Long): String = displayDateTimeFormatter.format(Date(timestamp))
    
    /**
     * Format timestamp to display date string
     */
    fun formatToDisplayDate(timestamp: Long): String = displayDateFormatter.format(Date(timestamp))
    
    /**
     * Format timestamp to display time string
     */
    fun formatToDisplayTime(timestamp: Long): String = displayTimeFormatter.format(Date(timestamp))
    
    /**
     * Parse API date-time string to timestamp
     */
    fun parseFromApi(dateTimeString: String): Long? {
        return try {
            dateTimeFormatter.parse(dateTimeString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse API date string to timestamp
     */
    fun parseDateFromApi(dateString: String): Long? {
        return try {
            dateFormatter.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if booking can be created (within 7 days)
     */
    fun canCreateBooking(startTime: Long): Boolean {
        val sevenDaysFromNow = now() + (7 * 24 * 60 * 60 * 1000L)
        return startTime <= sevenDaysFromNow
    }
    
    /**
     * Check if booking can be updated/cancelled (at least 12 hours before start)
     */
    fun canUpdateOrCancelBooking(startTime: Long): Boolean {
        val twelveHoursFromNow = now() + (12 * 60 * 60 * 1000L)
        return startTime >= twelveHoursFromNow
    }
    
    /**
     * Get days until booking start
     */
    fun getDaysUntil(startTime: Long): Int {
        val diff = startTime - now()
        return (diff / (24 * 60 * 60 * 1000L)).toInt()
    }
    
    /**
     * Get hours until booking start
     */
    fun getHoursUntil(startTime: Long): Int {
        val diff = startTime - now()
        return (diff / (60 * 60 * 1000L)).toInt()
    }
    
    /**
     * Check if booking is upcoming (future)
     */
    fun isUpcoming(startTime: Long): Boolean = startTime > now()
    
    /**
     * Check if booking is in the past
     */
    fun isPast(endTime: Long): Boolean = endTime < now()
    
    /**
     * Get current time in milliseconds for a specific date and time
     */
    fun getTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get current time in milliseconds for today with specific time
     */
    fun getTodayTimestamp(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
