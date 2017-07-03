package com.smartwalkie.voiceping;

import android.annotation.SuppressLint;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.framing.Framedata;
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


public class WebSocketConnection {
    public static final String TAG = WebSocketConnection.class.getSimpleName();

    private WebSocketClient mWebSocketClient;
    private String serverUrl;

    public WebSocketConnection(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void connect() {
        establishWebSocketConnection();
    }

    public void reconnect(int code) {
        disconnect(code, "Close and prepare for reconnection");
        establishWebSocketConnection();
    }

    private void establishWebSocketConnection() {

        if (mWebSocketClient != null && isConnected()) {
            return;
        }

        URI uri = null;
        try {
            uri = new URI(serverUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (mWebSocketClient == null) {
            mWebSocketClient = getWebsocketClient(uri);
        }

        if (mWebSocketClient == null) {

            for (int i = 0; i < 2; i++) {
                mWebSocketClient = getWebsocketClient(uri);

                if (mWebSocketClient != null) {
                    break;
                }
            }

            if (mWebSocketClient == null) {
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

                factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();
                mWebSocketClient.setSocket(factory.createSocket());

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
        }

        try {

            if (mWebSocketClient != null) {
                mWebSocketClient.connect();
            }

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(int code, String message) {

        if (mWebSocketClient != null) {
            mWebSocketClient = null;
        }
    }

    public void send(byte[] data) {

        if (mWebSocketClient != null) {
            if (!isConnected()) {

                if (mWebSocketClient.isConnecting()) {
                    try {
                        mWebSocketClient.wait(5000);
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
                    mWebSocketClient.send(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return mWebSocketClient != null && mWebSocketClient.isOpen();
    }

    public boolean isConnecting() {
        return mWebSocketClient != null && mWebSocketClient.isConnecting();
    }

    private void onWebSocketPong() {

    }

    private void onWebSocketMessage(ByteBuffer bytes) {

    }

    private void onWebSocketError(Exception ex) {

    }

    private void onWebSocketClose(int code, String reason, boolean remote) {

        if (shouldDisconnect(code)) {
            disconnect(code, reason);
        } else {
        }
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

        return result && mWebSocketClient != null && !mWebSocketClient.isClosed();
    }

    private void onWebSocketOpen() {

    }

    private WebSocketClient getWebsocketClient(URI uri) {
        if (mWebSocketClient == null) {
            HashMap<String, String> header = new HashMap<>();
            mWebSocketClient = new WebSocketClient(uri, new Draft_17(), header, 0) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    onWebSocketOpen();
                }

                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    onWebSocketClose(code, reason, remote);
                }

                @Override
                public void onError(Exception ex) {
                    onWebSocketError(ex);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    onWebSocketMessage(bytes);
                }

                @Override
                public void onWebsocketPing(WebSocket conn, Framedata f) {

                }

                @Override
                public void onWebsocketPong(WebSocket conn, Framedata f) {
                    onWebSocketPong();
                }
            };
        }

        return mWebSocketClient;
    }
}