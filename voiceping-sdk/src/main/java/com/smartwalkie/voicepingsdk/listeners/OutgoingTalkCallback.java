package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Interface to provide callback to outgoing talk action
 */
public interface OutgoingTalkCallback {

    /**
     * Invoked after outgoing talk started.
     *
     * @param audioRecorder AudioRecorder
     */
    void onOutgoingTalkStarted(AudioRecorder audioRecorder);

    /**
     * Invoked after outgoing talk stopped.
     */
    void onOutgoingTalkStopped();

    /**
     * Invoked on outgoing talk error.
     */
    void onOutgoingTalkError(PingException e);
}
