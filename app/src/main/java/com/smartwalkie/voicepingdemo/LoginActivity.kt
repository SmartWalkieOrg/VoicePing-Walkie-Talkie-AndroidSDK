package com.smartwalkie.voicepingdemo

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import android.os.Bundle
import pub.devrel.easypermissions.EasyPermissions
import android.widget.Toast
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import android.util.Log
import android.view.View
import com.smartwalkie.voicepingdemo.databinding.ActivityLoginBinding
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.exception.VoicePingException

class LoginActivity : AppCompatActivity(), PermissionCallbacks {
    private val TAG = "LoginActivity"
    private val RC_RECORD_AUDIO = 1000
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.app_name)
        binding.editServerUrl.setText(MyPrefs.serverUrl ?: "")
        binding.buttonConnect.setOnClickListener {
            EasyPermissions.requestPermissions(
                this@LoginActivity,
                "This app needs your permission to allow recording audio",
                RC_RECORD_AUDIO,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val userId = MyPrefs.userId ?: ""
        val company = MyPrefs.company ?: ""
        val serverUrl = MyPrefs.serverUrl ?: ""
        if (userId.isNotBlank() && company.isNotBlank() && serverUrl.isNotBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == RC_RECORD_AUDIO) attemptToLogin()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, "You need to allow the permission request!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Attempts to connect using details specified by the form.
     * If there are form errors, the errors are presented and no actual login attempt is made.
     */
    private fun attemptToLogin() {
        // Reset errors.
        binding.editUserId.error = null
        binding.editCompany.error = null
        binding.editServerUrl.error = null

        // Store values at the time of the connect attempt.
        val userId = binding.editUserId.text.toString().trim { it <= ' ' }
        val company = binding.editCompany.text.toString().trim { it <= ' ' }
        val serverUrl = binding.editServerUrl.text.toString().trim { it <= ' ' }

        when {
            userId.isEmpty() -> {
                binding.editUserId.error = getString(R.string.cannot_be_blank)
                binding.editUserId.requestFocus()
            }
            company.isEmpty() -> {
                binding.editCompany.error = getString(R.string.cannot_be_blank)
                binding.editCompany.requestFocus()
            }
            serverUrl.isEmpty() -> {
                binding.editServerUrl.error = getString(R.string.cannot_be_blank)
                binding.editServerUrl.requestFocus()
            }
            else -> {
                // Show a progress spinner, and kick off a background task to perform the user connect attempt.
                Utils.closeKeyboard(this, currentFocus)
                showProgress(true)
                VoicePing.connect(serverUrl, userId, company, object : ConnectCallback {
                    override fun onConnected() {
                        Log.v(TAG, "onConnected")
                        showProgress(false)
                        MyPrefs.userId = userId
                        MyPrefs.company = company
                        MyPrefs.serverUrl = serverUrl
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }

                    override fun onFailed(exception: VoicePingException) {
                        Log.v(TAG, "onFailed")
                        showProgress(false)
                        Toast.makeText(
                            this@LoginActivity,
                            R.string.failed_to_sign_in,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }

    private fun showProgress(show: Boolean) {
        binding.progressConnect.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutConnect.visibility = if (show) View.GONE else View.VISIBLE
    }
}