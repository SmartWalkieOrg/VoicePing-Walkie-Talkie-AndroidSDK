package com.smartwalkie.voiceping.listeners;

import com.smartwalkie.voiceping.models.Message;

public interface OutgoingAudioListener {
    void onAckStartSucceed(Message message);
    void onAckStartFailed(Message message);
    void onAckEndSucceed(Message message);
    void onMessageDelivered(Message message);
    void onMessageRead(Message message);
}
