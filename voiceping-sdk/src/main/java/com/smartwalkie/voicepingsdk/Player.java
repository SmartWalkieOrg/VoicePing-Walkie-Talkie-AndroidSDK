package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.media2359.voiceping.codec.Opus;
import com.smartwalkie.voicepingsdk.constants.AudioParameters;
import com.smartwalkie.voicepingsdk.listeners.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listeners.AudioPlayer;
import com.smartwalkie.voicepingsdk.listeners.ChannelListener;
import com.smartwalkie.voicepingsdk.listeners.IncomingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;


public class Player implements IncomingAudioListener, AudioPlayer {
    public final String TAG = Player.class.getSimpleName();

    public Context mContext;
    public AudioTrack audioTrack;
    private Handler mPlayerHandler;
    private ChannelListener mChannelListener;
    private AudioInterceptor mAudioInterceptor;

    public static final int INIT = 0;
    public static final int START = 1;
    public static final int PLAY = 2;
    public static final int STOP = 3;
    public static final int DESTROY = 4;
    public static final int END = 5;
    public static final int STOP_PLAYING_AFTER_A_TIME = 6;
    public static final int SPEAKER_MODE_CHANGE = 7;

    int currentState;
    long mStartTalkingTime = 0;
    private byte[] mCurrentPlayingPlayload;

    public Player(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        final Opus opus = Opus.getCodec(AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL);
        HandlerThread playerThread = new HandlerThread("PlayerThread", Thread.MAX_PRIORITY);
        playerThread.start();

        mPlayerHandler = new Handler(playerThread.getLooper(), new Handler.Callback() {

            @Override
            public boolean handleMessage(android.os.Message msg) {
                currentState = msg.what;

                switch (msg.what) {
                    case INIT:
                        audioTrack = initAudioTrack();
                        return true;
                    case START:
                        audioTrack.play();
                        return true;
                    case PLAY:
                        byte[] payload = (byte[]) msg.obj;
                        mCurrentPlayingPlayload = payload;
                        byte[] pcmFrame = new byte[1920];
                        if (AudioParameters.USE_CODEC) {
                            int decodeSize = opus.decode(payload, 0, payload.length, pcmFrame, 0, AudioParameters.FRAME_SIZE, 0);
                            Log.v(TAG, "USE_CODEC");
                            if (decodeSize > 0) {
                                if (mChannelListener != null) {
                                    mChannelListener.onTalkReceived(Player.this);
                                }
                                if (mAudioInterceptor != null) {
                                    pcmFrame = mAudioInterceptor.proceed(pcmFrame);
                                    if (pcmFrame == null || pcmFrame.length == 0) return false;
                                }
                                audioTrack.write(pcmFrame, 0, pcmFrame.length);
                                Log.v(TAG, "decodeSize: "+decodeSize);
                            }
                        } else {
                            if (mChannelListener != null) {
                                mChannelListener.onTalkReceived(Player.this);
                            }
                            if (mAudioInterceptor != null) {
                                payload = mAudioInterceptor.proceed(payload);
                                if (payload == null || payload.length == 0) return false;
                            }
                            audioTrack.write(payload, 0, payload.length);
                            Log.v(TAG, "!USE_CODEC");
                        }
                        return true;
                    case STOP:
                        Log.d(TAG, "Stop...");
                        audioTrack.stop();
                        audioTrack.flush();
                        mCurrentPlayingPlayload = null;
                        mStartTalkingTime =0;
                        return true;
                    case DESTROY:
                        Log.d(TAG, "Destroy...");
                        audioTrack.stop();
                        audioTrack.flush();
                        mCurrentPlayingPlayload = null;
                        audioTrack.release();
                        return true;
                    case STOP_PLAYING_AFTER_A_TIME:
                        Log.d(TAG, "Stop playing after a time...");
                        forceStop();
                        return true;
                }
                return false;
            }
        });
        mPlayerHandler.sendEmptyMessage(INIT);
    }

    public void start() {
        if (currentState != STOP) {
            mPlayerHandler.sendEmptyMessage(STOP);
        }
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mStartTalkingTime = System.currentTimeMillis();
        android.os.Message message = new android.os.Message();
        message.what = START;
        mPlayerHandler.sendMessage(message);
    }

    public void setChannelListener(ChannelListener channelListener) {
        mChannelListener = channelListener;
    }

    private void stop() {
        mPlayerHandler.sendEmptyMessage(STOP);
    }

    public void forceStop(){
        mPlayerHandler.removeMessages(PLAY);
        mPlayerHandler.removeMessages(STOP_PLAYING_AFTER_A_TIME);
        stop();
    }

    public void play(byte[] bytes) {
        android.os.Message message = new android.os.Message();
        message.what = PLAY;
        message.obj = bytes;
        mPlayerHandler.sendMessage(message);
    }

    private AudioTrack initAudioTrack() {
        int minBufferSize = AudioParameters.PLAY_MIN_BUFFER_SIZE;
        Log.d("Player", "init minbuffer size=" + minBufferSize);

        int streamType = AudioManager.STREAM_MUSIC;

        return new AudioTrack(
                streamType,
                AudioParameters.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioParameters.AUDIO_FORMAT,
                Math.min(minBufferSize, AudioParameters.FRAME_SIZE * AudioParameters.BUFFER_SIZE_FACTOR),
                AudioTrack.MODE_STREAM);
    }

    // IncomingAudioListener
    @Override
    public void onStartTalkingMessage(Message message) {
        Log.v(TAG, "onStartTalkingMessage: " + message.toString());
    }

    @Override
    public void onAudioTalkingMessage(Message message) {
        Log.v(TAG, "onAudioTalkingMessage: " + message.toString());
        play(message.getPayload());
    }

    @Override
    public void onStopTalkingMessage(Message message) {
        Log.v(TAG, "onStopTalkingMessage: " + message.toString());
    }

    // AudioPlayer
    @Override
    public void addAudioInterceptor(AudioInterceptor audioInterceptor) {
        mAudioInterceptor = audioInterceptor;
    }
}
