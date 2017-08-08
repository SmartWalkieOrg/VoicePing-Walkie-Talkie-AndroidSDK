package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.smartwalkie.voicepingsdk.events.AudioDataEvent;
import com.smartwalkie.voicepingsdk.listeners.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;
import com.smartwalkie.voicepingsdk.services.RecorderService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.LinkedBlockingQueue;


public class Recorder implements OutgoingAudioListener {
    private static final String TAG = Recorder.class.getSimpleName();

    /*public static Recorder getInstance() {
        if (instance == null) instance = new Recorder();
        return instance;
    }*/

//    private static Recorder instance;

    private Context mContext;
    private boolean isRecording;
    private String receiverId;
    private int channelType;

    private static final int STOPPED = 0;
    private static final int STARTED = 10;
    private static final int RECORDING = 20;
    private static final int SENDING = 30;
    private static final int WAITING_FOR_ACK_END = 60;

    private static final int ACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    private static int state = STARTED;

    private HandlerThread senderThread;
    private Handler senderHandler;
    private LinkedBlockingQueue<byte[]> mBlockingQueue;

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

    public Recorder(Context context) {
        mContext = context;
        mBlockingQueue = new LinkedBlockingQueue<>();
        state = STOPPED;
        EventBus.getDefault().register(this);
//        initSenderThread();
    }

    private void initSenderThread() {
        mBlockingQueue = new LinkedBlockingQueue<>();
        senderThread = new HandlerThread("SenderThread", Thread.MAX_PRIORITY);
        senderThread.start();
        senderHandler = new Handler(senderThread.getLooper()) {
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case START:
                        sendAckStart();
                        break;
                    case STOP_FOR_NOT_RECEIVED_ACK_START:
                        Log.d(TAG, "stop for not received ack start");
                        stopRecording();
                        state = STOPPED;
                        break;
                    case STOP_FOR_RECEIVED_ACK_START_FAILED:
                        Log.d(TAG, "stop for received ack start failed");
                        stopRecording();
                        state = STOPPED;
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
        state = STOPPED;
    }

    public void startTalking(String receiverId, int channelType) {
        Log.v(TAG, "startTalking");
        this.receiverId = receiverId;
        this.channelType = channelType;
        sendAckStart();
//        senderHandler.sendEmptyMessage(START);
    }

    public void stopTalking() {
        Log.v(TAG, "stopTalking");
//        senderHandler.removeMessages(CONTINUE_FOR_SENDING_AUDIO_DATA);
        stopRecording();
        sendAckStop();
//        senderHandler.sendEmptyMessage(STOP_NORMALLY);
    }

    private void sendAckStart() {
        Log.v(TAG, "sendAckStart");
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        Message message = MessageHelper.createAckStartMessage(
                userId, receiverId, channelType, System.currentTimeMillis());
        Connection.getInstance().send(message.getPayload());
        state = STARTED;
    }

    @Subscribe
    public void onAudioDataEvent(AudioDataEvent audioDataEvent) {
        send(audioDataEvent.getData());
    }

    public void send(byte[] data) {
        try {
            mBlockingQueue.put(data);
//            sendAudio();
            senderHandler.sendEmptyMessage(CONTINUE_FOR_SENDING_AUDIO_DATA);
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
        if (data == null || data.length == 0) return;
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        Message message = MessageHelper.createAudioMessage(
                userId, receiverId, channelType, data, data.length);
        Connection.getInstance().send(message.getPayload());
        state = SENDING;
    }

    private void sendAckStop() {
        Log.v(TAG, "sendAckStop");
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        byte[] message = MessageHelper.createAckStopMessage(userId, receiverId, channelType);
        Connection.getInstance().send(message);
        state = WAITING_FOR_ACK_END;
//        senderHandler.sendEmptyMessageDelayed(UPDATE_FOR_NOT_RECEIVED_ACK_END, ACK_TIMEOUT_IN_MILLIS);
    }

    private void startRecording() {
        Log.v(TAG, "startRecording");
//        isRecording = true;
        mContext.startService(new Intent(mContext, RecorderService.class));
        state = RECORDING;
    }

    private void stopRecording() {
        Log.v(TAG, "stopRecording");
//        isRecording = false;
        mContext.stopService(new Intent(mContext, RecorderService.class));
        mBlockingQueue.clear();
    }

    public boolean isRecording() {
        return isRecording;
    }

    // OutgoingAudioListener
    @Override
    public void onAckStartSucceed(Message message) {
        Log.v(TAG, "onAckStartSucceed: " + message);
        startRecording();
//        senderHandler.sendEmptyMessage(CONTINUE_FOR_RECEIVED_ACK_START);
    }

    @Override
    public void onAckStartFailed(Message message) {
        Log.v(TAG, "onAckStartFailed: " + message);
        stopRecording();
        state = STOPPED;
//        senderHandler.sendEmptyMessage(STOP_FOR_RECEIVED_ACK_START_FAILED);
    }

    @Override
    public void onAckEndSucceed(Message message) {
        Log.v(TAG, "onAckEndSucceed: " + message);
//        senderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_ACK_END);
    }

    @Override
    public void onMessageDelivered(Message message) {
        Log.v(TAG, "onMessageDelivered: " + message);
//        senderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_STATUS_DELIVERED);
    }

    @Override
    public void onMessageRead(Message message) {
        Log.v(TAG, "onMessageRead: " + message);
//        senderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_STATUS_READ);
    }
}
