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
import com.smartwalkie.voicepingsdk.models.MessageType;


public class Player implements IncomingAudioListener, AudioPlayer {

    private final String TAG = Player.class.getSimpleName();

    private Context mContext;
    private Opus mOpus;
    private AudioTrack mAudioTrack;
    private Handler mPlayerHandler;
    private ChannelListener mChannelListener;
    private AudioInterceptor mAudioInterceptor;

    private final int INIT = 0;
    private final int START = 1;
    private final int PLAY = 2;
    private final int STOP = 3;
    private final int DESTROY = 4;
    private final int END = 5;
    private final int STOP_PLAYING_AFTER_A_TIME = 6;
    private final int SPEAKER_MODE_CHANGE = 7;

    private int mCurrentState;
    private long mStartTalkingTime = 0;
    private byte[] mCurrentPlayingPlayload;

    public Player(Context context) {
        mContext = context;
        mOpus = Opus.getCodec(AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL);
        initPlayerThread();
    }

    private void initPlayerThread() {
        HandlerThread playerThread = new HandlerThread("PlayerThread", Thread.MAX_PRIORITY);
        playerThread.start();
        mPlayerHandler = new Handler(playerThread.getLooper(), new Handler.Callback() {

            @Override
            public boolean handleMessage(android.os.Message msg) {
                mCurrentState = msg.what;

                switch (msg.what) {
                    case INIT:
                        mAudioTrack = initAudioTrack();
                        return true;
                    case START:
                        mAudioTrack.play();
                        return true;
                    case PLAY:
                        byte[] payload = (byte[]) msg.obj;
                        mCurrentPlayingPlayload = payload;
                        byte[] pcmFrame = new byte[1920];
                        if (AudioParameters.USE_CODEC) {
                            int decodeSize = mOpus.decode(payload, 0, payload.length, pcmFrame, 0, AudioParameters.FRAME_SIZE, 0);
                            Log.v(TAG, "USE_CODEC");
                            if (decodeSize > 0) {
                                if (mChannelListener != null) {
                                    mChannelListener.onTalkReceived(Player.this);
                                }
                                if (mAudioInterceptor != null) {
                                    pcmFrame = mAudioInterceptor.proceed(pcmFrame);
                                    if (pcmFrame == null || pcmFrame.length == 0) return false;
                                }
                                mAudioTrack.write(pcmFrame, 0, pcmFrame.length);
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
                            mAudioTrack.write(payload, 0, payload.length);
                            Log.v(TAG, "!USE_CODEC");
                        }
                        return true;
                    case STOP:
                        Log.d(TAG, "Stop...");
                        mAudioTrack.stop();
                        mAudioTrack.flush();
                        mCurrentPlayingPlayload = null;
                        mStartTalkingTime =0;
                        return true;
                    case DESTROY:
                        Log.d(TAG, "Destroy...");
                        mAudioTrack.stop();
                        mAudioTrack.flush();
                        mCurrentPlayingPlayload = null;
                        mAudioTrack.release();
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
        if (mCurrentState != STOP) {
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

    private void forceStop(){
        mPlayerHandler.removeMessages(PLAY);
        mPlayerHandler.removeMessages(STOP_PLAYING_AFTER_A_TIME);
        stop();
    }

    private void play(byte[] bytes) {
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

    }

    @Override
    public void onAudioTalkingMessage(Message message) {

    }

    @Override
    public void onStopTalkingMessage(Message message) {

    }

    @Override
    public void onMessageReceived(Message message) {
        switch (message.getMessageType()) {
            case MessageType.START_TALKING:
                Log.v(TAG, "onStartTalkingMessage: " + message.toString());
                break;
            case MessageType.AUDIO:
                Log.v(TAG, "onAudioTalkingMessage: " + message.toString());
                play(message.getPayload());
                break;
            case MessageType.STOP_TALKING:
                Log.v(TAG, "onStopTalkingMessage: " + message.toString());
                break;
        }
    }

    // AudioPlayer
    @Override
    public void addAudioInterceptor(AudioInterceptor audioInterceptor) {
        mAudioInterceptor = audioInterceptor;
    }
}
