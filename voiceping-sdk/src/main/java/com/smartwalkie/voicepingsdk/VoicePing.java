package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.provider.Settings;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.listeners.ChannelListener;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

import java.util.HashMap;
import java.util.Map;

public class VoicePing {

    private Context mContext;
    private Player mPlayer;
    private Connection mConnection;
    private Recorder mRecorder;

    private VoicePing(Context context, String serverUrl) {
        mContext = context;
        mPlayer = new Player(context);
        mPlayer.start();
        mConnection = new Connection(context, serverUrl, mPlayer);
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
        mConnection.connect(props, callback);
    }

    public void disconnect(DisconnectCallback callback) {
        mConnection.disconnect(callback);
    }

    public void setChannelListener(ChannelListener channelListener) {
        mPlayer.setChannelListener(channelListener);
        mRecorder.setChannelListener(channelListener);
    }

    public void subscribe(String channelId, int channelType) {

    }

    public void unsubscribe(String channelId, int channelType) {

    }

    public void startTalking(String receiverId, int channelType) {
        mRecorder.startTalking(receiverId, channelType);
    }

    public void stopTalking() {
        mRecorder.stopTalking();
    }
}
