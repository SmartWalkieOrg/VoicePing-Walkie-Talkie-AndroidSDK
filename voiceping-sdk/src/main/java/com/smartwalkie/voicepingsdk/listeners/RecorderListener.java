package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.models.Message;

public interface RecorderListener {
    void onDelivered(Message message);
}
