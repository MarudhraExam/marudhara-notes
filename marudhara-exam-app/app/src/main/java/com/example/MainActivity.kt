package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

class MainActivity : ComponentActivity() {
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

