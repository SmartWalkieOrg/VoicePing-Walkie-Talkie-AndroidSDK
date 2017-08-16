package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Created by kukuhsain on 8/16/17.
 */

public interface OutgoingTalkCallback {

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
     * Invoked on outgoing talk error.
     */
    void onOutgoingTalkError(PingException e);
}
