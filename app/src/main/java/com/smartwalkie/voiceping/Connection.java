package com.smartwalkie.voiceping;

import android.annotation.SuppressLint;
import android.provider.Settings.Secure;
import android.util.Log;

import com.smartwalkie.voiceping.events.DisconnectEvent;
import com.smartwalkie.voiceping.events.MessageEvent;
import com.smartwalkie.voiceping.listeners.ConnectionListener;
import com.smartwalkie.voiceping.listeners.IncomingAudioListener;
import com.smartwalkie.voiceping.models.Message;
import com.smartwalkie.voiceping.models.MessageType;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;


public class Connection {
    public static final String TAG = Connection.class.getSimpleName();

    private WebSocketClient webSocketClient;
    private String serverUrl;
    private ConnectionListener connectionListener;
    private IncomingAudioListener incomingAudioListener;

    // Singleton
    private static Connection instance;
    public static Connection getInstance() {
        return instance;
    }
    // Singleton

    // Public Setters
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void setIncomingAudioListener(IncomingAudioListener incomingAudioListener) {
        this.incomingAudioListener = incomingAudioListener;
    }
    // Public Setters

    // Constructor
    public Connection(String serverUrl) {
        this.serverUrl = serverUrl;
        instance = this;
    }
    // Constructor

    // Public Methods
    public void reconnect(int code) {
        disconnect();
        connect();
    }

    public void connect() {
        if (webSocketClient != null && isConnected()) {
            return;
        }

        URI uri = null;
        try {
            uri = new URI(serverUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (webSocketClient == null) {
            webSocketClient = getWebSocketClient(uri);
        }

        if (webSocketClient == null) {
            for (int i = 0; i < 2; i++) {
                webSocketClient = getWebSocketClient(uri);
                if (webSocketClient != null) {
                    break;
                }
            }
            if (webSocketClient == null) {
                return;
            }
        }

        if (serverUrl.contains("wss")) {
            SSLContext sslContext;
            SSLSocketFactory factory;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new X509TrustManager[]{new X509TrustManager() {
                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());

                factory = sslContext.getSocketFactory();    // (SSLSocketFactory) SSLSocketFactory.getDefault();
                webSocketClient.setSocket(factory.createSocket());

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
        }

        try {
            if (webSocketClient != null) {
                webSocketClient.connect();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient = null;
        }
        EventBus.getDefault().post(new DisconnectEvent());
    }

    public void send(byte[] data) {
        if (webSocketClient != null) {
            if (!isConnected()) {
                if (webSocketClient.isConnecting()) {
                    try {
                        webSocketClient.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    reconnect(CloseFrame.BUGGYCLOSE);
                    return;
                }
            }
            try {
                if (isConnected()) {
                    webSocketClient.send(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    /**
     * Do NOT simplified these if statements. Clearer this way
     */
    private boolean shouldDisconnect(int code) {
        boolean result = true;
        if (code == -1 && isConnected()) {
            result = false;
        }
        return result && webSocketClient != null && !webSocketClient.isClosed();
    }
    // Public Methods

    private WebSocketClient getWebSocketClient(URI uri) {
        if (webSocketClient == null) {
            HashMap<String, String> header = new HashMap<>();
            header.put("VoicePingToken", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjoiOTUwMzBmYjAtYmVhMy0xMWU0LWI4YWYtZTMwM2MwZTQ2NGM3IiwidWlkIjo1NiwidXNlcm5hbWUiOiJzaXJpdXMiLCJjaGFubmVsSWRzIjpbMSwyMTc1LDIxOTldfQ.1wq50IorIxIq2xydFQEG8TKFJ3xxra22ts26SR8Du3c");
            header.put("DeviceId", Secure.getString(VoicePingClient.getInstance().getContentResolver(),
                    Secure.ANDROID_ID));
            webSocketClient = new WebSocketClient(uri, new Draft_17(), header, 0) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.v(TAG, "onOpen");
                }

                @Override
                public void onMessage(String message) {
                    Log.v(TAG, "onMessage");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (shouldDisconnect(code)) {
                        disconnect();
                    } else {
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.v(TAG, "onError");
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    Message message = MessageHelper.unpackMessage(bytes.array());
                    if (incomingAudioListener != null) {
                        if (MessageType.intToMessageType(message.messageType) == MessageType.StartTalking) {
                            incomingAudioListener.onStartTalkingMessage(message);
                        } else if (MessageType.intToMessageType(message.messageType) == MessageType.Audio) {
                            incomingAudioListener.onAudioTalkingMessage(message);
                        } else if (MessageType.intToMessageType(message.messageType) == MessageType.StopTalking) {
                            incomingAudioListener.onStopTalkingMessage(message);
                        }
                    }
                    Log.v(TAG, "message: " + message.messageType);

                    MessageEvent messageEvent = new MessageEvent(message);
                    EventBus.getDefault().post(messageEvent);

                    if (connectionListener != null) connectionListener.onMessage(message);
                }

                @Override
                public void onWebsocketPing(WebSocket conn, Framedata f) {
                    Log.v(TAG, "onWebsocketPing");
                    FramedataImpl1 resp = new FramedataImpl1(f);
                    resp.setOptcode(Framedata.Opcode.PONG);
                    conn.sendFrame(resp);
                }

                @Override
                public void onWebsocketPong(WebSocket conn, Framedata f) {
                    Log.v(TAG, "onWebsocketPong");
                }
            };
        }
        return webSocketClient;
    }
}
