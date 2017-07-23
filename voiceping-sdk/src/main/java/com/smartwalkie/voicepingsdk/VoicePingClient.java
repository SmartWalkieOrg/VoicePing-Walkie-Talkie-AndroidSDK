package com.smartwalkie.voicepingsdk;

import android.app.Application;

/**
 * Created by sirius on 7/3/17.
 */

public class VoicePingClient extends Application {
    private static VoicePingClient instance;
    public static VoicePingClient getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
