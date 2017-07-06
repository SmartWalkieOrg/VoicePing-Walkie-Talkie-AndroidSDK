package com.smartwalkie.voiceping;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by sirius on 7/3/17.
 */

public class MainService extends Service {

    private IBinder binder;
    private class LocalBinder extends Binder { }

    private WebSocketConnection websocket;
    private Recorder recorder;

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new LocalBinder();
        websocket = new WebSocketConnection("wss://2359staging-router.voiceoverping.net");
        websocket.connect();
        recorder = new Recorder();
        recorder.startRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
