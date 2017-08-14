package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.models.Message;

public interface OutgoingAudioListener {

    void onMessageReceived(Message message);
}
