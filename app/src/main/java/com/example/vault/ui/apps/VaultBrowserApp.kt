package com.example.vault.ui.apps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VaultBrowserApp(
    onBack: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val defaultHomeUrl = "https://www.google.com"

    var webView by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember { mutableStateOf(defaultHomeUrl) }
    var currentUrl by remember { mutableStateOf(defaultHomeUrl) }
    var pageTitle by remember { mutableStateOf("Google") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    fun navigateTo(queryOrUrl: String) {
        val trimmed = queryOrUrl.trim()
        if (trimmed.isEmpty()) return

        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            // Google as default search engine
            else -> "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }
        urlInput = finalUrl
        webView?.loadUrl(finalUrl)
    }

    // Hardware/gesture back handler: go back in page history if available, else exit browser app
    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView?.stopLoading()
                webView?.clearHistory()
                webView?.clearCache(true)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Navigation & URL Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Search / URL field with Google default
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Encrypted Connection",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF38BDF8)
                            )
                        } else if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search Google or type URL",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            focusManager.clearFocus()
                            navigateTo(urlInput)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                IconButton(onClick = {
                    focusManager.clearFocus()
                    navigateTo(urlInput)
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Google Search",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }

            // Quick Bookmark pills (Google first)
            val bookmarks = listOf(
                "Google" to "https://www.google.com",
                "Google News" to "https://news.google.com",
                "Wikipedia" to "https://www.wikipedia.org",
                "Reddit" to "https://www.reddit.com",
                "GitHub" to "https://github.com",
                "DuckDuckGo" to "https://duckduckgo.com"
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks) { (title, link) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF334155))
                            .clickable {
                                focusManager.clearFocus()
                                navigateTo(link)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Loading Progress bar
            if (isLoading && progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }

            // High Performance WebView
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        try {
                            val codeCache = java.io.File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                            java.io.File(codeCache, "js").mkdirs()
                            java.io.File(codeCache, "wasm").mkdirs()
                        } catch (_: Throwable) {}

                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                allowFileAccess = true
                                allowContentAccess = true
                                mediaPlaybackRequiresUserGesture = false
                            }

                            // Download handler
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                                try {
                                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(url)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    ctx.startActivity(intent)
                                    showToast("Opening download for $fileName")
                                } catch (_: Exception) {
                                    showToast("Cannot open download link")
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val uri = request?.url ?: return false
                                    val scheme = uri.scheme?.lowercase() ?: ""
                                    if (scheme == "http" || scheme == "https") {
                                        return false
                                    }
                                    // Handle custom schemes like mailto:, tel:, intent: safely
                                    return try {
                                        val externalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(externalIntent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        urlInput = it
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    pageTitle = view?.title ?: "Google"
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progress = newProgress / 100f
                                    if (newProgress == 100) isLoading = false
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    title?.let { pageTitle = it }
                                }
                            }

                            loadUrl(currentUrl)
                            webView = this
                        }
                    },
                    onRelease = { view ->
                        view.stopLoading()
                        view.destroy()
                    }
                )
            }

            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back in web history
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) Color.White else Color(0xFF475569)
                    )
                }

                // Forward in web history
                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) Color.White else Color(0xFF475569)
                    )
                }

                // Refresh Page
                IconButton(onClick = { webView?.reload() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = Color.White
                    )
                }

                // Home button (Google)
                IconButton(onClick = { navigateTo(defaultHomeUrl) }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Google Home",
                        tint = Color(0xFF38BDF8)
                    )
                }

                // Desktop / Mobile site toggle
                IconButton(
                    onClick = {
                        val wv = webView ?: return@IconButton
                        isDesktopMode = !isDesktopMode
                        val settings = wv.settings
                        if (isDesktopMode) {
                            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            showToast("Desktop mode enabled")
                        } else {
                            settings.userAgentString = null
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            showToast("Mobile mode enabled")
                        }
                        wv.reload()
                    }
                ) {
                    Icon(
                        imageVector = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.DesktopWindows,
                        contentDescription = if (isDesktopMode) "Switch to Mobile View" else "Switch to Desktop View",
                        tint = if (isDesktopMode) Color(0xFFF59E0B) else Color.White
                    )
                }

                // Share URL
                IconButton(
                    onClick = {
                        val current = webView?.url ?: currentUrl
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, current)
                            putExtra(Intent.EXTRA_TITLE, pageTitle)
                        }
                        val chooser = Intent.createChooser(sendIntent, "Share Link")
                        context.startActivity(chooser)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                // Clear private session
                IconButton(
                    onClick = {
                        try {
                            webView?.clearCache(true)
                            webView?.clearHistory()
                            CookieManager.getInstance().removeAllCookies(null)
                            showToast("Private cookies and session cache cleared")
                        } catch (_: Exception) {}
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Cache & Cookies",
                        tint = Color(0xFF34D399)
                    )
                }
            }
        }
    }
}
