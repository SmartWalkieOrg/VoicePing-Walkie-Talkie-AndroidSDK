package com.smartwalkie.voicepingsdk;


import android.app.Application;
import android.content.Context;
import android.provider.Settings;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.ConnectionListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.Session;

import java.util.HashMap;
import java.util.Map;

public class VoicePing implements ConnectionListener {

    private static Application mApplication;
    private static VoicePing INSTANCE;

    private Connection connection;
    private ConnectCallback connectCallback;
    private DisconnectCallback disconnectCallback;

    private Player player;
    private Recorder recorder;

    public static VoicePing getInstance() {
        if (INSTANCE == null) INSTANCE = new VoicePing();
        return INSTANCE;
    }

    private VoicePing() {
        player = Player.getInstance();
        player.start();
        recorder = Recorder.getInstance();
    }

    protected static Application getApplication() {
        return mApplication;
    }

    public static void configure(Application application, String serverUrl) {
        mApplication = application;
        getInstance()._configure(application, serverUrl);
    }

    public static void connect(int userId, ConnectCallback callback) {
        getInstance()._connect(userId, callback);
    }

    public static void connect(Map<String, String> props, ConnectCallback callback) {
        getInstance()._connect(props, callback);
    }

    private void _configure(Context context, String serverUrl) {
        Session.getInstance().setContext(context);
        Session.getInstance().setServerUrl(serverUrl);

        connection = new Connection(serverUrl);
        connection.setConnectionListener(this);
        connection.setIncomingAudioListener(player);
        connection.setOutgoingAudioListener(recorder);
    }

    private void _connect(int userId, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("user_id", Integer.toString(userId));
        _connect(props, callback);
    }

    private void _connect(Map<String, String> props, ConnectCallback callback) {
        this.connectCallback = callback;
        if (props.containsKey("user_id")) {
            String userId = props.get("user_id");
            Session.getInstance().setUserId(Integer.parseInt(userId));
        }
        props.put("DeviceId", Settings.Secure.getString(Session.getInstance().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID));
        connection.connect(props);
        /*
        Intent serviceIntent = new Intent(context, PingService.class);
        context.startService(serviceIntent);
        */
    }

    public static void disconnect(DisconnectCallback callback) {
        getInstance()._disconnect(callback);
    }

    private void _disconnect(DisconnectCallback callback) {
        disconnectCallback = callback;
        connection.disconnect();
    }

    public static void startTalking(int receiverId, int channelType) {
        getInstance()._startTalking(receiverId, channelType);
    }

    public static void stopTalking() {
        getInstance()._stopTalking();
    }

    private void _startTalking(int receiverId, int channelType) {
        recorder.startTalking(receiverId, channelType);
    }

    private void _stopTalking() {
        recorder.stopTalking();
    }

    // ConnectionListener
    @Override
    public void onMessage(Message message) {

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onConnected() {
        if (connectCallback != null) connectCallback.onConnected();
    }

    @Override
    public void onFailed() {
        if (connectCallback != null) connectCallback.onFailed(new PingException());
    }

    @Override
    public void onData(byte[] data) {

    }

    @Override
    public void onDisconnected() {
        if (disconnectCallback != null) disconnectCallback.onDisconnected();
    }
}
