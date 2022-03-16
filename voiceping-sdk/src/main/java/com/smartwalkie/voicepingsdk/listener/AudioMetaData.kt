package com.smartwalkie.voicepingsdk.listener

import com.smartwalkie.voicepingsdk.model.Channel

/**
 * Created by kukuhsain on 20/11/17.
 */
interface AudioMetaData {
    val startTime: Long
    val stopTime: Long
    val localPath: String?
    fun hasStartSignal(): Boolean
    fun hasStopSignal(): Boolean
    val channel: Channel?
    val downloadUrl: String?
    val durationInServer: Int
}