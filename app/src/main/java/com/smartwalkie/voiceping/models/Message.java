package com.smartwalkie.voiceping.models;

import android.util.Log;


public class Message {

    public static final int ERROR_TYPE_NO_ERROR  = 0;
    public static final int ERROR_TYPE_MISSED_START_TALKING  = 1;
    public static final int ERROR_TYPE_MISSED_STOP_TALKING  = 2;

    public int channelType, messageType, senderUserId, receiveChannelId, receiveId;
    public byte[] payload;

    public boolean finished;
    public boolean isRead;
    public boolean fromHistory;
    public boolean playNext;
    public long starttime;
    public long duration;
    public String fileName;
    public long timeStamp;
    public long startPlayTime;
    public long lastReceivingTime;
    public String offlineMessage;
    public String ackIds;
    public int contentType;
    public String content;
    public String format;

    public boolean needToPlaySubsequentMessage;
    public boolean isLastItemInList;
    public boolean isMutedText;

    public int status;

    /**
     * Handle the partial message, when start talking or stop talking signal can be missed
     * The value can be one of
     * {@link Message#ERROR_TYPE_NO_ERROR},
     * {@link Message#ERROR_TYPE_MISSED_START_TALKING} or
     * {@link Message#ERROR_TYPE_MISSED_STOP_TALKING}
     */
    public int errorType;

    public Message() {
        errorType = ERROR_TYPE_NO_ERROR;
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
        if (receiveChannelId != message.receiveChannelId)       return false;
        if (senderUserId != message.senderUserId)               return false;
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
        result = 31 * result + senderUserId;
        result = 31 * result + receiveChannelId;
        return result;
    }

    @Override
    public String toString() {
        return "{" +
            "senderUserId=" + senderUserId +
            ", receiveChannelId=" + receiveChannelId +
            ", receiveId=" + receiveId +
            ", finished=" + finished +
            ", fromHistory=" + fromHistory +
            ", starttime=" + starttime/1000 +
            ", duration=" + duration +
            ", contentType=" + contentType +
            ", content=" + content +
            ", errorType=" + errorType +
            '}';
    }

    public String toStringWithType() {
        return "{" +
            "messageType=" + messageType +
            ", senderUserId=" + senderUserId +
            ", receiveChannelId=" + receiveChannelId +
            ", receiveId=" + receiveId +
            ", finished=" + finished +
            ", fromHistory=" + fromHistory +
            ", starttime=" + starttime/1000 +
            ", duration=" + duration +
            ", contentType=" + contentType +
            ", content=" + content +
            ", channelType=" + channelType +
            ", errorType=" + errorType +
            '}';
    }

    /**
     * Get target id to compare
     * @return {@link Message#senderUserId} if {@link Message#channelType} == {@link ChannelType#GroupType} <p>
     *     {@link Message#receiveChannelId} if {@link Message#channelType} == {@link ChannelType#PrivateType}
     */
    public int getTargetId() {
        if (channelType == ChannelType.GroupType.getType()) {
            return receiveChannelId;
        } else {
            return senderUserId;
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
