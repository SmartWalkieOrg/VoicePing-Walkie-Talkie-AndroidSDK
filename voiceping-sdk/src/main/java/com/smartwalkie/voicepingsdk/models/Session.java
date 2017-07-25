package com.smartwalkie.voicepingsdk.models;

import android.content.Context;

public class Session {

    private static Session INSTANCE;
    private int userId;
    private String serverUrl;
    private Context context;

    public static Session getInstance() {
        if (INSTANCE == null) INSTANCE = new Session();
        return INSTANCE;
    }

    private Session() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
