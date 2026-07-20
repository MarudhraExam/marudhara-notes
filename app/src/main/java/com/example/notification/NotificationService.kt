package com.example.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import com.example.notification.db.AppDatabase
import com.example.notification.db.NotificationEntity
import com.example.notification.db.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token generated/refreshed: $token")
        
        // Automatically subscribe to the required topic on token refresh
        subscribeToUpdatesTopic()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message Data Payload: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Marudhara Exam"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: remoteMessage.data["body"] ?: "New updates are available!"
        val type = remoteMessage.data["type"] ?: "general"

        // Show the status bar notification tray notification
        NotificationHelper.showNotification(applicationContext, title, body)

        // Save incoming notification to local Room Database
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = NotificationRepository(db.notificationDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.insert(
                    NotificationEntity(
                        title = title,
                        message = body,
                        receivedTime = System.currentTimeMillis(),
                        isRead = false,
                        notificationType = type
                    )
                )
                Log.d(TAG, "Saved incoming notification to local Room Database successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save incoming notification to Room Database", e)
            }
        }
    }

    private fun subscribeToUpdatesTopic() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_NAME)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully subscribed to $TOPIC_NAME topic")
                    } else {
                        Log.w(TAG, "Failed to subscribe to $TOPIC_NAME topic", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to $TOPIC_NAME", e)
        }
    }

    companion object {
        private const val TAG = "NotificationService"
        private const val TOPIC_NAME = "marudhara_updates"
    }
}
