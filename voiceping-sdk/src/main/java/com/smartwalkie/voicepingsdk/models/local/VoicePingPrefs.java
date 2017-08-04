package com.smartwalkie.voicepingsdk.models.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.smartwalkie.voicepingsdk.VoicePing;

/**
 * Created by kukuhsain on 8/4/17.
 */

public class VoicePingPrefs {

    private static VoicePingPrefs INSTANCE;
    private SharedPreferences mSharedPreferences;

    public VoicePingPrefs() {
        mSharedPreferences = VoicePing.getApplication()
                .getSharedPreferences("voiceping_sdk.sp", Context.MODE_PRIVATE);
    }

    public static VoicePingPrefs getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VoicePingPrefs();
        }
        return INSTANCE;
    }

    public String getServerUrl() {
        return mSharedPreferences.getString("server_url", null);
    }

    public void putServerUrl(String serverUrl) {
        mSharedPreferences.edit().putString("server_url", serverUrl).apply();
    }

    public int getUserId() {
        return mSharedPreferences.getInt("user_id", 0);
    }

    public void putUserId(int userId) {
        mSharedPreferences.edit().putInt("user_id", userId).apply();
    }
}
