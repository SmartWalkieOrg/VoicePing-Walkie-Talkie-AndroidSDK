package com.smartwalkie.voicepingdemo

import android.content.Context
import android.content.SharedPreferences

object MyPrefs {
    private val sharedPrefs: SharedPreferences =
        VoicePingClientApp.context.getSharedPreferences("voiceping_sdk.sp", Context.MODE_PRIVATE)

    private const val USER_ID = "user_id"
    private const val COMPANY = "company"

    var userId: String?
        get() = sharedPrefs.getString(USER_ID, "")
        set(value) {
            sharedPrefs.edit().putString(USER_ID, value).apply()
        }

    var company: String?
        get() = sharedPrefs.getString(COMPANY, "")
        set(value) {
            sharedPrefs.edit().putString(COMPANY, value).apply()
        }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}