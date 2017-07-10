package com.smartwalkie.voiceping;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.smartwalkie.voiceping.events.DisconnectEvent;

import de.greenrobot.event.EventBus;

/**
 * Created by sirius on 7/3/17.
 */

public class PingService extends Service {

    public static final String TAG = Connection.class.getSimpleName();

    private IBinder binder;
    private class LocalBinder extends Binder { }

    private Connection connection;
    private Recorder recorder;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(VoicePingClient.getInstance())
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true);

        Intent intent = new Intent(VoicePingClient.getInstance(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(VoicePingClient.getInstance(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        startForeground(12345, notificationBuilder.build());
        binder = new LocalBinder();
        connection = new Connection("wss://2359staging-router.voiceoverping.net");
        connection.connect();
        recorder = new Recorder();
        recorder.startRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void onEvent(DisconnectEvent event) {
        Log.v(TAG, "onEvent");
        stopSelf();
    }
}
