package com.smartwalkie.voicepingdemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Reference implementation of the foreground service that host apps must run while connected
 * to VoicePing.
 *
 * Starting with Android 17 (API 37), audio playback and audio focus requests from an app that
 * is not visible are blocked unless the app has a foreground service with while-in-use
 * capabilities running. Without a service like this, incoming PTT audio is silently muted as
 * soon as the app goes to the background.
 *
 * The service declares two foreground service types:
 * - mediaPlayback: allows incoming PTT audio to keep playing in the background.
 * - microphone: allows outgoing PTT recording to keep working in the background. This type is
 *   only used when RECORD_AUDIO is granted, and (since Android 14) the service must be started
 *   while the app is visible.
 */
class PttForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), foregroundServiceTypes())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun foregroundServiceTypes(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        val hasRecordAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasRecordAudioPermission) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        return types
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Walkie Talkie",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps push-to-talk running while the app is in the background"
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText("Connected. Push-to-talk is active.")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    companion object {
        private const val CHANNEL_ID = "ptt_foreground_service"
        private const val NOTIFICATION_ID = 1

        /**
         * Must be called while the app is visible, otherwise starting a microphone-type
         * foreground service is not allowed on Android 14+.
         */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PttForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PttForegroundService::class.java))
        }
    }
}
