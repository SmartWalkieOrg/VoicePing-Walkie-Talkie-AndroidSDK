package com.smartwalkie.voiceping;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button talkButton;

    public byte[] buffer;
    public static DatagramSocket socket;
    private int port=8089;

    AudioRecord recorder;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = Math.max(AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat), 960*2);
    private boolean status = false;

    private final View.OnClickListener talkButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (status == false) {
                startStreaming();
                status = true;
            } else {
                recorder.release();
                status = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        talkButton = (Button) findViewById(R.id.talk_button);
        talkButton.setOnClickListener(talkButtonListener);
    }

    private void attemptToTalk() {
        Log.v(TAG, "attempToTalk");
    }

    public void startStreaming() {
        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    System.out.println("Socket Created");

                    byte[] buffer = new byte[minBufSize];
                    System.out.println("Buffer created of size " + minBufSize);

                    DatagramPacket packet;
                    final InetAddress destination = InetAddress.getByName("218.000.000.000");
                    System.out.println("Address retrieved");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channelConfig, audioFormat, minBufSize);
                    System.out.println("Recorder initialized");
                    recorder.startRecording();

                    while(status == true) {
                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);
                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer, buffer.length, destination, port);
                        //socket.send(packet);
                        System.out.println("MinBufferSize: " + minBufSize);
                    }
                } catch(UnknownHostException e) {
                    System.out.println("UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("IOException");
                }
            }
        });
        streamThread.start();
    }
}
