package com.smartwalkie.voiceping.listeners;

import com.smartwalkie.voiceping.models.Message;

/**
 * Created by sirius on 7/6/17.
 */

public interface ConnectionListener {
    void onMessage(Message message);
    void onConnecting();
    void onConnected();
    void onFailed();
    void onData(byte[] data);
    void onDisconnected();
}
