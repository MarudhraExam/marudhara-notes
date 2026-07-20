package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.store.SessionManager
import com.example.ui.theme.MarudharaAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    sessionManager: SessionManager,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage by sessionManager.appLanguageFlow.collectAsState(initial = "en")
    val isEn = appLanguage == "en"

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0.0f) }

    // Start entrance animations together
    LaunchedEffect(key1 = true) {
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1000)
        )
    }
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    // Logic to check session after 2.5 seconds
    LaunchedEffect(key1 = true) {
        delay(2500)
        val isLoggedIn = sessionManager.isLoggedInFlow.first()
        if (isLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFDF9), // Warm White top
                        Color(0xFFF8F5EE)  // Soft Cream bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Official Website Logo (Beautifully displayed in a clean container with soft shadow)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.marudhara_logo),
                        contentDescription = "Marudhara Exam Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Brand Title in Hindi (Dark Navy Blue)
            Text(
                text = "मरुधरा एग्जाम",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                color = MaterialTheme.colorScheme.primary, // Dark Blue
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Brand Title in English (Premium Golden Accent)
            Text(
                text = "MARUDHARA EXAM",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 3.sp
                ),
                color = MaterialTheme.colorScheme.secondary, // Golden Accent
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Elegant Subtitle (Dark Charcoal)
            Text(
                text = if (isEn) "Best Platform for Rajasthan Competitive Exams" else "राजस्थान प्रतियोगी परीक्षाओं के लिए सर्वश्रेष्ठ मंच",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF55524E), // Soft Charcoal
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Premium Loading indicator (Dark Blue)
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.5.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
