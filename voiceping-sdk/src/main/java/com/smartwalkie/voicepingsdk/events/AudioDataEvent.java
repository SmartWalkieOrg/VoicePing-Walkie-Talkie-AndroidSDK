package com.smartwalkie.voicepingsdk.events;

/**
 * Created by kukuhsain on 8/8/17.
 */

public class AudioDataEvent {

    private byte[] data;

    public AudioDataEvent(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
