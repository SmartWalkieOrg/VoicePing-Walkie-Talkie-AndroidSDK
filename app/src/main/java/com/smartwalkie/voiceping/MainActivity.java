package com.smartwalkie.voiceping;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;
import com.smartwalkie.voicepingsdk.listeners.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listeners.AudioPlayer;
import com.smartwalkie.voicepingsdk.listeners.AudioRecorder;
import com.smartwalkie.voicepingsdk.listeners.ChannelListener;
import com.smartwalkie.voicepingsdk.models.ChannelType;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        ChannelListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] CHANNEL_TYPES = { "PRIVATE", "GROUP" };

    private EditText receiverIdText;
    private Button talkButton;
    private Spinner channelTypeSpinner;
    private TextInputLayout channelInputLayout;
    private LinearLayout llAmplitude;
    private ProgressBar pbAmplitude;
    private TextView tvAmplitude;
    private int channelType = ChannelType.PRIVATE;

    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    String receiverId = receiverIdText.getText().toString().trim();

                    if (receiverId == null || receiverId.isEmpty()) {
                        receiverIdText.setError(getString(R.string.error_invalid_user_id));
                        receiverIdText.requestFocus();
                        break;
                    }

                    talkButton.setText("RELEASE TO STOP");
                    talkButton.setBackgroundColor(Color.YELLOW);
                    //VoicePing.startTalking(64, ChannelType.PRIVATE);
                    VoicePingClientApp.getVoicePing().startTalking(receiverId, channelType);
                    break;
                case MotionEvent.ACTION_UP:
                    talkButton.setText("START TALKING");
                    talkButton.setBackgroundColor(Color.GREEN);
                    VoicePingClientApp.getVoicePing().stopTalking();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    talkButton.setText("START TALKING");
                    talkButton.setBackgroundColor(Color.GREEN);
                    VoicePingClientApp.getVoicePing().stopTalking();
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelInputLayout = (TextInputLayout) findViewById(R.id.channel_input_layout);
        channelTypeSpinner = (Spinner) findViewById(R.id.channel_type_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, CHANNEL_TYPES);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelTypeSpinner.setAdapter(adapter);
        channelTypeSpinner.setOnItemSelectedListener(this);

        receiverIdText = (EditText) findViewById(R.id.receiver_id_text);

        talkButton = (Button) findViewById(R.id.talk_button);
        talkButton.setOnTouchListener(touchListener);

        llAmplitude = (LinearLayout) findViewById(R.id.ll_amplitude);
        pbAmplitude = (ProgressBar) findViewById(R.id.pb_amplitude);
        tvAmplitude = (TextView) findViewById(R.id.tv_amplitude);

        llAmplitude.setVisibility(View.GONE);

        String userId = getIntent().getStringExtra("user_id");
        if (userId != null) setTitle("User ID: " + userId);
        talkButton.setText("START TALKING");
        talkButton.setBackgroundColor(Color.GREEN);

        VoicePingClientApp.getVoicePing().setChannelListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.text_button_disconnect)
                        .setMessage("Are you sure you want to disconnect?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                VoicePingClientApp.getVoicePing()
                                        .disconnect(new DisconnectCallback() {
                                    @Override
                                    public void onDisconnected() {
                                        Log.v(TAG, "onDisconnected...");
                                        if (!isFinishing()) {
                                            startActivity(new Intent(MainActivity.this,
                                                    LoginActivity.class));
                                            finish();
                                            Toast.makeText(MainActivity.this, "Disconnected!",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            default:
                break;
        }
        return true;
    }

    // OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent != channelTypeSpinner) return;
        switch (position) {
            case 0:
                receiverIdText.setHint("Receiver ID");
                channelType = ChannelType.PRIVATE;
                break;
            case 1:
                receiverIdText.setHint("Group ID");
                channelType = ChannelType.GROUP;
                String receiverId = receiverIdText.getText().toString().trim();
                VoicePingClientApp.getVoicePing().subscribe(receiverId, ChannelType.GROUP);
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    // ChannelListener
    @Override
    public void onSubscribed(String channelId, int channelType) {

    }

    @Override
    public void onTalkStarted(AudioRecorder audioRecorder) {
        audioRecorder.addAudioInterceptor(new AudioInterceptor() {
            @Override
            public byte[] proceed(byte[] data) {
                ShortBuffer sb = ByteBuffer.wrap(data).asShortBuffer();
                short[] dataShortArray = new short[sb.limit()];
                sb.get(dataShortArray);
                int max = Math.abs(dataShortArray[0]);
                for (short singleShort : dataShortArray) {
                    if (max < singleShort) max = Math.abs(singleShort);
                }
                final int amplitude = max;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        llAmplitude.setVisibility(View.VISIBLE);
                        pbAmplitude.setProgress(amplitude);
                        tvAmplitude.setText("" + amplitude);
                    }
                });
                return data;
            }
        });
    }

    @Override
    public void onTalkStopped() {
        llAmplitude.setVisibility(View.GONE);
    }

    @Override
    public void onTalkReceived(AudioPlayer audioPlayer) {

    }

    @Override
    public void onUnsubscribed(String channelId, int channelType) {

    }

    @Override
    public void onError(PingException e) {
        e.printStackTrace();
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
