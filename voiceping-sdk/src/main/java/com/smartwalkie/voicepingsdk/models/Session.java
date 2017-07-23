package com.smartwalkie.voicepingsdk.models;

import android.content.Context;

public class Session {

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public int userId;
    public String serverUrl;
    public Context context;

    private static Session instance;

    private Session() {};

}
