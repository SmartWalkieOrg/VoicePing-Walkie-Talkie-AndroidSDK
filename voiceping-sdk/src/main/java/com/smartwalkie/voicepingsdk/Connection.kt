package com.smartwalkie.voicepingsdk

import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.callback.DisconnectCallback
import com.smartwalkie.voicepingsdk.listener.ConnectionStateListener
import com.smartwalkie.voicepingsdk.listener.OutgoingAudioListener

/**
 * Created by kukuhsain on 02/03/18.
 */
internal interface Connection {

    fun setConnectionStateListener(listener: ConnectionStateListener?)

    fun setOutgoingAudioListener(listener: OutgoingAudioListener?)

    val serverUrl: String?

    fun connect(userId: String, deviceId: String, callback: ConnectCallback)

    val connectionState: ConnectionState

    fun disconnect(callback: DisconnectCallback)

    fun send(data: ByteArray)
}