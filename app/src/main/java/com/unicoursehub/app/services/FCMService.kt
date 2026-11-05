package com.unicoursehub.app.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.util.NotificationHelper
import com.unicoursehub.app.util.SessionManager

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Device Token: $token")
        
        val session = SessionManager(applicationContext)
        if (session.isLoggedIn()) {
            val db = DatabaseHelper.getInstance(applicationContext)
            db.updateFcmToken(session.getUserId(), token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationHelper = NotificationHelper(applicationContext)
        val session = SessionManager(applicationContext)
        val db = DatabaseHelper.getInstance(applicationContext)

        // 1. Check for Data Payload (Logic-Driven)
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: "general"
            val title = data["title"] ?: "UniCourseHub"
            val message = data["message"] ?: ""

            if (session.isLoggedIn()) {
                db.addNotification(session.getUserId(), title, message)
            }

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
            
            if (session.isLoggedIn()) {
                db.addNotification(session.getUserId(), title, body)
            }
            
            notificationHelper.showGeneralNotification(title, body)
        }
    }
}
