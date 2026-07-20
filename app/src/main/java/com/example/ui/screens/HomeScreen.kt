package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

// Action Card data model matching allowed modules
data class ActionItem(
    val titleHindi: String,
    val titleEnglish: String,
    val icon: ImageVector,
    val color: Color,
    val targetUrl: String,
    val titleParam: String
)

// Dynamic Banner data model from Firestore
data class BannerItem(
    val imageUrl: String,
    val targetUrl: String,
    val order: Long
)

@Composable
fun HomeScreen(
    studentName: String,
    appLanguage: String,
    onNavigateToWeb: (title: String, url: String) -> Unit,
    onSelectBottomTab: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var banners by remember { mutableStateOf<List<BannerItem>>(emptyList()) }
    var isLoadingBanners by remember { mutableStateOf(true) }

    // Fetch dynamic live banners from production Firestore
    LaunchedEffect(key1 = true) {
        try {
            FirebaseFirestore.getInstance().collection("offerBanners")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        val img = doc.getString("image") ?: ""
                        val lnk = doc.getString("link") ?: ""
                        val ord = doc.getLong("order") ?: 0L
                        if (img.isNotEmpty()) {
                            BannerItem(
                                imageUrl = img,
                                targetUrl = lnk,
                                order = ord
                            )
                        } else null
                    }.sortedBy { it.order }
                    banners = list
                    isLoadingBanners = false
                }
                .addOnFailureListener {
                    isLoadingBanners = false
                }
        } catch (e: Exception) {
            e.printStackTrace()
            isLoadingBanners = false
        }
    }

    // Allowed 6 Tiles from Official website
    val actions = remember(appLanguage) {
        listOf(
            ActionItem(
                titleHindi = "OMR चेक",
                titleEnglish = "OMR Check",
                icon = Icons.Default.FactCheck,
                color = Color(0xFF10B981),
                targetUrl = "https://marudharaexam.in/omr.html",
                titleParam = "OMR चेक"
            ),
            ActionItem(
                titleHindi = "मॉक टेस्ट",
                titleEnglish = "Mock Tests",
                icon = Icons.Default.Assignment,
                color = Color(0xFF002B5B),
                targetUrl = "https://marudharaexam.in/mock-tests/index.html",
                titleParam = "मॉक टेस्ट"
            ),
            ActionItem(
                titleHindi = "OMR चेक परिणाम",
                titleEnglish = "OMR Check Result",
                icon = Icons.Default.Assessment,
                color = Color(0xFF3B82F6),
                targetUrl = "https://marudharaexam.in/results.html",
                titleParam = "परीक्षा परिणाम"
            ),
            ActionItem(
                titleHindi = "नवीनतम भर्तियां",
                titleEnglish = "Vacancy Updates",
                icon = Icons.Default.Campaign,
                color = Color(0xFFF59E0B),
                targetUrl = "https://marudharaexam.in/vacancy.html",
                titleParam = "नवीनतम भर्तियां"
            ),
            ActionItem(
                titleHindi = "विद्यार्थी कॉर्नर",
                titleEnglish = "Student Corner",
                icon = Icons.Default.School,
                color = Color(0xFF8B5CF6),
                targetUrl = "https://marudharaexam.in/student-corner.html",
                titleParam = "विद्यार्थी कॉर्नर"
            ),
            ActionItem(
                titleHindi = "भर्ती परिणाम",
                titleEnglish = "Vacancy Result",
                icon = Icons.Default.TaskAlt,
                color = Color(0xFFEC4899),
                targetUrl = "https://marudharaexam.in/vacancy-result.html",
                titleParam = "भर्ती परिणाम"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { if (banners.isEmpty()) 1 else banners.size })

    // Auto sliding effect for banners
    if (banners.size > 1) {
        LaunchedEffect(key1 = banners) {
            while (true) {
                delay(3500)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val isEn = appLanguage == "en"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        val isLandscape = screenWidth > screenHeight
        val isSmallScreen = screenHeight < 680.dp || screenWidth < 360.dp

        val bannerRatio = if (isLandscape) 2.6f else if (isSmallScreen) 2.0f else 1.9f
        val columnsCount = if (isLandscape) 3 else 2
        val gridSpacing = if (isSmallScreen) 8.dp else 12.dp
        val topPadding = if (isSmallScreen) 8.dp else 14.dp
        val bottomPadding = if (isSmallScreen) 8.dp else 16.dp
        val welcomeBottomPadding = if (isSmallScreen) 8.dp else 12.dp
        val sectionTopPadding = if (isSmallScreen) 6.dp else 12.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            // Welcome and Banner Header (Spans full columns)
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columnsCount) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    // Welcome Greeting
                    Text(
                        text = if (isEn) "Hello, $studentName 👋" else "नमस्ते, $studentName 👋",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isSmallScreen) 18.sp else 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = if (isEn) "Welcome to Marudhara Exam Companion App." else "मरुधरा एग्जाम Companion App में आपका स्वागत है।",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (isSmallScreen) 11.sp else 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        modifier = Modifier.padding(top = 1.dp, bottom = welcomeBottomPadding)
                    )

                    // Dynamic Banner Slider (Noticeably larger - aspectRatio 1.9f instead of 2.6f)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bannerRatio)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) { page ->
                        if (banners.isEmpty()) {
                            // Placeholder / Fallback dynamic visual with premium educational style
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF1E4E8C)
                                            )
                                        )
                                    )
                                    .padding(if (isSmallScreen) 12.dp else 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isLoadingBanners) {
                                        if (isEn) "Loading banners..." else "बैनर लोड हो रहे हैं..."
                                    } else {
                                        if (isEn) "Marudhara Exam - Pathway to Success" else "Marudhara Exam - सफलता की राह"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = if (isSmallScreen) 14.sp else 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        } else {
                            val banner = banners[page]
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = "Live Promo Banner",
                                contentScale = ContentScale.Fit, // Display the complete image without cropping
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (banner.targetUrl.isNotEmpty()) {
                                            onNavigateToWeb("विवरण", banner.targetUrl)
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Banner dots indicator
                    if (banners.size > 1) {
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(banners.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(1.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(if (pagerState.currentPage == iteration) 6.dp else 4.dp)
                                        .animateContentSize()
                                )
                            }
                        }
                    }
                }
            }

            // Section Title (Spans full columns)
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columnsCount) }) {
                Text(
                    text = if (isEn) "Official Portals" else "आधिकारिक लिंक (Official Portals)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSmallScreen) 13.sp else 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = sectionTopPadding, bottom = 2.dp)
                )
            }

            // Action Cards Grid
            items(actions) { item ->
                ActionCard(
                    item = item,
                    appLanguage = appLanguage,
                    isSmallScreen = isSmallScreen,
                    onClick = { onNavigateToWeb(item.titleParam, item.targetUrl) }
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    item: ActionItem,
    appLanguage: String,
    isSmallScreen: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // Clean white cards as requested
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSmallScreen) 1.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEFECE6)), // Elegant subtle border matching cream theme
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isSmallScreen) 8.dp else 12.dp, horizontal = if (isSmallScreen) 6.dp else 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isSmallScreen) 32.dp else 40.dp)
                    .background(
                        color = item.color.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.titleHindi,
                    tint = item.color,
                    modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))

            val isEn = appLanguage == "en"
            Text(
                text = if (isEn) item.titleEnglish else item.titleHindi,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isSmallScreen) 11.sp else 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = if (isEn) item.titleHindi else item.titleEnglish,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isSmallScreen) 9.sp else 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
