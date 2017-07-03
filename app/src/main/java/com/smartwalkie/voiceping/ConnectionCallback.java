package com.smartwalkie.voiceping;

/**
 * Created by fadils on 12/20/16.
 * www.github.com/fadils
 */

public interface ConnectionCallback {
    void onConnecting(int routerType);
    void onConnect(int routerType);
    void onError(int routerType, Exception e);
    void onData(int routerType, byte[] data);
    boolean onDisconnect(int routerType, boolean disconnectManually);
}
