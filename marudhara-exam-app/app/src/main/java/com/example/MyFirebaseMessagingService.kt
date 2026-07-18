package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed FCM token: $token")
        // In production, we can upload this token to our Firestore users/{uid} collection
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        // Extract title and body
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Marudhara Exam"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "New updates available!"
        
        // Extract target tap action URL
        val targetUrl = remoteMessage.data["url"] ?: remoteMessage.data["target_url"]

        sendNotification(title, body, targetUrl)
    }

    private fun sendNotification(title: String, body: String, targetUrl: String?) {
        val channelId = "marudhara_updates_channel"
        val notificationId = System.currentTimeMillis().toInt()

        // Create PendingIntent to launch MainActivity with destination URL extra
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (!targetUrl.isNullOrEmpty()) {
                putExtra("target_url", targetUrl)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.web_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Marudhara Exam Updates"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for new mock tests, vacancies and announcements"
            }
            notificationManager.createNotificationChannel(channel)
        }

        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.e("FCM", "Notification permission not granted or security exception: ${e.message}")
        }
    }
}
