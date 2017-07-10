package com.smartwalkie.voiceping;

import android.annotation.SuppressLint;
import android.provider.Settings;

import com.smartwalkie.voiceping.events.DisconnectEvent;
import com.smartwalkie.voiceping.models.Message;

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

    private WebSocketClient mWebSocketClient;
    private String serverUrl;
    public ConnectionListener listener;

    private static Connection instance;
    public static Connection getInstance() {
        return instance;
    }

    public Connection(String serverUrl) {
        this.serverUrl = serverUrl;
        instance = this;
    }

    public void reconnect(int code) {
        disconnect();
        connect();
    }

    public void connect() {

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

    public void disconnect() {

        if (mWebSocketClient != null) {
            mWebSocketClient = null;
        }
        EventBus.getDefault().post(new DisconnectEvent());
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

    private WebSocketClient getWebsocketClient(URI uri) {
        if (mWebSocketClient == null) {
            HashMap<String, String> header = new HashMap<>();
            header.put("VoicePingToken", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjoiOTUwMzBmYjAtYmVhMy0xMWU0LWI4YWYtZTMwM2MwZTQ2NGM3IiwidWlkIjo1NiwidXNlcm5hbWUiOiJzaXJpdXMiLCJjaGFubmVsSWRzIjpbMSwyMTc1LDIxOTldfQ.1wq50IorIxIq2xydFQEG8TKFJ3xxra22ts26SR8Du3c");
            header.put("DeviceId", Settings.Secure.getString(VoicePingClient.getInstance().getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            mWebSocketClient = new WebSocketClient(uri, new Draft_17(), header, 0) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                }

                @Override
                public void onMessage(String message) {
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
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    Message message = MessageHelper.getInstance().unpackMessage(bytes.array());
                    if (listener != null) listener.onMessage(message);
                }

                @Override
                public void onWebsocketPing(WebSocket conn, Framedata f) {
                    FramedataImpl1 resp = new FramedataImpl1(f);
                    resp.setOptcode(Framedata.Opcode.PONG);
                    conn.sendFrame(resp);
                }

                @Override
                public void onWebsocketPong(WebSocket conn, Framedata f) {
                }
            };
        }

        return mWebSocketClient;
    }
}
