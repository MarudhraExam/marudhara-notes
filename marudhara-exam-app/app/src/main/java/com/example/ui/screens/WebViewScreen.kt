package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.utils.NetworkMonitor
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    title: String,
    url: String,
    sessionManager: com.example.data.store.SessionManager,
    onNavigateBack: () -> Unit,
    onOpenPdf: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progressVal by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(url) }

    val isMockTest = remember(title, currentUrl) {
        title.contains("मार्कशीट", ignoreCase = true) ||
        title.contains("मॉक टेस्ट", ignoreCase = true) ||
        title.contains("Mock Test", ignoreCase = true) ||
        currentUrl.contains("/mock-tests/", ignoreCase = true) ||
        currentUrl.contains("mock-test", ignoreCase = true) ||
        currentUrl.contains("attempt", ignoreCase = true)
    }

    var isExamSolving by remember { mutableStateOf(false) }

    // Fetch credentials from SessionManager to synchronize session
    val studentMobile by sessionManager.mobileNumberFlow.collectAsState(initial = null)
    val studentName by sessionManager.studentNameFlow.collectAsState(initial = null)
    val savedPassword by sessionManager.savedPasswordFlow.collectAsState(initial = null)

    // Setup Network Monitor to check for live connectivity changes
    val networkMonitor = remember { NetworkMonitor(context) }
    var isOnline by remember { mutableStateOf(networkMonitor.isCurrentlyConnected()) }

    LaunchedEffect(key1 = true) {
        networkMonitor.isOnline.collectLatest { online ->
            isOnline = online
            if (online && hasError) {
                hasError = false
                webView?.reload()
            }
        }
    }

    // Intercept Back Pressed to go back in WebView history
    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onNavigateBack()
        }
    }

    // File picker callback
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback != null) {
            val results = if (result.resultCode == android.app.Activity.RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            } else {
                null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    webView?.onResume()
                    webView?.resumeTimers()
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    webView?.onPause()
                    webView?.pauseTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.clearHistory()
                    wv.clearCache(true)
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.removeAllViews()
                    wv.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            webView = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.marudhara_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(30.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "पीछे जाएं"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hasError = false
                        webView?.reload()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "ताज़ा करें"
                        )
                    }
                    if (isMockTest && isExamSolving) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "अधिक विकल्प",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("प्रश्न पैलेट (Question Palette)") },
                                    leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        webView?.evaluateJavascript("window.AndroidMockTestHelper && window.AndroidMockTestHelper.triggerPalette()", null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("भाषा बदलें (Change Language)") },
                                    leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        webView?.evaluateJavascript("window.AndroidMockTestHelper && window.AndroidMockTestHelper.triggerLanguage()", null)
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("टेस्ट सबमिट करें (Submit Test)", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        webView?.evaluateJavascript("window.AndroidMockTestHelper && window.AndroidMockTestHelper.triggerSubmit()", null)
                                    }
                                )
                            }
                        }
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isOnline || hasError) {
                OfflineScreen(
                    onRetry = {
                        hasError = false
                        if (networkMonitor.isCurrentlyConnected()) {
                            isOnline = true
                            webView?.reload()
                        } else {
                            isOnline = false
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern, non-blocking visual load indicator
                    if (isLoading || progressVal < 100) {
                        LinearProgressIndicator(
                            progress = { progressVal / 100f },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }

                    // Native WebView Container - Always visible to ensure no white screens
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                setBackgroundColor(android.graphics.Color.WHITE)
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                // Clean, standard web settings for maximum compatibility & speed
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.setSupportMultipleWindows(true)
                                settings.javaScriptCanOpenWindowsAutomatically = true

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    settings.safeBrowsingEnabled = true
                                }

                                val originalUA = settings.userAgentString
                                // For Razorpay and general payment gateway compatibility, strip Android WebView identifiers
                                // This tricks Razorpay into treating the app shell as standard Chrome Mobile, unlocking native UPI options.
                                val chromeUA = originalUA
                                    .replace("; wv", "")
                                    .replace("Version/4.0 ", "")
                                    .replace("Version/4.0", "")
                                settings.userAgentString = "$chromeUA MarudharaExamAndroidApp"

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                // Inject JavaScript Interfaces to support native integration
                                addJavascriptInterface(
                                    AndroidDownloadInterface(ctx) { mockId, attemptId ->
                                        val downloadUrl = "https://marudhara-payment-api.jmdseller2025.workers.dev/api/download-question-paper?mockId=$mockId&attemptId=$attemptId"
                                        val filename = "Marudhara_Question_Paper_${mockId}.pdf"

                                        (ctx as? android.app.Activity)?.runOnUiThread {
                                            try {
                                                val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                                    setTitle("मार्कशीट एवं प्रश्न पत्र डाउनलोड")
                                                    setDescription("Marudhara Exam - $filename")
                                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                                    setAllowedOverMetered(true)
                                                    setAllowedOverRoaming(true)
                                                    setMimeType("application/pdf")
                                                }
                                                val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                manager.enqueue(request)
                                                Toast.makeText(ctx, "डाउनलोड शुरू हो गया है। कृपया नोटिफिकेशन देखें।", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(ctx, "डाउनलोड विफल रहा: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    "AndroidDownloadInterface"
                                )

                                addJavascriptInterface(
                                    object {
                                        @android.webkit.JavascriptInterface
                                        fun setExamSolving(isSolving: Boolean) {
                                            (ctx as? android.app.Activity)?.runOnUiThread {
                                                if (isExamSolving != isSolving) {
                                                    isExamSolving = isSolving
                                                }
                                            }
                                        }
                                    },
                                    "AndroidExamInterface"
                                )

                                val bridgeInterface = MarudharaBridgeInterface(
                                    context = ctx,
                                    webView = this,
                                    isOnlineProvider = { isOnline },
                                    onOpenPdf = onOpenPdf
                                )
                                addJavascriptInterface(bridgeInterface, "AndroidBridge")
                                addJavascriptInterface(bridgeInterface, "Android")

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val requestUrl = request?.url?.toString() ?: ""

                                        // 1. Critical intercept for non-http/https custom schemes (UPI, intent://, paytmmp://, phonepe://, tez://, etc.)
                                        // This MUST run regardless of whether isForMainFrame is true or false, to prevent ERR_UNKNOWN_URL_SCHEME errors inside iframes.
                                        if (!requestUrl.startsWith("http://", ignoreCase = true) &&
                                            !requestUrl.startsWith("https://", ignoreCase = true)) {
                                            try {
                                                val intent = if (requestUrl.startsWith("intent://", ignoreCase = true)) {
                                                    Intent.parseUri(requestUrl, Intent.URI_INTENT_SCHEME)
                                                } else {
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                                }

                                                if (intent != null) {
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    val pm = ctx.packageManager
                                                    val info = pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                                                    if (info != null) {
                                                        ctx.startActivity(intent)
                                                    } else {
                                                        // Handle intent fallback or redirect to Play Store
                                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                                        if (!fallbackUrl.isNullOrEmpty()) {
                                                            view?.loadUrl(fallbackUrl)
                                                        } else {
                                                            val packageName = intent.`package`
                                                            if (!packageName.isNullOrEmpty()) {
                                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                })
                                                            }
                                                        }
                                                    }
                                                    return true
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            // Intercepted but failed to launch, return true to consume and prevent webview crash/error page
                                            return true
                                        }

                                        val isForMainFrame = request?.isForMainFrame ?: false

                                        // Only override navigation for main frame requests
                                        if (!isForMainFrame) {
                                            return false
                                        }

                                        // Only redirect to external browser if it's completely outside our app's ecosystem
                                        val isAppDomain = requestUrl.contains("marudharaexam.in", ignoreCase = true) ||
                                                          requestUrl.contains("localhost", ignoreCase = true) ||
                                                          requestUrl.contains("workers.dev", ignoreCase = true) ||
                                                          requestUrl.contains("firebase", ignoreCase = true) ||
                                                          requestUrl.contains("web.app", ignoreCase = true)

                                        if (!isAppDomain) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl)).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                ctx.startActivity(intent)
                                                return true
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }

                                        // Intercept "Download Question Paper" specifically
                                        val isQuestionPaper = requestUrl.contains("download-question-paper", ignoreCase = true) ||
                                                              requestUrl.contains("question-paper", ignoreCase = true) ||
                                                              requestUrl.contains("question_paper", ignoreCase = true)
                                        if (isQuestionPaper) {
                                            downloadFileNatively(ctx, requestUrl, "Marudhara_Question_Paper.pdf")
                                            return true
                                        }

                                        // Intercept direct PDF links
                                        if (requestUrl.endsWith(".pdf", ignoreCase = true) ||
                                            requestUrl.contains("/pdfs/", ignoreCase = true) ||
                                            requestUrl.contains(".pdf?", ignoreCase = true)) {
                                            onOpenPdf("दस्तावेज़", requestUrl)
                                            return true
                                        }

                                        // Intercept account screen redirection
                                        if (requestUrl.contains("/mock-tests/account.html", ignoreCase = true)) {
                                            onNavigateBack()
                                            return true
                                        }

                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                                        isLoading = true
                                        hasError = false
                                        if (url != null) {
                                            currentUrl = url
                                            // Auto detect exam solving state natively
                                            isExamSolving = url.contains("attempt", ignoreCase = true) ||
                                                            url.contains("solve", ignoreCase = true) ||
                                                            url.contains("start", ignoreCase = true) ||
                                                            url.contains("exam.html", ignoreCase = true)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        if (url != null) {
                                            currentUrl = url
                                            isExamSolving = url.contains("attempt", ignoreCase = true) ||
                                                            url.contains("solve", ignoreCase = true) ||
                                                            url.contains("start", ignoreCase = true) ||
                                                            url.contains("exam.html", ignoreCase = true)
                                        }

                                        // Apply global visual overrides to hide redundant headers, navigation bar, My Account/Logout, and specified titles
                                        view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)

                                        // Apply custom styled Layout overrides for ExamSolving/exam.html
                                        if (url != null && (url.contains("exam.html", ignoreCase = true) || isExamSolving || url.contains("attempt", ignoreCase = true) || url.contains("solve", ignoreCase = true) || url.contains("start", ignoreCase = true))) {
                                            view?.evaluateJavascript(EXAM_OVERRIDE_SCRIPT, null)
                                        }


                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        progressVal = newProgress
                                        if (newProgress >= 20) {
                                            view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                                        }
                                        if (newProgress >= 70) {
                                            val url = view?.url ?: ""
                                            if (url.contains("exam.html", ignoreCase = true) || isExamSolving) {
                                                view?.evaluateJavascript(EXAM_OVERRIDE_SCRIPT, null)
                                            }
                                        }
                                        if (newProgress == 100) {
                                            isLoading = false
                                            view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                                        }
}

                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallbackValue: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = filePathCallbackValue

                                        try {
                                            val intent = fileChooserParams?.createIntent()
                                            if (intent != null) {
                                                fileChooserLauncher.launch(intent)
                                            } else {
                                                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                                    addCategory(Intent.CATEGORY_OPENABLE)
                                                    type = "*/*"
                                                }
                                                fileChooserLauncher.launch(Intent.createChooser(fallbackIntent, "फ़ाइल चुनें"))
                                            }
                                        } catch (e: Exception) {
                                            filePathCallback?.onReceiveValue(null)
                                            filePathCallback = null
                                            return false
                                        }
                                        return true
                                    }

                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: android.os.Message?
                                    ): Boolean {
                                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                                        if (transport != null) {
                                            val tempWebView = WebView(view?.context ?: return false)
                                            tempWebView.webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view2: WebView?, request: WebResourceRequest?): Boolean {
                                                    val url = request?.url?.toString() ?: ""
                                                    view?.loadUrl(url)
                                                    return true
                                                }
                                            }
                                            transport.webView = tempWebView
                                            resultMsg.sendToTarget()
                                            return true
                                        }
                                        return false
                                    }
                                }

                                setDownloadListener { downloadUrl, _, contentDisposition, mimetype, _ ->
                                    val isPdf = downloadUrl.endsWith(".pdf", ignoreCase = true) ||
                                                downloadUrl.contains("/pdfs/", ignoreCase = true) ||
                                                downloadUrl.contains(".pdf?", ignoreCase = true) ||
                                                mimetype?.contains("pdf", ignoreCase = true) == true

                                    val isQuestionPaper = downloadUrl.contains("download-question-paper", ignoreCase = true) ||
                                                          downloadUrl.contains("question-paper", ignoreCase = true) ||
                                                          downloadUrl.contains("question_paper", ignoreCase = true)

                                    if (isQuestionPaper) {
                                        downloadFileNatively(ctx, downloadUrl, "Marudhara_Question_Paper.pdf")
                                    } else if (isPdf) {
                                        onOpenPdf("दस्तावेज़", downloadUrl)
                                    } else {
                                        downloadFileNatively(ctx, downloadUrl, null)
                                    }
                                }

                                webView = this
                                loadUrl(currentUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun downloadFileNatively(context: Context, downloadUrl: String, suggestedFilename: String? = null) {
    try {
        val uri = Uri.parse(downloadUrl)
        val extension = MimeTypeMap.getFileExtensionFromUrl(downloadUrl)
        val mimeType = if (!extension.isNullOrEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            "application/octet-stream"
        }

        val filename = if (!suggestedFilename.isNullOrEmpty()) {
            suggestedFilename
        } else {
            URLUtil.guessFileName(downloadUrl, null, mimeType)
        }

        val request = DownloadManager.Request(uri).apply {
            setTitle(filename)
            setDescription("Marudhara Exam - $filename")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            if (mimeType != null) {
                setMimeType(mimeType)
            }
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "डाउनलोड शुरू हो गया है। कृपया नोटिफिकेशन देखें।", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "डाउनलोड विफल रहा: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

class MarudharaBridgeInterface(
    private val context: Context,
    private val webView: WebView?,
    private val isOnlineProvider: () -> Boolean,
    private val onOpenPdf: (title: String, url: String) -> Unit
) {
    @JavascriptInterface
    fun call(method: String, payloadJson: String?): String? {
        return handleMethodCall(method, payloadJson)
    }

    @JavascriptInterface
    fun toast(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val message = json.optString("message", "")
            if (message.isNotEmpty()) {
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            val message = payloadJson ?: ""
            if (message.isNotEmpty() && !message.startsWith("{")) {
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return "true"
    }

    @JavascriptInterface
    fun share(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val title = json.optString("title", "")
            val text = json.optString("text", "")
            val url = json.optString("url", "")

            val shareText = buildString {
                if (title.isNotEmpty()) append("$title\n")
                if (text.isNotEmpty()) append("$text\n")
                if (url.isNotEmpty()) append(url)
            }.trim()

            if (shareText.isNotEmpty()) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "साझा करें")
                context.startActivity(shareIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "true"
    }

    @JavascriptInterface
    fun download(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val downloadUrl = json.optString("url", "")
            val filename = json.optString("filename", "")
            if (downloadUrl.isNotEmpty()) {
                (context as? android.app.Activity)?.runOnUiThread {
                    downloadFileNatively(context, downloadUrl, filename.ifEmpty { null })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "true"
    }

    @JavascriptInterface
    fun copy(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val text = json.optString("text", "")
            if (text.isNotEmpty()) {
                (context as? android.app.Activity)?.runOnUiThread {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Copied Text", text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "कॉपी किया गया", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            val text = payloadJson ?: ""
            if (text.isNotEmpty() && !text.startsWith("{")) {
                (context as? android.app.Activity)?.runOnUiThread {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Copied Text", text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "कॉपी किया गया", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return "true"
    }

    @JavascriptInterface
    fun vibrate(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val ms = json.optLong("milliseconds", 100L)
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "true"
    }

    @JavascriptInterface
    fun openExternal(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val externalUrl = json.optString("url", "")
            if (externalUrl.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "true"
    }

    @JavascriptInterface
    fun openPdf(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val pdfUrl = json.optString("url", "")
            if (pdfUrl.isNotEmpty()) {
                (context as? android.app.Activity)?.runOnUiThread {
                    onOpenPdf("दस्तावेज़", pdfUrl)
                }
            }
        } catch (e: Exception) {
            val pdfUrl = payloadJson ?: ""
            if (pdfUrl.isNotEmpty() && !pdfUrl.startsWith("{")) {
                (context as? android.app.Activity)?.runOnUiThread {
                    onOpenPdf("दस्तावेज़", pdfUrl)
                }
            }
        }
        return "true"
    }

    @JavascriptInterface
    fun openIntent(payloadJson: String?): String? {
        try {
            val json = org.json.JSONObject(payloadJson ?: "")
            val action = json.optString("action", "")
            if (action.isNotEmpty()) {
                val intent = Intent(action).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "true"
    }

    @JavascriptInterface
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val json = org.json.JSONObject().apply {
                put("versionName", versionName)
                put("versionCode", packageInfo.versionCode)
            }
            json.toString()
        } catch (e: Exception) {
            "{\"versionName\":\"1.0.0\",\"versionCode\":1}"
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            val json = org.json.JSONObject().apply {
                put("manufacturer", android.os.Build.MANUFACTURER)
                put("model", android.os.Build.MODEL)
                put("osVersion", android.os.Build.VERSION.RELEASE)
                put("sdkVersion", android.os.Build.VERSION.SDK_INT)
                put("brand", android.os.Build.BRAND)
            }
            json.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    @JavascriptInterface
    fun isOnline(): String {
        return try {
            val json = org.json.JSONObject().apply {
                put("online", isOnlineProvider())
            }
            json.toString()
        } catch (e: Exception) {
            "{\"online\":true}"
        }
    }

    @JavascriptInterface
    fun exitApp(): String {
        (context as? android.app.Activity)?.runOnUiThread {
            (context as? android.app.Activity)?.finish()
        }
        return "true"
    }

    private fun handleMethodCall(method: String, payloadJson: String?): String? {
        return when (method) {
            "toast" -> toast(payloadJson)
            "share" -> share(payloadJson)
            "download" -> download(payloadJson)
            "copy" -> copy(payloadJson)
            "vibrate" -> vibrate(payloadJson)
            "openExternal" -> openExternal(payloadJson)
            "openPdf" -> openPdf(payloadJson)
            "openIntent" -> openIntent(payloadJson)
            "getAppVersion" -> getAppVersion()
            "getDeviceInfo" -> getDeviceInfo()
            "isOnline" -> isOnline()
            "exitApp" -> exitApp()
            else -> null
        }
    }
}

private const val GLOBAL_HEADER_HIDE_SCRIPT = """
(function() {
    try {
        const isExamPage = window.location.href.includes('exam.html') || 
                           window.location.href.includes('attempt') || 
                           window.location.href.includes('solve') || 
                           window.location.href.includes('start');
        
        if (isExamPage) {
            document.documentElement.classList.add('exam-page');
            if (document.body) document.body.classList.add('exam-page');
        }

        // Add android-app class just in case loader.js hasn't executed yet
        document.documentElement.classList.add('android-app');
        if (document.body) document.body.classList.add('android-app');

        let style = document.getElementById('android-ui-override-style');
        if (!style) {
            style = document.createElement('style');
            style.id = 'android-ui-override-style';
            (document.head || document.documentElement).appendChild(style);
        }
        
        style.innerHTML = `
            header, .site-header, .app-header, .main-header, #header, #site-header,
            nav, .navbar, .main-nav, #navbar, .navigation,
            .top-actions, .header-actions, #logoutBtn, .logout-btn, .account-btn, .profile-btn,
            .mobile-menu-btn, .menu-btn, .brand, .logo, [class*="logo-"], [id*="logo-"],
            .brand-logo, .brand-name {
                display: none !important;
                height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                border: none !important;
                box-shadow: none !important;
                overflow: hidden !important;
            }
            .mock-header, .category-header, .page-header, .welcome-section, .welcome-card, .welcome-banner,
            .intro-section, .intro-banner, .page-title, .section-title-main, .hero-banner,
            .banner-section, .banner-container, .promo-banner, .page-header-container,
            .site-title, .site-description, .site-logo {
                display: none !important;
                height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                border: none !important;
                box-shadow: none !important;
                overflow: hidden !important;
            }
            html.android-app:not(.exam-page) .top-bar,
            html.android-app:not(.exam-page) .top-actions {
                display: none !important;
                height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                border: none !important;
                box-shadow: none !important;
                overflow: hidden !important;
            }
            .page-shell, .container, main, #main, .main-content, #content, .content-wrapper {
                padding-top: 8px !important;
                margin-top: 0 !important;
            }
        `;

        function runTextHide() {
            try {
                const walk = document.createTreeWalker(
                    document.body || document.documentElement,
                    NodeFilter.SHOW_TEXT,
                    null,
                    false
                );
                const nodesToHide = [];
                let node;
                while ((node = walk.nextNode())) {
                    const text = node.nodeValue.trim();
                    if (
                        text.includes("Marudhara Mock Categories") ||
                        text.includes("Find available mock tests") ||
                        (text.includes("Marudhara Exam") && node.parentElement && (node.parentElement.tagName === "H1" || node.parentElement.tagName === "H2") && !isExamPage)
                    ) {
                        const parent = node.parentElement;
                        if (parent && parent.tagName !== "SCRIPT" && parent.tagName !== "STYLE") {
                            nodesToHide.push(parent);
                        }
                    }
                }
                nodesToHide.forEach((el) => {
                    el.style.setProperty("display", "none", "important");
                    el.style.setProperty("height", "0", "important");
                    el.style.setProperty("margin", "0", "important");
                    el.style.setProperty("padding", "0", "important");
                });
            } catch (e) {}
        }
        runTextHide();
        setTimeout(runTextHide, 100);
        setTimeout(runTextHide, 300);
        setTimeout(runTextHide, 600);
        setTimeout(runTextHide, 1200);
        setTimeout(runTextHide, 2500);
    } catch(e) {
        console.error('Global web header hide failed', e);
    }
})()
""";

private const val EXAM_OVERRIDE_SCRIPT = """
(function() {
    function applyOverrides() {
        try {
            const css = `
                /* Main Page Shell */
                body, html {
                    background-color: #f8fafc !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    font-family: system-ui, -apple-system, sans-serif !important;
                }
                .page-shell {
                    padding: 12px 16px 24px 16px !important;
                    margin: 0 auto !important;
                    max-width: 100% !important;
                    box-sizing: border-box !important;
                    background: transparent !important;
                }

                /* Hide everything inside page-shell except top-bar and exam-grid */
                .page-shell > *:not(.top-bar):not(.exam-grid) {
                    display: none !important;
                    height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    border: none !important;
                    box-shadow: none !important;
                    overflow: hidden !important;
                }

                /* Hide heading & description in top-bar */
                .top-bar > div:first-child,
                .top-bar h1, .top-bar h2, .top-bar p,
                .top-bar .description, .top-bar .instruction {
                    display: none !important;
                    height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: hidden !important;
                }

                /* Broad CSS to hide all candidate/metadata/instructions/mock name elements */
                .mock-name, .category-name, .instructions, .instruction-card, 
                .candidate-section, .candidate-details, .candidate-profile, .candidate-name,
                .candidate-photo, .candidate-card, .student-details, .student-card,
                .exam-header, .exam-instructions, .exam-title, .exam-meta,
                #candidateInfo, #candidate-info, #studentInfo, #student-info,
                .alert, .alert-info, .card-header, .header-card, .welcome-card,
                #mockMeta, .mock-meta, .meta-card, .candidate-info, .student-info, .exam-info, .info-card, .info-item,
                [class*="candidate"], [class*="student"], [id*="candidate"], [id*="student"],
                [class*="meta"], [id*="meta"] {
                    display: none !important;
                    height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    border: none !important;
                    box-shadow: none !important;
                    overflow: hidden !important;
                }

                /* Style top bar to align timer and language as a single elegant horizontal card */
                .top-bar {
                    display: flex !important;
                    flex-direction: row !important;
                    justify-content: space-between !important;
                    align-items: stretch !important;
                    padding: 0 !important;
                    margin: 0 0 16px 0 !important;
                    background: #ffffff !important;
                    border: 1px solid #cbd5e1 !important;
                    border-radius: 12px !important;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05) !important;
                    box-sizing: border-box !important;
                    width: 100% !important;
                    height: 52px !important;
                    min-height: 52px !important;
                    overflow: hidden !important;
                }

                /* Left half: timer-box */
                .timer-box {
                    order: 1 !important;
                    flex: 1 !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                    background: #ffffff !important;
                    border: none !important;
                    border-right: 1px solid #cbd5e1 !important;
                    color: #334155 !important;
                    border-radius: 0 !important;
                    padding: 8px 12px !important;
                    font-size: 15px !important;
                    font-weight: 600 !important;
                    margin: 0 !important;
                    box-sizing: border-box !important;
                    height: 100% !important;
                }

                /* Right half: language-switcher */
                .language-switcher {
                    order: 2 !important;
                    flex: 1 !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                    background: #ffffff !important;
                    border: none !important;
                    padding: 8px 12px !important;
                    margin: 0 !important;
                    box-sizing: border-box !important;
                    height: 100% !important;
                    position: relative !important;
                }
                .language-switcher::before {
                    content: "🌐" !important;
                    font-size: 16px !important;
                    margin-right: 6px !important;
                    display: inline-flex !important;
                    align-items: center;
                }
                .language-switcher label {
                    display: none !important;
                }
                .language-select {
                    background: transparent !important;
                    border: none !important;
                    color: #1e293b !important;
                    font-size: 15px !important;
                    font-weight: 700 !important;
                    padding: 2px 20px 2px 0 !important;
                    height: 100% !important;
                    min-height: 100% !important;
                    box-sizing: border-box !important;
                    outline: none !important;
                    appearance: none !important;
                    -webkit-appearance: none !important;
                    background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%231e293b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'></polyline></svg>") !important;
                    background-repeat: no-repeat !important;
                    background-position: right center !important;
                    background-size: 14px !important;
                    cursor: pointer !important;
                }

                /* Single column layout for exam-grid */
                .exam-grid {
                    display: grid !important;
                    grid-template-columns: 1fr !important;
                    gap: 16px !important;
                    margin-top: 0 !important;
                    padding-top: 0 !important;
                }

                /* Style question card */
                .question-card {
                    padding: 16px 8px !important;
                    border-radius: 0 !important;
                    background: transparent !important;
                    border: none !important;
                    box-shadow: none !important;
                    margin-top: 0 !important;
                }
                #questionCount {
                    margin: 0 0 12px 0 !important;
                    font-size: 15px !important;
                    color: #1e3a8a !important;
                    font-weight: 700 !important;
                    text-transform: uppercase !important;
                    letter-spacing: 0.5px !important;
                }
                .question-text {
                    margin: 12px 0 20px 0 !important;
                    font-size: 18px !important;
                    line-height: 1.5 !important;
                    color: #0f172a !important;
                    font-weight: 600 !important;
                }

                /* Options styling */
                .options-list {
                    display: flex !important;
                    flex-direction: column !important;
                    gap: 12px !important;
                    margin: 0 0 24px 0 !important;
                    padding: 0 !important;
                }
                .options-list li {
                    list-style: none !important;
                    margin: 0 !important;
                    padding: 0 !important;
                }
                .options-list label {
                    padding: 14px 16px !important;
                    border-radius: 12px !important;
                    min-height: 52px !important;
                    font-size: 16px !important;
                    font-weight: 500 !important;
                    display: flex !important;
                    align-items: center !important;
                    background: #ffffff !important;
                    border: 1px solid #cbd5e1 !important;
                    color: #1e293b !important;
                    margin: 0 !important;
                    cursor: pointer !important;
                    box-shadow: 0 1px 2px rgba(0,0,0,0.02) !important;
                    transition: all 0.2s ease !important;
                }
                .options-list label:hover {
                    border-color: #cbd5e1 !important;
                    background: #f8fafc !important;
                }
                .options-list input[type="radio"]:checked + label,
                .options-list label:has(input[type="radio"]:checked),
                .options-list label.selected,
                .options-list label.checked {
                    border-color: #2563eb !important;
                    background: #eff6ff !important;
                    color: #1e3a8a !important;
                    box-shadow: 0 0 0 1px #2563eb !important;
                }
                .options-list input[type="radio"] {
                    margin-right: 12px !important;
                    width: 20px !important;
                    height: 20px !important;
                    accent-color: #2563eb !important;
                    cursor: pointer !important;
                }

                /* Nav buttons styling */
                .nav-buttons {
                    display: flex !important;
                    justify-content: space-between !important;
                    gap: 16px !important;
                    margin-top: 16px !important;
                }
                .nav-buttons button {
                    flex: 1 !important;
                    padding: 12px 16px !important;
                    min-height: 48px !important;
                    height: 48px !important;
                    font-size: 16px !important;
                    font-weight: 700 !important;
                    border-radius: 12px !important;
                    border: none !important;
                    cursor: pointer !important;
                    text-align: center !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.05) !important;
                    transition: all 0.2s ease !important;
                }
                #prevBtn {
                    background: #ffffff !important;
                    color: #2563eb !important;
                    border: 2px solid #2563eb !important;
                }
                #nextBtn {
                    background: #1d4ed8 !important;
                    color: #ffffff !important;
                }

                /* Palette Card container at the bottom */
                .palette-card {
                    display: flex !important;
                    flex-direction: column !important;
                    gap: 16px !important;
                    background: #ffffff !important;
                    border: 1px solid #cbd5e1 !important;
                    border-radius: 16px !important;
                    padding: 16px !important;
                    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03) !important;
                    margin: 24px 0 !important;
                    box-sizing: border-box !important;
                }

                .submit-card {
                    order: 1 !important;
                    width: 100% !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    background: transparent !important;
                }
                .submit-card p {
                    display: none !important;
                }

                /* Palette grid first child (which contains title and grid) */
                .palette-card > div:first-child {
                    display: flex !important;
                    flex-direction: column !important;
                    gap: 12px !important;
                    order: 2 !important;
                    width: 100% !important;
                }

                /* Palette status legends */
                .palette-legend, .legend-container, .status-legend, .palette-card .legend {
                    order: 2 !important;
                    display: flex !important;
                    flex-wrap: wrap !important;
                    justify-content: center !important;
                    gap: 12px !important;
                    margin: 8px 0 !important;
                    font-size: 13px !important;
                    width: 100% !important;
                }

                .palette-title {
                    order: 3 !important;
                    font-size: 14px !important;
                    font-weight: 700 !important;
                    color: #475569 !important;
                    text-align: center !important;
                    margin: 4px 0 !important;
                }

                .palette-grid {
                    order: 4 !important;
                    display: flex !important;
                    flex-wrap: wrap !important;
                    justify-content: center !important;
                    gap: 8px !important;
                    margin: 8px 0 !important;
                    width: 100% !important;
                }

                .palette-item {
                    width: 38px !important;
                    height: 38px !important;
                    min-width: 38px !important;
                    min-height: 38px !important;
                    font-size: 14px !important;
                    font-weight: 700 !important;
                    border-radius: 8px !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                    cursor: pointer !important;
                    border: 1px solid #cbd5e1 !important;
                    background: #ffffff !important;
                    color: #334155 !important;
                    transition: all 0.2s ease !important;
                }

                /* Palette item state highlights matching the legend colors */
                .palette-item.answered, .palette-item.status-answered {
                    background: #22c55e !important;
                    color: #ffffff !important;
                    border-color: #22c55e !important;
                }
                .palette-item.unanswered, .palette-item.status-unanswered {
                    background: #ffffff !important;
                    color: #334155 !important;
                    border-color: #cbd5e1 !important;
                }
                .palette-item.marked, .palette-item.status-marked {
                    background: #ef4444 !important;
                    color: #ffffff !important;
                    border-color: #ef4444 !important;
                }
                .palette-item.review, .palette-item.status-review {
                    background: #a855f7 !important;
                    color: #ffffff !important;
                    border-color: #a855f7 !important;
                }
                .palette-item.current, .palette-item.active, .palette-item.status-current {
                    background: #1d4ed8 !important;
                    color: #ffffff !important;
                    border-color: #1d4ed8 !important;
                    box-shadow: 0 0 0 2px #93c5fd !important;
                }

                /* Submit Exam button */
                #submitBtn {
                    min-height: 48px !important;
                    height: 48px !important;
                    padding: 12px 16px !important;
                    font-size: 16px !important;
                    font-weight: 700 !important;
                    border-radius: 12px !important;
                    width: 100% !important;
                    background: #15803d !important;
                    color: #ffffff !important;
                    border: none !important;
                    text-align: center !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                    gap: 8px !important;
                    box-shadow: 0 4px 6px -1px rgba(22, 163, 74, 0.2) !important;
                    cursor: pointer !important;
                    transition: all 0.2s ease !important;
                }
                #submitBtn::before {
                    content: "✈" !important;
                    font-size: 16px !important;
                    margin-right: 8px !important;
                    display: inline-flex !important;
                    align-items: center !important;
                    transform: rotate(45deg) !important;
                }
                #submitBtn:hover {
                    background: #166534 !important;
                }
            `;
            
            if (document.head) {
                const existingStyles = document.querySelectorAll('style[data-custom-exam-theme]');
                existingStyles.forEach(s => s.remove());

                const style = document.createElement('style');
                style.setAttribute('data-custom-exam-theme', 'true');
                style.innerHTML = css;
                document.head.appendChild(style);
            }

            // Programmatic element clean up and positioning
            const shell = document.querySelector('.page-shell');
            const topBar = document.querySelector('.top-bar');
            const examGrid = document.querySelector('.exam-grid');
            
            if (shell && topBar && examGrid) {
                // Ensure top-bar is the first element inside page-shell
                if (shell.firstElementChild !== topBar) {
                    shell.insertBefore(topBar, shell.firstElementChild);
                }
                // Ensure exam-grid is the second element inside page-shell
                if (topBar.nextElementSibling !== examGrid) {
                    shell.insertBefore(examGrid, topBar.nextSibling);
                }
                // Force hide everything else inside page-shell
                Array.from(shell.children).forEach(child => {
                    if (child !== topBar && child !== examGrid) {
                        child.style.setProperty('display', 'none', 'important');
                        child.style.setProperty('height', '0', 'important');
                        child.style.setProperty('margin', '0', 'important');
                        child.style.setProperty('padding', '0', 'important');
                        child.style.setProperty('border', 'none', 'important');
                        child.style.setProperty('box-shadow', 'none', 'important');
                    }
                });
            }

            if (topBar) {
                // Force hide anything inside top-bar except .timer-box and .language-switcher
                Array.from(topBar.children).forEach(child => {
                    if (!child.classList.contains('timer-box') && !child.classList.contains('language-switcher')) {
                        child.style.setProperty('display', 'none', 'important');
                        child.style.setProperty('height', '0', 'important');
                        child.style.setProperty('margin', '0', 'important');
                        child.style.setProperty('padding', '0', 'important');
                    }
                });
            }

            const timerEl = document.getElementById('timer');
            if (timerEl) {
                const updateTimerText = () => {
                    let txt = timerEl.textContent || '';
                    let timeStr = txt.replace(/Time left:|⏱|Time Left:/ig, '').trim();
                    if (!timeStr) {
                        timeStr = "00:00";
                    }
                    timerEl.innerHTML = `<span style="color: #2563eb; margin-right: 6px; font-size: 16px; display: inline-flex; align-items: center;">🕒</span> <span style="color: #64748b; font-weight: 500; font-size: 14px;">Time Left:</span> <span style="color: #ef4444; font-weight: 700; font-size: 15px; margin-left: 4px;">${'$'}{timeStr}</span>`;
                };
                
                if (!timerEl.dataset.observed) {
                    timerEl.dataset.observed = 'true';
                    const observer = new MutationObserver((mutations) => {
                        observer.disconnect();
                        updateTimerText();
                        observer.observe(timerEl, { childList: true, characterData: true, subtree: true });
                    });
                    observer.observe(timerEl, { childList: true, characterData: true, subtree: true });
                    updateTimerText();
                }
            }
        } catch(e) {
            console.error('Exam layout override failed', e);
        }
    }

    applyOverrides();
    setTimeout(applyOverrides, 50);
    setTimeout(applyOverrides, 150);
    setTimeout(applyOverrides, 300);
    setTimeout(applyOverrides, 600);
    setTimeout(applyOverrides, 1000);
    setTimeout(applyOverrides, 2000);
})()
"""
