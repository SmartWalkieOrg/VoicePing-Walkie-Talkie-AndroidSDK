package com.smartwalkie.voicepingsdk.callback

/**
 * Interface to provide callback to disconnect action.
 */
interface DisconnectCallback {
    /**
     * Invoked after successfully disconnected from server
     */
    fun onDisconnected()
}