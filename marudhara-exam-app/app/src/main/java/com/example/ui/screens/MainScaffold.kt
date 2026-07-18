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
import androidx.compose.ui.res.stringResource
import com.example.R
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var studentName by remember { mutableStateOf("प्रिय विद्यार्थी") }
    var mobileNumber by remember { mutableStateOf("") }
    
    var showExitDialog by remember { mutableStateOf(false) }

    // Read details from SessionManager
    LaunchedEffect(key1 = true) {
        studentName = sessionManager.studentNameFlow.firstOrNull() ?: "प्रिय विद्यार्थी"
        mobileNumber = sessionManager.mobileNumberFlow.firstOrNull() ?: ""
    }

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
                    text = stringResource(R.string.exit_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.exit_message),
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
                    Text(stringResource(R.string.exit_yes), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.exit_no), fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    val appLanguage by sessionManager.appLanguageFlow.collectAsState(initial = "en")

    val drawerItems = remember(appLanguage) {
        listOf(
            DrawerMenuItem(context.getString(R.string.nav_home), "Home Dashboard", Icons.Default.Home, DrawerAction.NavigateHome),
            DrawerMenuItem(context.getString(R.string.nav_omr), "Verify OMR sheet", Icons.Default.FactCheck, DrawerAction.OpenWeb(context.getString(R.string.nav_omr), "https://marudharaexam.in/omr.html")),
            DrawerMenuItem(context.getString(R.string.nav_mock), "Practice mock tests", Icons.Default.Assignment, DrawerAction.OpenWeb(context.getString(R.string.nav_mock), "https://marudharaexam.in/mock-tests/index.html")),
            DrawerMenuItem(context.getString(R.string.nav_results), "OMR Check Result", Icons.Default.Assessment, DrawerAction.OpenWeb(context.getString(R.string.nav_results), "https://marudharaexam.in/results.html")),
            DrawerMenuItem(context.getString(R.string.nav_vacancy), "Job alerts and news", Icons.Default.Campaign, DrawerAction.OpenWeb(context.getString(R.string.nav_vacancy), "https://marudharaexam.in/vacancy.html")),
            DrawerMenuItem(context.getString(R.string.nav_student), "Candidate portal", Icons.Default.School, DrawerAction.OpenWeb(context.getString(R.string.nav_student), "https://marudharaexam.in/student-corner.html")),
            DrawerMenuItem(context.getString(R.string.nav_vacancy_result), "Selected candidates", Icons.Default.TaskAlt, DrawerAction.OpenWeb(context.getString(R.string.nav_vacancy_result), "https://marudharaexam.in/vacancy-result.html")),
            DrawerMenuItem(context.getString(R.string.nav_share), "Share Application", Icons.Default.Share, DrawerAction.ShareApp),
            DrawerMenuItem(context.getString(R.string.nav_rate), "Rate on Play Store", Icons.Default.Star, DrawerAction.RateApp)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        // Small Circular Symbol
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "लोगो",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = studentName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = if (mobileNumber.isNotEmpty()) "+91 $mobileNumber" else "मारुधरा एग्जाम कैंडिडेट",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Menu Items
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    drawerItems.forEach { menuItem ->
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(
                                        text = menuItem.titleHindi,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = menuItem.titleEnglish,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = menuItem.icon,
                                    contentDescription = menuItem.titleHindi,
                                    tint = MaterialTheme.colorScheme.primary
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
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Marudhara Exam - राजस्थान प्रतियोगी परीक्षाओं की सर्वश्रेष्ठ तैयारी ऐप। अभी डाउनलोड करें: https://marudharaexam.in"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "शेयर करें"))
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
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Language Selection Section in Drawer
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.language_select),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            coroutineScope.launch {
                                                sessionManager.saveLanguage("en")
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    RadioButton(
                                        selected = appLanguage == "en",
                                        onClick = {
                                            coroutineScope.launch {
                                                sessionManager.saveLanguage("en")
                                            }
                                        }
                                    )
                                    Text(
                                        text = "English",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (appLanguage == "en") FontWeight.Bold else FontWeight.Normal),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            coroutineScope.launch {
                                                sessionManager.saveLanguage("hi")
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    RadioButton(
                                        selected = appLanguage == "hi",
                                        onClick = {
                                            coroutineScope.launch {
                                                sessionManager.saveLanguage("hi")
                                            }
                                        }
                                    )
                                    Text(
                                        text = "हिन्दी",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (appLanguage == "hi") FontWeight.Bold else FontWeight.Normal),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(start = 48.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.web_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .padding(1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Marudhara Exam",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.White
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
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "मेन्यू खोलें",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                onNavigateToWeb("नवीनतम भर्तियां", "https://marudharaexam.in/vacancy.html")
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "नोटिफिकेशन",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
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
                                tint = if (homeSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.nav_home),
                                fontSize = 9.sp,
                                fontWeight = if (homeSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (homeSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.7f)
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
                                tint = if (profileSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.nav_profile),
                                fontSize = 9.sp,
                                fontWeight = if (profileSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (profileSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.7f)
                            )
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
                        studentName = studentName,
                        onNavigateToWeb = onNavigateToWeb,
                        onSelectBottomTab = { selectedBottomTab = it }
                    )
                    4 -> ProfileScreen(
                        sessionManager = sessionManager,
                        onLogout = onLogout,
                        onNavigateToWeb = onNavigateToWeb
                    )
                    else -> HomeScreen(
                        studentName = studentName,
                        onNavigateToWeb = onNavigateToWeb,
                        onSelectBottomTab = { selectedBottomTab = it }
                    )
                }
            }
        }
    }
}
