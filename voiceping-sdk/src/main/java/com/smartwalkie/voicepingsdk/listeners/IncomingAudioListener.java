package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.models.Message;

/**
 * Created by sirius on 7/11/17.
 */

public interface IncomingAudioListener {
    public void onStartTalkingMessage(Message message);
    public void onAudioTalkingMessage(Message message);
    public void onStopTalkingMessage(Message message);
}
