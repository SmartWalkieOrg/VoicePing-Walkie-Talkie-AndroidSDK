package com.smartwalkie.voicepingsdk.callbacks;

import com.smartwalkie.voicepingsdk.exceptions.PingException;

/**
 * Created by kukuhsain on 8/2/17.
 */

public interface DisconnectCallback {

    void onDisconnected();

    void onFailed(PingException exception);
}
