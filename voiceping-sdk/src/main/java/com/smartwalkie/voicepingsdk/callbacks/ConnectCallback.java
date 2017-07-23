package com.smartwalkie.voicepingsdk.callbacks;


import com.smartwalkie.voicepingsdk.exceptions.PingException;

public interface ConnectCallback {
    public void onConnected();
    public void onFailed(PingException exception);
}
