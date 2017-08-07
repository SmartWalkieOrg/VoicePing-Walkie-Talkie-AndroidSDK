package com.smartwalkie.voicepingsdk.models;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Message {

    private ByteArrayOutputStream stream;

    public static final int ERROR_TYPE_NO_ERROR  = 0;
    public static final int ERROR_TYPE_MISSED_START_TALKING  = 1;
    public static final int ERROR_TYPE_MISSED_STOP_TALKING  = 2;

    private int channelType;
    private int messageType;
    private String senderId;
    private String receiverId;
    private byte[] payload;

    private boolean finished;
    private boolean isRead;
    private boolean fromHistory;
    private boolean playNext;
    private long starttime;
    private long duration;
    private String fileName;
    private long timeStamp;
    private long startPlayTime;
    private long lastReceivingTime;
    private String offlineMessage;
    private String ackIds;
    private int contentType;
    private String content;
    private String format;

    private boolean needToPlaySubsequentMessage;
    private boolean isLastItemInList;
    private boolean isMutedText;

    private int status;

    /**
     * Handle the partial message, when start talking or stop talking signal can be missed
     * The value can be one of
     * {@link Message#ERROR_TYPE_NO_ERROR},
     * {@link Message#ERROR_TYPE_MISSED_START_TALKING} or
     * {@link Message#ERROR_TYPE_MISSED_STOP_TALKING}
     */
    private int errorType;

    public Message() {
        stream = new ByteArrayOutputStream();
        errorType = ERROR_TYPE_NO_ERROR;
    }

    public boolean addData(byte[] data) {
        try {
            stream.write(data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getChannelType() {
        return channelType;
    }

    public void setChannelType(int channelType) {
        this.channelType = channelType;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isFromHistory() {
        return fromHistory;
    }

    public void setFromHistory(boolean fromHistory) {
        this.fromHistory = fromHistory;
    }

    public boolean isPlayNext() {
        return playNext;
    }

    public void setPlayNext(boolean playNext) {
        this.playNext = playNext;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getStartPlayTime() {
        return startPlayTime;
    }

    public void setStartPlayTime(long startPlayTime) {
        this.startPlayTime = startPlayTime;
    }

    public long getLastReceivingTime() {
        return lastReceivingTime;
    }

    public void setLastReceivingTime(long lastReceivingTime) {
        this.lastReceivingTime = lastReceivingTime;
    }

    public String getOfflineMessage() {
        return offlineMessage;
    }

    public void setOfflineMessage(String offlineMessage) {
        this.offlineMessage = offlineMessage;
    }

    public String getAckIds() {
        return ackIds;
    }

    public void setAckIds(String ackIds) {
        this.ackIds = ackIds;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isNeedToPlaySubsequentMessage() {
        return needToPlaySubsequentMessage;
    }

    public void setNeedToPlaySubsequentMessage(boolean needToPlaySubsequentMessage) {
        this.needToPlaySubsequentMessage = needToPlaySubsequentMessage;
    }

    public boolean isLastItemInList() {
        return isLastItemInList;
    }

    public void setLastItemInList(boolean lastItemInList) {
        isLastItemInList = lastItemInList;
    }

    public void setMutedText(boolean mutedText) {
        isMutedText = mutedText;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getErrorType() {
        return errorType;
    }

    public void setErrorType(int errorType) {
        this.errorType = errorType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;
        if (fromHistory != message.fromHistory) return false;
        if (needToPlaySubsequentMessage != message.needToPlaySubsequentMessage) return false;
        if(message.fromHistory){
            return fileName != null
                    && message.fileName != null
                    && fileName.equals(message.fileName);
        }
        if (channelType != message.channelType)                 return false;
        if (receiverId != message.receiverId)       return false;
        if (senderId != message.senderId)               return false;
        if (contentType != message.contentType)                 return false;

        //if 2 ackIds are different in case of text message
        if (ackIds != null
                && !ackIds.isEmpty()
                && message.ackIds != null
                && !message.ackIds.isEmpty()
                && !ackIds.equals(message.ackIds)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = channelType;
        result = 31 * result + senderId.length();
        result = 31 * result + receiverId.length();
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "messageType=" + messageType +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", finished=" + finished +
                ", fromHistory=" + fromHistory +
                ", starttime=" + starttime/1000 +
                ", duration=" + duration +
                ", contentType=" + contentType +
                ", content=" + content +
                ", channelType=" + channelType +
                ", errorType=" + errorType +
                ", length=" + stream.size() +
                '}';
    }

    /**
     * Get target id to compare
     * @return {@link Message#senderId} if {@link Message#channelType} == {@link ChannelType#GROUP} <p>
     *     {@link Message#receiverId} if {@link Message#channelType} == {@link ChannelType#PRIVATE}
     */
    public String getTargetId() {
        if (channelType == ChannelType.GROUP) {
            return receiverId;
        } else {
            return senderId;
        }
    }

    public long getDelayTime() {
        if(duration ==0){ //real time call
            return startPlayTime- starttime;
        } else {
            long playingTime = System.currentTimeMillis() - startPlayTime;
            long delayTime = duration - playingTime;
            Log.d("Phu","delay time = "+delayTime);
            if(delayTime>0){
                return delayTime;
            } else {
                return 0;
            }
        }
    }

    public boolean isMutedText() {
        return isMutedText;
    }

    public void setAsMutedText(boolean isMuted) {
        isMutedText = isMuted;
    }
}
