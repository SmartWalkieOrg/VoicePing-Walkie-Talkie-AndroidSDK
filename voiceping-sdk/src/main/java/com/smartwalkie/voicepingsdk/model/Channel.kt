package com.smartwalkie.voicepingsdk.model

class Channel(val type: Int, val senderId: String?, val receiverId: String?) {
    val pureSenderId = senderId?.substringAfter("_")
    val pureReceiverId = receiverId?.substringAfter("_")

    override fun equals(other: Any?): Boolean {
        return toString() == other.toString()
    }

    override fun toString(): String {
        return "{\"type\":$type,\"senderId\":\"$senderId\",\"receiverId\":\"$receiverId\"}"
    }
}