package com.smartwalkie.voicepingsdk.listeners;

/**
 * Created by kukuhsain on 8/8/17.
 */

public interface AudioInterceptor {

    byte[] proceed(byte[] data);
}
