package com.smartwalkie.voicepingsdk;

import com.smartwalkie.voicepingsdk.listener.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listener.AudioMetaData;
import com.smartwalkie.voicepingsdk.listener.AudioReceiver;
import com.smartwalkie.voicepingsdk.model.Channel;

/**
 * Created by kukuhsain on 20/11/17.
 */

class IncomingTalkSession implements AudioReceiver, AudioMetaData {

    private long mStartTime;
    private long mStopTime;
    private AudioInterceptor mAudioInterceptorBeforeDecoded;
    private AudioInterceptor mAudioInterceptorAfterDecoded;
    private String mLocalPath;
    private AudioLocalSaver mAudioLocalSaver;
    private boolean mHasStartSignal;
    private boolean mHasStopSignal;
    private Channel mChannel;
    private String mDownloadUrl;
    private int mDurationInServer;
    private Runnable mTimeoutCheckRunner;
    private int mAudioSessionId;

    public IncomingTalkSession(Channel channel, Runnable timeoutCheckRunner, int audioSessionId) {
        mChannel = channel;
        mTimeoutCheckRunner = timeoutCheckRunner;
        mAudioSessionId = audioSessionId;
        start();
    }

    public boolean isActive() {
        return mStopTime == 0;
    }

    public void start() {
        mStartTime = System.currentTimeMillis();
        mStopTime = 0;
    }

    public void stop() {
        mStopTime = System.currentTimeMillis();
        if (isSavedToLocal()) mAudioLocalSaver.close();
        mAudioLocalSaver = null;
    }

    public AudioInterceptor getAudioInterceptorBeforeDecoded() {
        return mAudioInterceptorBeforeDecoded;
    }

    public AudioInterceptor getAudioInterceptorAfterDecoded() {
        return mAudioInterceptorAfterDecoded;
    }

    public void setStartSignal(boolean hasStartSignal) {
        mHasStartSignal = hasStartSignal;
    }

    public void setStopSignal(boolean hasStopSignal) {
        mHasStopSignal = hasStopSignal;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    public void setDurationInServer(int millis) {
        mDurationInServer = millis;
    }

    public Runnable getTimeoutCheckRunner() {
        return mTimeoutCheckRunner;
    }

    public boolean isSavedToLocal() {
        return mAudioLocalSaver != null;
    }

    public void writeData(byte[] data) {
        if (isSavedToLocal()) mAudioLocalSaver.write(data);
    }

    // AudioReceiver
    @Override
    public void setInterceptorBeforeDecoded(AudioInterceptor audioInterceptor) {
        mAudioInterceptorBeforeDecoded = audioInterceptor;
    }

    @Override
    public void setInterceptorAfterDecoded(AudioInterceptor audioInterceptor) {
        mAudioInterceptorAfterDecoded = audioInterceptor;
    }

    @Override
    public void saveToLocal(String localPath) {
        mLocalPath = localPath;
        if (mLocalPath == null) return;
        mAudioLocalSaver = new AudioLocalSaver(localPath);
        mAudioLocalSaver.init();
    }

    @Override
    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    public void setAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    // AudioMetaData
    @Override
    public long getStartTime() {
        return mStartTime;
    }

    @Override
    public long getStopTime() {
        return mStopTime;
    }

    @Override
    public String getLocalPath() {
        return mLocalPath;
    }

    @Override
    public boolean hasStartSignal() {
        return mHasStartSignal;
    }

    @Override
    public boolean hasStopSignal() {
        return mHasStopSignal;
    }

    @Override
    public Channel getChannel() {
        return mChannel;
    }

    @Override
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    @Override
    public int getDurationInServer() {
        return mDurationInServer;
    }
}
