package com.smartwalkie.voiceping.models;

public enum MessageType {
    Unknown(-1),
    StartTalking(1),
    StopTalking(2),
    Audio(3),
    Connection(4),
    Status(5),
    AckStart(6),
    AckEnd(7),
    AckStartFail(8),
    DuplicateLogin(9),
    UpdateUser(10),
    DeleteUser(11),
    UpdateChannel(12),
    DeleteChannel(13),
    InvalidUser(14),
    ChannelAddUser(15),
    ChannelRemoveUser(16),
    Text(17),
    Image(18),
    OfflineMessage(19),
    DeliveredMessage(20),
    ReadMessage(21),
    AckText(22);

    int type;
    String name;

    MessageType(int type) {
        this.type = type;
        switch (type) {
            case 1:
                this.name = "StartTalking";
                break;
            case 2:
                this.name = "StopTalking";
                break;
            case 3:
                this.name = "Audio";
                break;
            case 4:
                this.name = "Connection";
                break;
            case 5:
                this.name = "Status";
                break;
            case 6:
                this.name = "AckStart";
                break;
            case 7:
                this.name = "AckEnd";
                break;
            case 8:
                this.name = "AckStartFail";
                break;
            case 9:
                this.name = "DuplicateLogin";
                break;
            case 10:
                this.name = "UpdateUser";
                break;
            case 11:
                this.name = "DeleteUser";
                break;
            case 12:
                this.name = "UpdateChannel";
                break;
            case 13:
                this.name = "DeleteChannel";
                break;
            case 14:
                this.name = "InvalidUser";
                break;
            case 15:
                this.name = "ChannelAddUser";
                break;
            case 16:
                this.name = "ChannelRemoveUser";
                break;
            case 17:
                this.name = "TextMessage";
                break;
            case 18:
                this.name = "ImageMessage";
                break;
            case 19:
                this.name = "OfflineMessage";
                break;
            case 20:
                this.name = "DeliveredMessage";
                break;
            case 21:
                this.name = "ReadMessage";
                break;
            case 22:
                this.name = "AckText";
                break;
            case 23:
                this.name = "AckFail";
                break;
            case -1:
                this.name = "Unknown";
                break;
        }
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return this.name;
    }

    public static MessageType intToMessageType(int type)  {
        for (MessageType messageType : MessageType.values()) {
            if(messageType.getType() == type)return messageType;
        }
        return Unknown;
    }
}
