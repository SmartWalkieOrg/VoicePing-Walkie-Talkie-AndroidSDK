package com.smartwalkie.voicepingsdk.callbacks;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Interface to provide callback to connect action.
 */
public interface ConnectCallback {

    /**
     * Invoked after successfully connected to server.
     */
    void onConnected();

    /**
     * Invoked on failed connecting process.
     *
     * @param exception
     */
    void onFailed(PingException exception);
}
