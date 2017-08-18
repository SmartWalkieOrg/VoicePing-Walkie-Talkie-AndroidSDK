package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.media.AudioFormat;
import android.provider.Settings;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.listeners.ChannelListener;
import com.smartwalkie.voicepingsdk.listeners.OutgoingTalkCallback;
import com.smartwalkie.voicepingsdk.models.AudioParam;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

import java.util.HashMap;
import java.util.Map;

/**
 * Main class of VoicePing.
 */
public class VoicePing {

    private Context mContext;
    private Player mPlayer;
    private Connection mConnection;
    private Recorder mRecorder;

    private VoicePing(Context context, String serverUrl, AudioParam audioParam) {
        mContext = context;
        mPlayer = new Player(context, audioParam);
        mPlayer.start();
        mConnection = new Connection(context, serverUrl, mPlayer);
        mRecorder = new Recorder(context, mConnection);
        mConnection.setOutgoingAudioListener(mRecorder);
        VoicePingPrefs.getInstance(context).putServerUrl(serverUrl);
    }

    /**
     * Instantiate VoicePing. VoicePing instance is the most important instance in this SDK.
     * The developer that use this SDK can do other processes using VoicePing instance.
     *
     * @param context
     * @param serverUrl
     * @return VoicePing instance
     */
    public static VoicePing init(Context context, String serverUrl) {
        AudioParam audioParam = new AudioParam(true, 16000, 960, 133, 1, 2,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        return new VoicePing(context, serverUrl, audioParam);
    }

    /**
     * Connect to server. This method can be assumed as sign in to server. After the user connected
     * to server, the user can then receive PTT from any other user using private channel.
     *
     * @param userId
     * @param callback
     */
    public void connect(String userId, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("user_id", userId);
        VoicePingPrefs.getInstance(mContext).putUserId(userId);
        props.put("DeviceId", Settings.Secure
                .getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        mConnection.connect(props, callback);
    }

    /**
     * Disconnect from server. This method can be assumed as sign out from server. After
     * disconnected from server, the user will not receive any incoming message.
     *
     * @param callback
     */
    public void disconnect(DisconnectCallback callback) {
        mConnection.disconnect(callback);
    }

    public void setChannelListener(ChannelListener channelListener) {
        mPlayer.setChannelListener(channelListener);
    }

    public void subscribe(String channelId, int channelType) {

    }

    public void unsubscribe(String channelId, int channelType) {

    }

    /**
     * Start talking using PTT.
     *
     * @param receiverId
     * @param channelType
     */
    public void startTalking(String receiverId, int channelType, OutgoingTalkCallback callback) {
        mRecorder.startTalking(receiverId, channelType, callback);
    }

    /**
     * Stop talking.
     */
    public void stopTalking() {
        mRecorder.stopTalking();
    }
}
