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
                if (messageType == MessageType.START_TALKING) {
                    if (incomingMessages.containsKey(key)) {
                        Message oldMessage = incomingMessages.remove(key); // lacks stop talking but a new one has been received
                    }
                    message = new Message();
                    incomingMessages.put(key, message);
                } else if (messageType == MessageType.AUDIO) {
                    if (!incomingMessages.containsKey(key)) {
                        message = new Message();
                        incomingMessages.put(key, message); // lacks start talking, let's just proceed
                    } else {
                        message = incomingMessages.get(key);
                    }
                } else if (messageType == MessageType.STOP_TALKING) {
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

                if (message.messageType == MessageType.START_TALKING && unpacker.getCountRemain() > 0) {
                    message.duration = unpacker.readLong();
                } else if (message.messageType == MessageType.AUDIO) {
                    message.payload = unpacker.readByteArray();
                    message.addData(message.payload);
                } else  if (message.messageType == MessageType.OFFLINE_MESSAGE) {
                    message.offlineMessage = unpacker.readString();
                } else if (message.messageType == MessageType.STOP_TALKING && unpacker.getCountRemain() > 0) {
                    message.ackIds = unpacker.readString();
                } else if (message.messageType == MessageType.MESSAGE_DELIVERED && unpacker.getCountRemain() > 0) {
                    message.ackIds = unpacker.readString();
                } else if( message.messageType == MessageType.ACK_END && unpacker.getCountRemain()>0){
                    message.ackIds = unpacker.readString();
                } else if(message.messageType == MessageType.MESSAGE_READ && unpacker.getCountRemain()>0){
                    message.ackIds = unpacker.readString();
                } else if(message.messageType == MessageType.STATUS && unpacker.getCountRemain() > 0) {
                    message.status = unpacker.readInt();
                    Log.d(TAG, "message from " + message.senderUserId + " user status " + message.status);
                } else if (message.messageType == MessageType.TEXT && unpacker.getCountRemain() > 0) {
/*
                    String jsonTextMessageIdentification = unpacker.readString();
                    TextMessageIdentification tmi = new Gson().fromJson(jsonTextMessageIdentification, TextMessageIdentification.class);
                    message.ackIds = tmi.message_id;
                    message.content = tmi.text;
                    message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "message from " + message.senderUserId + " text: " + message.content);
*/
                    Log.d(TAG, "textMessage from " + message.senderUserId + " text: " + message.content);
                } else if (message.messageType == MessageType.ACK_TEXT && unpacker.getCountRemain() > 0) {
                    message.ackIds = unpacker.readString();
                    //message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "textMessage from " + message.senderUserId + " ackIds: " + message.ackIds);
                }

                unpacker.readArrayEnd();
                stream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] createConnectMessage(int sendId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);
        try {
            packer.writeArrayBegin(3);
            packer.write(ChannelType.GROUP);
            packer.write(MessageType.CONNECTION);
            packer.write(sendId);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] createAckReceiveMessage(int senderId, int receiverId, int channelType, String ackIds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.MESSAGE_DELIVERED);
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.START_TALKING);
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(duration);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] createAudioMessage(int senderId, int receiverId, int channelType, byte[] payload, int length
                                     ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);
        try {

            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.AUDIO);
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(payload, 0, length);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] createStopRecordMessage(int senderId, int receiverId, int channelType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(4);
            packer.write(channelType);
            packer.write(MessageType.STOP_TALKING);
            packer.write(senderId);
            packer.write(receiverId);
            packer.writeArrayEnd(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] updateUserStatus(int senderId, int status) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(ChannelType.GROUP);
            packer.write(MessageType.STATUS);
            packer.write(senderId);
            packer.write(0);
            packer.write(status);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public static Message getRecordMessage(int id, int channelType) {
        Message message = new Message();
        message.channelType = channelType;
        //message.senderUserId = UserDataHelper.getUser().getId();
        message.senderUserId = 1;
        message.receiverUserId = id;
        message.receiverChannelId = id;
        return message;
    }

    public static byte[] createReadMessage(int sendTo, int type, String id) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(type);
            packer.write(MessageType.MESSAGE_READ);
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

    public static byte[] createTextMessageForSending(int channelType, int fromID, int toID, String message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);

            packer.write(channelType);
            packer.write(MessageType.TEXT);
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
