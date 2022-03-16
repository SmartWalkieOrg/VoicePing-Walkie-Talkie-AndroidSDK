package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.ConnectionState
import com.smartwalkie.voicepingsdk.exception.VoicePingException

interface ConnectionStateListener {
    fun onConnectionStateChanged(connectionState: ConnectionState)

    fun onConnectionError(e: VoicePingException)
}