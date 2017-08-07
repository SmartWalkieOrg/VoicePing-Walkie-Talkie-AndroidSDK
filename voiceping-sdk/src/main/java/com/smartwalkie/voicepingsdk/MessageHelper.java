package com.smartwalkie.voicepingsdk;

import android.util.Log;

import com.smartwalkie.voicepingsdk.models.ChannelType;
import com.smartwalkie.voicepingsdk.models.Message;
import com.smartwalkie.voicepingsdk.models.MessageType;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MessageHelper {

    public static final String TAG = MessageHelper.class.getSimpleName();
    private static final MessagePack messagePack = new MessagePack();
    private static final Map<String, Message> incomingMessages = new HashMap<>();
    private static final Map<String, Message> outgoingMessages = new HashMap<>();

    public static Message unpackMessage(byte[] payload) {
        ByteArrayInputStream stream = new ByteArrayInputStream(payload);
        MessagePackUnpacker unpacker = (MessagePackUnpacker) messagePack.createUnpacker(stream);

        try {
            unpacker.readArrayBegin();
            int channelType = unpacker.readInt();
            int messageType = unpacker.readInt();
            String senderId = unpacker.readString();
            String receiverId = unpacker.readString();

            Message message = null;
            try {
                String key = String.format("%d_%s_%s", channelType, receiverId, senderId);
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

                message.setChannelType(channelType);
                message.setMessageType(messageType);
                message.setSenderId(senderId);
                message.setReceiverId(receiverId);

                if (message.getMessageType() == MessageType.START_TALKING && unpacker.getCountRemain() > 0) {
                    message.setDuration(unpacker.readLong());
                } else if (message.getMessageType() == MessageType.AUDIO) {
                    message.setPayload(unpacker.readByteArray());
                    message.addData(message.getPayload());
                } else  if (message.getMessageType() == MessageType.OFFLINE_MESSAGE) {
                    message.setOfflineMessage(unpacker.readString());
                } else if (message.getMessageType() == MessageType.STOP_TALKING && unpacker.getCountRemain() > 0) {
                    message.setAckIds(unpacker.readString());
                } else if (message.getMessageType() == MessageType.MESSAGE_DELIVERED && unpacker.getCountRemain() > 0) {
                    message.setAckIds(unpacker.readString());
                } else if( message.getMessageType() == MessageType.ACK_END && unpacker.getCountRemain()>0){
                    message.setAckIds(unpacker.readString());
                } else if(message.getMessageType() == MessageType.MESSAGE_READ && unpacker.getCountRemain()>0){
                    message.setAckIds(unpacker.readString());
                } else if(message.getMessageType() == MessageType.STATUS && unpacker.getCountRemain() > 0) {
                    message.setAckIds(unpacker.readString());
                    Log.d(TAG, "message from " + message.getSenderId() + " user status " + message.getStatus());
                } else if (message.getMessageType() == MessageType.TEXT && unpacker.getCountRemain() > 0) {
/*
                    String jsonTextMessageIdentification = unpacker.readString();
                    TextMessageIdentification tmi = new Gson().fromJson(jsonTextMessageIdentification, TextMessageIdentification.class);
                    message.ackIds = tmi.message_id;
                    message.content = tmi.text;
                    message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "message from " + message.senderId + " text: " + message.content);
*/
                    Log.d(TAG, "textMessage from " + message.getSenderId() + " text: " + message.getContent());
                } else if (message.getMessageType() == MessageType.ACK_TEXT && unpacker.getCountRemain() > 0) {
                    message.setAckIds(unpacker.readString());
                    //message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "textMessage from " + message.getSenderId() + " ackIds: " + message.getAckIds());
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

    public static byte[] createConnectionMessage(String senderId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);
        try {
            packer.writeArrayBegin(3);
            packer.write(ChannelType.GROUP);
            packer.write(MessageType.CONNECTION);
            packer.write(senderId);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static Message createAckStartMessage(String senderId, String receiverId, int channelType, long duration) {
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

            String key = String.format("%d_%s_%s", channelType, receiverId, senderId);
            if (incomingMessages.containsKey(key)) {
                Message oldMessage = incomingMessages.remove(key); // there is an incomplete outgoing message to the same user/group
            }

            Message message = new Message();
            message.setChannelType(channelType);
            message.setMessageType(MessageType.START_TALKING);
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setDuration(duration);
            message.setPayload(out.toByteArray());

            outgoingMessages.put(key, message);

            return message;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message createAudioMessage(String senderId, String receiverId, int channelType, byte[] payload, int length
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

            String key = String.format("%d_%s_%s", channelType, receiverId, senderId);

            Message message;

            if (!incomingMessages.containsKey(key)) {
                message = new Message();    // let's just create a new one for now.
                incomingMessages.put(key, message);
            } else {
                message = incomingMessages.get(key);
            }

            message.setChannelType(channelType);
            message.setMessageType(MessageType.AUDIO);
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.addData(payload);
            message.setPayload(out.toByteArray());

            return message;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] createAckStopMessage(String senderId, String receiverId, int channelType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = messagePack.createPacker(out);

        try {
            packer.writeArrayBegin(4);
            packer.write(channelType);
            packer.write(MessageType.STOP_TALKING);
            packer.write(senderId);
            packer.write(receiverId);
            packer.writeArrayEnd(true);

            String key = String.format("%d_%s_%s", channelType, receiverId, senderId);
            Message message;

            if (!incomingMessages.containsKey(key)) {
                message = new Message();    // let's just create a new one for now.
                incomingMessages.put(key, message);
            } else {
                message = incomingMessages.get(key);
            }

            message.setChannelType(channelType);
            message.setMessageType(MessageType.STOP_TALKING);
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setPayload(out.toByteArray());

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

    public static byte[] createAckReadMessage(int sendTo, int type, String id) {
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

    public static byte[] createTextMessage(int channelType, int fromID, int toID, String message) {
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

    public static byte[] createStatusMessage(int senderId, int status) {
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

}
