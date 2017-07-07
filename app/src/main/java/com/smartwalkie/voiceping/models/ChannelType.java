package com.smartwalkie.voiceping.models;

public enum ChannelType {
    GroupType(0),
    PrivateType(1);
    int type;

    ChannelType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
