package com.smartwalkie.voiceping;

import android.util.Log;

import com.google.gson.Gson;
import com.smartwalkie.voiceping.models.ChannelType;
import com.smartwalkie.voiceping.models.Message;
import com.smartwalkie.voiceping.models.MessageType;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MessageHelper {

    public static final String TAG = MessageHelper.class.getSimpleName();

    private static MessagePack msgPack = new MessagePack();
    private static MessageHelper instance;

    private MessageHelper() {
        msgPack = new MessagePack();
    }

    public static MessageHelper getInstance() {
        if (instance == null) {
            instance = new MessageHelper();
        }
        return instance;
    }


    public static Message unpackMessage(byte[] payload) {
        ByteArrayInputStream in = new ByteArrayInputStream(payload);
        MessagePackUnpacker unpacker = (MessagePackUnpacker) msgPack.createUnpacker(in);

        try {
            Message message = new Message();
            //message.contentType = RecordModel.CONTENT_TYPE_AUDIO;
            unpacker.readArrayBegin();
            message.channelType = unpacker.readInt();
            message.messageType = unpacker.readInt();
            message.senderUserId = unpacker.readInt();
            message.receiveChannelId = unpacker.readInt();
            try {
                if (message.messageType == MessageType.StartTalking.getType() && unpacker.getCountRemain() > 0) {
                    message.duration = unpacker.readLong();
                } else if (message.messageType == MessageType.Audio.getType()) {
                    message.payload = unpacker.readByteArray();
                } else  if (message.messageType == MessageType.OfflineMessage.getType()) {
                    message.offlineMessage = unpacker.readString();
                } else if (message.messageType == MessageType.StopTalking.getType() && unpacker.getCountRemain() > 0) {
                    message.ackIds = unpacker.readString();
                } else if (message.messageType == MessageType.DeliveredMessage.getType() && unpacker.getCountRemain() > 0) {
                    message.ackIds = unpacker.readString();
                } else if( message.messageType == MessageType.AckEnd.getType() && unpacker.getCountRemain()>0){

                    message.ackIds = unpacker.readString();

                } else if(message.messageType == MessageType.ReadMessage.getType() && unpacker.getCountRemain()>0){
                    message.ackIds = unpacker.readString();
                } else if(message.messageType == MessageType.Status.getType() && unpacker.getCountRemain() > 0) {

                    message.status = unpacker.readInt();
                    Log.d(TAG, "message from " + message.senderUserId + " user status " + message.status);

                } else if (message.messageType == MessageType.Text.getType() && unpacker.getCountRemain() > 0) {

                    String jsonTextMessageIdentification = unpacker.readString();
                    //TextMessageIdentification tmi = new Gson().fromJson(jsonTextMessageIdentification, TextMessageIdentification.class);
                    //message.ackIds = tmi.message_id;
                    //message.content = tmi.text;
                    //message.contentType = RecordModel.CONTENT_TYPE_TEXT;
//                    Log.d(TAG, "message from " + message.senderUserId + " text: " + message.content);
                    Log.d("textMessage", "message from " + message.senderUserId + " text: " + message.content);

                } else if (message.messageType == MessageType.AckText.getType() && unpacker.getCountRemain() > 0) {

                    message.ackIds = unpacker.readString();
                    //message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d("textMessage", "message from " + message.senderUserId + " ackIds: " + message.ackIds);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            unpacker.readArrayEnd();
            if (message.messageType == MessageType.DeliveredMessage.getType()) {
            } else if (message.messageType == MessageType.ReadMessage.getType()) {
            } else {
            }
            return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] createConnectMessage(int sendId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);
        try {
            packer.writeArrayBegin(3);
            packer.write(ChannelType.GroupType.getType());
            packer.write(MessageType.Connection.getType());
            packer.write(sendId);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public byte[] createAckReceiveMessage(int senderId, int receivedId, int channelType, String ackIds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.DeliveredMessage.getType());
            packer.write(senderId);
            packer.write(receivedId);

            packer.write(ackIds);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public byte[] createStartRecordMessage(int senderId, int receiverId, int channelType, long duration) {
        String event = "SendStartMessage";
        long timestamp = System.currentTimeMillis();
        String description = "" + senderId + "_" + receiverId + "_" + channelType + "_" + duration;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.StartTalking.getType());
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(duration);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public byte[] createAudioMessage(int senderId, int receiverId, int channelType, byte[] payload, int length
                                     ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);
        try {

            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.Audio.getType());
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(payload, 0, length);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public byte[] createStopRecordMessage(int senderId, int receiverId, int channelType) {
        String event = "SendStopMessage";
        long timestamp = System.currentTimeMillis();
        String description = "" + senderId + "_" + receiverId + "_" + channelType;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);

        try {
            packer.writeArrayBegin(4);
            packer.write(channelType);
            packer.write(MessageType.StopTalking.getType());
            packer.write(senderId);
            packer.write(receiverId);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public byte[] updateUserStatus(int senderId, int status) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(ChannelType.GroupType.getType());
            packer.write(MessageType.Status.getType());
            packer.write(senderId);
            packer.write(0);
            packer.write(status);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public Message getRecordMessage(int id, int channelType) {
        Message message = new Message();
        message.channelType = channelType;
        //message.senderUserId = UserDataHelper.getUser().getId();
        message.senderUserId = 1;
        message.receiveId = id;
        message.receiveChannelId = id;
        return message;
    }

    public byte[] createReadMessage(int sendTo, int type, String id) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(type);
            packer.write(MessageType.ReadMessage.getType());
            //packer.write(UserDataHelper.getUserId());
            packer.write(1);
            packer.write(sendTo);

            packer.write(id);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static String getSendingIdInJsonArrayFormat(String id){
        List<String> ids=new ArrayList<>();
        ids.add(id);
        return new Gson().toJson(ids);
    }

    public byte[] createTextMessageForSending(int channelType, int fromID, int toID, String message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgPack.createPacker(out);

        try {
            packer.writeArrayBegin(5);

            packer.write(channelType);
            packer.write(MessageType.Text.getType());
            packer.write(fromID);
            packer.write(toID);
            packer.write(message);

            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
}
