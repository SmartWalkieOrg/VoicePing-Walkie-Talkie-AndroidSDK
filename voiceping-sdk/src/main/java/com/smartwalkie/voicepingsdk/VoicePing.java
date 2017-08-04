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

    private Connection mConnection;
    private ConnectCallback mConnectCallback;
    private DisconnectCallback mDisconnectCallback;

    private Player mPlayer;
    private Recorder mRecorder;

    public static VoicePing getInstance() {
        if (INSTANCE == null) INSTANCE = new VoicePing();
        return INSTANCE;
    }

    private VoicePing() {
        mPlayer = Player.getInstance();
        mPlayer.start();
        mRecorder = Recorder.getInstance();
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

    private void _configure(Context context, String serverUrl) {
        Session.getInstance().setContext(context);
        Session.getInstance().setServerUrl(serverUrl);

        mConnection = new Connection(serverUrl, this, mPlayer, mRecorder);
    }

    private void _connect(int userId, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("user_id", Integer.toString(userId));
        _connect(props, callback);
    }

    private void _connect(Map<String, String> props, ConnectCallback callback) {
        this.mConnectCallback = callback;
        if (props.containsKey("user_id")) {
            String userId = props.get("user_id");
            Session.getInstance().setUserId(Integer.parseInt(userId));
        }
        props.put("DeviceId", Settings.Secure.getString(
                Session.getInstance().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID));
        mConnection.connect(props);
    }

    public static void disconnect(DisconnectCallback callback) {
        getInstance()._disconnect(callback);
    }

    private void _disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        mConnection.disconnect();
    }

    public static void startTalking(int receiverId, int channelType) {
        getInstance()._startTalking(receiverId, channelType);
    }

    public static void stopTalking() {
        getInstance()._stopTalking();
    }

    private void _startTalking(int receiverId, int channelType) {
        mRecorder.startTalking(receiverId, channelType);
    }

    private void _stopTalking() {
        mRecorder.stopTalking();
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
        if (mConnectCallback != null) mConnectCallback.onConnected();
    }

    @Override
    public void onFailed() {
        if (mConnectCallback != null) mConnectCallback.onFailed(new PingException());
    }

    @Override
    public void onData(byte[] data) {

    }

    @Override
    public void onDisconnected() {
        if (mDisconnectCallback != null) mDisconnectCallback.onDisconnected();
    }
}
