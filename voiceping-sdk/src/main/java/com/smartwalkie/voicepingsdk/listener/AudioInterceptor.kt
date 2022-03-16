package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.model.Channel

/**
 * Interface to do audio interception.
 */
interface AudioInterceptor {
    /**
     * Proceed the code to intercept audio data. This process will happen in background thread.
     * Make sure that any code that will modify UI should be run on UI thread.
     *
     * @param data The audio data in byte[]
     * @param channel Channel instance
     * @return The data after being processed in byte[]
     */
    fun proceed(data: ByteArray, channel: Channel): ByteArray
}