package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Interface for listening events and actions to the channel.
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
     * @param audioPlayer AudioPlayer
     */
    void onIncomingTalkStarted(AudioPlayer audioPlayer);

    /**
     * Invoked after incoming talk stopped.
     */
    void onIncomingTalkStopped();

    /**
     * Invoked after successfully unsubscribing from a group channel.
     *
     * @param channelId
     * @param channelType
     */
    void onUnsubscribed(String channelId, int channelType);

    /**
     * Invoked on channel error.
     *
     * @param e PingException
     */
    void onChannelError(PingException e);
}
