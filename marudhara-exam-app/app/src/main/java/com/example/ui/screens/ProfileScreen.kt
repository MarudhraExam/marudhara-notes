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

                    // Append Custom App tag to User-Agent for easy backend detection
                    val originalUA = settings.userAgentString
                    settings.userAgentString = "$originalUA MarudharaExamAndroidApp"

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
                            
                            // Intercept PDF files to show or handle natively
                            if (requestUrl.endsWith(".pdf", ignoreCase = true) || 
                                requestUrl.contains("/pdfs/", ignoreCase = true) || 
                                requestUrl.contains(".pdf?", ignoreCase = true)) {
                                val filename = "Marudhara_Document_${System.currentTimeMillis()}.pdf"
                                try {
                                    val req = DownloadManager.Request(Uri.parse(requestUrl)).apply {
                                        setTitle("दस्तावेज़ डाउनलोड")
                                        setDescription("Marudhara Exam - $filename")
                                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                        setAllowedOverMetered(true)
                                        setAllowedOverRoaming(true)
                                        setMimeType("application/pdf")
                                    }
                                    val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    manager.enqueue(req)
                                    Toast.makeText(ctx, "डाउनलोड शुरू हो गया है। कृपया नोटिफिकेशन देखें।", Toast.LENGTH_LONG).show()
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
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()

                            // Hide header, navigation bar and redundant action elements from Profile dashboard
                            val cssHideScript = """
                                (function() {
                                    try {
                                        const style = document.createElement('style');
                                        style.innerHTML = `
                                            header, footer, nav, .top-bar, .top-actions, .mobile-menu-btn {
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
