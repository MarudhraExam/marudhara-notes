package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "marudhara_updates_channel"
    const val CHANNEL_NAME = "Exam Updates & Mock Tests"
    const val CHANNEL_DESC = "Notifications regarding exams, mock tests, and marudhara updates."

    /**
     * Creates the required notification channels for Android 8.0+
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Shows a notification with the given title and message.
     */
    fun showNotification(context: Context, title: String?, message: String?) {
        // Create an Intent to open MainActivity when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use standard launcher foreground icon or system fallback
        val appIcon = com.example.R.drawable.ic_launcher_foreground

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(appIcon)
            .setContentTitle(title ?: "Marudhara Exam")
            .setContentText(message ?: "New updates are available!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
                Log.d(TAG, "Notification displayed successfully")
            } else {
                Log.w(TAG, "Cannot display notification: POST_NOTIFICATIONS permission not granted.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    /**
     * Helper to check if Notification permission is granted (Android 13+)
     */
    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
