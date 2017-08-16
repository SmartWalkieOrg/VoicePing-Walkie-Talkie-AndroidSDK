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
     * Invoked after incoming talk started.
     *
     * @param audioPlayer
     */
    void onIncomingTalkStarted(AudioPlayer audioPlayer);

    /**
     * Invoked after incoming talk stopped.
     */
    void onIncomingTalkStopped();

    /**
     * Invoked on incoming talk error.
     */
    void onIncomingTalkError(PingException e);

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
    void onChannelError(PingException e);
}
