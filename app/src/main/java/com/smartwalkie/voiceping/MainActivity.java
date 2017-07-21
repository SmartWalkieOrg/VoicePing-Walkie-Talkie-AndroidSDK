package com.smartwalkie.voiceping;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.smartwalkie.voiceping.events.DisconnectEvent;
import com.smartwalkie.voiceping.models.ChannelType;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button talkButton;
    private Button disconnectButton;

    private final View.OnClickListener disconnectListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Connection connection = Connection.getInstance();
            connection.disconnect();
        }
    };

    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    talkButton.setText("RELEASE TO STOP");
                    talkButton.setBackgroundColor(Color.YELLOW);
                    VoicePing.startTalking(8168, ChannelType.PRIVATE);
                    break;
                case MotionEvent.ACTION_UP:
                    talkButton.setText("START TALKING");
                    talkButton.setBackgroundColor(Color.GREEN);
                    VoicePing.stopTalking();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    talkButton.setText("START TALKING");
                    talkButton.setBackgroundColor(Color.GREEN);
                    VoicePing.stopTalking();
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        talkButton = (Button) findViewById(R.id.talk_button);
        talkButton.setOnTouchListener(touchListener);

        disconnectButton = (Button) findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(disconnectListener);
    }

    public void onEvent(DisconnectEvent event) {
        Log.v(TAG, "onEvent");
        finish();
    }
}
