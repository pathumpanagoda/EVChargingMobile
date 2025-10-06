package com.evcharge.mobile.common

import android.content.Context
import android.preference.PreferenceManager

class Prefs private constructor(ctx: Context) {
    private val sp = PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext)

    fun setToken(v: String) = sp.edit().putString("auth.token", v).apply()
    fun getToken(): String? = sp.getString("auth.token", null)

    fun setRole(v: String) = sp.edit().putString("auth.role", v).apply()
    fun getRole(): String? = sp.getString("auth.role", null)

    fun isOwner() = getRole() == "EVOwner"
    fun isOperator() = getRole() == "StationOperator"
    fun isBackoffice() = getRole() == "Backoffice"

    fun setUserId(v: String) = sp.edit().putString("auth.userId", v).apply()
    fun getUserId(): String? = sp.getString("auth.userId", null)

    fun setNic(v: String) = sp.edit().putString("auth.nic", v).apply()
    fun getNic(): String? = sp.getString("auth.nic", null)

    fun clear() = sp.edit().clear().apply()

    companion object {
        @Volatile private var _i: Prefs? = null
        fun init(ctx: Context) { if (_i == null) _i = Prefs(ctx) }
        fun instance(): Prefs = _i ?: error("Prefs.init(context) not called")
    }
}