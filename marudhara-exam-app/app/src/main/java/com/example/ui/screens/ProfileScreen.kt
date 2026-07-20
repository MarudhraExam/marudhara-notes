package com.example.ui.screens

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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.store.SessionManager
import kotlinx.coroutines.flow.collectLatest

class AndroidDownloadInterface(
    private val context: Context,
    private val onDownloadTriggered: (mockId: String, attemptId: String) -> Unit
) {
    @JavascriptInterface
    fun downloadPdf(mockId: String, attemptId: String) {
        onDownloadTriggered(mockId, attemptId)
    }
}

class AndroidProfileInterface(
    private val onLogout: () -> Unit
) {
    @JavascriptInterface
    fun performNativeLogout() {
        onLogout()
    }
}

@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onLogout: () -> Unit,
    onNavigateToWeb: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progressVal by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    val currentUrl = "https://marudharaexam.in/mock-tests/account.html"

    val webViewAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "ProfileWebViewAlpha"
    )

    // Fetch credentials from SessionManager to synchronize session
    val studentMobile by sessionManager.mobileNumberFlow.collectAsState(initial = null)
    val studentName by sessionManager.studentNameFlow.collectAsState(initial = null)
    val savedPassword by sessionManager.savedPasswordFlow.collectAsState(initial = null)
    val appLanguage by sessionManager.appLanguageFlow.collectAsState(initial = "en")
    val isEn = appLanguage == "en"
    var showPdfDialog by remember { mutableStateOf(false) }
    var pendingPdfUrl by remember { mutableStateOf("") }

    // Intercept Back Pressed to go back in WebView history
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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

                    // Strip Android WebView signature markers (; wv and Version/4.0) to trick Razorpay into treating the app shell as Google Chrome Mobile.
                    // This unlocks all UPI payment apps (Google Pay, PhonePe, Paytm, BHIM, etc.).
                    val originalUA = settings.userAgentString
                    val chromeUA = originalUA
                        .replace("; wv", "")
                        .replace("Version/4.0 ", "")
                        .replace("Version/4.0", "")
                    settings.userAgentString = "$chromeUA MarudharaExamAndroidApp"

                    // Enable cookie synchronization for persistent login sessions
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    // Add JavaScript Interfaces
                    addJavascriptInterface(
                        AndroidDownloadInterface(ctx) { mockId, attemptId ->
                            val url = "https://marudhara-payment-api.jmdseller2025.workers.dev/api/download-question-paper?mockId=$mockId&attemptId=$attemptId"
                            val filename = "Marudhara_Question_Paper_${mockId}.pdf"
                            
                            (ctx as? android.app.Activity)?.runOnUiThread {
                                try {
                                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                                        setTitle(if (isEn) "Download Marksheet & Question Paper" else "मार्कशीट एवं प्रश्न पत्र डाउनलोड")
                                        setDescription("Marudhara Exam - $filename")
                                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                        setAllowedOverMetered(true)
                                        setAllowedOverRoaming(true)
                                        setMimeType("application/pdf")
                                        // Copy cookies and headers for secure PDF download
                                        val cookies = CookieManager.getInstance().getCookie(url)
                                        if (!cookies.isNullOrEmpty()) {
                                            addRequestHeader("Cookie", cookies)
                                        }
                                        addRequestHeader("User-Agent", "MarudharaExamAndroidApp")
                                        addRequestHeader("Referer", "https://marudharaexam.in/")
                                    }
                                    val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    manager.enqueue(request)
                                    Toast.makeText(ctx, if (isEn) "Download started. Please check notifications." else "डाउनलोड शुरू हो गया है। कृपया नोटिफिकेशन देखें।", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(ctx, (if (isEn) "Download failed: " else "डाउनलोड विफल रहा: ") + e.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        "AndroidDownloadInterface"
                    )

                    val bridgeInterface = MarudharaBridgeInterface(
                        context = ctx,
                        webView = this,
                        isOnlineProvider = { true },
                        onOpenPdf = { title, url -> onNavigateToWeb(title, url) }
                    )
                    addJavascriptInterface(bridgeInterface, "AndroidBridge")
                    addJavascriptInterface(bridgeInterface, "Android")

                    addJavascriptInterface(
                        AndroidProfileInterface {
                            (ctx as? android.app.Activity)?.runOnUiThread {
                                onLogout()
                            }
                        },
                        "AndroidProfileInterface"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val requestUrl = request?.url?.toString() ?: ""
                            
                            // 1. Intercept custom schemes (UPI, deep links) before isForMainFrame or anything else to handle Razorpay/UPI app launches.
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
                                return true // Return true to consume and prevent webview crash/error page
                            }

                            // 2. Intercept PDF files to delegate to the WebView container for choice handling
                            if (requestUrl.endsWith(".pdf", ignoreCase = true) || 
                                requestUrl.contains("/pdfs/", ignoreCase = true) || 
                                requestUrl.contains(".pdf?", ignoreCase = true)) {
                                (ctx as? android.app.Activity)?.runOnUiThread {
                                    onNavigateToWeb(if (isEn) "दस्तावेज़" else "दस्तावेज़", requestUrl)
                                }
                                return true
                            }

                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            hasError = false
                            view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()
                            view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                            view?.evaluateJavascript(BLOB_INTERCEPTION_SCRIPT, null)

                            // Hide header, navigation bar and redundant action elements from Profile dashboard
                            val cssHideScript = """
                                (function() {
                                    try {
                                        const style = document.createElement('style');
                                        style.innerHTML = `
                                            header, footer, nav, .top-bar, .top-actions, .mobile-menu-btn, #statusBar {
                                                display: none !important;
                                            }
                                            .page-shell {
                                                padding: 8px 12px 24px !important;
                                            }
                                            #loginOverlay, .nav-login, .login-btn {
                                                display: none !important;
                                            }
                                        `;
                                        document.head.appendChild(style);

                                        // Overwrite secure PDF download globally
                                        window.downloadSecureQuestionPaperPdf = function(mockId, attemptId) {
                                            if (window.AndroidDownloadInterface) {
                                                window.AndroidDownloadInterface.downloadPdf(mockId, attemptId);
                                                return Promise.resolve(true);
                                            }
                                            return Promise.resolve(false);
                                        };

                                        // Connect Logout Button click to Native Trigger
                                        const logout = document.getElementById('logoutBtn');
                                        if (logout) {
                                            logout.addEventListener('click', function(e) {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                if (window.AndroidProfileInterface) {
                                                    window.AndroidProfileInterface.performNativeLogout();
                                                }
                                            });
                                        }

                                        // Dynamically append Logout button below Profile card in page shell
                                        const appendLogout = () => {
                                            if (document.getElementById('native-logout-appended')) return;

                                            const shell = document.querySelector('.page-shell');
                                            if (shell) {
                                                const containerItem = document.createElement('div');
                                                containerItem.id = 'native-logout-appended';
                                                containerItem.style.setProperty('background', '#fff', 'important');
                                                containerItem.style.setProperty('border', '1px solid rgba(35,49,79,0.1)', 'important');
                                                containerItem.style.setProperty('border-radius', '18px', 'important');
                                                containerItem.style.setProperty('padding', '4px', 'important');
                                                containerItem.style.setProperty('margin-top', '16px', 'important');
                                                containerItem.style.setProperty('box-shadow', '0 2px 8px rgba(0,0,0,0.02)', 'important');

                                                const logoutBtn = document.createElement('a');
                                                logoutBtn.href = '#';
                                                logoutBtn.className = 'logout-btn-link';
                                                logoutBtn.innerHTML = '🚪 Logout';
                                                
                                                logoutBtn.style.setProperty('display', 'flex', 'important');
                                                logoutBtn.style.setProperty('align-items', 'center', 'important');
                                                logoutBtn.style.setProperty('color', '#d32f2f', 'important');
                                                logoutBtn.style.setProperty('font-weight', '700', 'important');
                                                logoutBtn.style.setProperty('font-size', '16px', 'important');
                                                logoutBtn.style.setProperty('padding', '16px 20px', 'important');
                                                logoutBtn.style.setProperty('text-decoration', 'none', 'important');
                                                logoutBtn.style.setProperty('border-radius', '14px', 'important');
                                                logoutBtn.style.setProperty('cursor', 'pointer', 'important');
                                                logoutBtn.style.setProperty('transition', 'background-color 0.2s ease', 'important');

                                                logoutBtn.addEventListener('mouseover', () => {
                                                    logoutBtn.style.setProperty('background-color', '#ffebee', 'important');
                                                });
                                                logoutBtn.addEventListener('mouseout', () => {
                                                    logoutBtn.style.setProperty('background-color', 'transparent', 'important');
                                                });

                                                logoutBtn.addEventListener('click', function(e) {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    if (window.AndroidProfileInterface) {
                                                        window.AndroidProfileInterface.performNativeLogout();
                                                    }
                                                });

                                                containerItem.appendChild(logoutBtn);
                                                shell.appendChild(containerItem);
                                            }
                                        };

                                        appendLogout();
                                        setTimeout(appendLogout, 500);
                                        setTimeout(appendLogout, 1500);
                                        setTimeout(appendLogout, 3000);
                                    } catch(e) {
                                        console.error('Profile view enhancement failed', e);
                                    }
                                })()
                            """.trimIndent()
                            view?.evaluateJavascript(cssHideScript, null)

                            val cleanName = (studentName ?: "प्रिय विद्यार्थी").trim()
                            val cleanMobile = (studentMobile ?: "").trim()
                            val pass = (savedPassword ?: "").trim()

                            if (cleanMobile.isNotEmpty()) {
                                // Inject session variables for auto login
                                val sessionJson = "{\"studentName\":\"${cleanName}\",\"studentMobile\":\"${cleanMobile}\"}"
                                val storageScript = """
                                    try {
                                        localStorage.setItem('mockExamSession', '$sessionJson');
                                        sessionStorage.setItem('mockExamSession', '$sessionJson');
                                    } catch(e) {
                                        console.error('Session storage sync failed', e);
                                    }
                                """.trimIndent()
                                view?.evaluateJavascript(storageScript, null)

                                // Auto fill the login form if it appears
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
                                            console.error('Dashboard login bypass failed', e);
                                        }
                                    })()
                                """.trimIndent()
                                view?.evaluateJavascript(formFillScript, null)
                            }

                            val currentLoadedUrl = url ?: ""
                            if (currentLoadedUrl.contains("index.html", ignoreCase = true)) {
                                // We are on index.html, inject script to auto-redirect back to account.html once login completes
                                val redirectBackScript = """
                                    (function() {
                                        try {
                                            const checkAuth = setInterval(function() {
                                                const loginOverlay = document.getElementById('loginOverlay');
                                                if (loginOverlay && loginOverlay.classList.contains('hidden')) {
                                                    clearInterval(checkAuth);
                                                    window.location.href = 'account.html';
                                                }
                                            }, 300);
                                            setTimeout(function() { clearInterval(checkAuth); }, 15000);
                                        } catch(e) {
                                            console.error('Redirect script failed', e);
                                        }
                                    })()
                                """.trimIndent()
                                view?.evaluateJavascript(redirectBackScript, null)
                            } else {
                                // Slight delay before fading in
                                view?.postDelayed({
                                    isLoading = false
                                }, 250)
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                isLoading = false
                            }
                        }

                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            hasError = true
                            isLoading = false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            progressVal = newProgress
                            if (newProgress >= 20) {
                                view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                                view?.evaluateJavascript(BLOB_INTERCEPTION_SCRIPT, null)
                            }
                            if (newProgress == 100) {
                                view?.evaluateJavascript(GLOBAL_HEADER_HIDE_SCRIPT, null)
                                view?.evaluateJavascript(BLOB_INTERCEPTION_SCRIPT, null)
                                val currentLoadedUrl = view?.url ?: ""
                                if (!currentLoadedUrl.contains("index.html", ignoreCase = true)) {
                                    view?.postDelayed({
                                        isLoading = false
                                    }, 250)
                                }
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

        // LaunchedEffect as an absolute timeout safeguard to prevent infinite loading state
        LaunchedEffect(isLoading) {
            if (isLoading) {
                kotlinx.coroutines.delay(12000) // 12 seconds timeout safeguard
                if (isLoading) {
                    isLoading = false
                    if (progressVal < 10) {
                        hasError = true
                    }
                }
            }
        }

        if (isLoading) {
            ShimmerLoadingPlaceholder()
        }

        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = if (isEn) "Network Connection Error" else "नेटवर्क कनेक्शन त्रुटि",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = if (isEn) 
                            "Unable to load profile. Please check your internet connection and try again." 
                        else 
                            "प्रोफ़ाइल लोड करने में असमर्थ। कृपया अपना इंटरनेट कनेक्शन जांचें और पुनः प्रयास करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = {
                            hasError = false
                            isLoading = true
                            webView?.reload()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isEn) "Retry" else "पुनः प्रयास करें")
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Header Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
        // Category Label
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
        // Subtitle block
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Render 3 List Shimmer elements
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = alpha))
                    )
                }
            }
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
            /* Comprehensive selectors to hide website headers, navbars, topbars, and logo sections */
            header, .site-header, .app-header, .main-header, #header, #site-header, .header, #header,
            nav, .navbar, .main-nav, #navbar, .navigation, .nav-container, .nav-bar, .navigation-bar,
            .top-bar, #top-bar, .topbar, #topbar, .top-header, #top-header, .header-top,
            .logo-bar, .logobar, #logo-bar, #logobar, .site-logo, .brand, .logo, [class*="logo-"], [id*="logo-"],
            .brand-logo, .brand-name, .desktop-nav, .desktop-navigation, .site-navigation, .site-nav,
            .hamburger, .hamburger-menu, .menu-toggle, .nav-toggle, .mobile-menu-btn, .menu-btn,
            .mobile-nav, .mobile-navigation, .nav-menu, .menu-container, .menu-wrapper,
            .navbar-container, .navbar-wrapper, .elementor-header, .elementor-location-header,
            .wp-block-navigation, .wp-custom-header, #mysticky-nav, .mysticky-navigation,
            .menu-main-container, .menu-primary-container, .primary-menu, .secondary-menu,
            .header-v1, .header-v2, .header-v3, .header-v4, .header-v5,
            .top-actions, .header-actions, #logoutBtn, .logout-btn, .account-btn, .profile-btn {
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
            .site-title, .site-description {
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

        // Dynamic element traversal hiding to act as a fail-safe
        function hideHeaderElementsDynamically() {
            try {
                // Hide specific tag names
                const tagNames = ['header', 'nav'];
                tagNames.forEach(tag => {
                    const elms = document.getElementsByTagName(tag);
                    for (let i = 0; i < elms.length; i++) {
                        elms[i].style.setProperty("display", "none", "important");
                        elms[i].style.setProperty("height", "0", "important");
                        elms[i].style.setProperty("margin", "0", "important");
                        elms[i].style.setProperty("padding", "0", "important");
                        elms[i].style.setProperty("overflow", "hidden", "important");
                    }
                });

                // Traverse body children to find structural containers that act as headers/logos/navs
                const body = document.body || document.documentElement;
                if (body) {
                    const children = body.children;
                    for (let i = 0; i < children.length; i++) {
                        const child = children[i];
                        const tagName = child.tagName.toLowerCase();
                        if (tagName === 'header' || tagName === 'nav') {
                            child.style.setProperty("display", "none", "important");
                            child.style.setProperty("height", "0", "important");
                            child.style.setProperty("margin", "0", "important");
                            child.style.setProperty("padding", "0", "important");
                            child.style.setProperty("overflow", "hidden", "important");
                            continue;
                        }

                        const id = (child.id || '').toLowerCase();
                        const cls = (typeof child.className === 'string' ? child.className : '').toLowerCase();

                        // Target classes/IDs that look like top-level headers, navbars, or logos
                        const looksLikeHeader = 
                            id.includes('header') || cls.includes('header') ||
                            id.includes('navbar') || cls.includes('navbar') ||
                            id.includes('nav-bar') || cls.includes('nav-bar') ||
                            id.includes('logo-bar') || cls.includes('logo-bar') ||
                            id.includes('logobar') || cls.includes('logobar') ||
                            id.includes('topbar') || cls.includes('topbar') ||
                            id.includes('top-bar') || cls.includes('top-bar') ||
                            id.includes('navigation') || cls.includes('navigation') ||
                            cls.includes('brand') || id.includes('brand') ||
                            cls.includes('menu') || id.includes('menu');

                        // Exclude major content-important elements
                        const looksLikeContent = 
                            id.includes('content') || cls.includes('content') ||
                            id.includes('main') || cls.includes('main') ||
                            id.includes('wrapper') || cls.includes('wrapper') ||
                            id.includes('container') || cls.includes('container') ||
                            id.includes('page-body') || cls.includes('page-body') ||
                            cls.includes('post') || cls.includes('article') ||
                            cls.includes('card') || cls.includes('table');

                        if (looksLikeHeader && !looksLikeContent) {
                            child.style.setProperty("display", "none", "important");
                            child.style.setProperty("height", "0", "important");
                            child.style.setProperty("margin", "0", "important");
                            child.style.setProperty("padding", "0", "important");
                            child.style.setProperty("overflow", "hidden", "important");
                        }
                    }
                }
            } catch (err) {}
        }

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
                    el.style.setProperty("overflow", "hidden", "important");
                });
            } catch (e) {}
        }

        // Execute immediately
        hideHeaderElementsDynamically();
        runTextHide();

        // Use MutationObserver for continuous, event-driven DOM modifications without polling or settimeouts
        const observer = new MutationObserver(function() {
            hideHeaderElementsDynamically();
            runTextHide();
        });

        observer.observe(document.documentElement || document.body, {
            childList: true,
            subtree: true
        });

        // Fail-safe listeners for standard page load phases
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', function() {
                hideHeaderElementsDynamically();
                runTextHide();
            });
        }
        window.addEventListener('load', function() {
            hideHeaderElementsDynamically();
            runTextHide();
        });
    } catch(e) {
        console.error('Global web header hide failed', e);
    }
})()
""";

private const val BLOB_INTERCEPTION_SCRIPT = """
(function() {
    try {
        if (window.hasBlobInterceptionApplied) return;
        window.hasBlobInterceptionApplied = true;

        // 1. Intercept URL.createObjectURL
        const originalCreateObjectURL = window.URL.createObjectURL;
        window.URL.createObjectURL = function(blob) {
            if (blob instanceof Blob && blob.type === 'application/pdf') {
                const reader = new FileReader();
                reader.onload = function() {
                    const base64Data = reader.result.split(',')[1];
                    if (window.AndroidBridge && window.AndroidBridge.downloadBlob) {
                        window.AndroidBridge.downloadBlob(base64Data, "Marudhara_Question_Paper.pdf", "application/pdf");
                    } else if (window.Android && window.Android.downloadBlob) {
                        window.Android.downloadBlob(base64Data, "Marudhara_Question_Paper.pdf", "application/pdf");
                    }
                };
                reader.readAsDataURL(blob);
                return originalCreateObjectURL(blob);
            }
            return originalCreateObjectURL(blob);
        };

        // 2. Intercept click on any anchor tags with href starting with blob:
        document.addEventListener('click', function(e) {
            let target = e.target;
            while (target && target.tagName !== 'A') {
                target = target.parentNode;
            }
            if (target && target.href && target.href.startsWith('blob:')) {
                e.preventDefault();
                const blobUrl = target.href;
                fetch(blobUrl)
                    .then(res => res.blob())
                    .then(blob => {
                        const reader = new FileReader();
                        reader.onload = function() {
                            const base64Data = reader.result.split(',')[1];
                            const filename = target.download || "Marudhara_Document.pdf";
                            if (window.AndroidBridge && window.AndroidBridge.downloadBlob) {
                                window.AndroidBridge.downloadBlob(base64Data, filename, "application/pdf");
                            } else if (window.Android && window.Android.downloadBlob) {
                                window.Android.downloadBlob(base64Data, filename, "application/pdf");
                            }
                        };
                        reader.readAsDataURL(blob);
                    }).catch(err => {
                        console.error("Failed to intercept blob click", err);
                    });
            }
        }, true);

        // 3. Intercept window.open
        const originalOpen = window.open;
        window.open = function(url, name, specs) {
            if (url && url.startsWith('blob:')) {
                fetch(url)
                    .then(res => res.blob())
                    .then(blob => {
                        const reader = new FileReader();
                        reader.onload = function() {
                            const base64Data = reader.result.split(',')[1];
                            if (window.AndroidBridge && window.AndroidBridge.downloadBlob) {
                                window.AndroidBridge.downloadBlob(base64Data, "Marudhara_Document.pdf", "application/pdf");
                            } else if (window.Android && window.Android.downloadBlob) {
                                window.Android.downloadBlob(base64Data, "Marudhara_Document.pdf", "application/pdf");
                            }
                        };
                        reader.readAsDataURL(blob);
                    });
                return null;
            }
            return originalOpen(url, name, specs);
        };
    } catch(e) {
        console.error("Blob interception setup failed", e);
    }
})();
"""
