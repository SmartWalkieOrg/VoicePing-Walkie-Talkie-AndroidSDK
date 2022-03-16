package com.smartwalkie.voicepingsdk.exception

import java.lang.RuntimeException

/**
 * Main exception of this SDK
 */
class VoicePingException(message: String, val errorCode: Int) : RuntimeException(message)