package com.smartwalkie.voicepingdemo

import android.media.MediaRecorder
import android.os.Build

object AudioSourceConfig {
    private val manufacturers = arrayOf(
        Pair("lg", MediaRecorder.AudioSource.VOICE_RECOGNITION),
        Pair("tcl", MediaRecorder.AudioSource.MIC),
        Pair("moto", MediaRecorder.AudioSource.VOICE_COMMUNICATION),
        Pair("samsung", MediaRecorder.AudioSource.VOICE_RECOGNITION),
        Pair("alps", MediaRecorder.AudioSource.MIC),
        Pair("asus", MediaRecorder.AudioSource.MIC)
    )

    fun getSource(): Int {
        val deviceManufacturer = Build.MANUFACTURER.lowercase()
        for (manufacturer in manufacturers) {
            if (deviceManufacturer == manufacturer.first.lowercase()) {
                return manufacturer.second
            }
        }
        return MediaRecorder.AudioSource.VOICE_COMMUNICATION
    }

    fun getAudioSourceText(source: Int): String {
        return when (source) {
            MediaRecorder.AudioSource.MIC -> "MIC"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            else -> "UNKNOWN"
        }
    }
}