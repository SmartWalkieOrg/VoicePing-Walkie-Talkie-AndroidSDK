package com.smartwalkie.voicepingsdk;


import android.app.Application;
import android.provider.Settings;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.ConnectionListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

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

    public static Application getApplication() {
        return mApplication;
    }

    public static void configure(Application application, String serverUrl) {
        mApplication = application;
        getInstance()._configure(serverUrl);
    }

    public static void connect(String userId, ConnectCallback callback) {
        getInstance()._connect(userId, callback);
    }

    private void _configure(String serverUrl) {
        mConnection = new Connection(serverUrl, this, mPlayer, mRecorder);
        VoicePingPrefs.getInstance().putServerUrl(serverUrl);
    }

    private void _connect(String userId, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("user_id", userId);
        _connect(props, callback);
    }

    private void _connect(Map<String, String> props, ConnectCallback callback) {
        this.mConnectCallback = callback;
        if (props.containsKey("user_id")) {
            String userId = props.get("user_id");
            VoicePingPrefs.getInstance().putUserId(userId);
        }
        props.put("DeviceId", Settings.Secure.getString(
                mApplication.getContentResolver(), Settings.Secure.ANDROID_ID));
        mConnection.connect(props);
    }

    public static void disconnect(DisconnectCallback callback) {
        getInstance()._disconnect(callback);
    }

    private void _disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        mConnection.disconnect();
    }

    public static void startTalking(String receiverId, int channelType) {
        getInstance()._startTalking(receiverId, channelType);
    }

    public static void stopTalking() {
        getInstance()._stopTalking();
    }

    private void _startTalking(String receiverId, int channelType) {
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
