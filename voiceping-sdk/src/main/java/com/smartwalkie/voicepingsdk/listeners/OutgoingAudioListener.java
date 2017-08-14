package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.models.Message;

public interface OutgoingAudioListener {

    void onMessageReceived(Message message);

    void onError(byte[] data, PingException e);
}
