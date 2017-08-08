package com.smartwalkie.voicepingsdk.listeners;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Created by kukuhsain on 8/7/17.
 */

public interface ChannelListener {

    void onSubscribed();

    void onTalkStarted(byte[] data);

    void onTalkReceived(byte[] data);

    void onUnsubscribed();

    void onError(PingException e);
}
