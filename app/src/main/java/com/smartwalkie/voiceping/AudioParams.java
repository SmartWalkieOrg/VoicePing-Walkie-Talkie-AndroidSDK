package com.smartwalkie.voiceping;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;

public class AudioParams {

    public static final int SAMPLE_RATE             = 16000;
    public static final int FRAME_SIZE              = 960;
    public static final int ENCODER_SIZE            = 133;

    public static final boolean USE_CODEC = true;
    public static final int CHANNEL = 1;


    public static final int BUFFER_SIZE_FACTOR = 2;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int RECORD_MIN_BUFFER_SIZE = Math.max(AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT), FRAME_SIZE * BUFFER_SIZE_FACTOR);
    public static final int PLAY_MIN_BUFFER_SIZE = Math.max(AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT), FRAME_SIZE * BUFFER_SIZE_FACTOR);

}
