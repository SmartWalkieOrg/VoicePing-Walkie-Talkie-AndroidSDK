package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.models.Message;

/**
 * Created by sirius on 7/11/17.
 */

public interface IncomingAudioListener {

    void onStartTalkingMessage(Message message);

    void onAudioTalkingMessage(Message message);

    void onStopTalkingMessage(Message message);
}
