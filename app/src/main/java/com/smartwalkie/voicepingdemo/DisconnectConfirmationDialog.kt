package com.smartwalkie.voicepingdemo

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.callback.DisconnectCallback

class DisconnectConfirmationDialog(activity: Activity, listener: Listener) {
    private val dialog: AlertDialog = AlertDialog.Builder(activity)
        .setTitle(R.string.sign_out)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(R.string.sign_out_confirmation)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            VoicePing.disconnect(object : DisconnectCallback {
                override fun onDisconnected() {
                    if (!activity.isFinishing) {
                        VoicePing.unmuteAll()
                        MyPrefs.clear()
                        listener.onDisconnected()
                    }
                }
            })
        }
        .setNegativeButton(android.R.string.cancel) { _, _ ->
            // do nothing
        }
        .create()

    fun show() {
        dialog.show()
    }

    interface Listener {
        fun onDisconnected()
    }
}