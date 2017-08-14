package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Interface for listener of channel.
 */
public interface ChannelListener {

    /**
     * Invoked after successfully subscribing to a group channel.
     *
     * @param channelId
     * @param channelType
     */
    void onSubscribed(String channelId, int channelType);

    /**
     * Invoked after talk started.
     *
     * @param audioRecorder
     */
    void onTalkStarted(AudioRecorder audioRecorder);

    /**
     * Invoked after talk stopped.
     */
    void onTalkStopped();

    /**
     * Invoked after talk received.
     *
     * @param audioPlayer
     */
    void onTalkReceived(AudioPlayer audioPlayer);

    /**
     * Invoked after successfully unsubscribing from a group channel.
     *
     * @param channelId
     * @param channelType
     */
    void onUnsubscribed(String channelId, int channelType);

    /**
     * Invoked on error.
     *
     * @param e
     */
    void onError(PingException e);
}
