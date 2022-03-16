package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.model.Channel

/**
 * Interface for Recorder.
 */
interface AudioRecorder {
    /**
     * Set interceptor that will be processed before the recorded audio data being encoded using
     * internal codec.
     *
     * @param audioInterceptor AudioInterceptor
     */
    fun setInterceptorBeforeEncoded(audioInterceptor: AudioInterceptor?)

    /**
     * Set interceptor that will be processed after the recorded audio data being encoded using
     * internal codec, but before sending them to server.
     *
     * @param audioInterceptor AudioInterceptor
     */
    fun setInterceptorAfterEncoded(audioInterceptor: AudioInterceptor?)

    val channel: Channel?

    val audioSessionId: Int
}