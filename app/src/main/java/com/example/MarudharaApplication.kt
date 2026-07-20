package com.example

import android.app.Application
import android.util.Log
import com.example.notification.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MarudharaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase programmatically using production configuration
        try {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyDHe87UG-QGy2Kxh7RI8t51qOGgppVd_YA")
                .setApplicationId("1:680152404373:web:32f4dcb9e16c525d33669c")
                .setProjectId("marudhara-exam")
                .setStorageBucket("marudhara-exam.firebasestorage.app")
                .build()
            
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options)
                Log.d("MarudharaApplication", "Firebase initialized programmatically in application lifecycle")
            }
        } catch (e: Exception) {
            Log.e("MarudharaApplication", "Failed to initialize Firebase", e)
        }

        // Pre-create notification channels on app startup
        try {
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            Log.e("MarudharaApplication", "Failed to create notification channels", e)
        }
    }
}
