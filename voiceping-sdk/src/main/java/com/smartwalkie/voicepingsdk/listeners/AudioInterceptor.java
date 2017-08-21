package com.smartwalkie.voicepingsdk.listeners;

/**
 * Interface to do audio interception.
 */
public interface AudioInterceptor {

    /**
     * Proceed the code to intercept audio data. This process will happen in background thread.
     * Make sure that any code that will modify UI should be run on UI thread.
     *
     * @param data The audio data in byte[].
     * @return The data after being processed in byte[].
     */
    byte[] proceed(byte[] data);
}
