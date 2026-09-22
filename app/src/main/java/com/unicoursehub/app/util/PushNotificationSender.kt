package com.unicoursehub.app.util

import android.content.Context
import android.util.Log
import com.unicoursehub.app.BuildConfig
import com.unicoursehub.app.data.DatabaseHelper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Utility to send REAL Cloud Push Notifications directly from the app.
 * This makes the app work with Firebase Cloud while keeping your SQLite database.
 */
object PushNotificationSender {

    private val client = OkHttpClient()
    
    // NOTE: For a real production app, you should use a server or Cloud Functions.
    // For this educational project, we use the FCM Legacy API to make it "Work with Cloud" easily.
    private val FCM_LEGACY_SERVER_KEY = BuildConfig.FCM_SERVER_KEY
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    fun sendStatusNotification(context: Context, targetUserId: Long, title: String, message: String) {
        val db = DatabaseHelper.getInstance(context)
        val user = db.getUserById(targetUserId)
        
        // 1. Show locally for instant feedback
        triggerFCMStyleNotification(context, mapOf("type" to "general", "title" to title, "message" to message))

        // 2. SEND TO CLOUD: Target the specific user's phone via their token
        user?.fcmToken?.let { token ->
            if (token.isNotEmpty()) {
                sendCloudPush(token, title, message, "general")
            }
        }
    }

    fun sendMessageNotification(context: Context, targetUserId: Long, senderName: String, text: String) {
        val db = DatabaseHelper.getInstance(context)
        val user = db.getUserById(targetUserId)
        val title = "Message from $senderName"

        // 1. Show locally for instant feedback
        triggerFCMStyleNotification(context, mapOf("type" to "message", "title" to title, "message" to text))
        
        // 2. SEND TO CLOUD: Push to the other user's device
        user?.fcmToken?.let { token ->
            if (token.isNotEmpty()) {
                sendCloudPush(token, title, text, "message")
            }
        }
    }

    private fun sendCloudPush(token: String, title: String, message: String, type: String) {
        if (FCM_LEGACY_SERVER_KEY == "YOUR_FCM_SERVER_KEY_HERE") {
            Log.e("FCM", "Cloud Push failed: You must add your FCM Server Key to PushNotificationSender.kt")
            return
        }

        val json = JSONObject().apply {
            put("to", token)
            put("data", JSONObject().apply {
                put("type", type)
                put("title", title)
                put("message", message)
            })
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(FCM_URL)
            .post(body)
            .addHeader("Authorization", "key=$FCM_LEGACY_SERVER_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM", "Cloud Push Network Failure: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("FCM", "Cloud Push Success: ${response.body?.string()}")
            }
        })
    }

    private fun triggerFCMStyleNotification(context: Context, data: Map<String, String>) {
        val notificationHelper = NotificationHelper(context)
        val title = data["title"] ?: "UniCourseHub"
        val message = data["message"] ?: ""

        when (data["type"]) {
            "message" -> notificationHelper.showMessageNotification(title, message)
            else -> notificationHelper.showGeneralNotification(title, message)
        }
    }
}
