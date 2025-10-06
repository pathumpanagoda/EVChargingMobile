package com.evcharge.mobile

import android.app.Application
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.data.db.UserDbHelper

class App : Application() {
    val dbHelper = UserDbHelper(this)
    
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
    
    companion object {
        @Volatile private var _instance: App? = null
        fun instance(): App = _instance ?: error("App not initialized")
    }
}