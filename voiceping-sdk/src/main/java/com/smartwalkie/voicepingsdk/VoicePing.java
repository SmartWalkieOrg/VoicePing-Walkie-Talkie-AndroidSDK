package com.smartwalkie.voicepingsdk;


import android.app.Application;
import android.provider.Settings;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.ChannelListener;
import com.smartwalkie.voicepingsdk.listeners.ConnectionListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

import java.util.HashMap;
import java.util.Map;

public class VoicePing implements ConnectionListener {

    private Application mApplication;
    private Connection mConnection;
    private ConnectCallback mConnectCallback;
    private DisconnectCallback mDisconnectCallback;
    private ChannelListener mChannelListener;

    private Recorder mRecorder;

    private VoicePing(Application application, String serverUrl) {
        mApplication = application;
        Player player = new Player(application);
        player.start();
        mConnection = new Connection(application, serverUrl, this, player);
        mRecorder = new Recorder(application, mConnection);
        mConnection.setOutgoingAudioListener(mRecorder);
        VoicePingPrefs.getInstance(application).putServerUrl(serverUrl);
    }

    public static VoicePing init(Application application, String serverUrl) {
        return new VoicePing(application, serverUrl);
    }

    public void connect(String userId, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("user_id", userId);
        VoicePingPrefs.getInstance(mApplication).putUserId(userId);
        props.put("DeviceId", Settings.Secure.getString(
                mApplication.getContentResolver(), Settings.Secure.ANDROID_ID));
        mConnection.connect(props);
        mConnectCallback = callback;
    }

    public void disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        mConnection.disconnect();
    }

    public void setChannelListener(ChannelListener channelListener) {
        mChannelListener = channelListener;
    }

    public void subscribe(String channelId, int channelType) {

    }

    public void unsubscribe(String channelId) {

    }

    public void startTalking(String receiverId, int channelType) {
        mRecorder.startTalking(receiverId, channelType);
    }

    public void stopTalking() {
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
        if (mConnectCallback != null) mConnectCallback.onFailed(new PingException("Failed to connect!"));
    }

    @Override
    public void onData(byte[] data) {

    }

    @Override
    public void onDisconnected() {
        if (mDisconnectCallback != null) mDisconnectCallback.onDisconnected();
    }
}
