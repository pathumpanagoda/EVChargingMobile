package com.evcharge.mobile.ui.bookings

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.evcharge.mobile.R
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingStatus
import java.util.*

/**
 * Test activity for booking modification debugging
 */
class BookingModifyTestActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_modify_test)
        
        testBookingModification()
    }
    
    private fun testBookingModification() {
        Log.d("BookingModifyTest", "Testing booking modification logic")
        
        // Create a test booking that should be modifiable
        val testBooking = Booking(
            id = "test-booking-123",
            ownerNic = "test-nic",
            stationId = "test-station",
            stationName = "Test Station",
            startTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours from now
            endTime = System.currentTimeMillis() + (25 * 60 * 60 * 1000), // 25 hours from now
            status = BookingStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        Log.d("BookingModifyTest", "Test booking created:")
        Log.d("BookingModifyTest", "  ID: ${testBooking.id}")
        Log.d("BookingModifyTest", "  Status: ${testBooking.status.name}")
        Log.d("BookingModifyTest", "  Start Time: ${testBooking.startTime}")
        Log.d("BookingModifyTest", "  Current Time: ${System.currentTimeMillis()}")
        
        // Test the modification logic
        val canModify = canModifyBooking(testBooking)
        Log.d("BookingModifyTest", "Can modify booking: $canModify")
        
        if (canModify) {
            Log.d("BookingModifyTest", "✅ Booking can be modified")
        } else {
            Log.d("BookingModifyTest", "❌ Booking cannot be modified")
        }
    }
    
    private fun canModifyBooking(booking: Booking): Boolean {
        // Check if booking can be modified (only pending bookings)
        if (booking.status.name != "PENDING") {
            Log.d("BookingModifyTest", "Booking is not PENDING, status: ${booking.status.name}")
            return false
        }
        
        // Check if booking can be modified (12-hour rule)
        val twelveHoursFromNow = System.currentTimeMillis() + (12 * 60 * 60 * 1000)
        Log.d("BookingModifyTest", "12 hours from now: $twelveHoursFromNow")
        Log.d("BookingModifyTest", "Booking start time: ${booking.startTime}")
        
        if (booking.startTime <= twelveHoursFromNow) {
            Log.d("BookingModifyTest", "Booking is within 12 hours, cannot modify")
            return false
        }
        
        Log.d("BookingModifyTest", "Booking passes all modification checks")
        return true
    }
}

