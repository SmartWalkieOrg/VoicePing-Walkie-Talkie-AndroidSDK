package com.smartwalkie.voiceping.events;

import com.smartwalkie.voiceping.models.Message;

public class MessageEvent {
    private Message message;

    public MessageEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
