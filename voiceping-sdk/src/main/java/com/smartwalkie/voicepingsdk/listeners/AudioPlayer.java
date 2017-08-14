package com.smartwalkie.voicepingsdk.listeners;

/**
 * Interface for Player.
 */
public interface AudioPlayer {

    /**
     * Add interceptor that will be processed before playing the received audio data from server.
     *
     * @param audioInterceptor
     */
    void addAudioInterceptor(AudioInterceptor audioInterceptor);
}
