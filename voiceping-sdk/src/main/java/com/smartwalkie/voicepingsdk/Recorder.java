package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.media2359.voiceping.codec.Opus;
import com.smartwalkie.voicepingsdk.exception.ErrorCode;
import com.smartwalkie.voicepingsdk.exception.VoicePingException;
import com.smartwalkie.voicepingsdk.listener.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listener.AudioRecorder;
import com.smartwalkie.voicepingsdk.listener.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.listener.OutgoingTalkCallback;
import com.smartwalkie.voicepingsdk.model.AudioParam;
import com.smartwalkie.voicepingsdk.model.Channel;
import com.smartwalkie.voicepingsdk.model.Message;
import com.smartwalkie.voicepingsdk.model.MessageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;


class Recorder implements OutgoingAudioListener, AudioRecorder {
    private final String TAG = Recorder.class.getSimpleName();

    private volatile boolean mIsRecording;
    private Context mContext;
    private Connection mConnection;
    private AudioParam mAudioParam;
    private String mReceiverId;
    private int mChannelType;
    private Channel mChannel;
    private Opus mOpus;
    private Handler mBackgroundHandler;
    private volatile AudioManager mAudioManager;
    private volatile AudioRecord mAudioRecord;
    private String mUserId;
    private OutgoingTalkCallback mOutgoingTalkCallback;
    private volatile AudioInterceptor mAudioInterceptorBeforeEncoded;
    private volatile AudioInterceptor mAudioInterceptorAfterEncoded;
    private RecorderThread mRecorderThread;
    private volatile long mStartRecordingTime;
    private long mLastRecordDuration;
    private int mLastMessageType;
    private AudioLocalSaver mAudioLocalSaver;
    private CustomAudioRecorder mCustomAudioRecorder;

    private Runnable mStartTalkingRunner = new Runnable() {
        @Override
        public void run() {
//                Log.d(TAG, "start talking runs");
            // Callback
            if (mOutgoingTalkCallback != null) {
                mOutgoingTalkCallback.onOutgoingTalkStarted(Recorder.this);
            }
            sendAckStart();
            startRecording();
        }
    };
    private Runnable mAckStartTimeoutCheckRunner = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "ACK_START Timeout!");
            stopRecording();
            if (mOutgoingTalkCallback != null) {
                mOutgoingTalkCallback.onOutgoingTalkError(new VoicePingException(
                        "ACK_START Timeout. Failed to initiate PTT Talk!",
                        ErrorCode.ACK_START_TIMEOUT));
                mOutgoingTalkCallback = null;
            }
        }
    };
    private Runnable mAckEndTimeoutCheckRunner = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "ACK_END Timeout!");
            if (mIsRecording) return;
            stopRecording();
            if (mOutgoingTalkCallback != null) {
                mOutgoingTalkCallback.onOutgoingTalkError(new VoicePingException(
                        "ACK_END Timeout. Failed to get download url!",
                        ErrorCode.ACK_END_TIMEOUT));
                mOutgoingTalkCallback = null;
            }
        }
    };

    private final int ACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    Recorder(Context context, Connection connection, AudioParam audioParam, Looper backgroundLooper) {
        mContext = context;
        mConnection = connection;
        mBackgroundHandler = new Handler(backgroundLooper);
        setAudioParam(audioParam);
    }

    public void setAudioParam(AudioParam audioParam) {
        mAudioParam = audioParam;
        mOpus = new Opus(audioParam.getSampleRate(), audioParam.getChannelSize());
    }

    private void initAudioRecord(AudioParam audioParam) {
//        Log.d(TAG, "init audio record");
//        stopAudioRecording();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioRecord = new AudioRecord(audioParam.getAudioSource(),
                audioParam.getSampleRate(),
                audioParam.getChannelInConfig(),
                audioParam.getAudioFormat(),
                audioParam.getRecordMinBufferSize());
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public void startTalking(String receiverId, int channelType, OutgoingTalkCallback callback, String destinationPath, CustomAudioRecorder recorder) {
        // Network check
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            callback.onOutgoingTalkError(new VoicePingException("Please check your internet connection!", ErrorCode.INTERNET_DISCONNECTED));
            return;
        }

        // Connection check
        if (mConnection.getConnectionState() == ConnectionState.DISCONNECTED) {
            callback.onOutgoingTalkError(new VoicePingException("You are disconnected!", ErrorCode.SOCKET_DISCONNECTED));
            return;
        }

        // Init
//        Log.d(TAG, "startTalk at: " + System.currentTimeMillis());
        mReceiverId = receiverId;
        mChannelType = channelType;
        mChannel = new Channel(mChannelType, mUserId, mReceiverId);
        mOutgoingTalkCallback = callback;
        mIsRecording = true;
        if (destinationPath != null && !destinationPath.isEmpty()) {
            mAudioLocalSaver = new AudioLocalSaver(destinationPath);
        }
        mCustomAudioRecorder = recorder;
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mBackgroundHandler.removeCallbacks(mAckEndTimeoutCheckRunner);

        // AckStart lies here
        // Callback lies here

        // Check whether should start recording immediately or start with delay
        long diffFromLastStartTalking = System.currentTimeMillis() - mStartRecordingTime;
//        Log.d(TAG, "diff from last start: " + diffFromLastStartTalking);
        if (diffFromLastStartTalking < 500) {
            // Start with delay
//            Log.d(TAG, "start with delay: " + diffFromLastStartTalking);
            mBackgroundHandler.postDelayed(mStartTalkingRunner, diffFromLastStartTalking);
        } else {
            // Start immediately
//            Log.d(TAG, "start immediately");
            mStartTalkingRunner.run();
        }
    }

    public void stopTalking() {
        Log.d(TAG, "stopTalk at: " + System.currentTimeMillis());
        // Remove unneeded Runner
        mBackgroundHandler.removeCallbacks(mStartTalkingRunner);

        // Stop
        stopRecording();

        // Callback
        if (mOutgoingTalkCallback != null) {
            boolean isTooShort = false;
            boolean isTooLong = false;
            if (mLastRecordDuration < mAudioParam.getMinDuration()) isTooShort = true;
            if (mLastRecordDuration > mAudioParam.getMaxDuration()) isTooLong = true;
            mOutgoingTalkCallback.onOutgoingTalkStopped(isTooShort, isTooLong);
        }

        // AckStop
        sendAckStop();
    }

    private void sendAckStart() {
//        Log.d(TAG, "sendAckStart");
        Message message = MessageHelper.createAckStartMessage(
                mUserId, mReceiverId, mChannelType, System.currentTimeMillis());
        mConnection.send(message.getPayload());
        mBackgroundHandler.postDelayed(mAckStartTimeoutCheckRunner, ACK_TIMEOUT_IN_MILLIS);
    }

    private void sendAckStop() {
//        Log.d(TAG, "sendAckStop");
        byte[] message = MessageHelper.createAckStopMessage(mUserId, mReceiverId, mChannelType);
        mConnection.send(message);
        mBackgroundHandler.postDelayed(mAckEndTimeoutCheckRunner, ACK_TIMEOUT_IN_MILLIS);
    }

    private void startRecording() {
//        Log.d(TAG, "startRecording");
        if (mAudioLocalSaver != null) mAudioLocalSaver.init();
//        initAudioRecord();
        mRecorderThread = new RecorderThread();
        mStartRecordingTime = System.currentTimeMillis();
        mRecorderThread.start();
    }

    private void stopRecording() {
//        Log.d(TAG, "stopRecording");
        mIsRecording = false;
        mAudioInterceptorBeforeEncoded = null;
        mAudioInterceptorAfterEncoded = null;
        mRecorderThread = null;
        if (mStartRecordingTime > 0) {
            mLastRecordDuration = System.currentTimeMillis() - mStartRecordingTime;
        }
//        mStartRecordingTime = 0;
        if (mAudioLocalSaver != null) {
            mAudioLocalSaver.close();
        }
        mCustomAudioRecorder = null;
    }

    // OutgoingAudioListener
    @Override
    public void onMessageReceived(Message message) {
        int messageType = message.getMessageType();
        Log.d(TAG, "onMessageReceived: " + MessageType.getText(messageType));
        if (messageType == MessageType.ACK_START_FAILED
                && mLastMessageType == MessageType.UNAUTHORIZED_GROUP) {
            return;
        }
        mLastMessageType = messageType;
        switch (messageType) {
            case MessageType.ACK_START:
//                Log.d(TAG, "onAckStartSucceed: " + message);
                mBackgroundHandler.removeCallbacks(mAckStartTimeoutCheckRunner);
                break;
            case MessageType.ACK_START_FAILED:
//                Log.d(TAG, "onAckStartFailed: " + message);
//                Log.d(TAG, "ackids: " + message.getAckIds());
                mBackgroundHandler.removeCallbacks(mAckStartTimeoutCheckRunner);
                stopRecording();
                if (mOutgoingTalkCallback != null) {
                    mOutgoingTalkCallback.onOutgoingTalkError(
                            new VoicePingException("ACK_START_FAILED. Failed to initiate PTT Talk!", ErrorCode.ACK_START_FAILED));
                    mOutgoingTalkCallback = null;
                }
                break;
            case MessageType.ACK_END:
//                Log.d(TAG, "onAckEndSucceed: " + message);
//                Log.d(TAG, "ackids: " + message.getAckIds());
                mBackgroundHandler.removeCallbacks(mAckEndTimeoutCheckRunner);
                if (!mIsRecording && mOutgoingTalkCallback != null) {
                    String serverUrl = mConnection.getServerUrl();
                    if (serverUrl == null) break;
                    if (serverUrl.startsWith("ws")) {
                        serverUrl = serverUrl.replaceFirst("ws", "http");
                    }
                    String downloadUrl = serverUrl + "/files/audio/" + message.getAckIds();
                    if (mLastRecordDuration < mAudioParam.getMinDuration()) {
                        downloadUrl = null;
                    }
//                    Log.d(TAG, "last record duration: " + mLastRecordDuration);
                    mLastRecordDuration = 0;
//                    mOutgoingTalkCallback.onDownloadUrlReceived(downloadUrl);
                    mOutgoingTalkCallback = null;
                }
                break;
            case MessageType.MESSAGE_DELIVERED:
//                Log.d(TAG, "onMessageDelivered: " + message);
                break;
            case MessageType.MESSAGE_READ:
//                Log.d(TAG, "onMessageRead: " + message);
                break;
            case MessageType.UNAUTHORIZED_GROUP:
                mBackgroundHandler.removeCallbacks(mAckStartTimeoutCheckRunner);
                stopRecording();
                if (mOutgoingTalkCallback != null) {
                    mOutgoingTalkCallback.onOutgoingTalkError(
                            new VoicePingException("UNAUTHORIZED_GROUP. Failed to initiate PTT Talk!", ErrorCode.UNAUTHORIZED_GROUP));
                    mOutgoingTalkCallback = null;
                }
                break;
        }
    }

    @Override
    public void onSendMessageFailed(byte[] data, VoicePingException e) {
        Message message = MessageHelper.unpackMessage(data);
        if (message != null && message.getMessageType() == MessageType.ACK_START) {
            if (mOutgoingTalkCallback != null) {
                mOutgoingTalkCallback.onOutgoingTalkError(e);
                mOutgoingTalkCallback = null;
            }
        }
    }

    @Override
    public void onConnectionFailure(VoicePingException e) {
        if (mOutgoingTalkCallback != null) {
            mOutgoingTalkCallback.onOutgoingTalkError(e);
            mOutgoingTalkCallback = null;
        }
    }

    // AudioRecorder
    @Override
    public void setInterceptorBeforeEncoded(AudioInterceptor audioInterceptor) {
        mAudioInterceptorBeforeEncoded = audioInterceptor;
    }

    @Override
    public void setInterceptorAfterEncoded(AudioInterceptor audioInterceptor) {
        mAudioInterceptorAfterEncoded = audioInterceptor;
    }

    @Override
    public Channel getChannel() {
        return mChannel;
    }

    @Override
    public int getAudioSessionId() {
        if (mAudioRecord != null) return mAudioRecord.getAudioSessionId();
        return 0;
    }

    private class RecorderThread extends Thread {

        @Override
        public void run() {
            Sender sender = new Sender();
            if (mCustomAudioRecorder == null) {
                try {
                    if (mAudioRecord == null || mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
//                        Log.d(TAG, "audioRecord is NOT initialized");
                        initAudioRecord(mAudioParam);
                    } else {
//                        Log.d(TAG, "audioRecord is initialized");
                    }
                    mAudioRecord.startRecording();
//                    Log.d(TAG, "audio session id: " + mAudioRecord.getAudioSessionId() + ", state: " + mAudioRecord.getState());
                } catch (IllegalStateException ise) {
                    ise.printStackTrace();
                    return;
                }
            }

            while (mIsRecording) {
                // Check if message is too long
                long duration = System.currentTimeMillis() - mStartRecordingTime;
                if (duration > mAudioParam.getMaxDuration()) {
                    mBackgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            stopTalking();
                        }
                    });
                    break;
                }

                // Record
                byte[] audioData = new byte[mAudioParam.getFrameSize() * 2 * mAudioParam.getChannelSize()];
                if (mCustomAudioRecorder != null) {
                    // Record using CustomAudioRecorder
                    audioData = mCustomAudioRecorder.record(audioData);
                } else {
                    if (mAudioManager == null) {
                        stopAudioRecording();
                        mIsRecording = false;
                        interrupt();
                        return;
                    }
                    // Record using AudioRecord
                    int numOfFrames = mAudioRecord.read(audioData, 0, mAudioParam.getFrameSize() * 2);
                    if (numOfFrames == AudioRecord.ERROR_INVALID_OPERATION || audioData.length == 0) {
                        stopAudioRecording();
                        mIsRecording = false;
                        interrupt();
                        return;
                    }
//                    Log.d(TAG, "numOfFrames: " + numOfFrames + ", data length: " + audioData.length);
                    audioData = AudioBooster.boost(mAudioParam.getRecordingBoostInDb(), audioData, numOfFrames);
                }

                // Check if the data exists
                if (audioData == null || audioData.length == 0) continue;

                // Intercept before encoded
                if (mAudioInterceptorBeforeEncoded != null) {
                    Channel channel = new Channel(mChannelType, mUserId, mReceiverId);
                    audioData = mAudioInterceptorBeforeEncoded.proceed(audioData, channel);
                }

                // Encode
                if (mAudioParam.isUsingOpusCodec()) {
                    byte[] encodedBytes = new byte[audioData.length];
                    int encodedSize = mOpus.encode(audioData, 0, mAudioParam.getFrameSize(), encodedBytes, 0, encodedBytes.length);
                    audioData = Arrays.copyOfRange(encodedBytes, 0, encodedSize);
                }

                // Intercept after encoded
                if (mAudioInterceptorAfterEncoded != null) {
                    Channel channel = new Channel(mChannelType, mUserId, mReceiverId);
                    audioData = mAudioInterceptorAfterEncoded.proceed(audioData, channel);
                }

                // Write to file
                if (mAudioLocalSaver != null) mAudioLocalSaver.write(audioData);

                /*Message message = MessageHelper.createAudioMessage(userId, mReceiverId,
                        mChannelType, data, data.length);
                mConnection.send(message.getPayload());*/
                sender.send(audioData);
            }
            stopAudioRecording();
        }

        private void stopAudioRecording() {
            mAudioInterceptorBeforeEncoded = null;
            mAudioInterceptorAfterEncoded = null;
            if (mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
//                Log.d(TAG, "stopAudioRecording");
//                Log.d(TAG, "audio session id: " + mAudioRecord.getAudioSessionId() + ", state: " + mAudioRecord.getState());
                try {
                    mAudioRecord.stop();
                    mAudioRecord.release();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            if (mAudioManager != null) {
                mAudioManager.stopBluetoothSco();
            }
        }
    }

    private class Sender {

        private int counter = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        public void send(byte[] data) {
            try {
                outputStream.write(data);
                counter++;
//                    Log.d(TAG, "concatenate data, counter: " + counter + ", current data: " + outputStream.size());
                if (counter >= mAudioParam.getFramePerSent()) {
                    sendVoiceMessage();
                    reset();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendVoiceMessage() {
            byte[] accumulatedData = outputStream.toByteArray();
//            Log.d(TAG, "send voice message, counter: " + counter + ", total data: " + accumulatedData.length);
            Message message = MessageHelper.createAudioMessage(mUserId, mReceiverId,
                    mChannelType, accumulatedData, accumulatedData.length);
            if (message != null) mConnection.send(message.getPayload());
        }

        private void reset() {
//            Log.d(TAG, "reset data");
            outputStream.reset();
            counter = 0;
        }
    }
}
