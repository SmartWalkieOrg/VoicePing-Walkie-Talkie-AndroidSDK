package com.smartwalkie.voiceping;


import android.content.Context;

import com.smartwalkie.voiceping.callbacks.ConnectCallback;
import com.smartwalkie.voiceping.exceptions.PingException;
import com.smartwalkie.voiceping.listeners.ConnectionListener;
import com.smartwalkie.voiceping.models.Message;

import java.util.HashMap;
import java.util.Map;

public class VoicePing implements
        ConnectionListener
{
    private static final VoicePing instance = new VoicePing();

    private Context context;
    private String serverUrl;

    private Connection connection;
    private ConnectCallback connectCallback;

    private Player player;

    public static VoicePing getInstance() {
        return instance;
    }

    private VoicePing() {
        player = new Player();
        player.start();
    }

    public static void configure(Context context, String serverUrl) {
        getInstance()._configure(context, serverUrl);
    }

    public static void connect(String username, ConnectCallback callback) {
        getInstance()._connect(username, callback);
    }

    public static void connect(Map<String, String> props, ConnectCallback callback) {
        getInstance()._connect(props, callback);
    }

    private void _configure(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;

        connection = new Connection(serverUrl);
        connection.setConnectionListener(this);
        connection.setIncomingAudioListener(player);
    }

    private void _connect(String username, ConnectCallback callback) {
        Map<String, String> props = new HashMap<>();
        props.put("username", username);
        _connect(props, callback);
    }

    private void _connect(Map<String, String> props, ConnectCallback callback) {
        this.connectCallback = callback;
        connection.connect(props);
        /*
        Intent serviceIntent = new Intent(context, PingService.class);
        context.startService(serviceIntent);
        */
    }

    // ConnectionListener
    @Override
    public void onMessage(Message message) {

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onConnected() {
        if (connectCallback != null) connectCallback.onConnected();
    }

    @Override
    public void onFailed() {
        if (connectCallback != null) connectCallback.onFailed(new PingException());
    }

    @Override
    public void onData(byte[] data) {

    }

    @Override
    public void onDisconnected() {

    }
}
