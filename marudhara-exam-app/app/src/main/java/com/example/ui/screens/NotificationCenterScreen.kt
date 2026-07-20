package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.store.SessionManager
import com.example.notification.NotificationViewModel
import com.example.notification.db.NotificationEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    var isEn by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = sessionManager) {
        sessionManager.appLanguageFlow.collect { lang ->
            isEn = (lang == "en")
        }
    }

    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEn) "Notification Center" else "सूचना केंद्र",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = if (isEn) "$unreadCount unread updates" else "$unreadCount बिना पढ़े अपडेट",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back" else "पीछे जाएं",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.markAllAsRead() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = if (isEn) "Mark all as read" else "सभी को पढ़ा हुआ मार्क करें",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showClearConfirmation = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = if (isEn) "Clear all local notifications" else "सभी हटाएं",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFDF9)
                )
            )
        },
        containerColor = Color(0xFFFFFDF9) // Warm background color matching application palette
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (notifications.isEmpty()) {
                NotificationEmptyState(isEn)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationItemCard(
                            item = item,
                            isEn = isEn,
                            onClick = {
                                viewModel.markAsRead(item.id)
                            }
                        )
                    }
                }
            }

            if (showClearConfirmation) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmation = false },
                    title = {
                        Text(
                            text = if (isEn) "Clear History?" else "इतिहास हटाएं?",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = if (isEn) 
                                "Are you sure you want to clear your local notification history? This action cannot be undone." 
                            else 
                                "क्या आप वाकई अपना स्थानीय नोटिफिकेशन इतिहास हटाना चाहते हैं? इसे वापस नहीं लाया जा सकता।"
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAll()
                                showClearConfirmation = false
                            }
                        ) {
                            Text(
                                text = if (isEn) "Clear" else "हटाएं",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmation = false }) {
                            Text(text = if (isEn) "Cancel" else "रद्द करें")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    item: NotificationEntity,
    isEn: Boolean,
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val formattedDateTime = remember(item.receivedTime) {
        try {
            val date = Date(item.receivedTime)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = timeFormat.format(date)
            
            if (DateUtils.isToday(item.receivedTime)) {
                if (isEn) "Today, $timeStr" else "आज, $timeStr"
            } else if (DateUtils.isToday(item.receivedTime + DateUtils.DAY_IN_MILLIS)) {
                if (isEn) "Yesterday, $timeStr" else "कल, $timeStr"
            } else {
                val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                dateFormat.format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }

    val cardBackground = if (item.isRead) {
        Color.White
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) // Warm tint for unread notification card
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable {
                onClick()
                isExpanded = !isExpanded
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification Type Icon
            val iconBg = if (item.isRead) Color(0xFFF1EDE6) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            val iconColor = if (item.isRead) Color(0xFF7F7B75) else MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.notificationType.lowercase(Locale.ROOT)) {
                        "exam", "vacancy" -> Icons.Default.Campaign
                        else -> Icons.Default.NotificationsActive
                    },
                    contentDescription = "Notification Icon",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                            color = if (item.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Unread Indicator small dot
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Medium
                    ),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = Color(0xFF7F7B75),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun NotificationEmptyState(isEn: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F2EA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Empty Notifications Icon",
                tint = Color(0xFF9E9A92),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isEn) "All caught up!" else "कोई नया नोटिफिकेशन नहीं है!",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isEn) 
                "We will notify you here when we publish new updates, exams schedules or results." 
            else 
                "नई भर्तियां, परीक्षा कार्यक्रम या परिणाम जारी होने पर हम आपको यहां सूचित करेंगे।",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF7F7B75),
                lineHeight = 18.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
