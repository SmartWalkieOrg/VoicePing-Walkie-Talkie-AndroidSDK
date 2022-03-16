package com.smartwalkie.voicepingsdk

/**
 * Created by kukuhsain on 12/03/18.
 */
interface CustomAudioRecorder {

    fun record(placeholderData: ByteArray): ByteArray
}