package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
import com.smartwalkie.voicepingsdk.models.MessageType;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;


public class Recorder implements OutgoingAudioListener, AudioRecorder {

    private final String TAG = Recorder.class.getSimpleName();

    public volatile static boolean IS_RECORDING;

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
    private RecorderThread mRecorderThread;

    private static final int ACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    private static final int CONTINUE_FOR_SENDING_AUDIO_DATA = 1000;

    public Recorder(Context context, Connection connection) {
        mContext = context;
        mConnection = connection;
        mBlockingQueue = new LinkedBlockingQueue<>();
        EventBus.getDefault().register(this);
//        initSenderThread();
        mOpus = Opus.getCodec(AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL);
    }

    private void initSenderThread() {
        HandlerThread senderThread = new HandlerThread("SenderThread", Thread.MAX_PRIORITY);
        senderThread.start();
        mSenderHandler = new Handler(senderThread.getLooper()) {

            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case CONTINUE_FOR_SENDING_AUDIO_DATA:
                        sendAudio();
                        break;
                }
            }
        };
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
        IS_RECORDING = true;
        sendAckStart();
    }

    public void stopTalking() {
        Log.v(TAG, "stopTalking");
//        mSenderHandler.removeMessages(CONTINUE_FOR_SENDING_AUDIO_DATA);
        stopRecording();
        sendAckStop();
        if (mChannelListener != null) mChannelListener.onOutgoingTalkStopped();
    }

    private void sendAckStart() {
        Log.v(TAG, "sendAckStart");
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        Message message = MessageHelper.createAckStartMessage(
                userId, mReceiverId, mChannelType, System.currentTimeMillis());
        mConnection.send(message.getPayload());
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

        if (data == null || data.length == 0) return;
        if (mAudioInterceptor != null) data = mAudioInterceptor.proceed(data);

        if (AudioParameters.USE_CODEC) {
            byte[] encodedBytes = new byte[data.length];
            int encodedSize = mOpus.encode(data, 0, AudioParameters.FRAME_SIZE, encodedBytes, 0, encodedBytes.length);
            data = Arrays.copyOfRange(encodedBytes, 0, encodedSize);
        }

        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        Message message = MessageHelper.createAudioMessage(
                userId, mReceiverId, mChannelType, data, data.length);
        mConnection.send(message.getPayload());
    }

    private void sendAckStop() {
        Log.v(TAG, "sendAckStop");
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        byte[] message = MessageHelper.createAckStopMessage(userId, mReceiverId, mChannelType);
        mConnection.send(message);
    }

    private void startRecording() {
        Log.v(TAG, "startRecording");
//        mContext.startService(new Intent(mContext, RecorderService.class));
        mRecorderThread = new RecorderThread();
        mRecorderThread.start();
    }

    private void stopRecording() {
        Log.v(TAG, "stopRecording");
        IS_RECORDING = false;
//        mBlockingQueue.clear();
        mAudioInterceptor = null;
        mRecorderThread = null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    // OutgoingAudioListener
    @Override
    public void onMessageReceived(Message message) {
        switch (message.getMessageType()) {
            case MessageType.ACK_START:
                Log.v(TAG, "onAckStartSucceed: " + message);
                if (mChannelListener != null) mChannelListener.onOutgoingTalkStarted(this);
                startRecording();
                break;
            case MessageType.ACK_START_FAILED:
                Log.v(TAG, "onAckStartFailed: " + message);
                stopRecording();
                break;
            case MessageType.ACK_END:
                Log.v(TAG, "onAckEndSucceed: " + message);
                break;
            case MessageType.MESSAGE_DELIVERED:
                Log.v(TAG, "onMessageDelivered: " + message);
                break;
            case MessageType.MESSAGE_READ:
                Log.v(TAG, "onMessageRead: " + message);
        }
    }

    @Override
    public void onError(byte[] data, PingException e) {
        Message message = MessageHelper.unpackMessage(data);
        if (message != null && message.getMessageType() == MessageType.ACK_START) {
            if (mChannelListener != null) mChannelListener.onError(e);
        }
    }

    // AudioRecorder
    @Override
    public void addAudioInterceptor(AudioInterceptor audioInterceptor) {
        mAudioInterceptor = audioInterceptor;
    }

    private class RecorderThread extends Thread {

        @Override
        public void run() {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    AudioParameters.SAMPLE_RATE,
                    AudioParameters.CHANNEL_CONFIG,
                    AudioParameters.AUDIO_FORMAT,
                    AudioParameters.RECORD_MIN_BUFFER_SIZE);

            long startRecordingTimestamp;
            try {
                audioRecord.startRecording();
                startRecordingTimestamp = System.currentTimeMillis();
            } catch (IllegalStateException ise) {
                ise.printStackTrace();
                return;
            }

            int numberOfFrames = 0;
            while (Recorder.IS_RECORDING) {
//            Log.d(getClass().getSimpleName(), "isRecording... number of frames: " + numberOfFrames);
                // check if message is too long
                long currentTimestamp = System.currentTimeMillis();
                long distance = currentTimestamp - startRecordingTimestamp;
                if (distance > 60 * 1000 + 5000) {
                    break;
                }

                byte[] recordedBytes = new byte[AudioParameters.FRAME_SIZE * 2 * AudioParameters.CHANNEL];
                int numOfFrames = audioRecord.read(recordedBytes, 0, AudioParameters.FRAME_SIZE * 2);
                if (numOfFrames == AudioRecord.ERROR_INVALID_OPERATION) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioManager.stopBluetoothSco();
                    Recorder.IS_RECORDING = false;
                    interrupt();
                    return;
                }

                numberOfFrames++;
//                EventBus.getDefault().post(new AudioDataEvent(Arrays.copyOfRange(recordedBytes, 0, numOfFrames)));

                byte[] data = Arrays.copyOfRange(recordedBytes, 0, numOfFrames);
                if (data == null || data.length == 0) return;
                if (mAudioInterceptor != null) data = mAudioInterceptor.proceed(data);

                if (AudioParameters.USE_CODEC) {
                    byte[] encodedBytes = new byte[data.length];
                    int encodedSize = mOpus.encode(data, 0, AudioParameters.FRAME_SIZE, encodedBytes, 0, encodedBytes.length);
                    data = Arrays.copyOfRange(encodedBytes, 0, encodedSize);
                }

                String userId = VoicePingPrefs.getInstance(mContext).getUserId();
                Message message = MessageHelper.createAudioMessage(
                        userId, mReceiverId, mChannelType, data, data.length);
                mConnection.send(message.getPayload());
            }

            audioRecord.stop();
            audioRecord.release();
        }
    }
}
