package com.smartwalkie.voiceping.models;

public final class MessageType {
    public static final int UNKNOWN = -1;
    public static final int START_TALKING = 1;
    public static final int STOP_TALKING = 2;
    public static final int AUDIO = 3;
    public static final int CONNECTION = 4;
    public static final int STATUS = 5;
    public static final int ACK_START = 6;
    public static final int ACK_END = 7;
    public static final int ACK_START_FAILED = 8;
    public static final int DUPLICATED_LOGIN = 9;
    public static final int USER_UPDATED = 10;
    public static final int USER_DELETED = 11;
    public static final int CHANNEL_UPDATED = 12;
    public static final int CHANNEL_DELETED = 13;
    public static final int INVALID_USER = 14;
    public static final int CHANNEL_ADDED_USER = 15;
    public static final int CHANNEL_REMOVED_USER = 16;
    public static final int TEXT = 17;
    public static final int IMAGE = 18;
    public static final int OFFLINE_MESSAGE = 19;
    public static final int MESSAGE_DELIVERED = 20;
    public static final int MESSAGE_READ = 21;
    public static final int ACK_TEXT = 22;
}
