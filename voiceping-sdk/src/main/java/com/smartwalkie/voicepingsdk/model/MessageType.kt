package com.smartwalkie.voicepingsdk.model

object MessageType {
    const val UNKNOWN = -1
    const val START_TALKING = 1
    const val STOP_TALKING = 2
    const val AUDIO = 3
    const val CONNECTION = 4
    const val STATUS = 5
    const val ACK_START = 6
    const val ACK_END = 7
    const val ACK_START_FAILED = 8
    const val DUPLICATE_CONNECT = 9
    const val USER_UPDATED = 10
    const val USER_DELETED = 11
    const val CHANNEL_UPDATED = 12
    const val CHANNEL_DELETED = 13
    const val INVALID_USER = 14
    const val CHANNEL_ADDED_USER = 15
    const val CHANNEL_REMOVED_USER = 16
    const val TEXT = 17
    const val IMAGE = 18
    const val OFFLINE_MESSAGE = 19
    const val MESSAGE_DELIVERED = 20
    const val MESSAGE_READ = 21
    const val ACK_TEXT = 22
    const val UNAUTHORIZED_GROUP = 27

    @JvmStatic
    fun getText(messageType: Int): String {
        return when (messageType) {
            START_TALKING -> "START_TALKING"
            STOP_TALKING -> "STOP_TALKING"
            AUDIO -> "AUDIO"
            CONNECTION -> "CONNECTION"
            STATUS -> "STATUS"
            ACK_START -> "ACK_START"
            ACK_END -> "ACK_END"
            ACK_START_FAILED -> "ACK_START_FAILED"
            DUPLICATE_CONNECT -> "DUPLICATED_LOGIN"
            USER_UPDATED -> "USER_UPDATED"
            USER_DELETED -> "USER_DELETED"
            CHANNEL_UPDATED -> "CHANNEL_UPDATED"
            CHANNEL_DELETED -> "CHANNEL_DELETED"
            INVALID_USER -> "INVALID_USER"
            CHANNEL_ADDED_USER -> "CHANNEL_ADDED_USER"
            CHANNEL_REMOVED_USER -> "CHANNEL_REMOVED_USER"
            TEXT -> "TEXT"
            IMAGE -> "IMAGE"
            OFFLINE_MESSAGE -> "OFFLINE_MESSAGE"
            MESSAGE_DELIVERED -> "MESSAGE_DELIVERED"
            MESSAGE_READ -> "MESSAGE_READ"
            ACK_TEXT -> "ACK_TEXT"
            UNAUTHORIZED_GROUP -> "UNAUTHORIZED_GROUP"
            else -> "UNKNOWN"
        }
    }
}