package com.smartwalkie.voicepingsdk.model

object ChannelType {
    const val GROUP = 0
    const val PRIVATE = 1

    fun getText(channelType: Int): String {
        return when (channelType) {
            GROUP -> "GROUP"
            PRIVATE -> "PRIVATE"
            else -> "UNKNOWN"
        }
    }

    fun isValid(channelType: Int): Boolean {
        return channelType == GROUP || channelType == PRIVATE
    }
}