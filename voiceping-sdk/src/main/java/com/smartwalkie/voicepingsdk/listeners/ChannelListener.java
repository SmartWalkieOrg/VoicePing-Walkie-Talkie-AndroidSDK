package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Created by kukuhsain on 8/7/17.
 */

public interface ChannelListener {

    void onSubscribed(String channelId, int channelType);

    void onTalkStarted(AudioRecorder audioRecorder);

    void onTalkReceived(AudioPlayer audioPlayer);

    void onUnsubscribed(String channelId, int channelType);

    void onError(PingException e);
}
