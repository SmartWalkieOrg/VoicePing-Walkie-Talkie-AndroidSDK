package com.smartwalkie.voicepingsdk.model

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlin.math.max

class AudioParam(
    var audioSource: Int,
    var isUsingOpusCodec: Boolean,
    var sampleRate: Int,
    var frameSize: Int,
    var channelSize: Int,
    var channelInConfig: Int,
    var channelOutConfig: Int,
    var audioFormat: Int,
    framePerSent: Int,
    minDuration: Int,
    maxDuration: Int,
    recordingBoostInDb: Double,
    receivingBoostInDb: Double,
    playbackBoostInDb: Double
) {
    var bufferSizeFactor: Int
    var recordMinBufferSize: Int
    var playMinBufferSize: Int
    var rawBufferSize: Int
    var framePerSent: Int
    var minDuration: Int
    var maxDuration: Int
    var recordingBoostInDb: Double
    var receivingBoostInDb: Double
    var playbackBoostInDb: Double
    private val resolution: Int
        get() = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8

    init {
        bufferSizeFactor = channelSize * resolution / 8
        recordMinBufferSize = max(
            AudioRecord.getMinBufferSize(sampleRate, channelInConfig, audioFormat),
            frameSize * bufferSizeFactor
        )
        playMinBufferSize = max(
            AudioTrack.getMinBufferSize(sampleRate, channelOutConfig, audioFormat),
            frameSize * bufferSizeFactor
        )
        rawBufferSize = frameSize * bufferSizeFactor
        this.framePerSent = framePerSent
        this.minDuration = minDuration
        this.maxDuration = maxDuration
        this.recordingBoostInDb = recordingBoostInDb
        this.receivingBoostInDb = receivingBoostInDb
        this.playbackBoostInDb = playbackBoostInDb
    }


    /**
     * Builder class to instantiate AudioParam with custom parameters
     */
    class Builder {
        private var audioSource: Int
        private var isUsingOpusCodec: Boolean
        private var sampleRate: Int
        private var frameSize: Int
        private var channelSize: Int
        private var channelInConfig: Int
        private var channelOutConfig: Int
        private var audioFormat: Int
        private var framePerSent: Int
        private var minDuration: Int
        private var maxDuration: Int
        private var recordingBoostInDb: Double
        private var receivingBoostInDb: Double
        private var playbackBoostInDb: Double

        init {
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            isUsingOpusCodec = true
            sampleRate = 16000
            frameSize = 960
            channelSize = 1
            channelInConfig = AudioFormat.CHANNEL_IN_MONO
            channelOutConfig = AudioFormat.CHANNEL_OUT_MONO
            audioFormat = AudioFormat.ENCODING_PCM_16BIT
            framePerSent = 1
            minDuration = 300
            maxDuration = 60 * 1000
            recordingBoostInDb = 0.0
            receivingBoostInDb = 0.0
            playbackBoostInDb = 0.0
        }

        /**
         * Set audio source for PTT recorder. The default value is
         * MediaRecorder.AudioSource.VOICE_COMMUNICATION.
         *
         * @param audioSource MediaRecorder.AudioSource
         * @return Builder
         */
        fun setAudioSource(audioSource: Int): Builder {
            this.audioSource = audioSource
            return this
        }

        /**
         * Set whether the SDK will use Opus as audio codec or not. The default value is true.
         *
         * @param useOpusCodec true for using Opus Codec, false otherwise
         * @return Builder
         */
        fun setUsingOpusCodec(useOpusCodec: Boolean): Builder {
            isUsingOpusCodec = useOpusCodec
            return this
        }

        /**
         * Set sample rate of the audio data. The default value is 16000.
         *
         * @param sampleRate Sample rate in Hz. Max 48000 Hz
         * @return Builder
         */
        fun setSampleRate(sampleRate: Int): Builder {
            this.sampleRate = sampleRate
            return this
        }

        /**
         * Set frame size of the audio data. The default value is 960.
         *
         * @param frameSize Frame size
         * @return Builder
         */
        fun setFrameSize(frameSize: Int): Builder {
            this.frameSize = frameSize
            return this
        }

        /**
         * Set channel size of the audio data. The default value is 1.
         *
         * @param channelSize Channel size. 1 for Mono, 2 for Stereo
         * @return Builder
         */
        fun setChannelSize(channelSize: Int): Builder {
            this.channelSize = channelSize
            return this
        }

        /**
         * Set Channel-In configuration of the audio data. The default value is
         * AudioFormat.CHANNEL_IN_MONO.
         *
         * @param channelInConfig Channel-In config
         * @return Builder
         */
        fun setChannelInConfig(channelInConfig: Int): Builder {
            this.channelInConfig = channelInConfig
            return this
        }

        /**
         * Set Channel-Out configuration of the audio data. The default value is
         * AudioFormat.CHANNEL_OUT_MONO.
         *
         * @param channelOutConfig Channel-Out config
         * @return Builder
         */
        fun setChannelOutConfig(channelOutConfig: Int): Builder {
            this.channelOutConfig = channelOutConfig
            return this
        }

        /**
         * Set audio format of the audio data. The default value is AudioFormat.ENCODING_PCM_16BIT.
         *
         * @param audioFormat Audio format
         * @return Builder
         */
        fun setAudioFormat(audioFormat: Int): Builder {
            this.audioFormat = audioFormat
            return this
        }

        /**
         * Set number of audio frames per sent. The default and minimum value is 1.
         *
         * @param framePerSent Number of frames per sent
         * @return Builder
         */
        fun setFramePerSent(framePerSent: Int): Builder {
            this.framePerSent = if (framePerSent >= 1) framePerSent else 1
            return this
        }

        /**
         * Set minimum duration for one PTT session. The default value is 300 ms.
         *
         * @param millis Minimum duration in milliseconds
         * @return Builder
         */
        fun setMinDuration(millis: Int): Builder {
            minDuration = millis
            return this
        }

        /**
         * Set maximum duration for one PTT session. The default value is 60000 ms.
         *
         * @param millis Maximum duration in milliseconds
         * @return Builder
         */
        fun setMaxDuration(millis: Int): Builder {
            maxDuration = millis
            return this
        }

        /**
         * Set recording boost in dB. The default value is 0.
         *
         * @param boostInDb Recording boost in dB
         * @return Builder
         */
        fun setRecordingBoostInDb(boostInDb: Double): Builder {
            recordingBoostInDb = boostInDb
            return this
        }

        /**
         * Set receiving boost in dB. The default value is 0.
         *
         * @param boostInDb Recording boost in dB
         * @return Builder
         */
        fun setReceivingBoostInDb(boostInDb: Double): Builder {
            receivingBoostInDb = boostInDb
            return this
        }

        /**
         * Set playback boost in dB. The default value is 0.
         *
         * @param boostInDb Recording boost in dB
         * @return Builder
         */
        fun setPlaybackBoostInDb(boostInDb: Double): Builder {
            playbackBoostInDb = boostInDb
            return this
        }

        /**
         * Build the Builder instance to be AudioParam instance, and initiate it.
         *
         * @return AudioParam instance
         */
        fun build(): AudioParam {
            return AudioParam(
                audioSource,
                isUsingOpusCodec,
                sampleRate,
                frameSize,
                channelSize,
                channelInConfig,
                channelOutConfig,
                audioFormat,
                framePerSent,
                minDuration,
                maxDuration,
                recordingBoostInDb,
                receivingBoostInDb,
                playbackBoostInDb
            )
        }
    }
}