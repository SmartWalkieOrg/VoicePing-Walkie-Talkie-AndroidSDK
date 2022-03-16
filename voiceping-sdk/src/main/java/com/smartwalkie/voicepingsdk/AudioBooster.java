package com.smartwalkie.voicepingsdk;

public class AudioBooster {

    public static byte[] boost(double decibel, byte[] recBuffer, int reallySampledBytes) {
        int recBufferBytePtr = 0;
        int i = 0;
        while (i < reallySampledBytes) {
            float sample = (float) (recBuffer[recBufferBytePtr + i] & 0xFF
                    | recBuffer[recBufferBytePtr + i + 1] << 8);

            // THIS is the point were the work is done:
            // Increase level by about 6dB:
//            sample *= 2;
            // Or increase level by 20dB:
//             sample *= 10;
            // Or if you prefer any dB value, then calculate the gain factor outside the loop
            // float gainFactor = (float)Math.pow( 10., dB / 20. );    // dB to gain factor
            // sample *= gainFactor;

            float gainFactor = (float) Math.pow(10., decibel / 20.);    // dB to gain factor
            sample *= gainFactor;

            // Avoid 16-bit-integer overflow when writing back the manipulated data:
            if (sample >= 32767f) {
                recBuffer[recBufferBytePtr + i] = (byte) 0xFF;
                recBuffer[recBufferBytePtr + i + 1] = 0x7F;
            } else if (sample <= -32768f) {
                recBuffer[recBufferBytePtr + i] = 0x00;
                recBuffer[recBufferBytePtr + i + 1] = (byte) 0x80;
            } else {
                int s = (int) (0.5f + sample);  // Here, dithering would be more appropriate
                recBuffer[recBufferBytePtr + i] = (byte) (s & 0xFF);
                recBuffer[recBufferBytePtr + i + 1] = (byte) (s >> 8 & 0xFF);
            }
            i += 2;
        }
        return recBuffer;
    }
}
