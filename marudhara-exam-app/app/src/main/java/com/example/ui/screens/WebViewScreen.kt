package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import android.view.View
import com.example.utils.NetworkMonitor
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

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

    val webViewAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "WebViewAlpha"
    )

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
                // Auto reload on reconnection
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                // Native WebView Wrapper
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            
                            // Essential settings for high performance & responsive rendering
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

                            // Append Custom App tag to User-Agent for easy backend detection
                            val originalUA = settings.userAgentString
                            settings.userAgentString = "$originalUA MarudharaExamAndroidApp"

                            // Enable cookie synchronization for persistent login sessions
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            addJavascriptInterface(
                                AndroidDownloadInterface(ctx) { mockId, attemptId ->
                                    val url = "https://marudhara-payment-api.jmdseller2025.workers.dev/api/download-question-paper?mockId=$mockId&attemptId=$attemptId"
                                    val filename = "Marudhara_Question_Paper_${mockId}.pdf"
                                    
                                    (ctx as? android.app.Activity)?.runOnUiThread {
                                        try {
                                            val request = DownloadManager.Request(Uri.parse(url)).apply {
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

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val requestUrl = request?.url?.toString() ?: ""
                                    
                                    // 1. Intercept PDF files to show in native viewer
                                    if (requestUrl.endsWith(".pdf", ignoreCase = true) || 
                                        requestUrl.contains("/pdfs/", ignoreCase = true) || 
                                        requestUrl.contains(".pdf?", ignoreCase = true)) {
                                        onOpenPdf("दस्तावेज़", requestUrl)
                                        return true
                                    }

                                    // 2. Intercept Account page to redirect to native profile screen
                                    if (requestUrl.contains("/mock-tests/account.html", ignoreCase = true)) {
                                        onNavigateBack()
                                        return true
                                    }

                                    return super.shouldOverrideUrlLoading(view, request)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    hasError = false
                                    if (url != null) {
                                        currentUrl = url
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    
                                    // Flush cookies to persistent storage
                                    CookieManager.getInstance().flush()

                                    // Inject hiding CSS stylesheet instantly to prevent showing Login, Logout or My Account
                                    val cssHideScript = """
                                        (function() {
                                            try {
                                                const style = document.createElement('style');
                                                style.innerHTML = `
                                                    #loginOverlay, #logoutBtn, #myAccountLink, .nav-login, .login-btn, .logout-btn, a[href*="account.html"] {
                                                        display: none !important;
                                                    }
                                                `;
                                                document.head.appendChild(style);

                                                // Programmatic layout clean up
                                                const overlay = document.getElementById('loginOverlay');
                                                if (overlay) overlay.style.display = 'none';
                                                const logout = document.getElementById('logoutBtn');
                                                if (logout) logout.style.display = 'none';
                                                const account = document.getElementById('myAccountLink');
                                                if (account) account.style.display = 'none';
                                            } catch(e) {
                                                console.error('Styling injection error', e);
                                            }
                                        })()
                                    """.trimIndent()
                                    view?.evaluateJavascript(cssHideScript, null)

                                    val currentUrl = url ?: ""
                                    if (currentUrl.contains("marudharaexam.in")) {
                                        val cleanName = (studentName ?: "प्रिय विद्यार्थी").trim()
                                        val cleanMobile = (studentMobile ?: "").trim()
                                        val pass = (savedPassword ?: "").trim()

                                        if (cleanMobile.isNotEmpty()) {
                                            // Inject LocalStorage/SessionStorage for Mock Tests matching website expectations
                                            val sessionJson = "{\"studentName\":\"${cleanName}\",\"studentMobile\":\"${cleanMobile}\"}"
                                            val storageScript = """
                                                try {
                                                    localStorage.setItem('mockExamSession', '$sessionJson');
                                                    sessionStorage.setItem('mockExamSession', '$sessionJson');
                                                } catch(e) {
                                                    console.error('LocalStorage error', e);
                                                }
                                            """.trimIndent()
                                            view?.evaluateJavascript(storageScript, null)

                                            // Form Auto-Login Background Assistant
                                            val formFillScript = """
                                                (function() {
                                                    try {
                                                        const mobileInput = document.getElementById('loginMobile');
                                                        const passwordInput = document.getElementById('loginPassword');
                                                        const loginButton = document.getElementById('loginBtn');
                                                        if (mobileInput && passwordInput && loginButton) {
                                                            mobileInput.value = '$cleanMobile';
                                                            passwordInput.value = '$pass';
                                                            
                                                            mobileInput.dispatchEvent(new Event('input', { bubbles: true }));
                                                            passwordInput.dispatchEvent(new Event('input', { bubbles: true }));
                                                            
                                                            setTimeout(function() {
                                                                loginButton.click();
                                                            }, 300);
                                                        }
                                                    } catch(e) {
                                                        console.error('Form fill auto login failed', e);
                                                    }
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(formFillScript, null)

                                            // Authenticate the user programmatically in the WebView context using official Firebase Auth library
                                            if (pass.isNotEmpty()) {
                                                val email = "$cleanMobile@mockstudent.marudharaexam.in"
                                                val firebaseScript = """
                                                    (function() {
                                                        try {
                                                            import('https://www.gstatic.com/firebasejs/12.1.0/firebase-auth.js').then((m) => {
                                                                const auth = m.getAuth();
                                                                if (!auth.currentUser) {
                                                                    m.signInWithEmailAndPassword(auth, '$email', '$pass').then((cred) => {
                                                                        console.log('WebView signed in natively via Firebase successfully');
                                                                    }).catch((err) => {
                                                                        console.error('WebView Firebase sign in error', err);
                                                                    });
                                                                }
                                                            }).catch((err) => {
                                                                console.error('WebView import Firebase SDK failed', err);
                                                            });
                                                        } catch(e) {
                                                            console.error('WebView injection error', e);
                                                        }
                                                    })()
                                                """.trimIndent()
                                                view?.evaluateJavascript(firebaseScript, null)
                                            }
                                        }
                                    }

                                    // Slightly delay hiding the loading overlay to allow the webpage to paint and prevent white flashes
                                    view?.postDelayed({
                                        isLoading = false
                                    }, 200)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    // Ignore minor/sub-resource failures, check if primary URL fails
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
                                    if (newProgress == 100) {
                                        view?.postDelayed({
                                            isLoading = false
                                        }, 200)
                                    }
                                }
                            }

                            // Enable download support for notes, results, and PDF documents
                            setDownloadListener { downloadUrl, _, _, _, _ ->
                                if (downloadUrl.endsWith(".pdf", ignoreCase = true) || 
                                    downloadUrl.contains("/pdfs/", ignoreCase = true) || 
                                    downloadUrl.contains(".pdf?", ignoreCase = true)) {
                                    onOpenPdf("दस्तावेज़", downloadUrl)
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(downloadUrl)
                                        }
                                        ctx.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback if no activity handles the link
                                    }
                                }
                            }

                            webView = this
                            loadUrl(currentUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(webViewAlpha)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "लोड हो रहा है...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
