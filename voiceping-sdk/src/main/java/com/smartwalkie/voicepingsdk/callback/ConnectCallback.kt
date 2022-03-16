package com.smartwalkie.voicepingsdk.callback

import com.smartwalkie.voicepingsdk.exception.VoicePingException

/**
 * Interface to provide callback to connect action.
 */
interface ConnectCallback {
    /**
     * Invoked after successfully connected to server.
     */
    fun onConnected()

    /**
     * Invoked on failed connecting process.
     *
     * @param exception
     */
    fun onFailed(exception: VoicePingException)
}