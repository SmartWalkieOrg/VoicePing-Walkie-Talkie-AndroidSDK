package com.smartwalkie.voicepingsdk;

import android.util.Log;

import com.smartwalkie.voicepingsdk.model.ChannelType;
import com.smartwalkie.voicepingsdk.model.Message;
import com.smartwalkie.voicepingsdk.model.MessageType;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.type.ValueType;
import org.msgpack.unpacker.MessagePackUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class MessageHelper {
    private static final String TAG = MessageHelper.class.getSimpleName();
    private static final MessagePack mMessagePack = new MessagePack();
//    private static final Map<String, Message> mIncomingMessages = new HashMap<>();
//    private static final Map<String, Message> mOutgoingMessages = new HashMap<>();

    public static Message unpackMessage(byte[] payload) {
        ByteArrayInputStream stream = new ByteArrayInputStream(payload);
        MessagePackUnpacker unpacker = (MessagePackUnpacker) mMessagePack.createUnpacker(stream);

        try {
            unpacker.readArrayBegin();
            int channelType = unpacker.readInt();
            int messageType = unpacker.readInt();
            String senderId = unpacker.getNextType() == ValueType.INTEGER ?
                    String.valueOf(unpacker.readInt()) : unpacker.readString();
            String receiverId = unpacker.getNextType() == ValueType.INTEGER ?
                    String.valueOf(unpacker.readInt()) : unpacker.readString();

            Message message = new Message();
            try {
                /*String key = String.format("%d_%s_%s", channelType, receiverId, senderId);
                if (messageType == MessageType.START_TALKING) {
                    if (mIncomingMessages.containsKey(key)) {
                        Message oldMessage = mIncomingMessages.remove(key); // lacks stop talking but a new one has been received
                    }
                    message = new Message();
                    mIncomingMessages.put(key, message);
                } else if (messageType == MessageType.AUDIO) {
                    if (!mIncomingMessages.containsKey(key)) {
                        message = new Message();
                        mIncomingMessages.put(key, message); // lacks start talking, let's just proceed
                    } else {
                        message = mIncomingMessages.get(key);
                    }
                } else if (messageType == MessageType.STOP_TALKING) {
                    if (!mIncomingMessages.containsKey(key)) {
                        // lacks start talking, can we just ignore it?
                        message = new Message();
                    } else {
                        message = mIncomingMessages.remove(key); // remove from list to be processed for last time
                    }
                } else {
                    message = new Message();
                }*/

                message.setChannelType(channelType);
                message.setMessageType(messageType);
                message.setSenderId(senderId);
                message.setReceiverId(receiverId);

                if (message.getMessageType() == MessageType.START_TALKING) {
                    message.setDuration(unpacker.readLong());
                } else if (message.getMessageType() == MessageType.AUDIO) {
                    message.setPayload(unpacker.readByteArray());
                    message.addData(message.getPayload());
                } else if (message.getMessageType() == MessageType.OFFLINE_MESSAGE) {
                    message.setOfflineMessage(unpacker.readString());
                } else if (message.getMessageType() == MessageType.STOP_TALKING) {
                    message.setAckIds(unpacker.readString());
                } else if (message.getMessageType() == MessageType.MESSAGE_DELIVERED) {
                    message.setAckIds(unpacker.readString());
                } else if (message.getMessageType() == MessageType.ACK_END) {
                    message.setAckIds(unpacker.readString());
                } else if (message.getMessageType() == MessageType.MESSAGE_READ) {
                    message.setAckIds(unpacker.readString());
                } else if (message.getMessageType() == MessageType.STATUS) {
                    message.setAckIds(unpacker.readString());
                    Log.d(TAG, "message from " + message.getSenderId() + " user status " + message.getStatus());
                } else if (message.getMessageType() == MessageType.TEXT) {
/*
                    String jsonTextMessageIdentification = unpacker.readString();
                    TextMessageIdentification tmi = new Gson().fromJson(jsonTextMessageIdentification, TextMessageIdentification.class);
                    message.ackIds = tmi.message_id;
                    message.content = tmi.text;
                    message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "message from " + message.senderId + " text: " + message.content);
*/
                    Log.d(TAG, "textMessage from " + message.getSenderId() + " text: " + message.getContent());
                } else if (message.getMessageType() == MessageType.ACK_TEXT) {
                    message.setAckIds(unpacker.readString());
                    //message.contentType = RecordModel.CONTENT_TYPE_TEXT;
                    Log.d(TAG, "textMessage from " + message.getSenderId() + " ackIds: " + message.getAckIds());
                } else if (message.getMessageType() == MessageType.DUPLICATE_CONNECT) {
                    message.setContent(unpacker.readString());
                    Log.d(TAG, "message type: DUPLICATED_LOGIN, content: " + message.getContent());
                }

                unpacker.readArrayEnd();
                stream.close();

            } catch (Exception e) {
//                e.printStackTrace();
            }

            return message;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    public static byte[] createConnectionMessage(String senderId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = mMessagePack.createPacker(out);
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
        Packer packer = mMessagePack.createPacker(out);

        try {
            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.START_TALKING);
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(duration);
            packer.writeArrayEnd(true);

            /*String key = String.format("%d_%s_%s", channelType, receiverId, senderId);
            if (mIncomingMessages.containsKey(key)) {
                Message oldMessage = mIncomingMessages.remove(key); // there is an incomplete outgoing message to the same user/group
            }*/

            Message message = new Message();
            message.setChannelType(channelType);
            message.setMessageType(MessageType.START_TALKING);
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setDuration(duration);
            message.setPayload(out.toByteArray());

//            mOutgoingMessages.put(key, message);

            return message;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message createAudioMessage(String senderId, String receiverId, int channelType, byte[] payload, int length) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = mMessagePack.createPacker(out);
        try {

            packer.writeArrayBegin(5);
            packer.write(channelType);
            packer.write(MessageType.AUDIO);
            packer.write(senderId);
            packer.write(receiverId);
            packer.write(payload, 0, length);
            packer.writeArrayEnd(true);

//            String key = String.format("%d_%s_%s", channelType, receiverId, senderId);

            Message message = new Message();

            /*if (!mIncomingMessages.containsKey(key)) {
                message = new Message();    // let's just create a new one for now.
                mIncomingMessages.put(key, message);
            } else {
                message = mIncomingMessages.get(key);
            }*/

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
        Packer packer = mMessagePack.createPacker(out);

        try {
            packer.writeArrayBegin(4);
            packer.write(channelType);
            packer.write(MessageType.STOP_TALKING);
            packer.write(senderId);
            packer.write(receiverId);
            packer.writeArrayEnd(true);

//            String key = String.format("%d_%s_%s", channelType, receiverId, senderId);

            Message message = new Message();

            /*if (!mIncomingMessages.containsKey(key)) {
                message = new Message();    // let's just create a new one for now.
                mIncomingMessages.put(key, message);
            } else {
                message = mIncomingMessages.get(key);
            }*/

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
        Packer packer = mMessagePack.createPacker(out);

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
        Packer packer = mMessagePack.createPacker(out);

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
        Packer packer = mMessagePack.createPacker(out);

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
        Packer packer = mMessagePack.createPacker(out);

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

    public static byte[] createSubscribeMessage(String senderId, String groupId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = mMessagePack.createPacker(out);

        try {
            packer.writeArrayBegin(4);
            packer.write(ChannelType.GROUP);
            packer.write(MessageType.CHANNEL_ADDED_USER);
            packer.write(senderId);
            packer.write(groupId);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public static byte[] createUnsubscribeMessage(String senderId, String groupId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = mMessagePack.createPacker(out);

        try {
            packer.writeArrayBegin(4);
            packer.write(ChannelType.GROUP);
            packer.write(MessageType.CHANNEL_REMOVED_USER);
            packer.write(senderId);
            packer.write(groupId);
            packer.writeArrayEnd(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

}
