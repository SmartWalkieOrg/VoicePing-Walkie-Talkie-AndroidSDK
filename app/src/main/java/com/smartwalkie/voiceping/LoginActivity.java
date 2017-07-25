package com.smartwalkie.voiceping;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class LoginActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks {

    public static final String TAG = LoginActivity.class.getSimpleName();

    private final int RC_RECORD_AUDIO = 1000;

    private AutoCompleteTextView usernameText;
    private EditText serverAddressText;
    private View progressView;
    private View connectFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameText = (AutoCompleteTextView) findViewById(R.id.username_text);

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
        usernameText.setError(null);
        serverAddressText.setError(null);

        // Store values at the time of the connect attempt.
        String username = usernameText.getText().toString();
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
        if (TextUtils.isEmpty(username)) {
            usernameText.setError(getString(R.string.error_field_required));
            focusView = usernameText;
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

            VoicePing.configure(VoicePingClient.getInstance(), "wss://2359media-router.voiceoverping.net");
            Map<String, String> props = new HashMap<>();
            props.put("user_id", "7708");
            props.put("VoicePingToken", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjoiZTY2OThlMjAtZmJjMy0xMWU1LTk4M2QtMjEzNWVmMzMzZGQwIiwidWlkIjo3NzA4LCJ1c2VybmFtZSI6InNpcml1c21kZWx5QGdtYWlsLmNvbSIsImNoYW5uZWxJZHMiOlsyMTg4LDM4MDFdfQ.6Myf87sz8EN5NGkJBWVm_8erPcmBO36YJyCVzX9xMEw");
            props.put("DeviceId", Settings.Secure.getString(VoicePingClient.getInstance().getContentResolver(),
                    Settings.Secure.ANDROID_ID));

            VoicePing.connect(props, new ConnectCallback() {
                @Override
                public void onConnected() {
                    Log.v(TAG, "onConnected");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent activityIntent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(activityIntent);
                            showProgress(false);
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

    /**
     * Shows the progress UI and hides the connect form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            connectFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            connectFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    connectFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            connectFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
