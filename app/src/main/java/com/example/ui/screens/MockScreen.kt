package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class MockSeries(
    val id: String,
    val examName: String,
    val description: String,
    val hindiTitle: String,
    val testCount: Int,
    val isPremium: Boolean,
    val color: Color
)

@Composable
fun MockScreen(
    onNavigateToWeb: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mockList = remember {
        listOf(
            MockSeries(
                id = "1",
                examName = "Rajasthan GK Special",
                description = "Comprehensive state history, art & geography",
                hindiTitle = "राजस्थान सामान्य ज्ञान",
                testCount = 45,
                isPremium = false,
                color = Color(0xFF002B5B)
            ),
            MockSeries(
                id = "2",
                examName = "REET Level I & II",
                description = "Strictly based on latest syllabus blueprints",
                hindiTitle = "रीट परीक्षा (REET Spec.)",
                testCount = 30,
                isPremium = true,
                color = Color(0xFF8B5CF6)
            ),
            MockSeries(
                id = "3",
                examName = "RAS Prelims Spec.",
                description = "High standards mock questions and feedback",
                hindiTitle = "आर.ए.एस. प्रारंभिक परीक्षा",
                testCount = 20,
                isPremium = true,
                color = Color(0xFFEC4899)
            ),
            MockSeries(
                id = "4",
                examName = "Patwar & LDC",
                description = "Topic-wise complete syllabus tests",
                hindiTitle = "पटवार एवं एल.डी.सी. टेस्ट",
                testCount = 35,
                isPremium = false,
                color = Color(0xFF10B981)
            ),
            MockSeries(
                id = "5",
                examName = "Rajasthan Police Spec.",
                description = "Full length exams with timer simulation",
                hindiTitle = "राजस्थान पुलिस कांस्टेबल",
                testCount = 25,
                isPremium = false,
                color = Color(0xFF06B6D4)
            ),
            MockSeries(
                id = "6",
                examName = "Sujas Bulletins Mock",
                description = "Questions based on monthly government bulletins",
                hindiTitle = "सूजस विशेष मॉक प्रश्न",
                testCount = 15,
                isPremium = false,
                color = Color(0xFFF59E0B)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Greeting & Summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "मॉक टेस्ट सीरीज़ (Mock Test Series)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "अपनी गति और तैयारी के स्तर को जांचने के लिए टेस्ट दें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Dynamic Informational Notice Card regarding payment and premium features
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "सुझाव",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "प्रीमियम मॉक टेस्ट एवं भुगतान सीधे वेबसाइट के माध्यम से सुरक्षित रूप से किए जा सकते हैं।",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Mock Tests Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mockList) { series ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigateToWeb(
                                series.hindiTitle,
                                "https://marudharaexam.in/mock-test"
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Badge / Premium indicator
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = series.color.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${series.testCount} टेस्ट",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = series.color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (series.isPremium) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Premium",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Free",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title Hindi
                        Text(
                            text = series.hindiTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Title English
                        Text(
                            text = series.examName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = series.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button-like action row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "शुरू करें",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "चालू करें",
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
