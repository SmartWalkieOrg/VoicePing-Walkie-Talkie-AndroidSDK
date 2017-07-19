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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MessageHelper {

    public static final String TAG = MessageHelper.class.getSimpleName();
    private static final MessagePack messagePack = new MessagePack();
    private static final Map<String, Message> incomingMessages = new HashMap<>();

    public static Message unpackMessage(byte[] payload) {
        ByteArrayInputStream stream = new ByteArrayInputStream(payload);
        MessagePackUnpacker unpacker = (MessagePackUnpacker) messagePack.createUnpacker(stream);

        try {
            unpacker.readArrayBegin();
            int channelType = unpacker.readInt();
            int messageType = unpacker.readInt();
            int senderUserId = unpacker.readInt();
            int receiveChannelId = unpacker.readInt();

            Message message = null;
            try {
                String key = String.format("%d_%d_%d", channelType, receiveChannelId, senderUserId);
                if (messageType == MessageType.StartTalking.getType()) {
                    if (incomingMessages.containsKey(key)) {
                        Message oldMessage = incomingMessages.remove(key); // lacks stop talking but a new one has been received
                    }
                    message = new Message();
                    incomingMessages.put(key, message);
                } else if (messageType == MessageType.Audio.getType()) {
                    if (!incomingMessages.containsKey(key)) {
                        message = new Message();
                        incomingMessages.put(key, message); // lacks start talking, let's just proceed
                    } else {
                        message = incomingMessages.get(key);
                    }
                } else if (messageType == MessageType.StopTalking.getType()) {
                    if (!incomingMessages.containsKey(key)) {
                        // lacks start talking, can we just ignore it?
                    } else {
                        message = incomingMessages.remove(key); // remove from list to be processed for last time
                    }
                } else {
                    message = new Message();
                }

                message.channelType = channelType;
                message.messageType = messageType;
                message.senderUserId = senderUserId;
                message.receiverChannelId = receiveChannelId;

                if (message.messageType == MessageType.StartTalking.getType() && unpacker.getCountRemain() > 0) {
                    message.duration = unpacker.readLong();
                } else if (message.messageType == MessageType.Audio.getType()) {
                    message.payload = unpacker.readByteArray();
                    message.addData(message.payload);
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
/*
                    String jsonTextMessageIdentification = unpacker.readString();
                    TextMessageIdentification tmi = new Gson().fromJson(jsonTextMessageIdentification, TextMessageIdentification.class);
                    message.ackIds = tmi.message_id;
                    message.content = tmi.text;
                    message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "message from " + message.senderUserId + " text: " + message.content);
*/
                    Log.d(TAG, "textMessage from " + message.senderUserId + " text: " + message.content);
                } else if (message.messageType == MessageType.AckText.getType() && unpacker.getCountRemain() > 0) {
                    message.ackIds = unpacker.readString();
                    //message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "textMessage from " + message.senderUserId + " ackIds: " + message.ackIds);
                }

                unpacker.readArrayEnd();
                stream.close();
                if (message.messageType == MessageType.DeliveredMessage.getType()) {
                } else if (message.messageType == MessageType.ReadMessage.getType()) {
                } else {
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] createConnectMessage(int sendId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);
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

    public byte[] createAckReceiveMessage(int senderId, int receiverId, int channelType, String ackIds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.DeliveredMessage.getType());
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(ackIds);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] createStartRecordMessage(int senderId, int receiverId, int channelType, long duration) {
        String event = "SendStartMessage";
        long timestamp = System.currentTimeMillis();
        String description = "" + senderId + "_" + receiverId + "_" + channelType + "_" + duration;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

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
        Packer packer = messagePack.createPacker(out);
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
        Packer packer = messagePack.createPacker(out);

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
        Packer packer = messagePack.createPacker(out);

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
        message.receiverUserId = id;
        message.receiverChannelId = id;
        return message;
    }

    public byte[] createReadMessage(int sendTo, int type, String id) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

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
        Packer packer = messagePack.createPacker(out);

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
