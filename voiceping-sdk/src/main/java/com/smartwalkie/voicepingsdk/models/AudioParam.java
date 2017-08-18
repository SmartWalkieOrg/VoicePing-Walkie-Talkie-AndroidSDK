package com.smartwalkie.voicepingsdk.models;

/**
 * Created by kukuhsain on 8/18/17.
 */

public class AudioParam {

    private boolean mIsUsingOpusCodec;
    private int mSampleRate;
    private int mFrameSize;
    private int mEncoderSize;
    private int mChannelSize;
    private int mBufferSizeFactor;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mRecordMinBufferSize;
    private int mPlayMinBufferSize;

    public AudioParam(boolean isUsingOpusCodec, int sampleRate, int frameSize, int encoderSize, 
                      int channelSize, int bufferSizeFactor, int channelConfig, int audioFormat,
                      int recordMinBufferSize, int playMinBufferSize) {
        
        mIsUsingOpusCodec = isUsingOpusCodec;
        mSampleRate = sampleRate;
        mFrameSize = frameSize;
        mEncoderSize = encoderSize;
        mChannelSize = channelSize;
        mBufferSizeFactor = bufferSizeFactor;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mRecordMinBufferSize = recordMinBufferSize;
        mPlayMinBufferSize = playMinBufferSize;
    }

    public boolean isIsUsingOpusCodec() {
        return mIsUsingOpusCodec;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getFrameSize() {
        return mFrameSize;
    }

    public int getEncoderSize() {
        return mEncoderSize;
    }

    public int getChannelSize() {
        return mChannelSize;
    }

    public int getBufferSizeFactor() {
        return mBufferSizeFactor;
    }

    public int getChannelConfig() {
        return mChannelConfig;
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
