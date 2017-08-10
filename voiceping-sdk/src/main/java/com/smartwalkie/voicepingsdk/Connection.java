package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.util.Log;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.IncomingAudioListener;
import com.smartwalkie.voicepingsdk.listeners.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.MessageType;
import com.smartwalkie.voicepingsdk.models.local.VoicePingPrefs;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class Connection extends WebSocketListener {

    private final String TAG = Connection.class.getSimpleName();

    private Context mContext;
    private OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
//    private WebSocketClient mWebSocketClient;
    private String mServerUrl;
    private Map<String, String> mHeaders;
    private ConnectCallback mConnectCallback;
    private DisconnectCallback mDisconnectCallback;
    private IncomingAudioListener mIncomingAudioListener;
    private OutgoingAudioListener mOutgoingAudioListener;

    public Connection(Context context, String serverUrl, IncomingAudioListener listener) {
        mContext = context;
        mServerUrl = serverUrl;
        mIncomingAudioListener = listener;

        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(3000, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void setOutgoingAudioListener(OutgoingAudioListener listener) {
        mOutgoingAudioListener = listener;
    }

    public void connect(Map<String, String> headers, ConnectCallback callback) {
        mHeaders = headers;
        mConnectCallback = callback;
        connect();
    }

    private void connect() {
        Request.Builder builder = new Request.Builder().url(mServerUrl);
        if (mHeaders.containsKey("user_id")) builder.addHeader("user_id", mHeaders.get("user_id"));
        if (mHeaders.containsKey("DeviceId")) builder.addHeader("DeviceId", mHeaders.get("DeviceId"));
        Request request = builder.build();

        mWebSocket = mOkHttpClient.newWebSocket(request, this);
//        mOkHttpClient.dispatcher().executorService().shutdown();
    }

    /*public void connect(Map<String, String> headers, ConnectCallback callback) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            return;
        }

        mHeaders = headers;
        mConnectCallback = callback;

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
    }*/

    public void disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        if (mWebSocket != null) {
            Log.d(TAG, "close WebSocket...");
            mWebSocket.close(1000, "User wants to disconnect!");
            if (mDisconnectCallback != null) {
                mDisconnectCallback.onDisconnected();
                mDisconnectCallback = null;
            }
        } else {
            if (mDisconnectCallback != null) {
                mDisconnectCallback.onFailed(new PingException("Failed to disconnect!"));
                mDisconnectCallback = null;
            }
        }
    }

    /*public void disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        if (mWebSocketClient != null) {
            Log.d(TAG, "close WebSocket...");
            mWebSocketClient.close();
            mWebSocketClient = null;
            if (mDisconnectCallback != null) mDisconnectCallback.onDisconnected();
        } else {
            if (mDisconnectCallback != null) mDisconnectCallback
                    .onFailed(new PingException("Failed to disconnect!"));
        }
    }*/

    public void send(byte[] data) {
        if (mWebSocket != null) {
            mWebSocket.send(ByteString.of(data));
        } else {
            Log.d(TAG, "WebSocket closed...");
            connect();
        }
    }

    /*public void send(byte[] data) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            mWebSocketClient.send(data);
        } else {
            Log.d(TAG, "WebSocket closed...");
        }
    }*/

    /*private WebSocketClient getWebSocketClient(URI uri) {
        if (mWebSocketClient == null) {
            mWebSocketClient = new WebSocketClient(uri, new Draft_17(), mHeaders, 0) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "onOpen...");
                    String userId = VoicePingPrefs.getInstance(mContext).getUserId();
                    mWebSocketClient.send(MessageHelper.createConnectionMessage(userId));
                    if (mConnectCallback != null) mConnectCallback.onConnected();
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
                    if (mDisconnectCallback != null) mDisconnectCallback.onDisconnected();
                }

                @Override
                public void onError(Exception ex) {
                    Log.v(TAG, "onError...");
                    ex.printStackTrace();
                    if (mConnectCallback != null) mConnectCallback
                            .onFailed(new PingException("Failed to connect!"));
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
    }*/

    @Override
    public void onOpen(okhttp3.WebSocket webSocket, Response response) {
        Log.d(TAG, "WebSocket onOpen...");
        if (response != null) Log.d(TAG, response.toString());
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        send(MessageHelper.createConnectionMessage(userId));
        if (mConnectCallback != null) {
            mConnectCallback.onConnected();
            mConnectCallback = null;
        }
    }

    @Override
    public void onMessage(okhttp3.WebSocket webSocket, String text) {
        Log.d(TAG, "WebSocket onMessage String...");
    }

    @Override
    public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
        Log.d(TAG, "WebSocket onMessage ByteString...");

        Message message = MessageHelper.unpackMessage(bytes.toByteArray());
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
    }

    @Override
    public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "WebSocket onClosing...");
        Log.d(TAG, "reason: " + reason);
    }

    @Override
    public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "WebSocket onClosed...");
        Log.d(TAG, "reason: " + reason);
        if (mDisconnectCallback != null) {
            mDisconnectCallback.onDisconnected();
            mDisconnectCallback = null;
        }
    }

    @Override
    public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
        Log.d(TAG, "WebSocket onFailure...");
        t.printStackTrace();
        if (response != null) Log.d(TAG, response.toString());
        if (mConnectCallback != null) {
            mConnectCallback.onFailed(new PingException("Failed to connect!"));
            mConnectCallback = null;
        }
        mWebSocket = null;
    }
}
