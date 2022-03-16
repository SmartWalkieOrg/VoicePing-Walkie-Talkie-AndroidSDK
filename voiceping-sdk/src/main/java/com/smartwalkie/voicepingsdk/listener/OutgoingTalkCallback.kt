package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.exception.VoicePingException

/**
 * Interface to provide callback to outgoing talk action
 */
interface OutgoingTalkCallback {
    /**
     * Invoked after outgoing talk started.
     *
     * @param audioRecorder AudioRecorder
     */
    fun onOutgoingTalkStarted(audioRecorder: AudioRecorder)

    /**
     * Invoked after outgoing talk stopped.
     */
    fun onOutgoingTalkStopped(isTooShort: Boolean, isTooLong: Boolean)

    /**
     * Invoked after download URL received.
     */
    fun onDownloadUrlReceived(downloadUrl: String)

    /**
     * Invoked on outgoing talk error.
     */
    fun onOutgoingTalkError(e: VoicePingException)
}