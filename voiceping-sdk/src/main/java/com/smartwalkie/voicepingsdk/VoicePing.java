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
     * @param context Application Context
     * @param serverUrl The URL of VoicePing SDK server
     * @return VoicePing instance with default parameters
     */
    public static VoicePing init(Context context, String serverUrl) {
        return new Builder().buildAndInit(context, serverUrl);
    }

    /**
     * Instantiate VoicePing.Builder class.
     *
     * @return VoicePing.Builder instance that can be used to build VoicePing instance with custom
     * parameters
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Connect to server. This method can be assumed as sign in to server. After the user connected
     * to server, the user can then receive PTT from any other user using private channel.
     *
     * @param userId User ID or Username
     * @param callback Callback
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
     * @param callback Callback
     */
    public void disconnect(DisconnectCallback callback) {
        mConnection.disconnect(callback);
    }

    /**
     * Set ChannelListener to VoicePing to do some advanced techniques to the audio data.
     *
     * @param channelListener Listener
     */
    public void setChannelListener(ChannelListener channelListener) {
        mPlayer.setChannelListener(channelListener);
    }

    /*public void subscribe(String channelId, int channelType) {

    }

    public void unsubscribe(String channelId, int channelType) {

    }*/

    /**
     * Start talking using VoicePing's PTT functionality.
     *
     * @param receiverId Receiver ID for private channel, or Group ID for group channel
     * @param channelType ChannelType.PRIVATE or ChannelType.GROUP
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

    public void subscribe(String groupId) {
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        mConnection.send(MessageHelper.createSubscribeMessage(userId, groupId));
    }

    public void unsubscribe(String groupId) {
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        mConnection.send(MessageHelper.createUnsubscribeMessage(userId, groupId));
    }

    /**
     * Builder class to instantiate VoicePing with custom parameters
     */
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

        /**
         * Set whether the SDK will use Opus as audio codec or not. The default value is true.
         *
         * @param usingOpusCodec true for using Opus Codec, false otherwise
         * @return Builder
         */
        public Builder setUsingOpusCodec(boolean usingOpusCodec) {
            isUsingOpusCodec = usingOpusCodec;
            return this;
        }

        /**
         * Set sample rate of the audio data. The default value is 16000.
         *
         * @param sampleRate Sample rate in Hz. Max 48000 Hz
         * @return Builder
         */
        public Builder setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        /**
         * Set frame size of the audio data. The default value is 960.
         *
         * @param frameSize Frame size
         * @return Builder
         */
        public Builder setFrameSize(int frameSize) {
            this.frameSize = frameSize;
            return this;
        }

        /**
         * Set channel size of the audio data. The default value is 1.
         *
         * @param channelSize Channel size. 1 for Mono, 2 for Stereo
         * @return Builder
         */
        public Builder setChannelSize(int channelSize) {
            this.channelSize = channelSize;
            return this;
        }

        /**
         * Set buffer size factor of the audio data. The default value is 2.
         *
         * @param bufferSizeFactor Buffer size in bytes
         * @return Builder
         */
        public Builder setBufferSizeFactor(int bufferSizeFactor) {
            this.bufferSizeFactor = bufferSizeFactor;
            return this;
        }

        /**
         * Set Channel-In configuration of the audio data. The default value is
         * AudioFormat.CHANNEL_IN_MONO
         *
         * @param channelInConfig Channel-In config
         * @return Builder
         */
        public Builder setChannelInConfig(int channelInConfig) {
            this.channelInConfig = channelInConfig;
            return this;
        }

        /**
         * Set Channel-Out configuration of the audio data. The default value is
         * AudioFormat.CHANNEL_OUT_MONO
         *
         * @param channelOutConfig Channel-Out config
         * @return Builder
         */
        public Builder setChannelOutConfig(int channelOutConfig) {
            this.channelOutConfig = channelOutConfig;
            return this;
        }

        /**
         * Set audio format of the audio data. The default value is AudioFormat.ENCODING_PCM_16BIT
         *
         * @param audioFormat Audio format
         * @return Builder
         */
        public Builder setAudioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }

        /**
         * Build the Builder instance to be VoicePing instance, and initiate it.
         *
         * @param context Application context
         * @param serverUrl The URL of VoicePing SDK server
         * @return VoicePing instance
         */
        public VoicePing buildAndInit(Context context, String serverUrl) {
            AudioParam audioParam = new AudioParam(isUsingOpusCodec, sampleRate, frameSize,
                    channelSize, bufferSizeFactor, channelInConfig, channelOutConfig, audioFormat);
            return new VoicePing(context, serverUrl, audioParam);
        }
    }
}
