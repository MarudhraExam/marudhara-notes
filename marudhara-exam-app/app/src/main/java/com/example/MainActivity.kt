package com.example

import android.os.Bundle
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.store.SessionManager
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {
  private val _deepLinkFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
  val deepLinkFlow = _deepLinkFlow.asSharedFlow()

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

      // Request notification permission on Android 13+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        }
      }

      // Subscribe to all_users topic to receive notifications
      FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            Log.d("FCM", "Subscribed to all_users topic successfully")
          } else {
            Log.e("FCM", "Subscription to all_users topic failed")
          }
        }

    } catch (e: Exception) {
      e.printStackTrace()
    }

    handleIntent(intent)

    setContent {
      val context = this
      val sessionManager = remember { SessionManager(context) }
      val appLanguage by sessionManager.appLanguageFlow.collectAsState(initial = "en")
      
      val localizedContext = remember(appLanguage) {
        val locale = java.util.Locale(appLanguage)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
      }

      MyApplicationTheme {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localizedContext) {
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

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    val targetUrl = intent?.getStringExtra("target_url")
    if (!targetUrl.isNullOrEmpty()) {
      _deepLinkFlow.tryEmit(targetUrl)
    }
  }
}

