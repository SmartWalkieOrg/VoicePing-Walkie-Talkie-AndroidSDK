package com.smartwalkie.voicepingsdk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

internal object NetworkUtil {

    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnectedOrConnecting
    }
}