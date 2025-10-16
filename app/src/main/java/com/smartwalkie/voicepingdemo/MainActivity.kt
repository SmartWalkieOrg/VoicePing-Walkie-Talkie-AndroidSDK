package com.smartwalkie.voicepingdemo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import com.smartwalkie.voicepingsdk.model.ChannelType
import android.os.Bundle
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import android.content.Intent
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.smartwalkie.voicepingdemo.databinding.ActivityMainBinding
import com.smartwalkie.voicepingsdk.ConnectionState
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.VoicePingButton
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.exception.ErrorCode
import com.smartwalkie.voicepingsdk.listener.*
import com.smartwalkie.voicepingsdk.model.Channel
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    ConnectionStateListener, IncomingTalkListener {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    private var mDestinationPath: String? = null
    private var mToast: Toast? = null
    private var channelType = ChannelType.GROUP
    private var disconnectConfirmationDialog: DisconnectConfirmationDialog? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = MyPrefs.userId ?: ""
        val company = MyPrefs.company ?: ""
        val serverUrl = MyPrefs.serverUrl ?: ""
        if (userId.isBlank() || company.isBlank() || serverUrl.isBlank()) {
            finish()
            return
        }
        initToolbar(userId, company)
        binding.textServerUrl.text = serverUrl
        val channelTypes = arrayOf("GROUP CALL", "PRIVATE CALL")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, channelTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerChannelType.adapter = adapter
        binding.spinnerChannelType.onItemSelectedListener = this
        binding.buttonJoin.setOnClickListener { joinGroup() }
        binding.buttonLeave.setOnClickListener { leaveGroup() }
        binding.layoutGroupButtons.visibility = View.VISIBLE
        binding.buttonMute.setOnClickListener { muteChannel() }
        binding.buttonUnmute.setOnClickListener { unmuteChannel() }
        binding.layoutIncomingTalk.visibility = View.GONE
        binding.editReceiverId.addTextChangedListener {
            val receiverId = it.toString()
            binding.voicePingButton.receiverId = receiverId
            binding.voicePingButton.setButtonEnabled(receiverId.isNotBlank())
        }
        binding.voicePingButton.listener = object : VoicePingButton.Listener {
            override fun onStarted() {
                log("VoicePingButton, PTT onStarted")
            }

            override fun onStopped() {
                log("VoicePingButton, PTT onStopped")
            }

            override fun onError(errorMessage: String) {
                log("VoicePingButton, PTT error: $errorMessage")
                val receiverId = binding.editReceiverId.text.toString().trim { it <= ' ' }
                if (receiverId.isEmpty()) {
                    binding.editReceiverId.error = getString(R.string.cannot_be_blank)
                }
            }
        }
        VoicePing.setIncomingTalkListener(this)
        binding.voicePingButton.channelType = ChannelType.PRIVATE
        binding.voicePingButton.setButtonEnabled(false)
        updateConnectionState(VoicePing.getConnectionState())
        VoicePing.setConnectionStateListener(this)
        if (VoicePing.getConnectionState() == ConnectionState.DISCONNECTED) {
            VoicePing.connect(serverUrl, userId, company, object : ConnectCallback {
                override fun onConnected() {
                    // Ignored
                }

                override fun onFailed(exception: VoicePingException) {
                    // Ignored
                }
            })
        }
    }

    private fun initToolbar(userId: String, company: String) {
        supportActionBar?.title = "User ID: $userId"
        supportActionBar?.subtitle = "Company: $company"
    }

    private fun updateConnectionState(connectionState: ConnectionState) {
        log("updateConnectionState: ${connectionState.name}")
        binding.textConnectionState.text = connectionState.name
        val colorResId = when (connectionState) {
            ConnectionState.DISCONNECTED -> R.color.red
            ConnectionState.CONNECTING -> R.color.yellow
            ConnectionState.CONNECTED -> R.color.green
        }
        binding.textConnectionState.setTextColor(ContextCompat.getColor(this, colorResId))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open_player -> startActivity(
                PlayerActivity.generateIntent(this, mDestinationPath)
            )
            R.id.action_disconnect -> showDisconnectConfirmationDialog()
        }
        return true
    }

    // OnItemSelectedListener
    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (parent !== binding.spinnerChannelType) return
        when (position) {
            0 -> {
                binding.textReceiverIdLabel.text = "Group ID"
                binding.editReceiverId.setText("${MyPrefs.company}")
                channelType = ChannelType.GROUP
                binding.layoutGroupButtons.visibility = View.VISIBLE
                binding.voicePingButton.channelType = ChannelType.GROUP
            }
            1 -> {
                binding.textReceiverIdLabel.text = "Target User ID"
                channelType = ChannelType.PRIVATE
                binding.editReceiverId.setText("@${MyPrefs.company}")
                binding.layoutGroupButtons.visibility = View.GONE
                binding.voicePingButton.channelType = ChannelType.PRIVATE
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    // ConnectionStateListener
    override fun onConnectionStateChanged(connectionState: ConnectionState) {
        runOnUiThread {
            updateConnectionState(connectionState)
        }
    }

    override fun onConnectionError(e: VoicePingException) {
        runOnUiThread {
            if (e.errorCode == ErrorCode.DUPLICATE_CONNECT) {
                if (!isFinishing) {
                    VoicePing.unmuteAll()
                    MyPrefs.clear()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            } else {
                showToast(e.message)
            }
        }
    }

    // IncomingTalkListener
    override fun onIncomingTalkStarted(
        audioReceiver: AudioReceiver,
        activeChannels: List<Channel>
    ) {
        log("onIncomingTalkStarted, channel: ${audioReceiver.channel.toString()}, session id: ${audioReceiver.audioSessionId}")
        // Audio processing
        Utils.enhanceLoudnessIfPossible(audioReceiver.audioSessionId, 300)
        Utils.boostBassIfPossible(audioReceiver.audioSessionId, 100.toShort())
        runOnUiThread {
            val channelType = audioReceiver.channel?.type ?: -1
            binding.layoutIncomingTalk.visibility = View.VISIBLE
            binding.textIncomingChannelType.text = ChannelType.getText(channelType)
            binding.textIncomingSenderId.text = audioReceiver.channel?.pureSenderId
        }
//        mDestinationPath = getExternalFilesDir(null).toString() + "/incoming_ptt_audio.opus"
//        audioReceiver.saveToLocal(mDestinationPath)
        audioReceiver.setInterceptorAfterDecoded(object : AudioInterceptor {
            override fun proceed(data: ByteArray, channel: Channel): ByteArray {
                val sb = ByteBuffer.wrap(data).asShortBuffer()
                val dataShortArray = ShortArray(sb.limit())
                sb[dataShortArray]
                val amplitude = Utils.getRmsAmplitude(dataShortArray)
                runOnUiThread {
                    binding.progressIncomingTalk.progress = amplitude.toInt() - 7000
                }
                return data
            }
        })
    }

    override fun onIncomingTalkStopped(
        audioMetaData: AudioMetaData,
        activeChannels: List<Channel>
    ) {
        log("onIncomingTalkStopped, channel: ${audioMetaData.channel.toString()}, download url: ${audioMetaData.downloadUrl}, active channels count: ${activeChannels.size}")
        if (activeChannels.isEmpty()) {
            runOnUiThread {
                binding.layoutIncomingTalk.visibility = View.GONE
            }
        }
    }

    override fun onIncomingTalkError(e: VoicePingException) {
        e.printStackTrace()
        runOnUiThread {
            binding.layoutIncomingTalk.visibility = View.GONE
        }
    }

    private fun joinGroup() {
        val groupId = binding.editReceiverId.text.toString().trim { it <= ' ' }
        if (groupId.isBlank()) {
            binding.editReceiverId.error = getString(R.string.cannot_be_blank)
            binding.editReceiverId.requestFocus()
            return
        }
        log("joinGroup, group ID: $groupId")
        VoicePing.joinGroup(groupId)
        showToast("Joined to $groupId")
        Utils.closeKeyboard(this, currentFocus)
    }

    private fun leaveGroup() {
        val groupId = binding.editReceiverId.text.toString().trim { it <= ' ' }
        if (groupId.isBlank()) {
            binding.editReceiverId.error = getString(R.string.cannot_be_blank)
            binding.editReceiverId.requestFocus()
            return
        }
        log("leaveGroup, group ID: $groupId")
        VoicePing.leaveGroup(groupId)
        showToast("Left from $groupId")
        Utils.closeKeyboard(this, currentFocus)
    }

    private fun muteChannel() {
        val receiverId = binding.editReceiverId.text.toString().trim { it <= ' ' }
        if (receiverId.isBlank()) return
        log("muteChannel, target ID: $receiverId, channel type: ${ChannelType.getText(channelType)}")
        VoicePing.mute(receiverId, channelType)
        showToast("Channel $receiverId muted")
    }

    private fun unmuteChannel() {
        val receiverId = binding.editReceiverId.text.toString().trim { it <= ' ' }
        if (receiverId.isBlank()) return
        log("unmuteChannel, target ID: $receiverId, channel type: ${ChannelType.getText(channelType)}")
        VoicePing.unmute(receiverId, channelType)
        showToast("Channel $receiverId unmuted")
    }

    private fun showDisconnectConfirmationDialog() {
        if (disconnectConfirmationDialog == null) {
            disconnectConfirmationDialog =
                DisconnectConfirmationDialog(this, object : DisconnectConfirmationDialog.Listener {
                    override fun onDisconnected() {
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                })
        }
        disconnectConfirmationDialog?.show()
    }

    private fun showToast(message: String?) {
        runOnUiThread {
            mToast?.cancel()
            mToast = Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT)
            mToast?.show()
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}