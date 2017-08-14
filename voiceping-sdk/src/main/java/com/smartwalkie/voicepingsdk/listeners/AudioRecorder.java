package com.smartwalkie.voicepingsdk.listeners;

/**
 * Interface for Recorder.
 */
public interface AudioRecorder {

    /**
     * Add interceptor that will be processed before sending the recorded audio data to server.
     *
     * @param audioInterceptor
     */
    void addAudioInterceptor(AudioInterceptor audioInterceptor);
}
