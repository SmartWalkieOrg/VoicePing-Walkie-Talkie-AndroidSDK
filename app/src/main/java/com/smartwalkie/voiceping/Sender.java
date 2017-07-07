package com.smartwalkie.voiceping;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.smartwalkie.voiceping.models.ChannelType;
import com.smartwalkie.voiceping.models.MessageType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


public class Sender {
    private static final String TAG = Sender.class.getSimpleName();
    private HandlerThread senderThread;
    Handler senderHandler;
    LinkedBlockingQueue<byte[]> blockingQueue;
    //List<byte[]> blockingQueue;
    long startTime;

    private List<String> waitingDeLiveriedList;
    int type;
    int senderId;
    int receiverId;
    int channelType;
    int messageCount;
    String saveLocalId;
    public static int step;
    String ackId;
    Recorder recorder;

    private static final byte[] END_COMMAND = new byte[]{(byte) 0xFF, (byte) 0XFF, (byte) 0xFF, (byte) 0XFF};

    public static final int TYPE_AUDIO_CALL = 1;
    public static final int TYPE_OFFLINE_MESSAGE = 2;

    private static final int WHAT_MESSAGE_START = 1000;
    private static final int WHAT_MESSAGE_AUDIO = 2000;
    private static final int WHAT_MESSAGE_STOP = 3000;
    private static final int WHAT_WAITING_FOR_ACK_END = 4000;
    private static final int WHAT_WAITING_FOR_ACK_START = 5000;
    private static final int WHAT_WAITING_FOR_DELIVERED = 6000;

    public static final int INITIAL = 1; // public for access in VoiceFrag
    private static final int ON_ACK_START = 3;
    private static final int ACK_END = 5;
    private static final int WAITING_ACK_START = 6;
    private static final int WAITING_ACK_END = 7;

    public static final int ACK_START_FAIL = -1; // public for access in VoiceFrag

    public static final int WAITING_FOR_ACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    public Sender() {
        waitingDeLiveriedList = new ArrayList<>();
        init();
    }

    private void init() {
        blockingQueue = new LinkedBlockingQueue<>();
        senderThread = new HandlerThread("Sender", Thread.MAX_PRIORITY);
        senderThread.start();
        senderHandler = new Handler(senderThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case WHAT_MESSAGE_START:
                        step = WAITING_ACK_START;
                        sendStartMessage();
                        break;
                    case WHAT_MESSAGE_STOP:
                        if(step == ON_ACK_START) {
                            senderHandler.sendEmptyMessageDelayed(WHAT_WAITING_FOR_ACK_END, WAITING_FOR_ACK_TIMEOUT_IN_MILLIS);
                            blockingQueue.clear();
                            step = WAITING_ACK_END;
                            sendStopMessage();
                        } else if (step == ACK_START_FAIL) {
                            onSendFail("ack_fail",true);
                            blockingQueue.clear();
                        } else {
                            senderHandler.sendEmptyMessageDelayed(WHAT_WAITING_FOR_ACK_START, WAITING_FOR_ACK_TIMEOUT_IN_MILLIS);
                            step = WAITING_ACK_START;
                        }
                        break;
                    case WHAT_MESSAGE_AUDIO:
                        while (step == ON_ACK_START) {
                            try {
                                if (blockingQueue.isEmpty()) {
                                    continue;
                                }
                                byte[] data = blockingQueue.take();
                                if (data.length == END_COMMAND.length && data == END_COMMAND) {
                                    if(type == TYPE_AUDIO_CALL && saveLocalId == null){
                                        saveLocalId = saveCurrentMessage();
                                    }
                                    senderHandler.sendEmptyMessage(WHAT_MESSAGE_STOP);
                                    break;
                                }
                                sendAudioMessage(data);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                Log.e(TAG, " testing take error");
                            }
                        }
                        break;
                    case WHAT_WAITING_FOR_ACK_START:
                        blockingQueue.clear();
                        onSendFail("not receive ackStart", true);
                        step = INITIAL;
                        break;
                    case WHAT_WAITING_FOR_ACK_END:
                        blockingQueue.clear();
                        onSendFail("not receive ackEnd",true);
                        step = INITIAL;
                        break;
                    case WHAT_WAITING_FOR_DELIVERED:
                        String ackID = msg.getData().getString("ackId");
                        if(ackID!=null && waitingDeLiveriedList.contains(ackID)){
                            waitingDeLiveriedList.remove(ackID);
                        }
                        break;
                }
            }
        };
        recorder = new Recorder();
        step = INITIAL;
    }


    public void startSending(int senderId, int receiverId, int channelType, int type, boolean onlyUsePhoneMic) {
        if (step != INITIAL && step != ACK_END && step != ACK_START_FAIL) {
            // when recording, waiting for ackstart or ack end. close it and mark as unsent
            terminateCurrentSending();
        }
        senderHandler.removeCallbacksAndMessages(null);
        saveLocalId =null;
        if(type == TYPE_AUDIO_CALL){
            if (recorder.isRecording()) {
                recorder.stopRecording();
            }
            recorder.startRecording();
        }
        messageCount = 0;
        startTime = System.currentTimeMillis();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.channelType = channelType;
        this.type = type;
        this.blockingQueue.clear();
        step = INITIAL;
        senderHandler.sendEmptyMessage(WHAT_MESSAGE_START);
        saveLocalId = null;

    }


    public void startSending(int senderId, int receiverId, int channelType, int type) {
        startSending(senderId, receiverId, channelType, type, false);
    }

    public void onAck(int receiverId, int channelType) {
        senderHandler.removeMessages(WHAT_WAITING_FOR_ACK_START);
        if (receiverId != this.receiverId || channelType != this.channelType) return;
        if (step == INITIAL || step == WAITING_ACK_START) {
            long delayTime = System.currentTimeMillis() - startTime;
            step = ON_ACK_START;
            senderHandler.sendEmptyMessage(WHAT_MESSAGE_AUDIO);
        }
    }

    public boolean isSending() {
        return step == ON_ACK_START;
    }


    public void stopSending(boolean isSaved) {
        try {
            blockingQueue.put(END_COMMAND);
            if (type == TYPE_AUDIO_CALL) {
                recorder.stopRecording();
            }

            if (step == WAITING_ACK_START) {
                if (isSaved && saveLocalId == null) {
                    saveLocalId = saveCurrentMessage(); // do not save message for ACK_START_FAIL
                }
                senderHandler.sendEmptyMessage(WHAT_MESSAGE_STOP);
            } else if (step == ACK_START_FAIL) {
            } else {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void putData(byte[] data, int length) {
        try {
            //conversation.putData(data);
            blockingQueue.put(Arrays.copyOfRange(data, 0, length));
        } catch (InterruptedException e) {
        }
    }


    public void putOfflineData(byte[] payload, String id) {
        if (payload == null || payload.length == 0) {
            return;
        }
        this.saveLocalId = id;
        ByteBuffer buf = ByteBuffer.wrap(payload);
        byte[] bytes;
        while (buf.remaining() >= AudioParams.ENCODER_SIZE) {
            bytes = new byte[AudioParams.ENCODER_SIZE];
            buf.get(bytes);
            try {
                blockingQueue.put(bytes);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //LogUtils.VPLog.getInstance().logDefault(TAG, " testing buf remaining " + buf.remaining());
        }
        try {
            blockingQueue.put(END_COMMAND);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (recorder != null) {
            //recorder.destroy();
            recorder = null;
        }
        senderThread.quit();
        senderThread = null;
        senderHandler = null;
//        onMessageSendListener = null;
    }

    public void onAckFail() {
        step = ACK_START_FAIL;
    }

    public void onAckEnd(String ackId) {
        this.ackId = ackId;
        if (step == WAITING_ACK_END) {

            step = ACK_END;

            senderHandler.removeMessages(WHAT_WAITING_FOR_ACK_END);
            if(isSaveToS3Server(ackId)) {
                onSendSuccess();
            } else {
                Message message =new Message();
                message.what = WHAT_WAITING_FOR_DELIVERED;
                Bundle bundle = new Bundle();
                bundle.putString("ackId", ackId);
                message.setData(bundle);
                waitingDeLiveriedList.add(ackId);
                senderHandler.sendMessageDelayed(message, WAITING_FOR_ACK_TIMEOUT_IN_MILLIS);
            }

        } else {
        }
    }

    public void terminateCurrentSending() {

        int oldStep = step;
        step = INITIAL;
        blockingQueue.clear();

        if (oldStep == WAITING_ACK_END) {

            senderHandler.removeMessages(WHAT_WAITING_FOR_ACK_END);
            onSendFail("terminate not receive ackEnd", false);

        } else if (oldStep == WAITING_ACK_START) {

            senderHandler.removeMessages(WHAT_WAITING_FOR_ACK_START);
            onSendFail("terminate not receive ackStart", false);
            if(recorder.isRecording()){
                recorder.stopRecording();
            }
            sendStopMessage();

        } else if(oldStep == ON_ACK_START){

            senderHandler.removeMessages(WHAT_WAITING_FOR_ACK_START);
            onSendFail("terminate at ON_ACK_START", false);
            if(recorder.isRecording()){
                recorder.stopRecording();
            }
            sendStopMessage();

        } else if (oldStep == ACK_START_FAIL) {

            onSendFail("terminate ack fail", false);

        }

    }

    private void sendStartMessage() {
        long timeStamp = System.currentTimeMillis();
        if (type == TYPE_OFFLINE_MESSAGE) {
            senderHandler.sendEmptyMessageDelayed(WHAT_WAITING_FOR_ACK_START, WAITING_FOR_ACK_TIMEOUT_IN_MILLIS);
        }
    }

    public void sendStopMessage() {


    }

    public void sendAudioMessage(byte[] data) {
        if(data == null || data.length == 0)return;

        messageCount++;

    }

    public String saveCurrentMessage() {

        return null;
    }

    public void onSendSuccess() {

    }

    private void onSendFail(String reason,boolean isPlaySound) {

    }


    private boolean isSaveToS3Server(String ackId){
        if(channelType == ChannelType.GroupType.getType()){
            return true;
        } else {
            String format = ChannelType.PrivateType.getType()+"_"+ MessageType.Audio.getType()+"_";
            if(ackId.startsWith(format) && !ackId.contains(".opus")){
                return false;
            } else {
                return true;
            }
        }
    }

    public void onDeliveredMessage(String deliveryId) {
        if (waitingDeLiveriedList.contains(deliveryId)) {
            waitingDeLiveriedList.remove(deliveryId);

        }
    }

    public String getStepName(int step) {
        switch (step) {
            case INITIAL: return "INITIAL";
            case ON_ACK_START: return "ON_ACK_START";
            case ACK_END: return "ACK_END";
            case WAITING_ACK_START: return "WAITING_ACK_START";
            case WAITING_ACK_END: return "WAITING_ACK_END";
            case ACK_START_FAIL: return "ACK_START_FAIL";

            default: return "UNKNOWN";
        }
    }

}
