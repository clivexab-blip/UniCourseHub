package com.unicoursehub.app.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unicoursehub.app.util.NotificationHelper

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Device Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationHelper = NotificationHelper(applicationContext)

        // 1. Check for Data Payload (Logic-Driven)
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: "general"
            val title = data["title"] ?: "UniCourseHub"
            val message = data["message"] ?: ""

            when (type) {
                "message" -> notificationHelper.showMessageNotification(title, message)
                "download" -> notificationHelper.showDownloadNotification(title, message, 100)
                "compression" -> notificationHelper.showCompressionNotification(title, message, 100)
                else -> notificationHelper.showGeneralNotification(title, message)
            }
            return
        }

        // 2. Handle standard Notification Payload
        remoteMessage.notification?.let {
            val title = it.title ?: "UniCourseHub"
            val body = it.body ?: ""
            notificationHelper.showGeneralNotification(title, body)
        }
    }
}
