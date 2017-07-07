package com.smartwalkie.voiceping;

import com.smartwalkie.voiceping.models.Message;

/**
 * Created by sirius on 7/6/17.
 */

public interface ConnectionListener {
    void onMessage(Message message);
    void onConnecting(int routerType);
    void onConnect(int routerType);
    void onError(int routerType, Exception e);
    void onData(int routerType, byte[] data);
    void onDisconnect(int routerType);
}
