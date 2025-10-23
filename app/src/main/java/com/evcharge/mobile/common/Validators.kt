package com.evcharge.mobile.common

/**
 * Validation utility functions
 */
object Validators {
    
    /**
     * Validate NIC (Sri Lanka National Identity Card)
     * Supports both old format (9 digits + V) and new format (12 digits)
     */
    fun isValidNIC(nic: String): Boolean {
        if (nic.isBlank()) return false
        
        // Remove any spaces or special characters
        val cleanNIC = nic.replace("\\s+".toRegex(), "").uppercase()
        
        // Old format: 9 digits followed by V (e.g., 123456789V)
        val oldFormatRegex = "^\\d{9}[VX]$".toRegex()
        
        // New format: 12 digits (e.g., 123456789012)
        val newFormatRegex = "^\\d{12}$".toRegex()
        
        return cleanNIC.matches(oldFormatRegex) || cleanNIC.matches(newFormatRegex)
    }
    
    /**
     * Validate email address
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    /**
     * Validate phone number (Sri Lanka format)
     * Supports formats: +94771234567, 0771234567, 0112345678
     */
    fun isValidPhone(phone: String): Boolean {
        if (phone.isBlank()) return false
        
        // Remove spaces, dashes, and parentheses
        val cleanPhone = phone.replace("[\\s\\-\\(\\)]".toRegex(), "")
        
        // Sri Lanka phone number patterns
        val patterns = listOf(
            "^\\+947[0-9]{8}$",  // +94771234567
            "^07[0-9]{8}$",      // 0771234567
            "^0[1-9][0-9]{7}$"   // 0112345678
        )
        
        return patterns.any { pattern -> cleanPhone.matches(pattern.toRegex()) }
    }
    
    /**
     * Validate password strength
     */
    fun isValidPassword(password: String): Boolean {
        if (password.isBlank()) return false
        return password.length >= 6
    }
    
    /**
     * Validate name (non-empty, reasonable length)
     */
    fun isValidName(name: String): Boolean {
        if (name.isBlank()) return false
        return name.trim().length >= 2 && name.trim().length <= 100
    }
    
    /**
     * Validate required field
     */
    fun isRequired(value: String): Boolean {
        return value.isNotBlank()
    }
    
    /**
     * Validate password confirmation
     */
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
    
    /**
     * Validate booking time constraints
     */
    fun canCreateBooking(startTime: Long): Boolean {
        return Datex.canCreateBooking(startTime)
    }
    
    /**
     * Validate booking update/cancel constraints
     */
    fun canUpdateOrCancelBooking(startTime: Long): Boolean {
        return Datex.canUpdateOrCancelBooking(startTime)
    }
    
    /**
     * Validate booking duration (no minimum or maximum limit)
     */
    fun isValidBookingDuration(startTime: Long, endTime: Long): Boolean {
        val duration = endTime - startTime
        return duration > 0 // Just ensure end time is after start time
    }
    
    /**
     * Validate future date
     */
    fun isFutureDate(timestamp: Long): Boolean {
        return timestamp > Datex.now()
    }
    
    /**
     * Get validation error message for NIC
     */
    fun getNICErrorMessage(): String = "Invalid NIC format. Use 9 digits + V or 12 digits."
    
    /**
     * Get validation error message for email
     */
    fun getEmailErrorMessage(): String = "Invalid email format."
    
    /**
     * Get validation error message for phone
     */
    fun getPhoneErrorMessage(): String = "Invalid phone number. Use Sri Lanka format."
    
    /**
     * Get validation error message for password
     */
    fun getPasswordErrorMessage(): String = "Password must be at least 6 characters."
    
    /**
     * Get validation error message for name
     */
    fun getNameErrorMessage(): String = "Name must be 2-100 characters."
    
    /**
     * Get validation error message for required field
     */
    fun getRequiredErrorMessage(fieldName: String): String = "$fieldName is required."
    
    /**
     * Get validation error message for password mismatch
     */
    fun getPasswordMismatchErrorMessage(): String = "Passwords do not match."
    
    /**
     * Get validation error message for booking creation
     */
    fun getBookingCreationErrorMessage(): String = "Bookings can only be created within 7 days from now."
    
    /**
     * Get validation error message for booking update/cancel
     */
    fun getBookingUpdateErrorMessage(): String = "Bookings can only be updated or cancelled at least 12 hours before start time."
    
    /**
     * Get validation error message for booking duration
     */
    fun getBookingDurationErrorMessage(): String = "End time must be after start time."
}
