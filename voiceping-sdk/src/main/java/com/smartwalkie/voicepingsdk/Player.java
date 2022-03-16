package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.media2359.voiceping.codec.Opus;
import com.smartwalkie.voicepingsdk.exception.VoicePingException;
import com.smartwalkie.voicepingsdk.listener.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listener.IncomingAudioListener;
import com.smartwalkie.voicepingsdk.listener.IncomingTalkListener;
import com.smartwalkie.voicepingsdk.model.AudioParam;
import com.smartwalkie.voicepingsdk.model.Channel;
import com.smartwalkie.voicepingsdk.model.Message;
import com.smartwalkie.voicepingsdk.model.MessageType;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class Player implements IncomingAudioListener {
    private final String TAG = Player.class.getSimpleName();

    private final Context mContext;
    private AudioParam mAudioParam;
    private final String mServerUrl;
    private Opus mOpus;
    private AudioTrack mAudioTrack;
    private final Handler mBackgroundHandler;
    private final Handler mPlayerHandler;
    private IncomingTalkListener mIncomingTalkListener;
    private final PlayerMuteManager mPlayerMuteManager;
    private final Map<String, IncomingTalkSession> mActiveSessions;

    private final int PLAY = 1;
    private final int STOP = 2;

    public Player(Context context, AudioParam audioParam, String serverUrl, Looper backgroundLooper) {
        mContext = context;
        mServerUrl = serverUrl;
        setAudioParam(audioParam);
        mPlayerMuteManager = new PlayerMuteManager(context);
        mBackgroundHandler = new Handler(backgroundLooper);
        mPlayerHandler = newPlayerHandler(backgroundLooper);
        mActiveSessions = new HashMap<>();
    }

    private Handler newPlayerHandler(Looper looper) {
        return new Handler(looper, new Handler.Callback() {

            @Override
            public boolean handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case PLAY:
                        if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                            Log.d(TAG, "Player starts...");
                            playAudioTrack();
                        }
//                        byte[] payload = (byte[]) msg.obj;
                        Message message = (Message) msg.obj;
                        byte[] payload = message.getPayload();
                        Channel channel = new Channel(message.getChannelType(), message.getSenderId(), message.getReceiverId());

                        IncomingTalkSession session = mActiveSessions.get(channel.toString());
                        AudioInterceptor interceptorBeforeDecoded = null;
                        AudioInterceptor interceptorAfterDecoded = null;
                        if (session != null) {
                            interceptorBeforeDecoded = session.getAudioInterceptorBeforeDecoded();
                            interceptorAfterDecoded = session.getAudioInterceptorAfterDecoded();
                            if (session.isSavedToLocal()) session.writeData(payload);
                        }

                        byte[] pcmFrame = new byte[mAudioParam.getRawBufferSize()];

                        ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
                        int singlePayloadSize = payload.length / mAudioParam.getFramePerSent();
                        while (byteBuffer.remaining() >= singlePayloadSize) {
                            // get 1 frame of encoded voice data
                            byte[] singlePayload = new byte[singlePayloadSize];
                            byteBuffer.get(singlePayload);

                            // intercept before being decoded
                            if (interceptorBeforeDecoded != null) {
                                singlePayload = interceptorBeforeDecoded.proceed(singlePayload, channel);
                                if (singlePayload == null || singlePayload.length == 0)
                                    return false;
                            }

                            if (mAudioParam.isUsingOpusCodec()) {
                                // decoding process
                                int decodeSize = mOpus.decode(singlePayload, 0, singlePayload.length, pcmFrame, 0, mAudioParam.getFrameSize(), 0);
                                if (decodeSize <= 0) return false;

                                // intercept after being decoded
                                if (interceptorAfterDecoded != null) {
                                    pcmFrame = interceptorAfterDecoded.proceed(pcmFrame, channel);
                                    if (pcmFrame == null || pcmFrame.length == 0) return false;
                                }

                                if (mPlayerMuteManager.isMuted(message)) return true;

                                // boost audio
                                pcmFrame = AudioBooster.boost(mAudioParam.getReceivingBoostInDb(), pcmFrame, pcmFrame.length);

                                // play voice
                                mAudioTrack.write(pcmFrame, 0, pcmFrame.length);
                            } else {
                                // intercept after being decoded
                                if (interceptorAfterDecoded != null) {
                                    singlePayload = interceptorAfterDecoded.proceed(singlePayload, channel);
                                    if (singlePayload == null || singlePayload.length == 0)
                                        return false;
                                }

                                if (mPlayerMuteManager.isMuted(message)) return true;

                                // boost audio
                                singlePayload = AudioBooster.boost(mAudioParam.getReceivingBoostInDb(), singlePayload, singlePayload.length);

                                // play voice
                                mAudioTrack.write(singlePayload, 0, singlePayload.length);
                            }
                        }
                        return true;
                    case STOP:
                        Log.d(TAG, "Player stops...");
                        if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED
                                && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                            mAudioTrack.stop();
                            mAudioTrack.flush();
                            mAudioTrack.release();
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });
    }

    public void setAudioParam(AudioParam audioParam) {
        mAudioParam = audioParam;
        mOpus = new Opus(audioParam.getSampleRate(), audioParam.getChannelSize());
    }

    public void setIncomingTalkListener(IncomingTalkListener listener) {
        mIncomingTalkListener = listener;
    }

    public void mute(String targetId, int channelType) {
        mPlayerMuteManager.mute(targetId, channelType);
    }

    public void muteAll() {
        mPlayerMuteManager.muteAll();
    }

    public void unmute(String targetId, int channelType) {
        mPlayerMuteManager.unmute(targetId, channelType);
    }

    public void unmuteAll() {
        mPlayerMuteManager.unmuteAll();
    }

    private void stop() {
        mPlayerHandler.sendEmptyMessage(STOP);
    }

    private void play(Message message) {
        android.os.Message msg = new android.os.Message();
        msg.what = PLAY;
        msg.obj = message;
        mPlayerHandler.sendMessage(msg);
    }

    private void initAudioTrackIfNeeded() {
        if (mAudioTrack == null || mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            mAudioTrack = newAudioTrack();
            Log.d(TAG, "new AudioTrack, session id: " + mAudioTrack.getAudioSessionId());
        }
    }

    private AudioTrack newAudioTrack() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        int minBufferSize = mAudioParam.getPlayMinBufferSize();
//        Log.d(TAG, "init minbuffer size=" + minBufferSize);
        int streamType = AudioManager.STREAM_MUSIC;
        return new AudioTrack(
                streamType,
                mAudioParam.getSampleRate(),
                mAudioParam.getChannelOutConfig(),
                mAudioParam.getAudioFormat(),
                Math.min(minBufferSize, mAudioParam.getFrameSize() * mAudioParam.getBufferSizeFactor()),
                AudioTrack.MODE_STREAM);
    }

    private void playAudioTrack() {
        try {
            mAudioTrack.play();
        } catch (IllegalStateException e) {
            if (mAudioTrack != null) mAudioTrack.release();
            mAudioTrack = newAudioTrack();
            playAudioTrack();
        }
    }

    private void stopPlaying(Channel channel, Message message) {
        IncomingTalkSession session = mActiveSessions.get(channel.toString());
        if (session == null || !session.isActive()) return;
        session.stop();
        session.setStopSignal(message != null);
        if (message != null) {
            String downloadUrl = getDownloadUrl(message);
            Log.d(TAG, "onStopTalkingMessage, download url: " + downloadUrl);
            session.setDownloadUrl(downloadUrl);
            session.setDurationInServer(getDurationInServer(message));
        }
        mBackgroundHandler.removeCallbacks(session.getTimeoutCheckRunner());
        if (mIncomingTalkListener != null) {
            mIncomingTalkListener.onIncomingTalkStopped(session, getActiveChannels());
        }
        if (getActiveChannels().isEmpty()) stop();
    }

    // IncomingAudioListener
    @Override
    public void onMessageReceived(Message message) {
        Log.d(TAG, "onMessageReceived: " + MessageType.getText(message.getMessageType()));
        final Channel channel = new Channel(
                message.getChannelType(), message.getSenderId(), message.getReceiverId());
        switch (message.getMessageType()) {
            case MessageType.START_TALKING:
                Log.d(TAG, "onStartTalkingMessage, message: " + message.toString());
                if (mActiveSessions.containsKey(channel.toString())) {
                    mActiveSessions.get(channel.toString()).setStartSignal(true);
                } else {
//                    if (mAudioTrack == null) mAudioTrack = newAudioTrack();
                    initAudioTrackIfNeeded();
                    IncomingTalkSession session = new IncomingTalkSession(channel, newTimeoutCheckRunner(channel), mAudioTrack.getAudioSessionId());
                    session.setStartSignal(true);
                    mActiveSessions.put(channel.toString(), session);
                    if (mIncomingTalkListener != null) {
                        mIncomingTalkListener.onIncomingTalkStarted(session, getActiveChannels());
                    }
                }
                break;
            case MessageType.AUDIO:
//                Log.d(TAG, "onAudioTalkingMessage: " + message.toString());
                final IncomingTalkSession session;
                if (mActiveSessions.containsKey(channel.toString())) {
                    session = mActiveSessions.get(channel.toString());
                    if (!session.isActive() && (System.currentTimeMillis() - session.getStopTime()) > 250) {
                        session.start();
                        initAudioTrackIfNeeded();
                        session.setAudioSessionId(mAudioTrack.getAudioSessionId());
                        if (mIncomingTalkListener != null) {
                            mIncomingTalkListener.onIncomingTalkStarted(session, getActiveChannels());
                        }
                    }
                } else {
//                    if (mAudioTrack == null) mAudioTrack = newAudioTrack();
                    initAudioTrackIfNeeded();
                    session = new IncomingTalkSession(channel, newTimeoutCheckRunner(channel), mAudioTrack.getAudioSessionId());
                    session.setStartSignal(true);
                    mActiveSessions.put(channel.toString(), session);
                    if (mIncomingTalkListener != null) {
                        mIncomingTalkListener.onIncomingTalkStarted(session, getActiveChannels());
                    }
                }
                if (!session.isActive()) return;
                play(message);
                mBackgroundHandler.removeCallbacks(session.getTimeoutCheckRunner());
                mBackgroundHandler.postDelayed(session.getTimeoutCheckRunner(), 5000);
                break;
            case MessageType.STOP_TALKING:
//                Log.d(TAG, "onStopTalkingMessage, message: " + message.toString());
//                Log.d(TAG, "onStopTalkingMessage, ack id: " + message.getAckIds());
                stopPlaying(channel, message);
                break;
        }
    }

    @Override
    public void onConnectionFailure(VoicePingException e) {
        if (mIncomingTalkListener != null) {
            mIncomingTalkListener.onIncomingTalkError(e);
        }
    }

    private List<Channel> getActiveChannels() {
        List<Channel> activeChannels = new ArrayList<>();
        for (Map.Entry<String, IncomingTalkSession> entry : mActiveSessions.entrySet()) {
            if (entry.getValue().isActive()) {
                activeChannels.add(entry.getValue().getChannel());
            }
        }
        return activeChannels;
    }

    private Runnable newTimeoutCheckRunner(final Channel channel) {
        return new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "no data timeout!");
                /*onConnectionFailure(new VoicePingException(
                        "No data timeout. Error because of no data received for 5 seconds!", ErrorCode.STOP_SIGNAL_TIMEOUT));*/
                stopPlaying(channel, null);
            }
        };
    }

    private String getDownloadUrl(Message message) {
        String downloadUrl = null;
        try {
            String serverUrl = mServerUrl;
            if (serverUrl.startsWith("ws")) {
                serverUrl = serverUrl.replaceFirst("ws", "http");
            }
            JSONObject jsonObject = new JSONObject(message.getAckIds());
            String messageId = jsonObject.getString("message_id");
            int duration = jsonObject.getInt("duration");
            if (duration > 0) downloadUrl = serverUrl + "/files/audio/" + messageId;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return downloadUrl;
    }

    private int getDurationInServer(Message message) {
        try {
            JSONObject jsonObject = new JSONObject(message.getAckIds());
            int duration = jsonObject.getInt("duration");
            Log.d(TAG, "PTT in server, duration: " + duration);
            return duration;
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
