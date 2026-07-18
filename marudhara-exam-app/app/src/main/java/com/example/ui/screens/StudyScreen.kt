package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StudyCategory(
    val title: String,
    val subtitle: String,
    val hindiTitle: String,
    val icon: ImageVector,
    val color: Color,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onNavigateToWeb: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = remember {
        listOf(
            StudyCategory(
                title = "Important Notes",
                subtitle = "Handwritten and typed revision notes",
                hindiTitle = "महत्वपूर्ण हस्तलिखित नोट्स",
                icon = Icons.Default.MenuBook,
                color = Color(0xFF002B5B),
                url = "https://marudharaexam.in/important-notes"
            ),
            StudyCategory(
                title = "Syllabus Tracker",
                subtitle = "Syllabus blueprints for all state exams",
                hindiTitle = "विस्तृत परीक्षा पाठ्यक्रम",
                icon = Icons.Default.AssignmentTurnedIn,
                color = Color(0xFF10B981),
                url = "https://marudharaexam.in/downloads"
            ),
            StudyCategory(
                title = "Previous Year Papers",
                subtitle = "Analyze question trends and weightage",
                hindiTitle = "पुराने परीक्षा प्रश्न पत्र",
                icon = Icons.Default.History,
                color = Color(0xFFF59E0B),
                url = "https://marudharaexam.in/downloads"
            ),
            StudyCategory(
                title = "Free PDF Books",
                subtitle = "Standard text books for study",
                hindiTitle = "निशुल्क डिजिटल पुस्तकें",
                icon = Icons.Default.LibraryBooks,
                color = Color(0xFF8B5CF6),
                url = "https://marudharaexam.in/downloads"
            ),
            StudyCategory(
                title = "Daily Sujas PDF",
                subtitle = "Rajasthan Govt official magazine updates",
                hindiTitle = "दैनिक सूजस बुलेटिन",
                icon = Icons.Default.Newspaper,
                color = Color(0xFFEC4899),
                url = "https://marudharaexam.in/daily-sujas"
            )
        )
    }

    val filteredCategories = remember(searchQuery) {
        categories.filter {
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.hindiTitle.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Search & Greeting Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "अध्ययन केंद्र (Study Center)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "सभी शिक्षण सामग्री एवं ई-बुक्स यहाँ उपलब्ध हैं।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // High-Contrast Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("पाठ्यक्रम या नोट्स खोजें...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "खोज"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "साफ़ करें"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // List of Categories
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredCategories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "कोई सामग्री नहीं मिली।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(filteredCategories) { category ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWeb(category.title, category.url) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Rounded container for Icon
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        color = category.color.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.title,
                                    tint = category.color,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.hindiTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = category.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "खोलें",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
