package com.smartwalkie.voicepingsdk.listeners;

/**
 * Interface to do audio interception.
 */
public interface AudioInterceptor {

    /**
     * Proceed the code to intercept audio data.
     *
     * @param data The audio data.
     * @return The data after being processed.
     */
    byte[] proceed(byte[] data);
}
