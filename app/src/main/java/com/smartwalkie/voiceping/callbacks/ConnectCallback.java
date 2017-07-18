package com.smartwalkie.voiceping.callbacks;


import com.smartwalkie.voiceping.exceptions.PingException;

public interface ConnectCallback {
    public void onConnected();
    public void onFailed(PingException exception);
}
