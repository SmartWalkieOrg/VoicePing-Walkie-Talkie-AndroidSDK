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

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
    private String mServerUrl;
    private Map<String, String> mHeaders;
    private ConnectCallback mConnectCallback;
    private DisconnectCallback mDisconnectCallback;
    private IncomingAudioListener mIncomingAudioListener;
    private OutgoingAudioListener mOutgoingAudioListener;
    private boolean mIsReconnecting;
    private boolean mIsOpened;
    private boolean mIsDisconnected;

    public Connection(Context context, String serverUrl, IncomingAudioListener listener) {
        mContext = context;
        mServerUrl = serverUrl;
        mIncomingAudioListener = listener;

        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void setOutgoingAudioListener(OutgoingAudioListener listener) {
        mOutgoingAudioListener = listener;
    }

    public void connect(Map<String, String> headers, ConnectCallback callback) {
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            callback.onFailed(new PingException("Please check your internet connection!"));
            return;
        }
        mHeaders = headers;
        mConnectCallback = callback;
        connect();
    }

    private void connect() {
        Request.Builder builder = new Request.Builder().url(mServerUrl);
        if (mHeaders.containsKey("user_id")) builder.addHeader("user_id", mHeaders.get("user_id"));
        if (mHeaders.containsKey("DeviceId")) builder.addHeader("DeviceId", mHeaders.get("DeviceId"));
        Request request = builder.build();

        Log.d(TAG, "connecting...");
        mWebSocket = mOkHttpClient.newWebSocket(request, this);
//        mOkHttpClient.dispatcher().executorService().shutdown();
    }

    private void reconnectWithDelay() {
        if (!mIsReconnecting) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mIsReconnecting = false;
                    if (!mIsOpened && !mIsDisconnected) {
                        mWebSocket = null;
                        connect();
                    }
                }
            }, 5000);
        }
        mIsReconnecting = true;
    }

    public void disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        if (mWebSocket != null) {
            Log.d(TAG, "close WebSocket...");
            mWebSocket.cancel();
            mIsDisconnected = true;
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

    public void send(byte[] data) {
        if (mWebSocket != null) {
            mWebSocket.send(ByteString.of(data));
        } else {
            Log.d(TAG, "WebSocket closed...");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d(TAG, "WebSocket onOpen...");
        if (response != null) Log.d(TAG, response.toString());
        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
        mIsDisconnected = false;
        mIsOpened = true;
        send(MessageHelper.createConnectionMessage(userId));
        if (mConnectCallback != null) {
            mConnectCallback.onConnected();
            mConnectCallback = null;
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d(TAG, "WebSocket onMessage String...");
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
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
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "WebSocket onClosed...");
        Log.d(TAG, "reason: " + reason);
        mIsDisconnected = true;
        mIsOpened = false;
        if (mDisconnectCallback != null) {
            mDisconnectCallback.onDisconnected();
            mDisconnectCallback = null;
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.d(TAG, "WebSocket onFailure...");
        t.printStackTrace();
        if (response != null) Log.d(TAG, response.toString());
        mIsOpened = false;
        if (mConnectCallback != null) {
            mConnectCallback.onFailed(new PingException("Failed to connect!"));
            mConnectCallback = null;
        }
        if (t instanceof UnknownHostException ||
                t instanceof SocketException ||
                t instanceof SocketTimeoutException) {
            reconnectWithDelay();
        }
    }
}
