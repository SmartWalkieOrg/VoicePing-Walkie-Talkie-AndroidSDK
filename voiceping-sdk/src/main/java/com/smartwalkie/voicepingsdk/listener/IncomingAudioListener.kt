package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.model.Message

/**
 * Created by sirius on 7/11/17.
 */
interface IncomingAudioListener {

    fun onMessageReceived(message: Message)

    fun onConnectionFailure(e: VoicePingException)
}