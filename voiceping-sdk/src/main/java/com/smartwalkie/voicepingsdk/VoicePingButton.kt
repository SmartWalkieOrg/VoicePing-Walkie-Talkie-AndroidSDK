package com.smartwalkie.voicepingsdk

import android.annotation.SuppressLint
import android.content.Context
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.smartwalkie.voicepingsdk.exception.ErrorCode
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.listener.AudioRecorder
import com.smartwalkie.voicepingsdk.listener.OutgoingTalkCallback
import com.smartwalkie.voicepingsdk.model.ChannelType

class VoicePingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OutgoingTalkCallback {

    private val TAG = "VoicePingButton"
    private val layoutVpButton: FrameLayout
    private val imageVpButton: ImageView
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toast: Toast? = null

    var listener: Listener? = null
    var receiverId: String? = null
    var channelType: Int = 0
    private var buttonEnabled: Boolean = true

    init {
        inflate(context, R.layout.view_voice_ping_button, this)
        layoutVpButton = findViewById(R.id.layout_vp_button)
        imageVpButton = findViewById(R.id.image_vp_button)
    }

    fun setButtonEnabled(enabled: Boolean) {
        buttonEnabled = enabled
        val resId = if (enabled) R.drawable.bg_rounded_primary else R.drawable.bg_rounded_grey
        layoutVpButton.setBackgroundResource(resId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!buttonEnabled) return super.onTouchEvent(event)
        val eventAction = event?.action ?: return super.onTouchEvent(event)
        if (eventAction != MotionEvent.ACTION_DOWN && eventAction != MotionEvent.ACTION_UP) {
            return super.onTouchEvent(event)
        }
        return proceedTouchAction(eventAction)
    }

    private fun proceedTouchAction(eventAction: Int): Boolean {
        val targetId = receiverId ?: ""
        val isValid = targetId.isNotBlank() && ChannelType.isValid(channelType)
        when (eventAction) {
            MotionEvent.ACTION_DOWN -> {
                // PTT button pressed
                if (isValid) {
                    layoutVpButton.setBackgroundResource(R.drawable.bg_rounded_yellow)
                    VoicePing.startTalking(targetId, channelType, this)
                    listener?.onStarted()
                } else {
                    layoutVpButton.setBackgroundResource(R.drawable.bg_rounded_grey)
                    listener?.onError("Invalid receiverId or channelType")
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                // PTT button released
                layoutVpButton.setBackgroundResource(R.drawable.bg_rounded_primary)
                if (isValid) {
                    VoicePing.stopTalking()
                    listener?.onStopped()
                }
                return true
            }
        }
        return false
    }

    override fun onOutgoingTalkStarted(audioRecorder: AudioRecorder) {
        var nsEnabled = false
        var aecEnabled = false
        var agcEnabled = false
        if (NoiseSuppressor.isAvailable()) {
            val noiseSuppressor = NoiseSuppressor.create(audioRecorder.audioSessionId)
            if (noiseSuppressor != null) {
                noiseSuppressor.enabled = true
                nsEnabled = true
            }
        }
        if (AcousticEchoCanceler.isAvailable()) {
            val echoCanceler = AcousticEchoCanceler.create(audioRecorder.audioSessionId)
            if (echoCanceler != null) {
                echoCanceler.enabled = true
                aecEnabled = true
            }
        }
        if (AutomaticGainControl.isAvailable()) {
            val automaticGainControl = AutomaticGainControl.create(audioRecorder.audioSessionId)
            if (automaticGainControl != null) {
                automaticGainControl.enabled = true
                agcEnabled = true
            }
        }
        log("onOutgoingTalkStarted, NoiseSuppressor enabled: $nsEnabled, AcousticEchoCanceler enabled: $aecEnabled, AutomaticGainControl enabled: $agcEnabled")
    }

    override fun onOutgoingTalkStopped(isTooShort: Boolean, isTooLong: Boolean) {
        log("onOutgoingTalkStopped, isTooShort: $isTooShort, isTooLong: $isTooLong")
        if (isTooShort) {
            showToast("Press and hold the button to send PTT. Release after you are done.")
        }
    }

    override fun onDownloadUrlReceived(downloadUrl: String) {
        log("onDownloadUrlReceived, URL: $downloadUrl")
    }

    override fun onOutgoingTalkError(e: VoicePingException) {
        log("onOutgoingTalkError, code: ${e.errorCode}, message: ${e.message}")
        val errorMessage = if (e.errorCode == ErrorCode.UNAUTHORIZED_GROUP) {
            "Please join the group first before sending PTT"
        } else {
            e.message
        }
        showToast(errorMessage)
    }

    private fun showToast(message: String?) {
        if (message.isNullOrBlank()) return
        mainHandler.post {
            toast?.cancel()
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            toast?.show()
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    interface Listener {
        fun onStarted()
        fun onStopped()
        fun onError(errorMessage: String)
    }
}