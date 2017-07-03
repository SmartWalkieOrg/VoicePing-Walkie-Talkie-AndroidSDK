package com.smartwalkie.voiceping;

public interface ConnectionAdapter {

    /**
     * Set callback for WebsocketConnection. Then establish websocket connection
     * @param connectionCallback VoicePingProcessor
     */
    void connect(ConnectionCallback connectionCallback);

    /**
     * Disconnect websocket connection.
     * Consequently, application state will follow.
     *
     * @param code disconnection code in CloseFrame class
     * @param message detail in reason why it is disconnected
     */
    void disconnect(int code, String message);

    /**
     * Connect without establishing the callback
     */
    void reconnect(int closeCode, String message);

    /**
     * Use to sendPing to remote
     */
    void sendPing();

    /**
     * Send the data
     * @param data whatever bytes to be sent to the server
     */
    void send(byte[] data);

    /**
     * To represent whether websocket connection is in connected state.
     * @return true if it is connected, false if otherwise
     */
    boolean isConnected();

    /**
     * To represent whether websocket connection is in connecting state.
     * @return true if it is connecting, false if otherwise
     */
    boolean isConnecting();

    /**
     * Whether PRIMARY or SECONDARY
     * @return integer representing router's ID
     */
    int getRouterType();

    /**
     * What to do after trying to connect with no avail in a given time
     */
    void onConnectTimeout();

    /**
     * Get a ping sent buffer to see if ping pong is late
     * @return pingSentBuffer
     */
    long getPingSentBuffer();

}
