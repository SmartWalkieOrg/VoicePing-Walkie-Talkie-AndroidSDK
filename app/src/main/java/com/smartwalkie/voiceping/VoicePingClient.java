package com.smartwalkie.voiceping;

import android.app.Application;

import com.smartwalkie.voicepingsdk.VoicePing;

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
        VoicePing.init(this);
    }
}
