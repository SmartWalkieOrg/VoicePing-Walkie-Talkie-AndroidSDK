package com.smartwalkie.voiceping;

/**
 * Created by sirius on 7/6/17.
 */

public interface WebSocketConnectionEventListener {
    void onConnecting(int routerType);
    void onConnect(int routerType);
    void onError(int routerType, Exception e);
    void onData(int routerType, byte[] data);
    void onDisconnect(int routerType);
}
