package com.smartwalkie.voicepingsdk

import java.nio.ByteBuffer

object VoicePingUtils {

    fun bytesToShorts(data: ByteArray): ShortArray {
        val sb = ByteBuffer.wrap(data).asShortBuffer()
        val shortArray = ShortArray(sb.limit())
        sb[shortArray]
        return shortArray
    }

    fun shortsToBytes(data: ShortArray): ByteArray {
        val byteArray = ByteArray(data.size * 2)
        ByteBuffer.wrap(byteArray).asShortBuffer().put(data)
        return byteArray
    }
}