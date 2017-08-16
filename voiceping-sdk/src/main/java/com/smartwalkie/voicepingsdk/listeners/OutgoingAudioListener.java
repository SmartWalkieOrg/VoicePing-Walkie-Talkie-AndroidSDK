package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.models.Message;

public interface OutgoingAudioListener {

    void onMessageReceived(Message message);

    void onSendMessageFailed(byte[] data, PingException e);

    void onConnectionFailure();
}
