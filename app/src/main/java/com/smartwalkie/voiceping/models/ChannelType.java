package com.smartwalkie.voiceping.models;

public enum ChannelType {
    Group_type(0),
    Private_type(1);
    int type;

    ChannelType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
