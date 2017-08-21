package com.smartwalkie.voicepingsdk.exceptions;

/**
 * Main exception of this SDK
 */
public class PingException extends RuntimeException {

    /**
     * Constructor of PingException.
     *
     * @param message Error message
     */
    public PingException(String message) {
        super(message);
    }
}
