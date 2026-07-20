package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.store.SessionManager
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      Log.d("MainActivity", "FCM Notification permission granted")
    } else {
      Log.d("MainActivity", "FCM Notification permission denied")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Firebase Programmatically with the website's exact production project config
    try {
      val options = FirebaseOptions.Builder()
        .setApiKey("AIzaSyDHe87UG-QGy2Kxh7RI8t51qOGgppVd_YA")
        .setApplicationId("1:680152404373:web:32f4dcb9e16c525d33669c")
        .setProjectId("marudhara-exam")
        .setStorageBucket("marudhara-exam.firebasestorage.app")
        .build()
      
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this, options)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // Subscribe to topic and log current token for diagnostic purposes
    try {
      FirebaseMessaging.getInstance().subscribeToTopic("marudhara_updates")
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            Log.d("MainActivity", "Successfully subscribed to marudhara_updates topic")
          } else {
            Log.e("MainActivity", "Failed to subscribe to marudhara_updates topic", task.exception)
          }
        }

      FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val token = task.result
          Log.d("MainActivity", "Generated FCM registration token: $token")
        } else {
          Log.w("MainActivity", "Failed to retrieve FCM token", task.exception)
        }
      }
    } catch (e: Exception) {
      Log.e("MainActivity", "Error initializing FCM messaging configurations", e)
    }

    // Request Android 13+ Notification Permission (Only once)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val sharedPrefs = getSharedPreferences("notification_prefs", MODE_PRIVATE)
      val hasAsked = sharedPrefs.getBoolean("asked_notification_permission", false)
      if (!hasAsked) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
          requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          sharedPrefs.edit().putBoolean("asked_notification_permission", true).apply()
        }
      }
    }

    setContent {
      val context = this
      val sessionManager = remember { SessionManager(context) }
      
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          AppNavigation(
            sessionManager = sessionManager,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

