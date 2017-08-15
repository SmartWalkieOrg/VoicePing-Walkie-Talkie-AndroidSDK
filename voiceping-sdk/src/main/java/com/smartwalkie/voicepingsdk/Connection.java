package com.smartwalkie.voicepingsdk;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.IncomingAudioListener;
import com.smartwalkie.voicepingsdk.listeners.OutgoingAudioListener;
import com.smartwalkie.voicepingsdk.models.Message;
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


public class Connection {

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
    private Handler mHandler;

    private final int CONNECTED = 100;
    private final int DISCONNECTED = 200;
    private final int FAILURE = 300;

    public Connection(Context context, String serverUrl, IncomingAudioListener listener) {
        mContext = context;
        mServerUrl = serverUrl;
        mIncomingAudioListener = listener;

        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        mHandler = new Handler(new Handler.Callback() {

            @Override
            public boolean handleMessage(android.os.Message message) {
                switch (message.what) {
                    case CONNECTED:
                        String userId = VoicePingPrefs.getInstance(mContext).getUserId();
                        mIsDisconnected = false;
                        mIsOpened = true;
                        send(MessageHelper.createConnectionMessage(userId));
                        if (mConnectCallback != null) {
                            mConnectCallback.onConnected();
                            mConnectCallback = null;
                        }
                        return true;
                    case DISCONNECTED:
                        mIsDisconnected = true;
                        mIsOpened = false;
                        if (mDisconnectCallback != null) {
                            mDisconnectCallback.onDisconnected();
                            mDisconnectCallback = null;
                        }
                        return true;
                    case FAILURE:
                        mIsOpened = false;
                        if (mConnectCallback != null) {
                            mConnectCallback.onFailed(new PingException("Failed to connect!"));
                            mConnectCallback = null;
                        }
                        return true;
                }
                return false;
            }
        });
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
        mWebSocket = mOkHttpClient.newWebSocket(request, new SocketListener());
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
                        connect();
                    }
                }
            }, 5000);
        }
        mIsReconnecting = true;
    }

    public void disconnect(DisconnectCallback callback) {
        mDisconnectCallback = callback;
        Log.d(TAG, "close WebSocket...");
        mWebSocket.cancel();
        mIsDisconnected = true;
        if (mDisconnectCallback != null) {
            mDisconnectCallback.onDisconnected();
            mDisconnectCallback = null;
        }
    }

    public void send(byte[] data) {
        if (mOutgoingAudioListener != null && !mIsOpened) {
            mOutgoingAudioListener.onError(data, new PingException("WebSocket is not connected!"));
            return;
        }
        mWebSocket.send(ByteString.of(data));
    }

    private class SocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "WebSocket onOpen...");
            if (response != null) Log.d(TAG, response.toString());
            mHandler.sendEmptyMessage(CONNECTED);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
//            Log.d(TAG, "WebSocket onMessage ByteString...");
            Message message = MessageHelper.unpackMessage(bytes.toByteArray());
//            Log.d(TAG, "message: " + message.getMessageType());
            if (mIncomingAudioListener != null) mIncomingAudioListener.onMessageReceived(message);
            if (mOutgoingAudioListener != null) mOutgoingAudioListener.onMessageReceived(message);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.d(TAG, "WebSocket onClosed...");
            Log.d(TAG, "reason: " + reason);
            mHandler.sendEmptyMessage(DISCONNECTED);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d(TAG, "WebSocket onFailure...");
            t.printStackTrace();
            if (response != null) Log.d(TAG, response.toString());
            mHandler.sendEmptyMessage(FAILURE);
            if (t instanceof UnknownHostException ||
                    t instanceof SocketException ||
                    t instanceof SocketTimeoutException) {
                reconnectWithDelay();
            }
        }
    }
}
