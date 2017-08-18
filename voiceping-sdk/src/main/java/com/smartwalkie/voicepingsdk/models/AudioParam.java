package com.smartwalkie.voicepingsdk.models;

import android.media.AudioRecord;
import android.media.AudioTrack;

/**
 * Created by kukuhsain on 8/18/17.
 */

public class AudioParam {

    private boolean mIsUsingOpusCodec;
    private int mSampleRate;
    private int mFrameSize;
    private int mChannelSize;
    private int mBufferSizeFactor;
    private int mChannelInConfig;
    private int mChannelOutConfig;
    private int mAudioFormat;
    private int mRecordMinBufferSize;
    private int mPlayMinBufferSize;

    public AudioParam(boolean isUsingOpusCodec, int sampleRate, int frameSize, int channelSize,
                      int bufferSizeFactor, int channelInConfig, int channelOutConfig,
                      int audioFormat) {
        
        mIsUsingOpusCodec = isUsingOpusCodec;
        mSampleRate = sampleRate;
        mFrameSize = frameSize;
        mChannelSize = channelSize;
        mBufferSizeFactor = bufferSizeFactor;
        mChannelInConfig = channelInConfig;
        mChannelOutConfig = channelOutConfig;
        mAudioFormat = audioFormat;
        mRecordMinBufferSize = Math.max(AudioRecord.getMinBufferSize(sampleRate, channelInConfig,
                audioFormat), frameSize * bufferSizeFactor);
        mPlayMinBufferSize = Math.max(AudioTrack.getMinBufferSize(sampleRate, channelOutConfig,
                audioFormat), frameSize * bufferSizeFactor);
    }

    public boolean isUsingOpusCodec() {
        return mIsUsingOpusCodec;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getFrameSize() {
        return mFrameSize;
    }

    public int getChannelSize() {
        return mChannelSize;
    }

    public int getBufferSizeFactor() {
        return mBufferSizeFactor;
    }

    public int getChannelInConfig() {
        return mChannelInConfig;
    }

    public int getChannelOutConfig() {
        return mChannelOutConfig;
    }

    public int getAudioFormat() {
        return mAudioFormat;
    }

    public int getRecordMinBufferSize() {
        return mRecordMinBufferSize;
    }

    public int getPlayMinBufferSize() {
        return mPlayMinBufferSize;
    }
}
