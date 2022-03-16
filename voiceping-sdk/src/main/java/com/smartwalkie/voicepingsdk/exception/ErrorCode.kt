package com.smartwalkie.voicepingsdk.exception

/**
 * Created by kukuhsain on 17/11/17.
 */
object ErrorCode {
    const val UNKNOWN = 0
    const val INTERNET_DISCONNECTED = 1
    const val SOCKET_DISCONNECTED = 2
    const val ACK_START_FAILED = 3
    const val ACK_START_TIMEOUT = 4
    const val ACK_END_TIMEOUT = 5
    const val CONNECTION_FAILURE = 6
    const val UNAUTHORIZED_GROUP = 7
    const val DUPLICATE_CONNECT = 8
}