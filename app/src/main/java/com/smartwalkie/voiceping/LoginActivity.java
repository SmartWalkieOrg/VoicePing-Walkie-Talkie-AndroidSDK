package com.smartwalkie.voiceping;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.smartwalkie.voicepingsdk.VoicePing;
import com.smartwalkie.voicepingsdk.callbacks.ConnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.PingException;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class LoginActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks {

    public static final String TAG = LoginActivity.class.getSimpleName();

    private final int RC_RECORD_AUDIO = 1000;

    private AutoCompleteTextView userIdText;
    private EditText serverAddressText;
    private View progressView;
    private View connectFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userIdText = (AutoCompleteTextView) findViewById(R.id.user_id_text);

        serverAddressText = (EditText) findViewById(R.id.server_address_text);
        serverAddressText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.connect || id == EditorInfo.IME_NULL) {
                    attemptToLogin();
                    return true;
                }
                return false;
            }
        });

        Button connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyPermissions.requestPermissions(LoginActivity.this,
                        "This app needs your permission to allow recording audio",
                        RC_RECORD_AUDIO,
                        Manifest.permission.RECORD_AUDIO);
            }
        });

        connectFormView = findViewById(R.id.connect_form);
        progressView = findViewById(R.id.connect_progress);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_RECORD_AUDIO) attemptToLogin();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "You need to allow the permission request!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Attempts to connect using details specified by the form.
     * If there are form errors, the errors are presented and no actual login attempt is made.
     */
    private void attemptToLogin() {
        // Reset errors.
        userIdText.setError(null);
        serverAddressText.setError(null);

        // Store values at the time of the connect attempt.
        int userId = 0;
        try {
            userId = Integer.parseInt(userIdText.getText().toString());
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }

        String serverAddress = serverAddressText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid server address, if the user entered one.
        if (TextUtils.isEmpty(serverAddress)) {
            serverAddressText.setError(getString(R.string.error_invalid_server_address));
            focusView = serverAddressText;
            cancel = true;
        }

        // Check for a valid username.
        if (userId <= 0) {
            userIdText.setError(getString(R.string.error_invalid_user_id));
            focusView = userIdText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt connect and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user connect attempt.
            showProgress(true);

            VoicePing.configure(VoicePingClient.getInstance(), serverAddress);
            /*
            Map<String, String> props = new HashMap<>();
            props.put("user_id", "63");
            props.put("VoicePingToken", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjoiOWQ1MmJhMDAtYzFkOC0xMWU2LWEwMGYtNmI2NTNhMTlkM2VlIiwidWlkIjo2MywidXNlcm5hbWUiOiJzaXJwaW5nIiwiY2hhbm5lbElkcyI6WzIsNF19.2ubViVWK-In_30TLUSEGlfxj773Vi4TgYRu4iRCNFQc");
            props.put("user_id", "64");
            props.put("VoicePingToken", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjoiN2E0N2UyNTAtZjhiMS0xMWU2LTlkM2MtZjlhODE4ZWEzNTI3IiwidWlkIjo2NCwidXNlcm5hbWUiOiJzaXJwaW5nMSIsImNoYW5uZWxJZHMiOlszXX0.WoibsCj-t0aC-ZfnyYENm4W4koa6VlEDDRqz5qNnx0E");
            */
            VoicePing.connect(userId, new ConnectCallback() {
                @Override
                public void onConnected() {
                    Log.v(TAG, "onConnected");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                }

                @Override
                public void onFailed(PingException exception) {
                    Log.v(TAG, "onFailed");
                }
            });
        }
    }

    private void showProgress(boolean show) {
        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        connectFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
