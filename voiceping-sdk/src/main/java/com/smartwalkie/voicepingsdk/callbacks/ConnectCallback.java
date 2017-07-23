package com.smartwalkie.voicepingsdk.callbacks;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

public interface ConnectCallback {

    void onConnected();

    void onFailed(PingException exception);
}
