package com.smartwalkie.voiceping;

import android.app.Application;

/**
 * Created by sirius on 7/3/17.
 */

public class VoicePingApplication extends Application {
    private static VoicePingApplication instance;
    public static VoicePingApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
