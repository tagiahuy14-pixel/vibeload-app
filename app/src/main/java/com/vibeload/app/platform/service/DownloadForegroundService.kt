package com.vibeload.app.platform.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vibeload.app.R
import com.vibeload.app.VibeLoadApp
import com.vibeload.app.presentation.MainActivity

class DownloadForegroundService : Service() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Đang tải tệp tin"
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        val isPaused = intent?.getBooleanExtra(EXTRA_IS_PAUSED, false) ?: false

        when (action) {
            ACTION_START -> {
                val notif = buildNotification(title, progress, isPaused)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }
            }
            ACTION_UPDATE -> {
                val notif = buildNotification(title, progress, isPaused)
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(
        title: String,
        progress: Int,
        isPaused: Boolean
    ): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = if (isPaused) "Đã tạm dừng" else "$progress%"

        return NotificationCompat.Builder(this, VibeLoadApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPaused) "Tác vụ đang tạm dừng" else "Đang tải xuống...")
            .setSubText(subText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.vibeload.app.action.START"
        const val ACTION_UPDATE = "com.vibeload.app.action.UPDATE"
        const val ACTION_STOP = "com.vibeload.app.action.STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_IS_PAUSED = "extra_is_paused"

        fun start(context: Context, title: String) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, title: String, percent: Int, speed: Float, isPaused: Boolean) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, percent)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            context.startService(intent)
        }

        fun updateStatus(context: Context, statusText: String) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, statusText)
            }
            context.startService(intent)
        }

        fun notifyComplete(context: Context, title: String, isSuccess: Boolean) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = NotificationCompat.Builder(context, VibeLoadApp.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(if (isSuccess) "Tải thành công!" else "Tải thất bại!")
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID + 1, notif)
            stop(context)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
