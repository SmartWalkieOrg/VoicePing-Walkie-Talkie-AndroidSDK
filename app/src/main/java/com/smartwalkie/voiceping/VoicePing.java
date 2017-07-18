package com.smartwalkie.voiceping;


import com.smartwalkie.voiceping.callbacks.ConnectCallback;

import java.util.Map;

public class VoicePing {
    private static final VoicePing instance = new VoicePing();

    public static VoicePing getInstance() {
        return instance;
    }

    private VoicePing() {
    }

    public static void configure(String serverUrl) {
        getInstance()._configure(serverUrl);
    }

    public static void connect(String username, ConnectCallback callback) {
        getInstance()._connect(username, callback);
    }

    public static void connect(Map<String, Object> props, ConnectCallback callback) {
        getInstance()._connect(props, callback);
    }

    private void _configure(String serverUrl) {

    }

    private void _connect(String username, ConnectCallback callback) {

    }

    private void _connect(Map<String, Object> props, ConnectCallback callback) {

    }

}
