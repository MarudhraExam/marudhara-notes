package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.store.SessionManager
import android.widget.Toast
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.NotificationViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class DrawerMenuItem(
    val titleHindi: String,
    val titleEnglish: String,
    val icon: ImageVector,
    val action: DrawerAction
)

sealed class DrawerAction {
    object NavigateHome : DrawerAction()
    data class OpenWeb(val title: String, val url: String) : DrawerAction()
    object ShareApp : DrawerAction()
    object RateApp : DrawerAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    sessionManager: SessionManager,
    onLogout: () -> Unit,
    onNavigateToWeb: (title: String, url: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val appLanguage by sessionManager.appLanguageFlow.collectAsState(initial = "en")
    val isEn = appLanguage == "en"

    val notificationViewModel: NotificationViewModel = viewModel()
    val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()

    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var studentName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    
    var showExitDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    val performLogoutAction: () -> Unit = {
        coroutineScope.launch {
            isLoggingOut = true
            try {
                // 1. Sign out Firebase Auth
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                
                // 2. Clear Web Storage, Cookies and Cache
                android.webkit.WebStorage.getInstance().deleteAllData()
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
                
                // 3. Trigger parent onLogout (clears SessionManager and navigates to Login)
                onLogout()
                
                // 4. Show success toast
                Toast.makeText(context, if (isEn) "Logged out successfully." else "सफलतापूर्वक लॉगआउट हो गया।", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, if (isEn) "Unable to logout. Please try again." else "लॉगआउट करने में असमर्थ। कृपया पुनः प्रयास करें।", Toast.LENGTH_LONG).show()
            } finally {
                isLoggingOut = false
            }
        }
    }

    val defaultStudentName = if (isEn) "Student" else "प्रिय विद्यार्थी"

    // Read details from SessionManager
    LaunchedEffect(key1 = true) {
        studentName = sessionManager.studentNameFlow.firstOrNull() ?: ""
        mobileNumber = sessionManager.mobileNumberFlow.firstOrNull() ?: ""
    }

    val displayName = if (studentName.isNotEmpty()) studentName else defaultStudentName

    // Dynamic Back Handler
    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (selectedBottomTab != 0) {
            // Smoothly fall back to Home tab
            selectedBottomTab = 0
        } else {
            // Show custom exit confirmation
            showExitDialog = true
        }
    }

    // Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = if (isEn) "Exit App?" else "बाहर जाएं?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (isEn) "Are you sure you want to exit Marudhara Exam?" else "क्या आप वास्तव में Marudhara Exam एप्लीकेशन बंद करना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEn) "Exit" else "हाँ (Exit)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(if (isEn) "Cancel" else "निरस्त करें", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    val drawerItems = remember {
        listOf(
            DrawerMenuItem("मुख्य पृष्ठ", "Home Dashboard", Icons.Default.Home, DrawerAction.NavigateHome),
            DrawerMenuItem("OMR चेक", "Verify OMR sheet", Icons.Default.FactCheck, DrawerAction.OpenWeb("OMR चेक", "https://marudharaexam.in/omr.html")),
            DrawerMenuItem("मॉक टेस्ट", "Practice mock tests", Icons.Default.Assignment, DrawerAction.OpenWeb("मॉक टेस्ट", "https://marudharaexam.in/mock-tests/index.html")),
            DrawerMenuItem("OMR चेक परिणाम", "OMR Check Result", Icons.Default.Assessment, DrawerAction.OpenWeb("OMR चेक परिणाम", "https://marudharaexam.in/results.html")),
            DrawerMenuItem("नवीनतम भर्तियां", "Job alerts and news", Icons.Default.Campaign, DrawerAction.OpenWeb("नवीनतम भर्तियां", "https://marudharaexam.in/vacancy.html")),
            DrawerMenuItem("विद्यार्थी कॉर्नर", "Candidate portal", Icons.Default.School, DrawerAction.OpenWeb("विद्यार्थी कॉर्नर", "https://marudharaexam.in/student-corner.html")),
            DrawerMenuItem("भर्ती परिणाम", "Selected candidates", Icons.Default.TaskAlt, DrawerAction.OpenWeb("भर्ती परिणाम", "https://marudharaexam.in/vacancy-result.html")),
            DrawerMenuItem("ऐप शेयर करें", "Share Application", Icons.Default.Share, DrawerAction.ShareApp),
            DrawerMenuItem("रेट करें", "Rate on Play Store", Icons.Default.Star, DrawerAction.RateApp)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFFFDF9), // Warm White Drawer background
                modifier = Modifier.width(300.dp)
            ) {
                // Redesigned Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFDF9)) // Warm White
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column {
                        // Beautiful Official Brand Logo PNG
                        Image(
                            painter = painterResource(id = com.example.R.drawable.marudhara_logo),
                            contentDescription = "Marudhara Exam Logo",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, // Dark Blue text
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = if (mobileNumber.isNotEmpty()) "+91 $mobileNumber" else (if (isEn) "Marudhara Exam Candidate" else "मारुधरा एग्जाम कैंडिडेट"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E0D8), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Menu Items + Language switcher at the bottom
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFDF9))
                        .padding(horizontal = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        drawerItems.forEach { menuItem ->
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text(
                                            text = if (isEn) menuItem.titleEnglish else menuItem.titleHindi,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isEn) menuItem.titleHindi else menuItem.titleEnglish,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = menuItem.icon,
                                        contentDescription = menuItem.titleHindi,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    when (val act = menuItem.action) {
                                        is DrawerAction.NavigateHome -> {
                                            selectedBottomTab = 0
                                        }
                                        is DrawerAction.OpenWeb -> {
                                            onNavigateToWeb(act.title, act.url)
                                        }
                                        is DrawerAction.ShareApp -> {
                                            val shareText = if (isEn) {
                                                "Marudhara Exam - Best prep app for Rajasthan Competitive Exams. Download now: https://marudharaexam.in"
                                            } else {
                                                "Marudhara Exam - राजस्थान प्रतियोगी परीक्षाओं की सर्वश्रेष्ठ तैयारी ऐप। अभी डाउनलोड करें: https://marudharaexam.in"
                                            }
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, if (isEn) "Share App" else "शेयर करें"))
                                        }
                                        is DrawerAction.RateApp -> {
                                            try {
                                                val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                                context.startActivity(rateIntent)
                                            } catch (e: Exception) {
                                                val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                                context.startActivity(rateIntent)
                                            }
                                        }
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE5E0D8), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Language Selector inside Drawer Bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Text(
                            text = if (isEn) "Choose Language / भाषा चुनें" else "भाषा चुनें (Choose Language)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // English Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        sessionManager.saveAppLanguage("en")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEn) MaterialTheme.colorScheme.primary else Color(0xFFEFECE6),
                                    contentColor = if (isEn) Color.White else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Hindi Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        sessionManager.saveAppLanguage("hi")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isEn) MaterialTheme.colorScheme.primary else Color(0xFFEFECE6),
                                    contentColor = if (!isEn) Color.White else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("हिन्दी", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color(0xFFFFFDF9), // Warm White Bar
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(start = 52.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.marudhara_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Marudhara Exam" else "मरुधरा एग्जाम",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = if (isEn) "Open Menu" else "मेन्यू खोलें",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier.size(48.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = Color.White
                                            ) {
                                                Text(text = unreadCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = if (isEn) "Notifications" else "नोटिफिकेशन",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            if (selectedBottomTab == 4) {
                                var showProfileMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showProfileMenu = !showProfileMenu },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = if (isEn) "Profile Options" else "प्रोफ़ाइल विकल्प",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showProfileMenu,
                                        onDismissRequest = { showProfileMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (isEn) "🚪 Logout" else "🚪 लॉगआउट (Logout)") },
                                            onClick = {
                                                showProfileMenu = false
                                                performLogoutAction()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = Color(0xFFFFFDF9), // Warm White bottom navigation
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        HorizontalDivider(color = Color(0xFFE5E0D8), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp), // Extremely sleek but comfortable touch target height
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home Item
                            val homeSelected = selectedBottomTab == 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedBottomTab = 0 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (homeSelected) MaterialTheme.colorScheme.primary else Color(0xFF7F7B75),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isEn) "Home" else "मुख्य पृष्ठ",
                                    fontSize = 10.sp,
                                    fontWeight = if (homeSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (homeSelected) MaterialTheme.colorScheme.primary else Color(0xFF7F7B75)
                                )
                            }

                            // Profile Item
                            val profileSelected = selectedBottomTab == 4
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedBottomTab = 4 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = if (profileSelected) MaterialTheme.colorScheme.primary else Color(0xFF7F7B75),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isEn) "Profile" else "प्रोफ़ाइल",
                                    fontSize = 10.sp,
                                    fontWeight = if (profileSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (profileSelected) MaterialTheme.colorScheme.primary else Color(0xFF7F7B75)
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Switch between bottoms tabs
                when (selectedBottomTab) {
                    0 -> HomeScreen(
                        studentName = displayName,
                        appLanguage = appLanguage,
                        onNavigateToWeb = onNavigateToWeb,
                        onSelectBottomTab = { selectedBottomTab = it }
                    )
                    4 -> ProfileScreen(
                        sessionManager = sessionManager,
                        onLogout = performLogoutAction,
                        onNavigateToWeb = onNavigateToWeb
                    )
                    else -> HomeScreen(
                        studentName = displayName,
                        appLanguage = appLanguage,
                        onNavigateToWeb = onNavigateToWeb,
                        onSelectBottomTab = { selectedBottomTab = it }
                    )
                }

                if (isLoggingOut) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isEn) "Logging out..." else "लॉगआउट हो रहा है...",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
