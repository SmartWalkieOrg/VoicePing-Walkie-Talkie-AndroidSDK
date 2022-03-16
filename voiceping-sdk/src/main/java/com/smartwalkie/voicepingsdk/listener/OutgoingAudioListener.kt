package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.model.Message

interface OutgoingAudioListener {

    fun onMessageReceived(message: Message)

    fun onSendMessageFailed(data: ByteArray, e: VoicePingException)

    fun onConnectionFailure(e: VoicePingException)
}