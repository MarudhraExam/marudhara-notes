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

                            // 2. Intercept PDF files to show or handle natively
                            if (requestUrl.endsWith(".pdf", ignoreCase = true) || 
                                requestUrl.contains("/pdfs/", ignoreCase = true) || 
                                requestUrl.contains(".pdf?", ignoreCase = true)) {
                                val filename = "Marudhara_Document_${System.currentTimeMillis()}.pdf"
                                try {
                                    val req = DownloadManager.Request(Uri.parse(requestUrl)).apply {
                                        setTitle(if (isEn) "Download Document" else "दस्तावेज़ डाउनलोड")
                                        setDescription("Marudhara Exam - $filename")
                                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                        setAllowedOverMetered(true)
                                        setAllowedOverRoaming(true)
                                        setMimeType("application/pdf")
                                    }
                                    val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    manager.enqueue(req)
                                    Toast.makeText(ctx, if (isEn) "Download started. Please check notifications." else "डाउनलोड शुरू हो गया है। कृपया नोटिफिकेशन देखें।", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
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

                            // Hide header, navigation bar and redundant action elements from Profile dashboard
                            val cssHideScript = """
                                (function() {
                                    try {
                                        const style = document.createElement('style');
                                        style.innerHTML = `
                                            header, footer, nav, .top-bar, .top-actions, .mobile-menu-btn, .section, #statusBar {
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

                            // Slight delay before fading in
                            view?.postDelayed({
                                isLoading = false
                            }, 250)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
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
                            if (newProgress == 100) {
                                view?.postDelayed({
                                    isLoading = false
                                }, 250)
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
            ShimmerLoadingPlaceholder()
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
