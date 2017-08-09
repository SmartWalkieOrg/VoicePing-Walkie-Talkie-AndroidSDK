package com.smartwalkie.voicepingsdk;

import android.content.Context;
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

    private Context mContext;
    private Connection mConnection;
    private ConnectCallback mConnectCallback;
    private DisconnectCallback mDisconnectCallback;

    private Player mPlayer;
    private Recorder mRecorder;

    private VoicePing(Context context, String serverUrl) {
        mContext = context;
        mPlayer = new Player(context);
        mPlayer.start();
        mConnection = new Connection(context, serverUrl, this, mPlayer);
        mRecorder = new Recorder(context, mConnection);
        mConnection.setOutgoingAudioListener(mRecorder);
        VoicePingPrefs.getInstance(context).putServerUrl(serverUrl);
    }

    public static VoicePing init(Context context, String serverUrl) {
        return new VoicePing(context, serverUrl);
    }

    public void connect(String userId, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("user_id", userId);
        VoicePingPrefs.getInstance(mContext).putUserId(userId);
        props.put("DeviceId", Settings.Secure
                .getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        mConnection.connect(props);
        mConnectCallback = callback;
    }

    public void disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        mConnection.disconnect();
    }

    public void setChannelListener(ChannelListener channelListener) {
        mPlayer.setChannelListener(channelListener);
        mRecorder.setChannelListener(channelListener);
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
