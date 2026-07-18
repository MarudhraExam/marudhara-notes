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
import androidx.compose.ui.res.stringResource
import com.example.R
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

// Action Card data model matching allowed modules
data class ActionItem(
    val titleResId: Int,
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
    val actions = remember {
        listOf(
            ActionItem(
                titleResId = R.string.nav_omr,
                titleEnglish = "OMR Check",
                icon = Icons.Default.FactCheck,
                color = Color(0xFF10B981),
                targetUrl = "https://marudharaexam.in/omr.html",
                titleParam = "OMR चेक"
            ),
            ActionItem(
                titleResId = R.string.nav_mock,
                titleEnglish = "Mock Tests",
                icon = Icons.Default.Assignment,
                color = Color(0xFF002B5B),
                targetUrl = "https://marudharaexam.in/mock-tests/index.html",
                titleParam = "मॉक टेस्ट"
            ),
            ActionItem(
                titleResId = R.string.nav_results,
                titleEnglish = "OMR Check Result",
                icon = Icons.Default.Assessment,
                color = Color(0xFF3B82F6),
                targetUrl = "https://marudharaexam.in/results.html",
                titleParam = "OMR Check Result"
            ),
            ActionItem(
                titleResId = R.string.nav_vacancy,
                titleEnglish = "Vacancy Updates",
                icon = Icons.Default.Campaign,
                color = Color(0xFFF59E0B),
                targetUrl = "https://marudharaexam.in/vacancy.html",
                titleParam = "नवीनतम भर्तियां"
            ),
            ActionItem(
                titleResId = R.string.nav_student,
                titleEnglish = "Student Corner",
                icon = Icons.Default.School,
                color = Color(0xFF8B5CF6),
                targetUrl = "https://marudharaexam.in/student-corner.html",
                titleParam = "विद्यार्थी कॉर्नर"
            ),
            ActionItem(
                titleResId = R.string.nav_vacancy_result,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Welcome and Banner Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 1.dp)
        ) {
            // Welcome Greeting
            Text(
                text = stringResource(R.string.welcome_back, studentName),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.welcome_sub),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 0.dp, bottom = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic Banner Slider
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3.2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray.copy(alpha = 0.15f))
            ) { page ->
                if (banners.isEmpty()) {
                    // Placeholder / Fallback dynamic visual
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLoadingBanners) stringResource(R.string.banner_loading) else stringResource(R.string.banner_slogan),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val banner = banners[page]
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = "Live Promo Banner",
                        contentScale = ContentScale.Crop,
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

            Spacer(modifier = Modifier.height(2.dp))

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

        // Section Title
        Text(
            text = stringResource(R.string.portals_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
        )

        // Action Cards Grid (Only the 6 official website modules)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(actions) { item ->
                ActionCard(
                    item = item,
                    onClick = { onNavigateToWeb(item.titleParam, item.targetUrl) }
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    item: ActionItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = item.color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(item.titleResId),
                    tint = item.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(item.titleResId),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = item.titleEnglish,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
