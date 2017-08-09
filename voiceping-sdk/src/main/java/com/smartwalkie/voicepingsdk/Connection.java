package com.smartwalkie.voicepingsdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.pusher.java_websocket.WebSocket;
import com.pusher.java_websocket.client.WebSocketClient;
import com.pusher.java_websocket.drafts.Draft_17;
import com.pusher.java_websocket.framing.Framedata;
import com.pusher.java_websocket.framing.FramedataImpl1;
import com.pusher.java_websocket.handshake.ServerHandshake;
import com.smartwalkie.voicepingsdk.listeners.ConnectionListener;
import com.smartwalkie.voicepingsdk.listeners.IncomingAudioListener;
import com.smartwalkie.voicepingsdk.listeners.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.MessageType;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;


public class Connection {

    private static final String TAG = Connection.class.getSimpleName();

    private WebSocketClient mWebSocketClient;
    private String mServerUrl;
    private Map<String, String> mHeaders;
    private ConnectionListener mConnectionListener;
    private IncomingAudioListener mIncomingAudioListener;
    private OutgoingAudioListener mOutgoingAudioListener;
    private Context mContext;

    // Constructor
    public Connection(Context context,
                      String serverUrl,
                      ConnectionListener connectionListener,
                      IncomingAudioListener incomingAudioListener) {

        mContext = context;
        mServerUrl = serverUrl;
        mConnectionListener = connectionListener;
        mIncomingAudioListener = incomingAudioListener;
    }

    public void setOutgoingAudioListener(OutgoingAudioListener listener) {
        mOutgoingAudioListener = listener;
    }

    public void connect(Map<String, String> headers) {
        mHeaders = headers;
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            return;
        }

        URI uri = null;
        try {
            uri = new URI(mServerUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (mWebSocketClient == null) {
            mWebSocketClient = getWebSocketClient(uri);
        }

        if (mServerUrl.contains("wss://")) {
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
                mWebSocketClient.setSocket(factory.createSocket());

            } catch (Exception e) {
                e.printStackTrace();
            }
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
            Log.d(TAG, "close WebSocket...");
            mWebSocketClient.close();
            mWebSocketClient = null;
            if (mConnectionListener != null) mConnectionListener.onDisconnected();
        }
    }

    public void send(byte[] data) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            mWebSocketClient.send(data);
        } else {
            Log.d(TAG, "WebSocket closed...");
        }
    }

    private WebSocketClient getWebSocketClient(URI uri) {
        if (mWebSocketClient == null) {
            mWebSocketClient = new WebSocketClient(uri, new Draft_17(), mHeaders, 0) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "onOpen...");
                    String userId = VoicePingPrefs.getInstance(mContext).getUserId();
                    mWebSocketClient.send(MessageHelper.createConnectionMessage(userId));
                    if (mConnectionListener != null) mConnectionListener.onConnected();
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "onMessage...");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "onClose...");
                    Log.d(TAG, "reason: " + reason);
                    Log.d(TAG, "remote: " + remote);
                    if (mConnectionListener != null) mConnectionListener.onDisconnected();
                }

                @Override
                public void onError(Exception ex) {
                    Log.v(TAG, "onError...");
                    ex.printStackTrace();
                    if (mConnectionListener != null) mConnectionListener.onFailed();
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    Message message = MessageHelper.unpackMessage(bytes.array());
                    if (mIncomingAudioListener != null) {
                        if (message.getMessageType() == MessageType.START_TALKING) {
                            mIncomingAudioListener.onStartTalkingMessage(message);
                        } else if (message.getMessageType() == MessageType.AUDIO) {
                            mIncomingAudioListener.onAudioTalkingMessage(message);
                        } else if (message.getMessageType() == MessageType.STOP_TALKING) {
                            mIncomingAudioListener.onStopTalkingMessage(message);
                        }
                    }

                    if (mOutgoingAudioListener != null) {
                        switch (message.getMessageType()) {
                            case MessageType.ACK_START:
                                mOutgoingAudioListener.onAckStartSucceed(message);
                                break;
                            case MessageType.ACK_START_FAILED:
                                mOutgoingAudioListener.onAckStartFailed(message);
                                break;
                            case MessageType.ACK_END:
                                mOutgoingAudioListener.onAckEndSucceed(message);
                                break;
                            case MessageType.MESSAGE_DELIVERED:
                                mOutgoingAudioListener.onMessageDelivered(message);
                                break;
                            case MessageType.MESSAGE_READ:
                                mOutgoingAudioListener.onMessageRead(message);
                        }
                    }

                    Log.d(TAG, "message: " + message.getMessageType());

                    if (mConnectionListener != null) mConnectionListener.onMessage(message);
                }

                @Override
                public void onWebsocketPing(WebSocket conn, Framedata f) {
                    Log.v(TAG, "onWebsocketPing...");
                    FramedataImpl1 resp = new FramedataImpl1(f);
                    resp.setOptcode(Framedata.Opcode.PONG);
                    conn.sendFrame(resp);
                }

                @Override
                public void onWebsocketPong(WebSocket conn, Framedata f) {
                    Log.v(TAG, "onWebsocketPong...");
                }
            };
        }
        return mWebSocketClient;
    }
}
