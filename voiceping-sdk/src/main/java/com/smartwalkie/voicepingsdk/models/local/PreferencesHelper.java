package com.smartwalkie.voicepingsdk.models.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.smartwalkie.voicepingsdk.VoicePing;

/**
 * Created by kukuhsain on 8/4/17.
 */

public class PreferencesHelper {

    private static PreferencesHelper INSTANCE;
    private SharedPreferences mSharedPreferences;

    public PreferencesHelper() {
        mSharedPreferences = VoicePing.getApplication()
                .getSharedPreferences("voiceping_sdk.sp", Context.MODE_PRIVATE);
    }

    public static PreferencesHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PreferencesHelper();
        }
        return INSTANCE;
    }

    public String getServerUrl() {
        return mSharedPreferences.getString("server_url", null);
    }

    public void putServerUrl(String serverUrl) {
        mSharedPreferences.edit().putString("server_url", serverUrl).apply();
    }

    public String getUserId() {
        return mSharedPreferences.getString("user_id", null);
    }

    public void putUserId(String userId) {
        mSharedPreferences.edit().putString("user_id", userId).apply();
    }
}
