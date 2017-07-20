package com.smartwalkie.voiceping;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.media2359.voiceping.codec.Opus;
import com.smartwalkie.voiceping.constants.AudioParameters;
import com.smartwalkie.voiceping.listeners.OutgoingAudioListener;
import com.smartwalkie.voiceping.models.Message;


public class Recorder implements OutgoingAudioListener {
    private static final String TAG = Recorder.class.getSimpleName();

    public static Recorder getInstance() {
        if (instance == null) instance = new Recorder();
        return instance;
    }

    private static Recorder instance;

    private Opus opus;
    boolean isRecording;

    private Thread recorderThread;

    private int senderId;
    private int receiverId;
    private int channelType;

    private Recorder() {
        if(AudioParameters.USE_CODEC) {
            this.opus = Opus.getCodec(AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL);
        }
    }

    private void sendMessage(byte[] payload, int offset, int length) {

    }

    public void startTalking(int receiverId, int channelType) {
        byte[] message = MessageHelper.createStartRecordMessage(56, receiverId, channelType, 1);
        Connection.getInstance().send(message);
    }

    public void startRecording() {
        isRecording = true;
        recorderThread = new RecorderThread();
        recorderThread.start();
    }

    public void stopRecording() {
        isRecording = false;
        try {
            if(recorderThread != null) {
                recorderThread.interrupt();
                recorderThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    // OutgoingAudioListener
    @Override
    public void onAckStartSucceed(Message message) {
        Log.v(TAG, "onAckStartSucceed: " + message);
    }

    @Override
    public void onAckStartFailed(Message message) {
        Log.v(TAG, "onAckStartFailed: " + message);
    }

    @Override
    public void onAckEndSucceed(Message message) {
        Log.v(TAG, "onAckEndSucceed: " + message);
    }

    @Override
    public void onMessageDelivered(Message message) {
        Log.v(TAG, "onMessageDelivered: " + message);
    }

    @Override
    public void onMessageRead(Message message) {
        Log.v(TAG, "onMessageRead: " + message);
    }
    // OutgoingAudioListener

    class RecorderThread extends Thread {
        private AudioRecord audioRecord;
        private AudioManager am;

        private long mStartRecordingTimestamp;

        public RecorderThread() {
            am = (AudioManager) VoicePingClient.getInstance().getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AudioParameters.SAMPLE_RATE, AudioParameters.CHANNEL_CONFIG, AudioParameters.AUDIO_FORMAT, AudioParameters.RECORD_MIN_BUFFER_SIZE);

            try {
                audioRecord.startRecording();
                mStartRecordingTimestamp = System.currentTimeMillis();
            } catch (Exception e) {
                return;
            }

            int totalFrame = 0;

            while (isRecording) {

                // check if message is too long
                long currentTimestamp = System.currentTimeMillis();
                long distance = currentTimestamp - mStartRecordingTimestamp;
                if (distance > 60 * 1000 + 5000) {
                    break;
                }

                byte[] pcmFrame = new byte[AudioParameters.FRAME_SIZE * 2 * AudioParameters.CHANNEL];
                int numOfFrames = audioRecord.read(pcmFrame, 0, AudioParameters.FRAME_SIZE * 2);
                if (numOfFrames == AudioRecord.ERROR_INVALID_OPERATION) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    am.stopBluetoothSco();
                    isRecording = false;
                    return;
                }

                if (AudioParameters.USE_CODEC) {
                    byte[] readData = new byte[pcmFrame.length];
                    int encodedSize = opus.encode(pcmFrame, 0, AudioParameters.FRAME_SIZE, readData, 0, readData.length);
                    totalFrame ++;
                    sendMessage(readData, 0, encodedSize);
                } else {
                    sendMessage(pcmFrame, 0, numOfFrames);
                }
            }

            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

}
