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

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new LocalBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
