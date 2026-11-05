package com.vibeload.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class VibeLoadApp : Application(), ImageLoaderFactory {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Chaquopy Python Runtime
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Initialize Notification Channels
        createNotificationChannels()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .respectCacheHeaders(false)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Ongoing Download Progress (Silent, Low Importance)
            val downloadChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(downloadChannel)

            // Channel 2: Download Complete (High Importance, Sound, Vibration, Heads-up banner)
            val completeChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_COMPLETE_ID,
                "Tải xuống hoàn tất",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nổi và chuông/rung khi tải tệp hoàn tất"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(completeChannel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "vibeload_download_channel"
        const val NOTIFICATION_CHANNEL_COMPLETE_ID = "vibeload_complete_channel"
        lateinit var instance: VibeLoadApp
            private set
    }
}
