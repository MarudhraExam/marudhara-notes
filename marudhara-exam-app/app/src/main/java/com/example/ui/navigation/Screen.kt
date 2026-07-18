package com.example.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Study : Screen("study")
    object Mock : Screen("mock")
    object Updates : Screen("updates")
    object Profile : Screen("profile")
    
    // Parametric WebView Screen to reuse for any website page (OMR, Mock, Results, Sujas, etc.)
    object WebView : Screen("web_view/{title}/{url}") {
        fun createRoute(title: String, url: String): String {
            val encodedUrl = Uri.encode(url)
            val encodedTitle = Uri.encode(title)
            return "web_view/$encodedTitle/$encodedUrl"
        }
    }

    // Native PDF Viewer Screen
    object PdfViewer : Screen("pdf_viewer/{title}/{url}") {
        fun createRoute(title: String, url: String): String {
            val encodedUrl = Uri.encode(url)
            val encodedTitle = Uri.encode(title)
            return "pdf_viewer/$encodedTitle/$encodedUrl"
        }
    }
}
