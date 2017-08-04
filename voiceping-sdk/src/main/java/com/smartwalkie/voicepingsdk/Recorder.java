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
import com.smartwalkie.voicepingsdk.listeners.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.Session;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;


public class Recorder implements OutgoingAudioListener {
    private static final String TAG = Recorder.class.getSimpleName();

    public static Recorder getInstance() {
        if (instance == null) instance = new Recorder();
        return instance;
    }

    private static Recorder instance;

    private Opus opus;
    private boolean isRecording;

    private Thread recorderThread;

    private int receiverId;
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
    private LinkedBlockingQueue<byte[]> blockingQueue;

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

    private Recorder() {
        if(AudioParameters.USE_CODEC) {
            this.opus = Opus.getCodec(AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL);
        }
        initSenderThread();
    }

    private void initSenderThread() {
        blockingQueue = new LinkedBlockingQueue<>();
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

    public void startTalking(int receiverId, int channelType) {
        Log.v(TAG, "startTalking");
        this.receiverId = receiverId;
        this.channelType = channelType;
        senderHandler.sendEmptyMessage(START);
    }

    public void stopTalking() {
        Log.v(TAG, "stopTalking");
        senderHandler.removeMessages(CONTINUE_FOR_SENDING_AUDIO_DATA);
        senderHandler.sendEmptyMessage(STOP_NORMALLY);
    }

    private void sendAckStart() {
        Log.v(TAG, "sendAckStart");
        Message message = MessageHelper.createAckStartMessage(Session.getInstance().getUserId(), receiverId, channelType, System.currentTimeMillis());
        Connection.getInstance().send(message.getPayload());
        state = STARTED;
//        senderHandler.sendEmptyMessageDelayed(STOP_FOR_NOT_RECEIVED_ACK_START, ACK_TIMEOUT_IN_MILLIS);
    }

    private void send(byte[] payload, int offset, int length) {
        try {
            blockingQueue.put(Arrays.copyOfRange(payload, offset, length));
            senderHandler.sendEmptyMessage(CONTINUE_FOR_SENDING_AUDIO_DATA);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void sendAudio() {
        if (blockingQueue.isEmpty()) return;
        byte[] data = null;
        try {
            data = blockingQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (data == null || data.length == 0) return;
        Message message = MessageHelper.createAudioMessage(Session.getInstance().getUserId(), receiverId, channelType, data, data.length);
        Connection.getInstance().send(message.getPayload());
        state = SENDING;
    }

    private void sendAckStop() {
        Log.v(TAG, "sendAckStop");
        byte[] message = MessageHelper.createAckStopMessage(Session.getInstance().getUserId(), receiverId, channelType);
        Connection.getInstance().send(message);
        state = WAITING_FOR_ACK_END;
        senderHandler.sendEmptyMessageDelayed(UPDATE_FOR_NOT_RECEIVED_ACK_END, ACK_TIMEOUT_IN_MILLIS);
    }

    private void startRecording() {
        Log.v(TAG, "startRecording");
        isRecording = true;
        recorderThread = new RecorderThread();
        recorderThread.start();
        state = RECORDING;
    }

    private void stopRecording() {
        Log.v(TAG, "stopRecording");
        isRecording = false;
        blockingQueue.clear();
    }

    public boolean isRecording() {
        return isRecording;
    }

    // OutgoingAudioListener
    @Override
    public void onAckStartSucceed(Message message) {
        Log.v(TAG, "onAckStartSucceed: " + message);
        senderHandler.sendEmptyMessage(CONTINUE_FOR_RECEIVED_ACK_START);
    }

    @Override
    public void onAckStartFailed(Message message) {
        Log.v(TAG, "onAckStartFailed: " + message);
        senderHandler.sendEmptyMessage(STOP_FOR_RECEIVED_ACK_START_FAILED);
    }

    @Override
    public void onAckEndSucceed(Message message) {
        Log.v(TAG, "onAckEndSucceed: " + message);
        senderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_ACK_END);
    }

    @Override
    public void onMessageDelivered(Message message) {
        Log.v(TAG, "onMessageDelivered: " + message);
        senderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_STATUS_DELIVERED);
    }

    @Override
    public void onMessageRead(Message message) {
        Log.v(TAG, "onMessageRead: " + message);
        senderHandler.sendEmptyMessage(UPDATE_FOR_RECEIVED_STATUS_READ);
    }
    // OutgoingAudioListener

    class RecorderThread extends Thread {
        private AudioRecord audioRecord;
        private AudioManager audioManager;

        private long mStartRecordingTimestamp;

        public RecorderThread() {
            audioManager = (AudioManager) VoicePing.getApplication().getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL_CONFIG, AudioParameters.AUDIO_FORMAT, AudioParameters.RECORD_MIN_BUFFER_SIZE);
            try {
                audioRecord.startRecording();
                mStartRecordingTimestamp = System.currentTimeMillis();
            } catch (IllegalStateException ise) {
                ise.printStackTrace();
                return;
            }

            int numberOfFrames = 0;
            while (isRecording) {
                Log.d(getClass().getSimpleName(), "isRecording... number of frames: " + numberOfFrames);
                // check if message is too long
                long currentTimestamp = System.currentTimeMillis();
                long distance = currentTimestamp - mStartRecordingTimestamp;
                if (distance > 60 * 1000 + 5000) {
                    break;
                }

                byte[] recordedBytes = new byte[AudioParameters.FRAME_SIZE * 2 * AudioParameters.CHANNEL];
                int numOfFrames = audioRecord.read(recordedBytes, 0, AudioParameters.FRAME_SIZE * 2);
                if (numOfFrames == AudioRecord.ERROR_INVALID_OPERATION) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    audioManager.stopBluetoothSco();
                    isRecording = false;
                    return;
                }

                if (AudioParameters.USE_CODEC) {
                    byte[] encodedBytes = new byte[recordedBytes.length];
                    int encodedSize = opus.encode(recordedBytes, 0, AudioParameters.FRAME_SIZE, encodedBytes, 0, encodedBytes.length);
                    numberOfFrames++;
                    send(encodedBytes, 0, encodedSize);
                } else {
                    send(recordedBytes, 0, numOfFrames);
                }
            }

            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            interrupt();
        }
    }

}
