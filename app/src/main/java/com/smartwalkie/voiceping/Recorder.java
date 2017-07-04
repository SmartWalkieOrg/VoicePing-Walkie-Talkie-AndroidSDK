package com.smartwalkie.voiceping;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.media2359.voiceping.codec.Opus;


public class Recorder {
    private static final String TAG = Recorder.class.getSimpleName();

    private Opus opus;
    boolean isRecording;

    private Thread recorderThread;

    public Recorder() {
        if(AudioParams.USE_CODEC) {
            this.opus = Opus.getCodec(AudioParams.SAMPLE_RATE, AudioParams.CHANNEL);
        }
    }

    private void sendMessage(byte[] payload, int offset, int length) {

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

    class RecorderThread extends Thread {
        private AudioRecord audioRecord;
        private AudioManager am;

        private long mStartRecordingTimestamp;

        public RecorderThread() {
            am = (AudioManager) VoicePingApplication.getInstance().getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            int audioSource;

            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;

            this.audioRecord = new AudioRecord(
                    audioSource,
                    AudioParams.SAMPLE_RATE,
                    AudioParams.CHANNEL_CONFIG,
                    AudioParams.AUDIO_FORMAT,
                    AudioParams.RECORD_MIN_BUFFER_SIZE);

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

                byte[] pcmFrame = new byte[AudioParams.FRAME_SIZE * 2 * AudioParams.CHANNEL];
                int numOfFrames = audioRecord.read(pcmFrame, 0, AudioParams.FRAME_SIZE * 2);
                if (numOfFrames == AudioRecord.ERROR_INVALID_OPERATION) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    am.stopBluetoothSco();
                    isRecording = false;
                    return;
                }

                if (AudioParams.USE_CODEC) {
                    byte[] readData = new byte[pcmFrame.length];
                    int encodedSize = opus.encode(pcmFrame, 0, AudioParams.FRAME_SIZE, readData, 0, readData.length);
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
