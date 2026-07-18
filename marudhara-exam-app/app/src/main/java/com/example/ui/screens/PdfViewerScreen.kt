package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    title: String,
    url: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Zoom & pan states
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    BackHandler {
        onNavigateBack()
    }

    // Download & Render Effect
    LaunchedEffect(url) {
        if (url.isEmpty()) {
            errorMessage = "त्रुटि: अमान्य पीडीएफ लिंक।"
            isDownloading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                // Create a secure temporary file inside the cache directory
                val filename = "pdf_" + url.hashCode() + ".pdf"
                val file = File(context.cacheDir, filename)

                if (!file.exists()) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        withContext(Dispatchers.Main) {
                            errorMessage = "पीडीएफ लोड करने में असमर्थ (त्रुटि कोड: ${connection.responseCode})"
                            isDownloading = false
                        }
                        return@withContext
                    }

                    val fileLength = connection.contentLength
                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(file)

                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    while (inputStream.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            withContext(Dispatchers.Main) {
                                downloadProgress = total.toFloat() / fileLength.toFloat()
                            }
                        }
                        outputStream.write(data, 0, count)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                }

                pdfFile = file

                // Open with PdfRenderer
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)
                val count = renderer.pageCount
                
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until count) {
                    val page = renderer.openPage(i)
                    // Render at high-quality resolution
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // Fill background white
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                    
                    // Update main progress periodically
                    val progressRatio = (i + 1).toFloat() / count.toFloat()
                    withContext(Dispatchers.Main) {
                        downloadProgress = progressRatio
                    }
                }
                renderer.close()
                fileDescriptor.close()

                withContext(Dispatchers.Main) {
                    pageCount = count
                    renderedPages = bitmaps
                    isDownloading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "डाउनलोड या रेंडरिंग विफल रही: ${e.localizedMessage ?: "संजाल त्रुटि"}"
                    isDownloading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "पीछे जाएं",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scale = (scale + 0.25f).coerceAtMost(3f)
                    }) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "ज़ूम इन", tint = Color.White)
                    }
                    IconButton(onClick = {
                        scale = (scale - 0.25f).coerceAtLeast(1f)
                        if (scale == 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "ज़ूम आउट", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (downloadProgress > 0f && downloadProgress < 1f) {
                                "लोड हो रहा है: ${(downloadProgress * 100).toInt()}%"
                            } else {
                                "दस्तावेज़ डाउनलोड किया जा रहा है..."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            modifier = Modifier
                                .width(200.dp)
                                .height(4.dp)
                        )
                    }
                }
                errorMessage != null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "त्रुटि",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("वापस जाएं", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    // Zoomable scrollable list of rendered pages
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 3f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(renderedPages) { index, bitmap ->
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "पेज ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF1F5F9))
                                                .padding(vertical = 4.dp, horizontal = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "पेज ${index + 1} / $pageCount",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
