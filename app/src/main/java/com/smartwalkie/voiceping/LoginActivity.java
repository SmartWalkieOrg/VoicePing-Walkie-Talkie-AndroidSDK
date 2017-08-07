package com.smartwalkie.voiceping;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
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

    private EditText userIdText;
    private View progressView;
    private View connectFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        userIdText = (EditText) findViewById(R.id.user_id_text);

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

        // Store values at the time of the connect attempt.
        int userId;
        try {
            userId = Integer.parseInt(userIdText.getText().toString());
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            userIdText.setError(getString(R.string.error_invalid_user_id));
            userIdText.requestFocus();
            return;
        }

        // Check for a valid username.
        if (userId <= 0) {
            userIdText.setError(getString(R.string.error_invalid_user_id));
            userIdText.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user connect attempt.
            showProgress(true);
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
