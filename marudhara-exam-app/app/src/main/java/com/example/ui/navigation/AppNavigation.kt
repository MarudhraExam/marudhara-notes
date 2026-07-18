package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.store.SessionManager
import com.example.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                sessionManager = sessionManager,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                sessionManager = sessionManager,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // Navigate to official website register page inside WebView wrapper
                    navController.navigate(
                        Screen.WebView.createRoute("अकाउंट रजिस्ट्रेशन", "https://marudharaexam.in/login") // Registration is handled on website login/dashboard routing
                    )
                },
                onNavigateToForgotPassword = {
                    // Navigate to official website forgot-password page inside WebView wrapper
                    navController.navigate(
                        Screen.WebView.createRoute("पासवर्ड भूल गए?", "https://marudharaexam.in/login")
                    )
                }
            )
        }

        // Home Dashboard Scaffolding (Home, Study, Mock, Updates, Profile)
        composable(Screen.Home.route) {
            MainScaffold(
                sessionManager = sessionManager,
                onLogout = {
                    coroutineScope.launch {
                        sessionManager.clearSession()
                    }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToWeb = { title, url ->
                    navController.navigate(Screen.WebView.createRoute(title, url))
                }
            )
        }

        // Parameterized Hybrid WebView Container
        composable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "वेबसाइट विवरण"
            val url = backStackEntry.arguments?.getString("url") ?: "https://marudharaexam.in"
            
            WebViewScreen(
                title = title,
                url = url,
                sessionManager = sessionManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenPdf = { pdfTitle, pdfUrl ->
                    navController.navigate(Screen.PdfViewer.createRoute(pdfTitle, pdfUrl))
                }
            )
        }

        // Native PDF Viewer
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "दस्तावेज़"
            val url = backStackEntry.arguments?.getString("url") ?: ""
            
            PdfViewerScreen(
                title = title,
                url = url,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
