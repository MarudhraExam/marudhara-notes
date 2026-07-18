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
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

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

    val appLanguage by sessionManager.appLanguageFlow.collectAsState(initial = "en")
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact Language Selector Bar
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (appLanguage == "en") "Language / भाषा" else "भाषा / Language",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "English",
                        color = if (appLanguage == "en") MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = if (appLanguage == "en") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    sessionManager.saveLanguage("en")
                                }
                            }
                            .padding(4.dp)
                    )
                    Text(
                        text = "हिन्दी",
                        color = if (appLanguage == "hi") MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = if (appLanguage == "hi") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    sessionManager.saveLanguage("hi")
                                }
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                                            html, body {
                                                max-width: 100vw !important;
                                                overflow-x: hidden !important;
                                                box-sizing: border-box !important;
                                                background-color: #F8FAFC !important;
                                                font-family: system-ui, -apple-system, sans-serif !important;
                                                color: #0F172A !important;
                                                margin: 0 !important;
                                                padding: 0 !important;
                                            }
                                            *, *:before, *:after {
                                                box-sizing: inherit !important;
                                            }
                                            .page-shell {
                                                padding: 16px 12px 32px !important;
                                                max-width: 100vw !important;
                                                overflow-x: hidden !important;
                                                box-sizing: border-box !important;
                                            }
                                            #statusBar {
                                                background-color: #EFF6FF !important;
                                                color: #1D4ED8 !important;
                                                border: 1px solid #DBEAFE !important;
                                                border-radius: 10px !important;
                                                padding: 10px 14px !important;
                                                font-size: 12px !important;
                                                font-weight: 600 !important;
                                                margin-bottom: 16px !important;
                                                max-width: 100% !important;
                                                box-sizing: border-box !important;
                                            }
                                            .profile-card {
                                                display: flex !important;
                                                flex-wrap: wrap !important;
                                                align-items: center !important;
                                                background: linear-gradient(135deg, #002B5B 0%, #004085 100%) !important;
                                                border-radius: 16px !important;
                                                padding: 18px 20px !important;
                                                margin-bottom: 20px !important;
                                                color: #FFFFFF !important;
                                                box-shadow: 0 4px 12px rgba(0, 43, 91, 0.15) !important;
                                                max-width: 100% !important;
                                                box-sizing: border-box !important;
                                            }
                                            .profile-photo, #profileInitial {
                                                width: 56px !important;
                                                height: 56px !important;
                                                background-color: #F59E0B !important;
                                                color: #002B5B !important;
                                                font-size: 22px !important;
                                                font-weight: 800 !important;
                                                border-radius: 50% !important;
                                                display: flex !important;
                                                align-items: center !important;
                                                justify-content: center !important;
                                                margin-right: 16px !important;
                                                border: 2px solid #FFFFFF !important;
                                                flex-shrink: 0 !important;
                                            }
                                            .profile-details {
                                                flex: 1 !important;
                                                min-width: 200px !important;
                                                word-break: break-word !important;
                                                box-sizing: border-box !important;
                                            }
                                            .profile-details h2, #profileName {
                                                font-size: 18px !important;
                                                font-weight: bold !important;
                                                color: #FFFFFF !important;
                                                margin: 0 0 2px 0 !important;
                                            }
                                            .profile-details p, #profileMobile {
                                                font-size: 13px !important;
                                                color: rgba(255, 255, 255, 0.85) !important;
                                                margin: 0 !important;
                                            }
                                            .section {
                                                background: #FFFFFF !important;
                                                border-radius: 12px !important;
                                                padding: 14px 16px !important;
                                                margin-bottom: 16px !important;
                                                box-shadow: 0 1px 3px rgba(0,0,0,0.05) !important;
                                                border: 1px solid #E2E8F0 !important;
                                                max-width: 100% !important;
                                                box-sizing: border-box !important;
                                                overflow-x: hidden !important;
                                            }
                                            .section h2 {
                                                font-size: 14px !important;
                                                font-weight: 800 !important;
                                                color: #002B5B !important;
                                                margin: 0 0 12px 0 !important;
                                                border-bottom: 2px solid #F1F5F9 !important;
                                                padding-bottom: 6px !important;
                                            }
                                            .table-wrap {
                                                overflow-x: auto !important;
                                                width: 100% !important;
                                                box-sizing: border-box !important;
                                                margin: 0 !important;
                                                padding: 0 !important;
                                                -webkit-overflow-scrolling: touch !important;
                                            }
                                            table {
                                                width: 100% !important;
                                                border-collapse: collapse !important;
                                                table-layout: fixed !important;
                                            }
                                            th {
                                                background-color: #F8FAFC !important;
                                                color: #64748B !important;
                                                font-size: 10px !important;
                                                text-transform: uppercase !important;
                                                font-weight: bold !important;
                                                padding: 8px !important;
                                                text-align: left !important;
                                                border-bottom: 1px solid #E2E8F0 !important;
                                                word-wrap: break-word !important;
                                                word-break: break-word !important;
                                            }
                                            td {
                                                padding: 10px 8px !important;
                                                font-size: 11px !important;
                                                color: #334155 !important;
                                                border-bottom: 1px solid #F1F5F9 !important;
                                                vertical-align: middle !important;
                                                word-wrap: break-word !important;
                                                word-break: break-word !important;
                                            }
                                            tr:last-child td {
                                                border-bottom: none !important;
                                            }
                                            .badge {
                                                padding: 3px 6px !important;
                                                border-radius: 10px !important;
                                                font-size: 9px !important;
                                                font-weight: bold !important;
                                            }
                                            .badge.paid {
                                                background-color: #D1FAE5 !important;
                                                color: #065F46 !important;
                                            }
                                            .button-primary, button.download-btn, a.download-btn {
                                                background-color: #002B5B !important;
                                                color: #FFFFFF !important;
                                                border-radius: 6px !important;
                                                padding: 6px 10px !important;
                                                font-size: 10px !important;
                                                font-weight: bold !important;
                                                border: none !important;
                                                cursor: pointer !important;
                                                text-decoration: none !important;
                                                display: inline-flex !important;
                                                align-items: center !important;
                                                gap: 4px !important;
                                            }
                                            .empty-state {
                                                padding: 16px !important;
                                                text-align: center !important;
                                                color: #94A3B8 !important;
                                                font-size: 12px !important;
                                            }
                                            #loginOverlay, .nav-login, .login-btn {
                                                display: none !important;
                                            }
                                            
                                            /* Mobile Responsive Conversions for tables to Cards */
                                            @media screen and (max-width: 600px) {
                                                table, thead, tbody, th, td, tr {
                                                    display: block !important;
                                                    width: 100% !important;
                                                }
                                                thead {
                                                    display: none !important;
                                                }
                                                tr {
                                                    margin-bottom: 12px !important;
                                                    border: 1px solid #E2E8F0 !important;
                                                    border-radius: 8px !important;
                                                    padding: 8px !important;
                                                    background: #FFFFFF !important;
                                                    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
                                                }
                                                td {
                                                    text-align: right !important;
                                                    padding: 8px 6px !important;
                                                    border-bottom: 1px solid #F1F5F9 !important;
                                                    display: flex !important;
                                                    justify-content: space-between !important;
                                                    align-items: center !important;
                                                    min-height: 36px !important;
                                                }
                                                td:last-child {
                                                    border-bottom: none !important;
                                                }
                                                td::before {
                                                    content: attr(data-label) !important;
                                                    float: left !important;
                                                    font-weight: bold !important;
                                                    color: #64748B !important;
                                                    font-size: 11px !important;
                                                }
                                            }
                                        `;
                                        document.head.appendChild(style);

                                        // Dynamically label table cells and add bilingual actions for Purchased Categories and Completed Mock Tests
                                        const tables = document.querySelectorAll('table');
                                        tables.forEach((table, tableIdx) => {
                                            const headers = Array.from(table.querySelectorAll('th')).map(th => th.textContent.trim());
                                            const rows = table.querySelectorAll('tbody tr');
                                            rows.forEach(row => {
                                                const cells = row.querySelectorAll('td');
                                                cells.forEach((cell, idx) => {
                                                    if (headers[idx]) {
                                                        cell.setAttribute('data-label', headers[idx]);
                                                    }
                                                });
                                                
                                                const heading = table.previousElementSibling?.textContent || '';
                                                const isPurchased = heading.toLowerCase().includes('package') || heading.toLowerCase().includes('purchased') || heading.includes('पैकेज') || heading.includes('श्रेणी') || tableIdx === 0;
                                                const isHistory = heading.toLowerCase().includes('history') || heading.toLowerCase().includes('completed') || heading.includes('इतिहास') || heading.includes('परिणाम') || tableIdx === 1;

                                                const actions = row.querySelectorAll('a, button');
                                                actions.forEach(action => {
                                                    const text = action.textContent.toLowerCase();
                                                    if (isPurchased) {
                                                        if (text.includes('view') || text.includes('start') || text.includes('attempt') || text.includes('देखें') || text.includes('शुरू')) {
                                                            action.innerHTML = 'View Test / टेस्ट देखें ◀';
                                                            action.classList.add('action-btn');
                                                        }
                                                    } else if (isHistory) {
                                                        if (text.includes('result') || text.includes('marks') || text.includes('score') || text.includes('pdf') || text.includes('डाउनलोड') || text.includes('अंक')) {
                                                            action.innerHTML = 'View Result / परिणाम देखें 📊';
                                                            action.classList.add('action-btn');
                                                        }
                                                    }
                                                });
                                            });
                                        });

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
                                val storageScript = """
                                    try {
                                        const sessionStr = JSON.stringify({studentName: "$cleanName", studentMobile: "$cleanMobile"});
                                        const existing = localStorage.getItem('mockExamSession');
                                        if (!existing || existing !== sessionStr) {
                                            localStorage.setItem('mockExamSession', sessionStr);
                                            sessionStorage.setItem('mockExamSession', sessionStr);
                                            location.reload();
                                        }
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

        }

        // Native M3 Logout button at the very bottom
        Button(
            onClick = {
                (context as? android.app.Activity)?.runOnUiThread {
                    onLogout()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (appLanguage == "hi") "लॉगआउट करें (Logout)" else "Logout",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }

        if (isLoading) {
            ProfileShimmerLoadingPlaceholder()
        }
    }
}

@Composable
fun ProfileShimmerLoadingPlaceholder() {
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
