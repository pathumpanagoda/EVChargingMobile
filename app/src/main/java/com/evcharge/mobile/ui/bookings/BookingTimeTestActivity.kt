package com.evcharge.mobile.ui.bookings

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.evcharge.mobile.R
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Test activity for booking time logic
 */
class BookingTimeTestActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_modify_test)
        
        testBookingTimeLogic()
    }
    
    private fun testBookingTimeLogic() {
        Log.d("BookingTimeTest", "Testing booking time logic")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        // Test case 1: Booking created yesterday, starts tomorrow (should be modifiable)
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
        val tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours from now
        
        val testBooking1 = Booking(
            id = "test-1",
            ownerNic = "test-nic",
            stationId = "test-station",
            stationName = "Test Station",
            startTime = tomorrow,
            endTime = tomorrow + (2 * 60 * 60 * 1000), // 2 hours later
            status = BookingStatus.PENDING,
            createdAt = yesterday,
            updatedAt = yesterday
        )
        
        Log.d("BookingTimeTest", "Test Case 1: Booking created yesterday, starts tomorrow")
        Log.d("BookingTimeTest", "  Created: ${dateFormat.format(Date(testBooking1.createdAt))}")
        Log.d("BookingTimeTest", "  Starts: ${dateFormat.format(Date(testBooking1.startTime))}")
        Log.d("BookingTimeTest", "  Current: ${dateFormat.format(Date(System.currentTimeMillis()))}")
        
        val canModify1 = canModifyBooking(testBooking1)
        Log.d("BookingTimeTest", "  Can modify: $canModify1")
        
        // Test case 2: Booking starts in 6 hours (should NOT be modifiable)
        val sixHoursFromNow = System.currentTimeMillis() + (6 * 60 * 60 * 1000)
        
        val testBooking2 = Booking(
            id = "test-2",
            ownerNic = "test-nic",
            stationId = "test-station",
            stationName = "Test Station",
            startTime = sixHoursFromNow,
            endTime = sixHoursFromNow + (2 * 60 * 60 * 1000),
            status = BookingStatus.PENDING,
            createdAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 hours ago
            updatedAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000)
        )
        
        Log.d("BookingTimeTest", "Test Case 2: Booking starts in 6 hours")
        Log.d("BookingTimeTest", "  Created: ${dateFormat.format(Date(testBooking2.createdAt))}")
        Log.d("BookingTimeTest", "  Starts: ${dateFormat.format(Date(testBooking2.startTime))}")
        Log.d("BookingTimeTest", "  Current: ${dateFormat.format(Date(System.currentTimeMillis()))}")
        
        val canModify2 = canModifyBooking(testBooking2)
        Log.d("BookingTimeTest", "  Can modify: $canModify2")
        
        Log.d("BookingTimeTest", "Test completed!")
    }
    
    private fun canModifyBooking(booking: Booking): Boolean {
        // Check if booking can be modified (only pending bookings)
        if (booking.status.name != "PENDING") {
            Log.d("BookingTimeTest", "Booking is not PENDING, status: ${booking.status.name}")
            return false
        }
        
        // Check if booking can be modified (12-hour rule)
        // The rule should be: cannot modify within 12 hours of the START TIME
        val twelveHoursFromStartTime = booking.startTime - (12 * 60 * 60 * 1000)
        val currentTime = System.currentTimeMillis()
        
        Log.d("BookingTimeTest", "  Current time: $currentTime")
        Log.d("BookingTimeTest", "  Booking start time: ${booking.startTime}")
        Log.d("BookingTimeTest", "  12 hours before start time: $twelveHoursFromStartTime")
        
        if (currentTime >= twelveHoursFromStartTime) {
            Log.d("BookingTimeTest", "  Current time is within 12 hours of start time, cannot modify")
            return false
        }
        
        Log.d("BookingTimeTest", "  Booking passes all modification checks")
        return true
    }
}

