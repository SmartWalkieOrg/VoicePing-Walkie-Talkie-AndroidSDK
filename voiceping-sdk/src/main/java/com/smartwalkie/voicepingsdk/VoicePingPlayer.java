package com.smartwalkie.voicepingsdk;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

import com.media2359.voiceping.codec.Opus;
import com.smartwalkie.voicepingsdk.listener.AudioInterceptor;
import com.smartwalkie.voicepingsdk.model.AudioParam;
import com.smartwalkie.voicepingsdk.model.Channel;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by kukuhsain on 10/10/17.
 */

public class VoicePingPlayer {

    private final String TAG = VoicePingPlayer.class.getSimpleName();

    private AudioParam mAudioParam;
    private Opus mOpus;
    private String mFilePath;
    private VoicePingPlayerThread mVoicePingPlayerThread;
    private Handler mMainHandler;

    private Channel mChannel;
    private long mDurationMillis;
    private volatile long mCurrentPositionBytes;

    private final int mBufferSize;
    private final int mRawBufferSize;
    private final int mBytesPerSec;

    private OnPlaybackStartedListener mOnPlaybackStartedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnErrorListener mOnErrorListener;
    private AudioInterceptor mAudioInterceptorBeforeDecoded;
    private AudioInterceptor mAudioInterceptorAfterDecoded;

    private volatile int mState;

    private final int INITIALIZED = 101;
    private final int PREPARED = 102;
    private final int STARTED = 103;
    private final int PAUSED = 104;
    private final int STOPPED = 105;
    private final int SEEK_TO = 106;

    // TODO: change updating mAudioParam from constructor to setDataSource
    public VoicePingPlayer(AudioParam audioParam, int bufferSize) {
        mAudioParam = audioParam;
        mBufferSize = bufferSize;
        mRawBufferSize = mAudioParam.getRawBufferSize();
        mBytesPerSec = mAudioParam.getSampleRate() * mAudioParam.getBufferSizeFactor();
    }

    public void setDataSource(String filePath) throws FileNotFoundException {
        mFilePath = filePath;
        File file = new File(filePath);
        if (!file.exists()) throw new FileNotFoundException();
        // TODO: check if the file is qualified to be played with this player
        // TODO: read AudioParam and Channel from file header
        // TODO: update mAudioParam
        // TODO: update mChannel
        long rawDataLengthInBytes = (file.length() / mBufferSize) * mRawBufferSize;
        mDurationMillis = rawDataLengthInBytes * 1000 / mBytesPerSec;
        mState = INITIALIZED;
    }

    public void prepare() {
        mOpus = Opus.getCodec(mAudioParam.getSampleRate(), mAudioParam.getChannelSize());
        mMainHandler = new Handler();
        mCurrentPositionBytes = 0;
        mState = PREPARED;
    }

    public void start() {
        if (isPlaying()) {
            stop();
            if (mVoicePingPlayerThread != null) mVoicePingPlayerThread.terminate();
        }
        mVoicePingPlayerThread = new VoicePingPlayerThread();
        mVoicePingPlayerThread.start();
    }

    public void seekTo(long position) {
        long tempPositionBytes = (position * mBytesPerSec / 1000) * mBufferSize / mRawBufferSize;
        mCurrentPositionBytes = (tempPositionBytes / mBufferSize) * mBufferSize;
        mState = SEEK_TO;
    }

    public void pause() {
        mState = PAUSED;
    }

    public void stop() {
        mState = STOPPED;
    }

    public void release() {
        mVoicePingPlayerThread.interrupt();
    }

    public boolean isPlaying() {
        return mState == STARTED;
    }

    public Channel getChannel() {
        return mChannel;
    }

    public long getDuration() {
        return mDurationMillis;
    }

    public long getCurrentPosition() {
        long rawDataLengthInBytes = (mCurrentPositionBytes / mBufferSize) * mRawBufferSize;
        return rawDataLengthInBytes * 1000 / mBytesPerSec;
    }

    public void setOnPlaybackStartedListener(OnPlaybackStartedListener onPlaybackStartedListener) {
        mOnPlaybackStartedListener = onPlaybackStartedListener;
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        mOnCompletionListener = onCompletionListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    public void setInterceptorBeforeDecoded(AudioInterceptor audioInterceptor) {
        mAudioInterceptorBeforeDecoded = audioInterceptor;
    }

    public void setInterceptorAfterDecoded(AudioInterceptor audioInterceptor) {
        mAudioInterceptorAfterDecoded = audioInterceptor;
    }

    public interface OnPlaybackStartedListener {

        void onStart(int audioSessionId);
    }

    public interface OnCompletionListener {

        void onComplete();
    }

    public interface OnErrorListener {

        void onError(IOException e);
    }

    private class VoicePingPlayerThread extends Thread {

        private AudioTrack mAudioTrack;
        private volatile boolean mIsRunning = true;

        @Override
        public void run() {
            // init
            initAudioTrack();
            // play
            playAudioTrack();
            // prepare to stream data from file
            byte[] stream = new byte[mBufferSize];
            int i;
            try {
                File file = new File(mFilePath);
                FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis);

                if (mState == STOPPED) mCurrentPositionBytes = 0;
                if (mState == PAUSED || mState == SEEK_TO) dis.skip(mCurrentPositionBytes);

                mState = STARTED;
                if (mOnPlaybackStartedListener != null) {
                    mOnPlaybackStartedListener.onStart(mAudioTrack.getAudioSessionId());
                }
                while ((i = dis.read(stream, 0, stream.length)) > -1) {
                    if (mState != STARTED || !mIsRunning) {
                        stopAudioTrack();
                        if (mState == SEEK_TO) VoicePingPlayer.this.start();
                        return;
                    }
                    playStream(stream);
                    mCurrentPositionBytes = mCurrentPositionBytes + i;
                }
                mState = STOPPED;

                // call OnCompletionListener on main thread
                if (mOnCompletionListener != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mOnCompletionListener.onComplete();
                        }
                    });
                }

            } catch (final IOException e) {
                e.printStackTrace();

                // call OnErrorListener on main thread
                if (mOnErrorListener != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mOnErrorListener.onError(e);
                        }
                    });
                }
            }
            stopAudioTrack();
        }

        private void initAudioTrack() {
            int minBufferSize = mAudioParam.getPlayMinBufferSize();
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    mAudioParam.getSampleRate(),
                    mAudioParam.getChannelOutConfig(),
                    mAudioParam.getAudioFormat(),
                    Math.min(minBufferSize, mAudioParam.getFrameSize() * mAudioParam.getBufferSizeFactor()),
                    AudioTrack.MODE_STREAM);
        }

        private void playAudioTrack() {
            try {
                mAudioTrack.play();
            } catch (IllegalStateException e) {
                if (mAudioTrack != null) mAudioTrack.release();
                initAudioTrack();
                playAudioTrack();
            }
        }

        private void stopAudioTrack() {
            mAudioTrack.stop();
            mAudioTrack.flush();
            mAudioTrack.release();
        }

        private void playStream(byte[] payload) {
            byte[] pcmFrame = new byte[mRawBufferSize];
            // Intercept before decoded
            if (mAudioInterceptorBeforeDecoded != null) {
                payload = mAudioInterceptorBeforeDecoded.proceed(payload, mChannel);
                if (payload == null || payload.length == 0) return;
            }
            if (mAudioParam.isUsingOpusCodec()) {
                int decodeSize = mOpus.decode(payload, 0, payload.length, pcmFrame, 0, mAudioParam.getFrameSize(), 0);
                if (decodeSize > 0) {
                    // intercept after decoded
                    if (mAudioInterceptorAfterDecoded != null) {
                        pcmFrame = mAudioInterceptorAfterDecoded.proceed(pcmFrame, mChannel);
                        if (pcmFrame == null || pcmFrame.length == 0) return;
                    }
                    // boost audio
                    pcmFrame = AudioBooster.boost(mAudioParam.getPlaybackBoostInDb(), pcmFrame, pcmFrame.length);

                    // play
                    mAudioTrack.write(pcmFrame, 0, pcmFrame.length);
                }
            } else {
                // intercept after decoded
                if (mAudioInterceptorAfterDecoded != null) {
                    payload = mAudioInterceptorAfterDecoded.proceed(payload, mChannel);
                    if (payload == null || payload.length == 0) return;
                }
                // boost audio
                payload = AudioBooster.boost(mAudioParam.getPlaybackBoostInDb(), payload, payload.length);

                // play
                mAudioTrack.write(payload, 0, payload.length);
            }
        }

        public void terminate() {
            mIsRunning = false;
        }
    }
}
