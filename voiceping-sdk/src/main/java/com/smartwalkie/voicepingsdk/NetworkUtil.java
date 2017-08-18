package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


class NetworkUtil {

    static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}
