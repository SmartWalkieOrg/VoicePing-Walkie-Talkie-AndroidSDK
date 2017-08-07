package com.smartwalkie.voiceping;

import android.app.Application;

import com.smartwalkie.voicepingsdk.VoicePing;

/**
 * Created by sirius on 7/3/17.
 */

public class VoicePingClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VoicePing.configure(this, "ws://vpjsex.n2j22nmzkr.ap-southeast-1.elasticbeanstalk.com");
    }
}
