package com.smartwalkie.voiceping;

import android.app.Application;

import com.smartwalkie.voicepingsdk.VoicePing;

/**
 * Created by sirius on 7/3/17.
 */

public class VoicePingClientApp extends Application {

    private static VoicePing mVoicePing;

    @Override
    public void onCreate() {
        super.onCreate();
        mVoicePing = VoicePing
                .init(this, "ws://vpjsex.n2j22nmzkr.ap-southeast-1.elasticbeanstalk.com");
    }

    public static VoicePing getVoicePing() {
        return mVoicePing;
    }
}
