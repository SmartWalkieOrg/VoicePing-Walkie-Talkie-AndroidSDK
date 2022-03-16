package com.smartwalkie.voicepingsdk;

import android.media.AudioFormat;

import com.smartwalkie.voicepingsdk.model.AudioParam;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by kukuhsain on 10/2/17.
 */

class AudioConverter {

    public static void rawToWave(final File rawFile, File destinationFile, AudioParam audioParam) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        int bitsPerSample;
        if (audioParam.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
            bitsPerSample = 16;
        } else {
            bitsPerSample = 8;
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(destinationFile));
            // Write WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // sub-chunk 1 id
            writeInt(output, 16); // sub-chunk 1 size (16 for PCM)
            writeShort(output, (short) 1); // audio format (1 for PCM)
            writeShort(output, (short) audioParam.getChannelSize()); // number of channels
            writeInt(output, audioParam.getSampleRate()); // sample rate
            writeInt(output, audioParam.getSampleRate() * audioParam.getChannelSize() * bitsPerSample / 8); // byte rate
            writeShort(output, (short) (audioParam.getChannelSize() * bitsPerSample / 8)); // block align
            writeShort(output, (short) bitsPerSample); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size

            // Write WAVE audio data
            output.write(fullyReadFileToBytes(rawFile));

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        int read = fis.read(bytes, 0, size);
        if (read < size) {
            int remain = size - read;
            while (remain > 0) {
                read = fis.read(tmpBuff, 0, remain);
                System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                remain -= read;
            }
        }
        fis.close();
        return bytes;
    }

    private static void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private static void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value);
        output.write(value >> 8);
    }

    private static void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}
