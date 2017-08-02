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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

import com.smartwalkie.voicepingsdk.VoicePing;
import com.smartwalkie.voicepingsdk.events.DisconnectEvent;
import com.smartwalkie.voicepingsdk.models.ChannelType;
import com.smartwalkie.voicepingsdk.models.Session;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] CHANNEL_TYPES = { "PRIVATE", "GROUP" };

    private AutoCompleteTextView receiverIdText;
    private Button talkButton;
    private Spinner channelTypeSpinner;
    private TextInputLayout channelInputLayout;
    private int channelType = ChannelType.PRIVATE;

    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    int receiverId = 0;
                    try {
                        receiverId = Integer.parseInt(receiverIdText.getText().toString());
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }

                    if (receiverId <= 0) {
                        receiverIdText.setError(getString(R.string.error_invalid_user_id));
                        receiverIdText.requestFocus();
                        break;
                    }

                    talkButton.setText("RELEASE TO STOP");
                    talkButton.setBackgroundColor(Color.YELLOW);
                    //VoicePing.startTalking(64, ChannelType.PRIVATE);
                    VoicePing.startTalking(receiverId, channelType);
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

        channelInputLayout = (TextInputLayout) findViewById(R.id.channel_input_layout);
        channelTypeSpinner = (Spinner) findViewById(R.id.channel_type_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, CHANNEL_TYPES);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelTypeSpinner.setAdapter(adapter);
        channelTypeSpinner.setOnItemSelectedListener(this);

        receiverIdText = (AutoCompleteTextView) findViewById(R.id.receiver_id_text);

        talkButton = (Button) findViewById(R.id.talk_button);
        talkButton.setOnTouchListener(touchListener);

        setTitle("User ID: " + Session.getInstance().getUserId());
        talkButton.setText("START TALKING");
        talkButton.setBackgroundColor(Color.GREEN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
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
                                VoicePing.disconnect();
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

    public void onEvent(DisconnectEvent event) {
        Log.v(TAG, "onDisconnectEvent...");
        if (!isFinishing()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent != channelTypeSpinner) return;
        switch (position) {
            case 0:
                channelInputLayout.setHint("Receiver ID");
                receiverIdText.setHint("Receiver ID");
                channelType = ChannelType.PRIVATE;
                break;
            case 1:
                channelInputLayout.setHint("Group ID");
                receiverIdText.setHint("Group ID");
                channelType = ChannelType.GROUP;
                break;
            default:
                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    // OnItemSelectedListener
}
