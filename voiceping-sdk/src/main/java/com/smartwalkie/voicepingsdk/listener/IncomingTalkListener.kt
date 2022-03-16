package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.model.Channel

/**
 * Interface to provide listener to incoming talk action
 */
interface IncomingTalkListener {
    /**
     * Invoked after incoming talk started.
     *
     * @param audioReceiver AudioReceiver
     */
    fun onIncomingTalkStarted(audioReceiver: AudioReceiver, activeChannels: List<Channel>)

    /**
     * Invoked after incoming talk stopped.
     */
    fun onIncomingTalkStopped(audioMetaData: AudioMetaData, activeChannels: List<Channel>)

    /**
     * Invoked on channel error.
     *
     * @param e VoicePingException
     */
    fun onIncomingTalkError(e: VoicePingException)
}