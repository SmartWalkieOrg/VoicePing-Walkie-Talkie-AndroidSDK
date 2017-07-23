package com.smartwalkie.voicepingsdk.events;

import com.smartwalkie.voicepingsdk.models.Message;

public class MessageEvent {
    private Message message;

    public MessageEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
