package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.model.Channel

/**
 * Interface for Player.
 */
interface AudioReceiver {
    /**
     * Set interceptor that will be processed before the received audio data being decoded using
     * internal codec.
     *
     * @param audioInterceptor AudioInterceptor
     */
    fun setInterceptorBeforeDecoded(audioInterceptor: AudioInterceptor?)

    /**
     * Set interceptor that will be processed after the received audio data being
     * decoded using internal codec, but before playing them.
     *
     * @param audioInterceptor AudioInterceptor
     */
    fun setInterceptorAfterDecoded(audioInterceptor: AudioInterceptor?)

    fun saveToLocal(localPath: String?)

    val channel: Channel?

    val audioSessionId: Int
}