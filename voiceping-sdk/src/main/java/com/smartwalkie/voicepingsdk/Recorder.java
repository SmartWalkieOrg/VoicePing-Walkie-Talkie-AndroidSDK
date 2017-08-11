package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.media2359.voiceping.codec.Opus;
import com.smartwalkie.voicepingsdk.constants.AudioParameters;
import com.smartwalkie.voicepingsdk.events.AudioDataEvent;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listeners.AudioRecorder;
import com.smartwalkie.voicepingsdk.listeners.ChannelListener;
import com.smartwalkie.voicepingsdk.listeners.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;
import com.smartwalkie.voicepingsdk.services.RecorderService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;


public class Recorder implements OutgoingAudioListener, AudioRecorder {

    private final String TAG = Recorder.class.getSimpleName();

    public static boolean IS_RECORDING;

    private Context mContext;
    private Connection mConnection;
    private Handler mSenderHandler;
    private LinkedBlockingQueue<byte[]> mBlockingQueue;
    private boolean isRecording;
    private String mReceiverId;
    private int mChannelType;
    private Opus mOpus;
    private ChannelListener mChannelListener;
    private AudioInterceptor mAudioInterceptor;

    private static final int STOPPED = 0;
    private static final int STARTED = 10;
    private static final int RECORDING = 20;
    private static final int SENDING = 30;
    private static final int WAITING_FOR_ACK_END = 60;

    private static final int ACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    private static int mState;

    private static final int START = 1000;
    private static final int STOP_FOR_RECEIVED_ACK_START_FAILED = 2000;
    private static final int STOP_FOR_NOT_RECEIVED_ACK_START = 3000;
    private static final int CONTINUE_FOR_RECEIVED_ACK_START = 4000;
    private static final int CONTINUE_FOR_SENDING_AUDIO_DATA = 5000;
    private static final int STOP_NORMALLY = 6000;
    private static final int UPDATE_FOR_NOT_RECEIVED_ACK_END = 7000;
    private static final int UPDATE_FOR_RECEIVED_ACK_END = 8000;
    private static final int UPDATE_FOR_RECEIVED_STATUS_DELIVERED = 9000;
    private static final int UPDATE_FOR_RECEIVED_STATUS_READ = 10000;

    public Recorder(Context context, Connection connection) {
        mContext = context;
        mConnection = connection;
        mBlockingQueue = new LinkedBlockingQueue<>();
        mState = STOPPED;
        EventBus.getDefault().register(this);
        initSenderThread();
        mOpus = Opus.getCodec(AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL);
    }

    private void initSenderThread() {
        HandlerThread senderThread = new HandlerThread("SenderThread", Thread.MAX_PRIORITY);
        senderThread.start();
        mSenderHandler = new Handler(senderThread.getLooper()) {

            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case START:
                        sendAckStart();
                        break;
                    case STOP_FOR_NOT_RECEIVED_ACK_START:
                        Log.d(TAG, "stop for not received ack start");
                        stopRecording();
                        mState = STOPPED;
                        break;
                    case STOP_FOR_RECEIVED_ACK_START_FAILED:
                        Log.d(TAG, "stop for received ack start failed");
                        stopRecording();
                        mState = STOPPED;
                        break;
                    case CONTINUE_FOR_RECEIVED_ACK_START:
                        startRecording();
                        break;
                    case CONTINUE_FOR_SENDING_AUDIO_DATA:
                        sendAudio();
                        break;
                    case STOP_NORMALLY:
                        Log.d(TAG, "stop normally...");
                        stopRecording();
                        sendAckStop();
                        break;
                    case UPDATE_FOR_NOT_RECEIVED_ACK_END:
                        break;
                    case UPDATE_FOR_RECEIVED_ACK_END:
                        break;
                    case UPDATE_FOR_RECEIVED_STATUS_DELIVERED:
                        break;
                    case UPDATE_FOR_RECEIVED_STATUS_READ:
                        break;
                }
            }
        };
        mState = STOPPED;
    }

    public void setChannelListener(ChannelListener channelListener) {
        mChannelListener = channelListener;
    }

    public void startTalking(String receiverId, int channelType) {
        if (!NetworkUtil.isNetworkConnected(mContext) && mChannelListener != null) {
            mChannelListener.onError(new PingException("Please check your internet connection!"));
            return;
        }
        Log.v(TAG, "startTalking");
        this.mReceiverId = receiverId;
        this.mChannelType = channelType;
        mSenderHandler.sendEmptyMessage(START);
    }

    public void stopTalking() {
        Log.v(TAG, "stopTalking");
        mSenderHandler.removeMessages(CONTINUE_FOR_SENDING_AUDIO_DATA);
        mSenderHandler.sendEmptyMessage(STOP_NORMALLY);
    }

    private void sendAckStart() {
        Log.v(TAG, "sendAckStart");
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        Message message = MessageHelper.createAckStartMessage(
                userId, mReceiverId, mChannelType, System.currentTimeMillis());
        mConnection.send(message.getPayload());
        mState = STARTED;
    }

    @Subscribe
    public void onAudioDataEvent(AudioDataEvent audioDataEvent) {
        try {
            mBlockingQueue.put(audioDataEvent.getData());
            mSenderHandler.sendEmptyMessage(CONTINUE_FOR_SENDING_AUDIO_DATA);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void sendAudio() {
        if (mBlockingQueue.isEmpty()) return;
        byte[] data = null;
        try {
            data = mBlockingQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mChannelListener != null) mChannelListener.onTalkStarted(this);
        if (mAudioInterceptor != null) data = mAudioInterceptor.proceed(data);

        if (data == null || data.length == 0) return;

        if (AudioParameters.USE_CODEC) {
            byte[] encodedBytes = new byte[data.length];
            int encodedSize = mOpus.encode(data, 0, AudioParameters.FRAME_SIZE, encodedBytes, 0, encodedBytes.length);
            data = Arrays.copyOfRange(encodedBytes, 0, encodedSize);
        }

        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        Message message = MessageHelper.createAudioMessage(
                userId, mReceiverId, mChannelType, data, data.length);
        mConnection.send(message.getPayload());
        mState = SENDING;
    }

    private void sendAckStop() {
        Log.v(TAG, "sendAckStop");
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        byte[] message = MessageHelper.createAckStopMessage(userId, mReceiverId, mChannelType);
        mConnection.send(message);
        mState = WAITING_FOR_ACK_END;
        mSenderHandler.sendEmptyMessageDelayed(UPDATE_FOR_NOT_RECEIVED_ACK_END, ACK_TIMEOUT_IN_MILLIS);
    }

    private void startRecording() {
        Log.v(TAG, "startRecording");
        IS_RECORDING = true;
        mContext.startService(new Intent(mContext, RecorderService.class));
        mState = RECORDING;
    }

    private void stopRecording() {
        Log.v(TAG, "stopRecording");
        IS_RECORDING = false;
        mBlockingQueue.clear();
    }

    public boolean isRecording() {
        return isRecording;
    }

    // OutgoingAudioListener
    @Override
    public void onAckStartSucceed(Message message) {
        Log.v(TAG, "onAckStartSucceed: " + message);
        mSenderHandler.sendEmptyMessage(CONTINUE_FOR_RECEIVED_ACK_START);
    }

    @Override
    public void onAckStartFailed(Message message) {
        Log.v(TAG, "onAckStartFailed: " + message);
        mSenderHandler.sendEmptyMessage(STOP_FOR_RECEIVED_ACK_START_FAILED);
    }

    @Override
    public void onAckEndSucceed(Message message) {
        Log.v(TAG, "onAckEndSucceed: " + message);
        mSenderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_ACK_END);
    }

    @Override
    public void onMessageDelivered(Message message) {
        Log.v(TAG, "onMessageDelivered: " + message);
        mSenderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_STATUS_DELIVERED);
    }

    @Override
    public void onMessageRead(Message message) {
        Log.v(TAG, "onMessageRead: " + message);
        mSenderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_STATUS_READ);
    }

    // AudioRecorder
    @Override
    public void addAudioInterceptor(AudioInterceptor audioInterceptor) {
        mAudioInterceptor = audioInterceptor;
    }
}
