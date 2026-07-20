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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UpdateItem(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val date: String,
    val category: String, // "Vacancy" or "Result" or "Notification"
    val description: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    onNavigateToWeb: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val updatesList = remember {
        listOf(
            UpdateItem(
                id = "1",
                titleHindi = "REET मुख्य परीक्षा 2026 भर्ती विज्ञापन जारी",
                titleEnglish = "REET Main Exam 2026 Notification out",
                date = "18 जुलाई 2026",
                category = "Vacancy",
                description = "राजस्थान कर्मचारी चयन बोर्ड द्वारा प्राथमिक एवं उच्च प्राथमिक स्तर पर शिक्षकों के पदों हेतु विस्तृत अधिसूचना जारी।",
                url = "https://marudharaexam.in/vacancies"
            ),
            UpdateItem(
                id = "2",
                titleHindi = "वरिष्ठ अध्यापक परीक्षा (Grade II) परिणाम घोषित",
                titleEnglish = "RPSC Senior Teacher (Grade II) Results Declared",
                date = "15 जुलाई 2026",
                category = "Result",
                description = "राजस्थान लोक सेवा आयोग द्वारा द्वितीय श्रेणी शिक्षक भर्ती परीक्षा का अंतिम परिणाम एवं कट-ऑफ सूची जारी कर दी गयी है।",
                url = "https://marudharaexam.in/results"
            ),
            UpdateItem(
                id = "3",
                titleHindi = "दैनिक सूजस मासिक विशेषांक जारी - जुलाई 2026",
                titleEnglish = "Daily Sujas Bulletin - July 2026 Special",
                date = "12 जुलाई 2026",
                category = "Notification",
                description = "राजस्थान सरकार द्वारा जारी दैनिक आर्थिक नीतियों और ग्रामीण विकास कार्यक्रमों का विस्तृत विश्लेषण पढ़ें।",
                url = "https://marudharaexam.in/daily-sujas"
            ),
            UpdateItem(
                id = "4",
                titleHindi = "राजस्थान पुलिस कांस्टेबल शारीरिक दक्षता परीक्षा तिथि",
                titleEnglish = "Rajasthan Police Constable PET Exam Dates",
                date = "10 जुलाई 2026",
                category = "Vacancy",
                description = "कांस्टेबल भर्ती परीक्षा के पात्र उम्मीदवारों की शारीरिक मापतौल परीक्षा का आयोजन अगस्त माह में किया जायेगा।",
                url = "https://marudharaexam.in/vacancies"
            ),
            UpdateItem(
                id = "5",
                titleHindi = "पटवार भर्ती परीक्षा 2026 नवीन पाठ्यक्रम जारी",
                titleEnglish = "Patwar Recruitment 2026 New Syllabus",
                date = "08 जुलाई 2026",
                category = "Notification",
                description = "पटवारी पद हेतु सामान्य विज्ञान एवं मानसिक योग्यता के अध्यायों में महत्वपूर्ण बदलाव। नया पाठ्यक्रम यहाँ डाउनलोड करें।",
                url = "https://marudharaexam.in/downloads"
            )
        )
    }

    val filteredUpdates = remember(searchQuery, selectedCategory) {
        updatesList.filter { item ->
            val matchesSearch = item.titleHindi.contains(searchQuery, ignoreCase = true) ||
                                item.titleEnglish.contains(searchQuery, ignoreCase = true) ||
                                item.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Search and Title Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "नवीनतम समाचार व अपडेट्स",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "नवीनतम भर्तियों और परिणामों की सटीक जानकारी प्राप्त करें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("भर्तियां, परिणाम या सूजस खोजें...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "खोज")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "साफ़ करें")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categories Filter Scrollable Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "All" to "सभी अपडेट्स",
                    "Vacancy" to "नई भर्तियां",
                    "Result" to "परीक्षा परिणाम",
                    "Notification" to "सूचनाएं"
                ).forEach { (catId, catLabel) ->
                    val isSelected = selectedCategory == catId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catId },
                        label = {
                            Text(
                                text = catLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // List View
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredUpdates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "इस श्रेणी में कोई अपडेट नहीं है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(filteredUpdates) { item ->
                    val badgeColor = when (item.category) {
                        "Vacancy" -> Color(0xFFF59E0B) // Gold
                        "Result" -> Color(0xFF10B981) // Green
                        else -> Color(0xFF3B82F6) // Blue
                    }
                    val badgeLabel = when (item.category) {
                        "Vacancy" -> "भर्ती (Vacancy)"
                        "Result" -> "परिणाम (Result)"
                        else -> "अधिसूचना (Notice)"
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWeb(item.titleHindi, item.url) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Category Badge & Date Row
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = badgeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = item.date,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Hindi Title
                            Text(
                                text = item.titleHindi,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // English Sub-title
                            Text(
                                text = item.titleEnglish,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Brief Description
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action button link
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "विवरण देखें (View Details)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "खोलें",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
