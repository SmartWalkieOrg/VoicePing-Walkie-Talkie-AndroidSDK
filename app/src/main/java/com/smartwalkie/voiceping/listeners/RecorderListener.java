package com.smartwalkie.voiceping.listeners;

import com.smartwalkie.voiceping.models.Message;

public interface RecorderListener {
    void onDelivered(Message message);
}
