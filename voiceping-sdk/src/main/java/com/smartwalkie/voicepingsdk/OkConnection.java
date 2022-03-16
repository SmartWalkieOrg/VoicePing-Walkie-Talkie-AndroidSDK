package com.smartwalkie.voicepingsdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.smartwalkie.voicepingsdk.callback.ConnectCallback;
import com.smartwalkie.voicepingsdk.callback.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exception.ErrorCode;
import com.smartwalkie.voicepingsdk.exception.VoicePingException;
import com.smartwalkie.voicepingsdk.listener.ConnectionStateListener;
import com.smartwalkie.voicepingsdk.listener.IncomingAudioListener;
import com.smartwalkie.voicepingsdk.listener.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.model.ChannelType;
import com.smartwalkie.voicepingsdk.model.Message;
import com.smartwalkie.voicepingsdk.model.MessageType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

class OkConnection implements Connection {
    private final String TAG = OkConnection.class.getSimpleName();
    private final int CONNECTED = 100;
    private final int DISCONNECTED = 200;
    private final int MESSAGE = 300;
    private final int FAILURE = 400;

    private final Context mContext;
    private final OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
    private final String mServerUrl;
    private String mUserId;
    private String mDeviceId;
    private final Handler mMainHandler;
    private final Handler mBackgroundHandler;

    private ConnectCallback mConnectCallback;
    private DisconnectCallback mDisconnectCallback;
    private IncomingAudioListener mIncomingAudioListener;
    private OutgoingAudioListener mOutgoingAudioListener;
    private ConnectionStateListener mConnectionStateListener;
    private volatile ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private volatile boolean mIsReconnecting;

    OkConnection(Context context, String serverUrl, IncomingAudioListener listener, Looper backgroundLooper) {
        mContext = context;
        if (serverUrl != null && !serverUrl.isEmpty()) {
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
        }
        mServerUrl = serverUrl;
        mIncomingAudioListener = listener;
        mOkHttpClient = createHttpClient(serverUrl);
        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundHandler = new Handler(backgroundLooper, new Handler.Callback() {

            @Override
            public boolean handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case CONNECTED:
                        updateConnectionState(ConnectionState.CONNECTED);
                        send(MessageHelper.createConnectionMessage(mUserId));
                        Message dummyAckStart = MessageHelper.createAckStartMessage(mUserId, "00000", ChannelType.PRIVATE, System.currentTimeMillis());
                        if (dummyAckStart != null) {
                            send(dummyAckStart.getPayload());
                        }
                        if (mConnectCallback != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mConnectCallback != null) {
                                        mConnectCallback.onConnected();
                                        mConnectCallback = null;
                                    }
                                }
                            });
                        }
                        return true;
                    case DISCONNECTED:
                        updateConnectionState(ConnectionState.DISCONNECTED);
                        if (mDisconnectCallback != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDisconnectCallback != null) {
                                        mDisconnectCallback.onDisconnected();
                                        mDisconnectCallback = null;
                                    }
                                }
                            });
                        }
                        return true;
                    case MESSAGE:
                        Message message = (Message) msg.obj;
                        if (message == null) return false;
                        if (message.getMessageType() == MessageType.DUPLICATE_CONNECT) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    disconnect(null);
                                    if (mConnectionStateListener != null) {
                                        mConnectionStateListener.onConnectionError(
                                                new VoicePingException("DUPLICATE_CONNECT", ErrorCode.DUPLICATE_CONNECT));
                                    }
                                }
                            });
                        }
                        if (mIncomingAudioListener != null && isIncomingMessageType(message.getMessageType())) {
                            mIncomingAudioListener.onMessageReceived(message);
                        }
                        if (mOutgoingAudioListener != null && isOutgoingMessageType(message.getMessageType())) {
                            mOutgoingAudioListener.onMessageReceived(message);
                        }
                        return true;
                    case FAILURE:
                        if (mConnectionState == ConnectionState.DISCONNECTED) return true;
                        if (mConnectionState == ConnectionState.CONNECTED) {
                            updateConnectionState(ConnectionState.DISCONNECTED);
                        }
                        if (mConnectCallback != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mConnectCallback != null) {
                                        mConnectCallback.onFailed(new VoicePingException("Failed to connect!", ErrorCode.CONNECTION_FAILURE));
                                        mConnectCallback = null;
                                    }
                                }
                            });
                        }
                        if (mIncomingAudioListener != null) {
                            mIncomingAudioListener.onConnectionFailure(new VoicePingException(
                                    "Connection failure. Please check your internet connection!", ErrorCode.CONNECTION_FAILURE));
                        }
                        if (mOutgoingAudioListener != null) {
                            mOutgoingAudioListener.onConnectionFailure(new VoicePingException(
                                    "Connection failure. Please check your internet connection!", ErrorCode.CONNECTION_FAILURE));
                        }
                        reconnectWithDelay();
                        return true;
                }
                return false;
            }
        });
    }

    private OkHttpClient createHttpClient(String serverUrl) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
        if (serverUrl != null && serverUrl.startsWith("wss:")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                X509TrustManager trustManager = new X509TrustManager() {
                    @SuppressLint("TrustAllX509TrustManager")
                    /**
                     * TODO
                     * We should place another client check here for adding an extra security measure
                     */
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    /**
                     * TODO
                     * We should place another server check here for adding an extra security measure
                     */
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
                clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return clientBuilder.build();
    }

    private boolean isIncomingMessageType(int messageType) {
        return messageType == MessageType.START_TALKING
                || messageType == MessageType.AUDIO
                || messageType == MessageType.STOP_TALKING;
    }

    private boolean isOutgoingMessageType(int messageType) {
        return messageType == MessageType.ACK_START
                || messageType == MessageType.ACK_START_FAILED
                || messageType == MessageType.ACK_END
                || messageType == MessageType.MESSAGE_DELIVERED
                || messageType == MessageType.MESSAGE_READ
                || messageType == MessageType.UNAUTHORIZED_GROUP;
    }

    @Override
    public void setConnectionStateListener(@Nullable ConnectionStateListener listener) {
        mConnectionStateListener = listener;
    }

    @Override
    public void setOutgoingAudioListener(OutgoingAudioListener listener) {
        mOutgoingAudioListener = listener;
    }

    @Override
    public String getServerUrl() {
        return mServerUrl;
    }

    @Override
    public void connect(String userId, String deviceId, ConnectCallback callback) {
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            callback.onFailed(new VoicePingException("Please check your internet connection!", ErrorCode.INTERNET_DISCONNECTED));
            return;
        }
        mUserId = userId;
        mDeviceId = deviceId;
        if (callback != null) {
            mConnectCallback = callback;
        }
        connect();
    }

    @NotNull
    @Override
    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    private void updateConnectionState(ConnectionState connectionState) {
        if (mConnectionState == connectionState) return;
        Log.d(TAG, "updateConnectionState: " + connectionState.name());
        mConnectionState = connectionState;
        if (mConnectionStateListener != null) {
            mConnectionStateListener.onConnectionStateChanged(connectionState);
        }
    }

    private void connect() {
        updateConnectionState(ConnectionState.CONNECTING);
        Request request = new Request.Builder().url(mServerUrl)
                .addHeader("VoicePingToken", mUserId != null ? mUserId : "")
                .addHeader("DeviceId", mDeviceId != null ? mDeviceId : "")
                .build();
        Log.d(TAG, "Connecting. Request headers: " + request.headers().toString());
        mWebSocket = mOkHttpClient.newWebSocket(request, new SocketListener());
//        mOkHttpClient.dispatcher().executorService().shutdown();
    }

    private void reconnectWithDelay() {
        updateConnectionState(ConnectionState.CONNECTING);
        if (!mIsReconnecting) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mIsReconnecting = false;
                    if (mConnectionState == ConnectionState.CONNECTING) {
                        connect();
                    }
                }
            }, 5000);
        }
        mIsReconnecting = true;
    }

    @Override
    public void disconnect(DisconnectCallback callback) {
        if (callback != null) {
            mDisconnectCallback = callback;
        }
        Log.d(TAG, "close WebSocket...");
        if (mWebSocket != null) mWebSocket.cancel();
        updateConnectionState(ConnectionState.DISCONNECTED);
        if (mDisconnectCallback != null) {
            mDisconnectCallback.onDisconnected();
            mDisconnectCallback = null;
        }
    }

    @Override
    public void send(byte[] data) {
        if (mConnectionState != ConnectionState.CONNECTED) {
            if (mOutgoingAudioListener != null) {
                mOutgoingAudioListener.onSendMessageFailed(data, new VoicePingException("WebSocket is not connected!", ErrorCode.SOCKET_DISCONNECTED));
            }
            return;
        }
        if (mWebSocket != null) mWebSocket.send(ByteString.of(data));
    }

    private class SocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            String responseString = response != null ? response.toString() : "";
            Log.d(TAG, "WebSocket onOpen, response: " + responseString);
            mBackgroundHandler.sendEmptyMessage(CONNECTED);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Message message = MessageHelper.unpackMessage(bytes.toByteArray());
            if (message == null) return;
//            Log.d(TAG, "message: " + MessageType.getText(message.getMessageType()));

            android.os.Message msg = new android.os.Message();
            msg.what = MESSAGE;
            msg.obj = message;
            mBackgroundHandler.sendMessage(msg);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.d(TAG, "WebSocket onClosed, reason: " + reason);
            mBackgroundHandler.sendEmptyMessage(DISCONNECTED);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d(TAG, "WebSocket onFailure, error: " + t.getMessage());
            t.printStackTrace();
            if (response != null) Log.d(TAG, response.toString());
            mBackgroundHandler.sendEmptyMessage(FAILURE);
        }
    }
}
