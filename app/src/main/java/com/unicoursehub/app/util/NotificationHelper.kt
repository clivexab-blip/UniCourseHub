package com.unicoursehub.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unicoursehub.app.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads_channel"
        const val CHANNEL_COMPRESSION = "compression_channel"
        const val CHANNEL_GENERAL = "general_channel"
        const val CHANNEL_MESSAGES = "messages_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                context.getString(R.string.channel_downloads_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_downloads_desc)
            }

            val compressionChannel = NotificationChannel(
                CHANNEL_COMPRESSION,
                context.getString(R.string.channel_compression_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_compression_desc)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                context.getString(R.string.channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(downloadChannel)
            manager.createNotificationChannel(compressionChannel)
            manager.createNotificationChannel(generalChannel)
            manager.createNotificationChannel(messageChannel)
        }
    }

    fun showGeneralNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun showMessageNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun showDownloadNotification(title: String, message: String, progress: Int = -1): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(progress in 0..99)
            .setOnlyAlertOnce(true)

        if (progress in 0..100) {
            builder.setProgress(100, progress, progress == 0)
        } else {
            builder.setProgress(0, 0, false)
        }

        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(1, notification)
        return notification
    }

    fun showCompressionNotification(title: String, message: String, progress: Int = -1): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_COMPRESSION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(progress in 0..99)
            .setOnlyAlertOnce(true)

        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
        }

        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(2, notification)
        return notification
    }

    fun createCompressionNotification(title: String, message: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_COMPRESSION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .build()
    }
}
