package com.evcharge.mobile

import android.app.Application
import com.evcharge.mobile.data.db.UserDbHelper

/**
 * Main Application class for EV Charging Mobile app
 */
class App : Application() {
    
    companion object {
        lateinit var instance: App
            private set
    }
    
    lateinit var dbHelper: UserDbHelper
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        dbHelper = UserDbHelper(this)
    }
}
