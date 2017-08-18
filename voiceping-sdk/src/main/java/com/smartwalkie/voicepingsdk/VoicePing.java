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
        mRecorder = new Recorder(context, mConnection, audioParam);
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
        return new Builder().buildAndInit(context, serverUrl);
    }

    public static Builder newBuilder() {
        return new Builder();
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

    public static final class Builder {

        private boolean isUsingOpusCodec;
        private int sampleRate;
        private int frameSize;
        private int channelSize;
        private int bufferSizeFactor;
        private int channelInConfig;
        private int channelOutConfig;
        private int audioFormat;

        private Builder() {
            isUsingOpusCodec = true;
            sampleRate = 16000;
            frameSize = 960;
            channelSize = 1;
            bufferSizeFactor = 2;
            channelInConfig = AudioFormat.CHANNEL_IN_MONO;
            channelOutConfig = AudioFormat.CHANNEL_OUT_MONO;
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        }

        public Builder setUsingOpusCodec(boolean usingOpusCodec) {
            isUsingOpusCodec = usingOpusCodec;
            return this;
        }

        public Builder setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder setFrameSize(int frameSize) {
            this.frameSize = frameSize;
            return this;
        }

        public Builder setChannelSize(int channelSize) {
            this.channelSize = channelSize;
            return this;
        }

        public Builder setBufferSizeFactor(int bufferSizeFactor) {
            this.bufferSizeFactor = bufferSizeFactor;
            return this;
        }

        public Builder setChannelInConfig(int channelInConfig) {
            this.channelInConfig = channelInConfig;
            return this;
        }

        public Builder setChannelOutConfig(int channelOutConfig) {
            this.channelOutConfig = channelOutConfig;
            return this;
        }

        public Builder setAudioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }

        public VoicePing buildAndInit(Context context, String serverUrl) {
            AudioParam audioParam = new AudioParam(isUsingOpusCodec, sampleRate, frameSize,
                    channelSize, bufferSizeFactor, channelInConfig, channelOutConfig, audioFormat);
            return new VoicePing(context, serverUrl, audioParam);
        }
    }
}
