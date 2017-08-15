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
     * Invoked after outgoing talk started.
     *
     * @param audioRecorder
     */
    void onOutgoingTalkStarted(AudioRecorder audioRecorder);

    /**
     * Invoked after outgoing talk stopped.
     */
    void onOutgoingTalkStopped();

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
