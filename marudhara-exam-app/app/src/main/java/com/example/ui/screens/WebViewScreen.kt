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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
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

class AndroidExamInterface(
    private val onTimeUpdated: (String) -> Unit,
    private val onLanguageUpdated: (String) -> Unit
) {
    @JavascriptInterface
    fun updateTime(time: String) {
        onTimeUpdated(time)
    }

    @JavascriptInterface
    fun updateLanguage(langCode: String) {
        onLanguageUpdated(langCode)
    }
}

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
    var remainingTime by remember { mutableStateOf("00:00") }
    var currentLanguage by remember { mutableStateOf("hi") }

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
                hasError = false
                webView?.reload()
            }
        }
    }

    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            if (currentUrl.contains("exam.html")) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Mock Exam / मॉक टेस्ट",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Dynamic remaining time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Timer",
                                    tint = Color(0xFF002B5B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = remainingTime,
                                    color = Color(0xFF002B5B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Language switcher dropdown
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (currentLanguage == "en") "English" else "हिंदी",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("English") },
                                        onClick = {
                                            expanded = false
                                            webView?.evaluateJavascript(
                                                "document.getElementById('languageSelect').value = 'en'; document.getElementById('languageSelect').dispatchEvent(new Event('change'));",
                                                null
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("हिंदी") },
                                        onClick = {
                                            expanded = false
                                            webView?.evaluateJavascript(
                                                "document.getElementById('languageSelect').value = 'hi'; document.getElementById('languageSelect').dispatchEvent(new Event('change'));",
                                                null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Exam"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF002B5B),
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        content = { padding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (!isOnline) {
                    OfflineScreen(
                        onRetry = {
                            hasError = false
                            webView?.reload()
                        }
                    )
                } else if (hasError) {
                    OfflineScreen(
                        onRetry = {
                            hasError = false
                            webView?.reload()
                        }
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }

                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                // Download file listener
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun downloadFile(urlStr: String, fileName: String) {
                                            try {
                                                val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                val request = DownloadManager.Request(Uri.parse(urlStr)).apply {
                                                    setTitle(fileName)
                                                    setDescription("प्रश्नोत्तरी सामग्री डाउनलोड हो रही है...")
                                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                                    setAllowedOverMetered(true)
                                                    setAllowedOverRoaming(true)
                                                }
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
                                    AndroidExamInterface(
                                        onTimeUpdated = { time ->
                                            (ctx as? android.app.Activity)?.runOnUiThread {
                                                remainingTime = time
                                            }
                                        },
                                        onLanguageUpdated = { lang ->
                                            (ctx as? android.app.Activity)?.runOnUiThread {
                                                currentLanguage = lang
                                            }
                                        }
                                    ),
                                    "AndroidExamInterface"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val requestUrl = request?.url?.toString() ?: ""
                                        
                                        if (requestUrl.endsWith(".pdf", ignoreCase = true) || 
                                            requestUrl.contains("/pdfs/", ignoreCase = true) || 
                                            requestUrl.contains(".pdf?", ignoreCase = true)) {
                                            onOpenPdf("दस्तावेज़", requestUrl)
                                            return true
                                        }

                                        if (requestUrl.contains("/mock-tests/account.html", ignoreCase = true)) {
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, urlStr: String?) {
                                        super.onPageFinished(view, urlStr)
                                        val currentUrl = urlStr ?: ""
                                        
                                        if (studentMobile != null && savedPassword != null) {
                                            val cleanMobile = studentMobile!!.replace("+91", "").trim()
                                            val pass = savedPassword!!
                                            
                                            val sessionSyncScript = """
                                                try {
                                                    const style = document.createElement('style');
                                                    style.innerHTML = `
                                                        #loginOverlay, #logoutBtn, #myAccountLink, .nav-login, .login-btn, .logout-btn, a[href*="account.html"] {
                                                            display: none !important;
                                                        }
                                                    `;
                                                    document.head.appendChild(style);

                                                    const overlay = document.getElementById('loginOverlay');
                                                    if (overlay) overlay.style.display = 'none';
                                                    const logout = document.getElementById('logoutBtn');
                                                    if (logout) logout.style.display = 'none';
                                                    const account = document.getElementById('myAccountLink');
                                                    if (account) account.style.display = 'none';

                                                    if (!localStorage.getItem('mockExamSession')) {
                                                        try {
                                                            const mobileInput = document.getElementById('loginMobile');
                                                            const passwordInput = document.getElementById('loginPassword');
                                                            const loginButton = document.getElementById('loginBtn');
                                                            if (mobileInput && passwordInput && loginButton) {
                                                                mobileInput.value = '$cleanMobile';
                                                                passwordInput.value = '$pass';
                                                                loginButton.click();
                                                                setTimeout(function() {
                                                                    location.reload();
                                                                }, 300);
                                                            }
                                                        } catch (err) {
                                                            console.error("Auto login credentials inject error", err);
                                                        }
                                                    }
                                                } catch (e) {
                                                    console.error("Session sync error", e);
                                                }
                                            """.trimIndent()
                                            view?.evaluateJavascript(sessionSyncScript, null)
                                        }

                                        if (currentUrl.contains("exam.html")) {
                                            val examInterfaceScript = """
                                                (function() {
                                                    if (document.getElementById('native-action-bar')) return;

                                                    function syncTimeToApp() {
                                                        try {
                                                            const timerEl = document.querySelector('.timer-box') || document.querySelector('.timer') || document.getElementById('timer');
                                                            if (timerEl && window.AndroidExamInterface) {
                                                                window.AndroidExamInterface.updateTime(timerEl.textContent.trim());
                                                            }
                                                            const langSelect = document.getElementById('languageSelect');
                                                            if (langSelect && window.AndroidExamInterface) {
                                                                window.AndroidExamInterface.updateLanguage(langSelect.value.trim());
                                                            }
                                                        } catch (e) {
                                                            console.error("Sync to app error", e);
                                                        }
                                                    }
                                                    setInterval(syncTimeToApp, 1000);

                                                    const style = document.createElement('style');
                                                    style.innerHTML = `
                                                        html, body {
                                                            height: 100vh !important;
                                                            overflow: hidden !important;
                                                            margin: 0 !important;
                                                            padding: 0 !important;
                                                            background-color: #F8FAFC !important;
                                                            font-family: system-ui, -apple-system, sans-serif !important;
                                                        }
                                                        .page-shell {
                                                            height: 100vh !important;
                                                            display: flex !important;
                                                            flex-direction: column !important;
                                                            padding: 0 !important;
                                                            margin: 0 !important;
                                                            max-width: 100% !important;
                                                            box-sizing: border-box !important;
                                                        }
                                                        .top-bar {
                                                            display: none !important; /* Hide original top bar since native has it */
                                                        }
                                                        #mockMeta, .nav-buttons, .submit-card {
                                                            display: none !important;
                                                        }
                                                        .exam-grid {
                                                            display: flex !important;
                                                            flex-direction: column !important;
                                                            flex: 1 !important;
                                                            padding: 8px !important;
                                                            gap: 8px !important;
                                                            overflow: hidden !important;
                                                            box-sizing: border-box !important;
                                                            height: calc(100vh - 44px - 96px) !important;
                                                        }
                                                        .question-card {
                                                            background: #FFFFFF !important;
                                                            border: 1px solid #E2E8F0 !important;
                                                            border-radius: 12px !important;
                                                            padding: 12px !important;
                                                            margin: 0 !important;
                                                            display: flex !important;
                                                            flex-direction: column !important;
                                                            box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
                                                            overflow: hidden !important;
                                                            flex: 1 !important;
                                                        }
                                                        .question-card h2 {
                                                            font-size: 14px !important;
                                                            color: #002B5B !important;
                                                            font-weight: 800 !important;
                                                            margin: 0 0 8px 0 !important;
                                                            border-bottom: 2px solid #F1F5F9 !important;
                                                            padding-bottom: 6px !important;
                                                        }
                                                        .question-text {
                                                            font-size: 13.5px !important;
                                                            line-height: 1.5 !important;
                                                            margin: 0 !important;
                                                            font-weight: 600 !important;
                                                            color: #1E293B !important;
                                                            overflow-y: auto !important;
                                                            flex: 1 !important;
                                                            padding-right: 4px !important;
                                                        }
                                                        #optionsForm {
                                                            margin-top: 8px !important;
                                                        }
                                                        .options-list {
                                                            display: flex !important;
                                                            flex-direction: column !important;
                                                            gap: 8px !important;
                                                            margin: 0 !important;
                                                            padding: 0 !important;
                                                        }
                                                        .options-list label {
                                                            display: flex !important;
                                                            align-items: center !important;
                                                            background-color: #FFFFFF !important;
                                                            border: 1.5px solid #E2E8F0 !important;
                                                            border-radius: 10px !important;
                                                            padding: 10px 12px !important;
                                                            margin: 0 !important;
                                                            cursor: pointer !important;
                                                            font-size: 13px !important;
                                                            line-height: 1.35 !important;
                                                            transition: all 0.1s ease !important;
                                                            color: #334155 !important;
                                                            box-shadow: 0 1px 2px rgba(0,0,0,0.02) !important;
                                                        }
                                                        .options-list label:hover {
                                                            background-color: #F8FAFC !important;
                                                        }
                                                        .options-list input[type="radio"] {
                                                            margin-right: 10px !important;
                                                            transform: scale(1.15) !important;
                                                        }
                                                        .options-list label.checked-option {
                                                            background-color: #EFF6FF !important;
                                                            border-color: #3B82F6 !important;
                                                            color: #1D4ED8 !important;
                                                            font-weight: bold !important;
                                                            box-shadow: 0 2px 6px rgba(59, 130, 246, 0.12) !important;
                                                        }
                                                        .palette-btn.marked-for-review {
                                                            background-color: #F59E0B !important;
                                                            color: #FFFFFF !important;
                                                            border-color: #D97706 !important;
                                                        }
                                                        .palette-card {
                                                            background: #FFFFFF !important;
                                                            border: 1px solid #E2E8F0 !important;
                                                            border-radius: 10px !important;
                                                            padding: 6px 10px !important;
                                                            margin: 0 8px !important;
                                                            box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
                                                            display: flex !important;
                                                            flex-direction: row !important;
                                                            align-items: center !important;
                                                            gap: 8px !important;
                                                            height: 44px !important;
                                                            box-sizing: border-box !important;
                                                            overflow: hidden !important;
                                                        }
                                                        .palette-card h3 {
                                                            display: none !important;
                                                        }
                                                        .palette-grid {
                                                            display: flex !important;
                                                            flex-direction: row !important;
                                                            gap: 5px !important;
                                                            overflow-x: auto !important;
                                                            flex: 1 !important;
                                                            padding: 2px 0 !important;
                                                            margin: 0 !important;
                                                        }
                                                        .palette-grid button, .palette-grid .palette-btn {
                                                            min-width: 28px !important;
                                                            height: 28px !important;
                                                            border-radius: 50% !important;
                                                            font-size: 11px !important;
                                                            font-weight: bold !important;
                                                            display: flex !important;
                                                            align-items: center !important;
                                                            justify-content: center !important;
                                                            cursor: pointer !important;
                                                            border: 1px solid #CBD5E1 !important;
                                                            background: #F1F5F9 !important;
                                                            color: #475569 !important;
                                                            padding: 0 !important;
                                                            margin: 0 !important;
                                                        }
                                                        .palette-grid button.answered, .palette-grid .palette-btn.answered {
                                                            background-color: #10B981 !important;
                                                            color: #FFFFFF !important;
                                                            border-color: #059669 !important;
                                                        }
                                                        .palette-grid button.current, .palette-grid .palette-btn.current {
                                                            border: 2px solid #002B5B !important;
                                                            background-color: #DBEAFE !important;
                                                            color: #002B5B !important;
                                                        }
                                                        #native-action-bar {
                                                            display: flex !important;
                                                            flex-direction: column !important;
                                                            background: #FFFFFF !important;
                                                            border-top: 1px solid #E2E8F0 !important;
                                                            padding: 8px !important;
                                                            gap: 8px !important;
                                                            height: 96px !important;
                                                            box-sizing: border-box !important;
                                                            width: 100% !important;
                                                            position: fixed !important;
                                                            bottom: 0 !important;
                                                            left: 0 !important;
                                                            z-index: 99999 !important;
                                                        }
                                                        .native-row {
                                                            display: flex !important;
                                                            flex-direction: row !important;
                                                            gap: 8px !important;
                                                            width: 100% !important;
                                                        }
                                                        .native-btn {
                                                            flex: 1 !important;
                                                            height: 36px !important;
                                                            border-radius: 8px !important;
                                                            font-size: 11px !important;
                                                            font-weight: bold !important;
                                                            border: none !important;
                                                            cursor: pointer !important;
                                                            display: flex !important;
                                                            align-items: center !important;
                                                            justify-content: center !important;
                                                            transition: all 0.1s ease !important;
                                                            white-space: nowrap !important;
                                                        }
                                                        .primary-btn {
                                                            background-color: #002B5B !important;
                                                            color: #FFFFFF !important;
                                                        }
                                                        .secondary-btn {
                                                            background-color: #E2E8F0 !important;
                                                            color: #334155 !important;
                                                        }
                                                        .warning-btn {
                                                            background-color: #F1F5F9 !important;
                                                            color: #475569 !important;
                                                            border: 1px solid #CBD5E1 !important;
                                                        }
                                                        .review-btn {
                                                            background-color: #FEF3C7 !important;
                                                            color: #D97706 !important;
                                                            border: 1px solid #F59E0B !important;
                                                        }
                                                        .danger-btn {
                                                            background-color: #FEE2E2 !important;
                                                            color: #DC2626 !important;
                                                            border: 1px solid #EF4444 !important;
                                                        }
                                                    `;
                                                    document.head.appendChild(style);

                                                    let markedQuestions = JSON.parse(localStorage.getItem('markedQuestions') || '{}');

                                                    const actionBar = document.createElement('div');
                                                    actionBar.id = 'native-action-bar';
                                                    actionBar.innerHTML = `
                                                        <div class="native-row">
                                                            <button id="native-clear-btn" class="native-btn warning-btn">🗑️ Clear (हटाएं)</button>
                                                            <button id="native-review-btn" class="native-btn review-btn">⭐ Review (समीक्षा)</button>
                                                        </div>
                                                        <div class="native-row">
                                                            <button id="native-prev-btn" class="native-btn secondary-btn">◀ Prev (पिछला)</button>
                                                            <button id="native-next-btn" class="native-btn primary-btn">Next (अगला) ▶</button>
                                                            <button id="native-submit-btn" class="native-btn danger-btn">📤 Submit (सबमिट)</button>
                                                        </div>
                                                    `;
                                                    
                                                    const pageShell = document.querySelector('.page-shell');
                                                    if (pageShell) {
                                                        pageShell.appendChild(actionBar);
                                                    } else {
                                                        document.body.appendChild(actionBar);
                                                    }

                                                    function clickTarget(selectors, keywords) {
                                                        let target = null;
                                                        for (const sel of selectors) {
                                                            const el = document.getElementById(sel) || document.querySelector(sel);
                                                            if (el) {
                                                                target = el;
                                                                break;
                                                            }
                                                        }
                                                        if (!target) {
                                                            const buttons = document.querySelectorAll('button, input[type="button"], a.btn, .btn');
                                                            for (const btn of buttons) {
                                                                const text = btn.textContent.toLowerCase();
                                                                for (const kw of keywords) {
                                                                    if (text.includes(kw)) {
                                                                        target = btn;
                                                                        break;
                                                                    }
                                                                }
                                                                if (target) break;
                                                            }
                                                        }
                                                        if (target && !target.disabled) {
                                                            target.click();
                                                            setTimeout(updateUIRefresh, 150);
                                                        }
                                                    }

                                                    document.getElementById('native-prev-btn').addEventListener('click', function() {
                                                        clickTarget(['prevBtn', 'prevQuestion', 'previous'], ['prev', 'previous', 'पिछला']);
                                                    });

                                                    document.getElementById('native-next-btn').addEventListener('click', function() {
                                                        clickTarget(['nextBtn', 'nextQuestion', 'next'], ['next', 'अगला']);
                                                    });

                                                    document.getElementById('native-submit-btn').addEventListener('click', function() {
                                                        clickTarget(['submitBtn', 'submitTest', 'submit'], ['submit', 'सबमिट', 'पूरा करें']);
                                                    });

                                                    document.getElementById('native-clear-btn').addEventListener('click', function() {
                                                        clickTarget(['clearBtn', 'clearResponse', 'resetBtn'], ['clear', 'reset', 'साफ़', 'हटाएं']);
                                                    });

                                                    document.getElementById('native-review-btn').addEventListener('click', function() {
                                                        clickTarget(['markBtn', 'markReviewBtn', 'reviewBtn'], ['review', 'mark', 'मार्क', 'समीक्षा']);
                                                    });

                                                    function updateUIRefresh() {
                                                        try {
                                                            const labels = document.querySelectorAll('.options-list label');
                                                            labels.forEach(lbl => {
                                                                const radio = lbl.querySelector('input[type="radio"]');
                                                                if (radio && radio.checked) {
                                                                    lbl.classList.add('checked-option');
                                                                } else {
                                                                    lbl.classList.remove('checked-option');
                                                                }
                                                            });

                                                            const paletteGrid = document.getElementById('palette');
                                                            if (paletteGrid) {
                                                                const btns = paletteGrid.querySelectorAll('button, .palette-btn');
                                                                btns.forEach((btn, index) => {
                                                                    const qNum = index + 1;
                                                                    if (markedQuestions[qNum]) {
                                                                        btn.classList.add('marked-for-review');
                                                                    } else {
                                                                        btn.classList.remove('marked-for-review');
                                                                    }
                                                                    if (!btn.hasAttribute('data-native-wired')) {
                                                                        btn.setAttribute('data-native-wired', 'true');
                                                                        btn.addEventListener('click', function() {
                                                                            setTimeout(updateUIRefresh, 200);
                                                                        });
                                                                    }
                                                                });
                                                            }

                                                            const originalPrev = document.getElementById('prevBtn');
                                                            const originalNext = document.getElementById('nextBtn');
                                                            const nativePrev = document.getElementById('native-prev-btn');
                                                            const nativeNext = document.getElementById('native-next-btn');
                                                            
                                                            if (originalPrev && nativePrev) {
                                                                nativePrev.disabled = originalPrev.disabled;
                                                                nativePrev.style.opacity = originalPrev.disabled ? '0.4' : '1';
                                                            }
                                                            if (originalNext && nativeNext) {
                                                                nativeNext.disabled = originalNext.disabled;
                                                                nativeNext.style.opacity = originalNext.disabled ? '0.4' : '1';
                                                            }
                                                        } catch (e) {
                                                            console.error('UI Refresh error', e);
                                                        }
                                                    }

                                                    const observer = new MutationObserver(function() {
                                                        updateUIRefresh();
                                                    });
                                                    const target = document.getElementById('examContent');
                                                    if (target) {
                                                        observer.observe(target, { childList: true, subtree: true });
                                                    }
                                                    
                                                    setTimeout(updateUIRefresh, 300);
                                                    setInterval(updateUIRefresh, 1000);
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(examInterfaceScript, null)
                                        }

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
                        WebViewShimmerLoadingPlaceholder()
                    }
                }
            }
        }
    )
}

@Composable
fun WebViewShimmerLoadingPlaceholder() {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = alpha))
        )
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = alpha))
            )
        }
    }
}
