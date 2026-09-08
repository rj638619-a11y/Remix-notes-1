package com.example.vault.ui.apps

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.ImageCompressor
import com.example.vault.data.VaultRepository
import com.example.vault.model.VaultItem
import com.example.vault.model.VaultItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class BrowserTab(
    val id: String,
    var title: String = "New Tab",
    var url: String = "https://www.google.com",
    var isDesktop: Boolean = false
)

private data class HistoryEntry(
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

private data class SniffedMedia(
    val type: String, // "video" or "photo"
    val url: String,
    val name: String
)

private val KNOWN_AD_DOMAINS = setOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
    "adnxs.com", "criteo.com", "taboola.com", "outbrain.com", "popads.net", "adcolony.com",
    "unityads.unity3d.com", "applovin.com", "scorecardresearch.com", "advertising.com",
    "amazon-adsystem.com", "rubiconproject.com", "pubmatic.com", "openx.net", "inmobi.com",
    "moatads.com", "adskeeper.com", "mgid.com", "smartadserver.com", "casalemedia.com",
    "adform.net", "serving-sys.com", "quantserve.com", "zedo.com", "bidswitch.net", "adroll.com"
)

private fun isAdRequest(url: String): Boolean {
    val lower = url.lowercase()
    val host = try { Uri.parse(url).host?.lowercase() ?: "" } catch (_: Exception) { "" }
    if (KNOWN_AD_DOMAINS.any { host == it || host.endsWith(".$it") }) return true
    if (lower.contains("/pagead/") || lower.contains("/ads.js") || lower.contains("google-analytics.com/analytics.js") ||
        lower.contains("facebook.net/en_us/fbevents.js") || lower.contains("/adview?") || lower.contains("&ad_type=") ||
        lower.contains("/banners/") || lower.contains("googletagservices.com/tag/js/gpt.js")
    ) {
        return true
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VaultBrowserApp(
    repository: VaultRepository,
    onBack: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val defaultHomeUrl = "https://www.google.com"

    // Multi-tab state
    val tabs = remember {
        mutableStateListOf(
            BrowserTab(id = UUID.randomUUID().toString(), title = "Google", url = defaultHomeUrl)
        )
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var lastActiveTabId by remember { mutableStateOf(activeTabId) }
    val currentTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // Browser navigation & page state
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    var urlInput by remember { mutableStateOf(currentTab.url) }
    var currentUrl by remember { mutableStateOf(currentTab.url) }
    var pageTitle by remember { mutableStateOf(currentTab.title) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(currentTab.isDesktop) }

    // Ad Blocker state
    var adBlockerEnabled by remember { mutableStateOf(true) }
    var blockedAdsCount by remember { mutableIntStateOf(0) }

    // Chrome Menu state
    var showMenu by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showMediaDetectorSheet by remember { mutableStateOf(false) }

    // Find in Page state
    var isFindInPageActive by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    // Long press context menu state
    var longPressTargetUrl by remember { mutableStateOf<String?>(null) }
    var longPressIsImage by remember { mutableStateOf(false) }
    var longPressLinkUrl by remember { mutableStateOf<String?>(null) }

    // Media sniffer results
    val detectedMedia = remember { mutableStateListOf<SniffedMedia>() }
    var isScanningMedia by remember { mutableStateOf(false) }

    // Bookmarks & History lists (in-memory & reactive)
    val bookmarks = remember {
        mutableStateListOf(
            "Google" to "https://www.google.com",
            "Google News" to "https://news.google.com",
            "Wikipedia" to "https://www.wikipedia.org",
            "Reddit" to "https://www.reddit.com",
            "DuckDuckGo" to "https://duckduckgo.com"
        )
    }
    val history = remember { mutableStateListOf<HistoryEntry>() }

    // Vault downloaded items
    val allVaultItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val downloadedItems = remember(allVaultItems) {
        allVaultItems.filter { it.relativePath.startsWith("photos/") || it.relativePath.startsWith("videos/") || it.relativePath.startsWith("documents/") }
    }

    // File upload launcher
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        fileChooserCallback?.onReceiveValue(uris.toTypedArray())
        fileChooserCallback = null
    }

    fun navigateTo(queryOrUrl: String) {
        val trimmed = queryOrUrl.trim()
        if (trimmed.isEmpty()) return

        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }
        urlInput = finalUrl
        currentUrl = finalUrl
        currentTab.url = finalUrl
        webView?.loadUrl(finalUrl)
    }

    // Isolated In-Vault Downloader with cookies, userAgent, referer and blob support
    fun downloadToVault(url: String, suggestedName: String? = null, mimeOverride: String? = null) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return

        if (trimmed.startsWith("blob:", ignoreCase = true)) {
            showToast("Extracting blob media...")
            val js = """
                (function() {
                    try {
                        fetch('$trimmed')
                            .then(function(res) { return res.blob(); })
                            .then(function(blob) {
                                var reader = new FileReader();
                                reader.onloadend = function() {
                                    if (window.VaultAndroid && reader.result) {
                                        window.VaultAndroid.saveBlobData(reader.result, '${suggestedName ?: "media_download"}');
                                    }
                                };
                                reader.readAsDataURL(blob);
                            })
                            .catch(function(err) {
                                console.error('Blob fetch error', err);
                            });
                    } catch (e) {
                        console.error(e);
                    }
                })()
            """.trimIndent()
            webView?.evaluateJavascript(js, null)
            return
        }

        coroutineScope.launch {
            showToast("Downloading to Vault...")
            val ua = webView?.settings?.userAgentString
            val cookies = try { CookieManager.getInstance().getCookie(trimmed) } catch (_: Exception) { null }
            val ref = currentUrl.ifBlank { null }

            val item = repository.downloadUrlToVault(
                url = trimmed,
                suggestedFileName = suggestedName,
                mimeTypeOverride = mimeOverride,
                userAgent = ua,
                cookies = cookies,
                referer = ref
            )
            if (item != null) {
                val folderName = when (item.type) {
                    VaultItemType.PHOTO -> "Vault Gallery"
                    VaultItemType.VIDEO -> "Vault Videos"
                    VaultItemType.AUDIO -> "Vault Audio"
                    else -> "Vault Files"
                }
                showToast("Saved to $folderName: ${item.name}")
            } else {
                showToast("Download failed. Check link or connection.")
            }
        }
    }

    // Sniff all videos, photos, audio and downloadable media currently on the web page
    fun scanPageMedia() {
        val wv = webView ?: return
        isScanningMedia = true

        val js = """
            (function() {
                var list = [];
                var seen = {};

                function add(type, u, n) {
                    if (!u || typeof u !== 'string') return;
                    u = u.trim();
                    if (!u || u.indexOf('javascript:') === 0 || u.indexOf('about:') === 0) return;
                    if (seen[u]) return;
                    seen[u] = true;
                    list.push({ type: type, url: u, name: (n || (type + '_' + list.length)).substring(0, 50) });
                }

                // 1. OpenGraph & Twitter Meta Tags
                var metas = document.getElementsByTagName('meta');
                for (var i = 0; i < metas.length; i++) {
                    var prop = metas[i].getAttribute('property') || metas[i].getAttribute('name') || '';
                    var content = metas[i].getAttribute('content') || '';
                    if (!content) continue;
                    if (prop.indexOf('og:video') !== -1 || prop.indexOf('twitter:player:stream') !== -1) {
                        add('video', content, 'Page Video');
                    } else if (prop.indexOf('og:image') !== -1 || prop.indexOf('twitter:image') !== -1) {
                        add('photo', content, 'Page Cover Image');
                    }
                }

                // 2. Video elements & sources
                var vids = document.getElementsByTagName('video');
                for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    var s = v.currentSrc || v.src;
                    if (s) add('video', s, 'Video_' + (i + 1));
                    if (v.poster) add('photo', v.poster, 'Poster_' + (i + 1));
                    var sources = v.getElementsByTagName('source');
                    for (var j = 0; j < sources.length; j++) {
                        if (sources[j].src) add('video', sources[j].src, 'Video_' + (i + 1) + '_' + (j + 1));
                    }
                }

                // 3. Audio elements & sources
                var auds = document.getElementsByTagName('audio');
                for (var i = 0; i < auds.length; i++) {
                    var a = auds[i];
                    var asrc = a.currentSrc || a.src;
                    if (asrc) add('audio', asrc, 'Audio_' + (i + 1));
                    var asources = a.getElementsByTagName('source');
                    for (var j = 0; j < asources.length; j++) {
                        if (asources[j].src) add('audio', asources[j].src, 'Audio_' + (i + 1) + '_' + (j + 1));
                    }
                }

                // 4. Image elements (with src, srcset, lazy loading attributes)
                var imgs = document.getElementsByTagName('img');
                for (var i = 0; i < Math.min(imgs.length, 60); i++) {
                    var img = imgs[i];
                    var isrc = img.currentSrc || img.src || img.getAttribute('data-src') || img.getAttribute('data-original') || img.getAttribute('data-lazy-src');
                    if (isrc && !isrc.startsWith('data:image/svg')) {
                        var alt = img.alt || img.title || ('Image_' + (i + 1));
                        add('photo', isrc, alt.trim());
                    }
                }

                // 5. Direct links to media
                var links = document.getElementsByTagName('a');
                for (var i = 0; i < Math.min(links.length, 100); i++) {
                    var href = links[i].href || '';
                    if (!href) continue;
                    var clean = href.split('?')[0].toLowerCase();
                    if (clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mkv') || clean.endsWith('.mov') || clean.endsWith('.m4v') || clean.endsWith('.ts')) {
                        add('video', href, links[i].innerText.trim() || ('Video_Link_' + (i + 1)));
                    } else if (clean.endsWith('.jpg') || clean.endsWith('.jpeg') || clean.endsWith('.png') || clean.endsWith('.webp') || clean.endsWith('.gif') || clean.endsWith('.heic') || clean.endsWith('.avif')) {
                        add('photo', href, links[i].innerText.trim() || ('Photo_Link_' + (i + 1)));
                    } else if (clean.endsWith('.mp3') || clean.endsWith('.m4a') || clean.endsWith('.wav') || clean.endsWith('.aac') || clean.endsWith('.flac') || clean.endsWith('.ogg')) {
                        add('audio', href, links[i].innerText.trim() || ('Audio_Link_' + (i + 1)));
                    }
                }

                return JSON.stringify(list);
            })()
        """.trimIndent()

        wv.evaluateJavascript(js) { result ->
            isScanningMedia = false
            try {
                if (!result.isNullOrBlank() && result != "null" && result != "\"\"") {
                    val unescaped = if (result.startsWith("\"") && result.endsWith("\"")) {
                        org.json.JSONTokener(result).nextValue().toString()
                    } else result
                    val arr = JSONArray(unescaped)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val mUrl = obj.getString("url")
                        if (mUrl.isNotBlank() && detectedMedia.none { it.url == mUrl }) {
                            detectedMedia.add(
                                SniffedMedia(
                                    type = obj.optString("type", "photo"),
                                    url = mUrl,
                                    name = obj.optString("name", "media_${i + 1}").ifBlank { "media_${i + 1}" }
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            showMediaDetectorSheet = true
        }
    }

    // Hardware/gesture back handler
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else if (isFindInPageActive) {
            isFindInPageActive = false
            webView?.clearMatches()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else if (tabs.size > 1) {
            // Close active tab and switch to previous
            val idx = tabs.indexOfFirst { it.id == activeTabId }
            if (idx != -1) {
                tabs.removeAt(idx)
                activeTabId = tabs.last().id
            }
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
            // -------------------------------------------------------------
            // TOP BAR: Navigation, URL Input, AdBlocker Shield, Tabs, Menu
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Exit Browser button
                IconButton(
                    onClick = {
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Chrome-style Omnibox (Search / URL input)
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Shield,
                            contentDescription = "SSL Secured",
                            tint = if (currentUrl.startsWith("https://")) Color(0xFF34D399) else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlInput = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search or enter URL",
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
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )

                // Ad Blocker Shield indicator
                IconButton(
                    onClick = {
                        adBlockerEnabled = !adBlockerEnabled
                        showToast(if (adBlockerEnabled) "Ad Blocker Enabled ($blockedAdsCount blocked)" else "Ad Blocker Disabled")
                        webView?.reload()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (adBlockerEnabled && blockedAdsCount > 0) {
                                Badge(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (blockedAdsCount > 99) "99+" else "$blockedAdsCount",
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Ad Blocker",
                            tint = if (adBlockerEnabled) Color(0xFF34D399) else Color(0xFF64748B),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Chrome Tabs count button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.5.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp))
                        .clickable { showTabsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tabs.size}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3-Dots Chrome Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Tab", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                showMenu = false
                                val newTab = BrowserTab(
                                    id = UUID.randomUUID().toString(),
                                    title = "Google",
                                    url = defaultHomeUrl
                                )
                                tabs.add(newTab)
                                activeTabId = newTab.id
                                urlInput = defaultHomeUrl
                                currentUrl = defaultHomeUrl
                                webView?.loadUrl(defaultHomeUrl)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Download Media on Page", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                showMenu = false
                                scanPageMedia()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Find in Page", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            onClick = {
                                showMenu = false
                                isFindInPageActive = true
                            }
                        )

                        HorizontalDivider(color = Color(0xFF334155))

                        // Ad Blocker Toggle
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ad Blocker", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = adBlockerEnabled,
                                        onCheckedChange = {
                                            adBlockerEnabled = it
                                            showToast(if (it) "Ad Blocker enabled" else "Ad Blocker disabled")
                                            webView?.reload()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = if (adBlockerEnabled) Color(0xFF10B981) else Color(0xFF64748B)) },
                            onClick = {
                                adBlockerEnabled = !adBlockerEnabled
                                webView?.reload()
                            }
                        )

                        // Desktop Site Toggle
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Desktop Site", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isDesktopMode,
                                        onCheckedChange = {
                                            isDesktopMode = it
                                            currentTab.isDesktop = it
                                            val wv = webView ?: return@Switch
                                            wv.settings.userAgentString = if (it) {
                                                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                            } else null
                                            wv.reload()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            onClick = {
                                isDesktopMode = !isDesktopMode
                                currentTab.isDesktop = isDesktopMode
                                val wv = webView ?: return@DropdownMenuItem
                                wv.settings.userAgentString = if (isDesktopMode) {
                                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                } else null
                                wv.reload()
                            }
                        )

                        HorizontalDivider(color = Color(0xFF334155))

                        DropdownMenuItem(
                            text = { Text("Bookmarks", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            onClick = {
                                showMenu = false
                                showBookmarksSheet = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("History", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            onClick = {
                                showMenu = false
                                showHistorySheet = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Vault Downloads", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF34D399)) },
                            onClick = {
                                showMenu = false
                                showDownloadsSheet = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Clear Cookies & Cache", color = Color(0xFFF87171), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFF87171)) },
                            onClick = {
                                showMenu = false
                                try {
                                    webView?.clearCache(true)
                                    webView?.clearHistory()
                                    CookieManager.getInstance().removeAllCookies(null)
                                    history.clear()
                                    showToast("Browser cache and private cookies cleared")
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // FIND IN PAGE FLOATING TOOLBAR
            // -------------------------------------------------------------
            AnimatedVisibility(
                visible = isFindInPageActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            if (it.isNotBlank()) {
                                webView?.findAllAsync(it)
                            } else {
                                webView?.clearMatches()
                            }
                        },
                        singleLine = true,
                        placeholder = { Text("Find in page...", color = Color(0xFF64748B), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    )

                    IconButton(onClick = { webView?.findNext(false) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = Color.White)
                    }
                    IconButton(onClick = { webView?.findNext(true) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = Color.White)
                    }
                    IconButton(onClick = {
                        isFindInPageActive = false
                        webView?.clearMatches()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Find", tint = Color(0xFF94A3B8))
                    }
                }
            }

            // Bookmark Quick Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(bookmarks.take(8)) { (title, link) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF334155))
                            .clickable {
                                focusManager.clearFocus()
                                navigateTo(link)
                            }
                            .padding(horizontal = 9.dp, vertical = 3.dp)
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

            // Web Loading Progress indicator
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

            // -------------------------------------------------------------
            // HIGH PERFORMANCE FULL WEBVIEW
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = false
                            isScrollbarFadingEnabled = true

                            // Fast Cookie Management
                            try {
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)
                            } catch (_: Exception) {}

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
                                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                                loadsImagesAutomatically = true
                                blockNetworkImage = false
                                setNeedInitialFocus(false)
                            }

                            // JavaScript Interface for direct blob / canvas / stream downloads
                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun saveBlobData(dataUri: String, name: String) {
                                    if (dataUri.isNotBlank()) {
                                        coroutineScope.launch {
                                            val item = repository.downloadUrlToVault(
                                                url = dataUri,
                                                suggestedFileName = name.ifBlank { "blob_media" }
                                            )
                                            withContext(Dispatchers.Main) {
                                                if (item != null) {
                                                    val folder = if (item.type == VaultItemType.PHOTO) "Vault Gallery" else "Vault Videos"
                                                    showToast("Saved to $folder: ${item.name}")
                                                } else {
                                                    showToast("Failed to save media.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }, "VaultAndroid")

                            // Isolated Vault In-Browser Downloader
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                                val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                downloadToVault(url, guessedName, mimetype)
                            }

                            // Long-press context detector for images and videos
                            setOnLongClickListener {
                                val result = hitTestResult
                                val type = result.type
                                when (type) {
                                    WebView.HitTestResult.IMAGE_TYPE,
                                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                        longPressTargetUrl = result.extra
                                        longPressIsImage = true
                                        true
                                    }
                                    WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                        longPressLinkUrl = result.extra
                                        longPressIsImage = false
                                        true
                                    }
                                    else -> false
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                // Ad Blocker Engine + Real-Time Media Stream Sniffer
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: return null

                                    // Real-time media sniffing for streaming videos / images
                                    val lowerUrl = reqUrl.lowercase()
                                    if (lowerUrl.contains(".mp4") || lowerUrl.contains(".webm") || lowerUrl.contains(".m4v") || lowerUrl.contains(".m3u8")) {
                                        view?.post {
                                            if (detectedMedia.none { it.url == reqUrl }) {
                                                detectedMedia.add(SniffedMedia(type = "video", url = reqUrl, name = "Stream_${detectedMedia.size + 1}"))
                                            }
                                        }
                                    } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".gif")) {
                                        if (!lowerUrl.contains("favicon") && !lowerUrl.contains("pixel") && !lowerUrl.contains("tracker")) {
                                            view?.post {
                                                if (detectedMedia.none { it.url == reqUrl }) {
                                                    detectedMedia.add(SniffedMedia(type = "photo", url = reqUrl, name = "Image_${detectedMedia.size + 1}"))
                                                }
                                            }
                                        }
                                    }

                                    if (adBlockerEnabled && isAdRequest(reqUrl)) {
                                        view?.post {
                                            blockedAdsCount++
                                        }
                                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    val scheme = uri.scheme?.lowercase() ?: ""
                                    if (scheme == "http" || scheme == "https") {
                                        return false
                                    }
                                    return try {
                                        val extIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(extIntent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    blockedAdsCount = 0
                                    url?.let {
                                        currentUrl = it
                                        urlInput = it
                                        currentTab.url = it
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    val title = view?.title ?: "Google"
                                    pageTitle = title
                                    currentTab.title = title

                                    // Add to history
                                    if (!url.isNullOrBlank() && !url.startsWith("data:")) {
                                        history.add(0, HistoryEntry(title, url))
                                        if (history.size > 80) history.removeLast()
                                    }
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
                                    title?.let {
                                        pageTitle = it
                                        currentTab.title = it
                                    }
                                }

                                // Fullscreen Video support
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    customView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    customViewCallback?.onCustomViewHidden()
                                    customView = null
                                    customViewCallback = null
                                }

                                // File upload support
                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = filePathCallback
                                    try {
                                        fileChooserLauncher.launch("*/*")
                                    } catch (_: Exception) {
                                        fileChooserCallback = null
                                        return false
                                    }
                                    return true
                                }
                            }

                            loadUrl(currentUrl)
                            webView = this
                        }
                    },
                    update = { wv ->
                        if (activeTabId != lastActiveTabId) {
                            lastActiveTabId = activeTabId
                            wv.loadUrl(currentTab.url)
                        }
                    },
                    onRelease = { view ->
                        view.stopLoading()
                        view.destroy()
                    }
                )

                // HTML5 Fullscreen Video View Overlay
                if (customView != null) {
                    AndroidView(
                        factory = { _ ->
                            FrameLayout(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                addView(customView)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }

            // -------------------------------------------------------------
            // BOTTOM TOOLBAR: Back, Forward, Reload, Home, Download Media, Bookmarks
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { webView?.reload() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { navigateTo(defaultHomeUrl) }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Google Home",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Dedicated Media Downloader button
                IconButton(onClick = { scanPageMedia() }) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download Photos/Videos",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Bookmark toggle
                val isBookmarked = bookmarks.any { it.second == currentUrl }
                IconButton(onClick = {
                    if (isBookmarked) {
                        bookmarks.removeAll { it.second == currentUrl }
                        showToast("Bookmark removed")
                    } else {
                        bookmarks.add(0, pageTitle to currentUrl)
                        showToast("Bookmark saved")
                    }
                }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFF59E0B) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // LONG PRESS CONTEXT DIALOG (FOR IMAGES & LINKS)
        // -------------------------------------------------------------
        if (longPressTargetUrl != null || longPressLinkUrl != null) {
            val targetMediaUrl = longPressTargetUrl
            val targetLink = longPressLinkUrl

            AlertDialog(
                onDismissRequest = {
                    longPressTargetUrl = null
                    longPressLinkUrl = null
                },
                title = {
                    Text(
                        text = if (targetMediaUrl != null) "Image Options" else "Link Options",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val displayTarget = (targetMediaUrl ?: targetLink ?: "").take(60)
                        Text(displayTarget, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                        if (targetMediaUrl != null) {
                            // Download Image to Vault
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF334155))
                                    .clickable {
                                        downloadToVault(targetMediaUrl)
                                        longPressTargetUrl = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Download to Vault Gallery", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Open Image in New Tab
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        val newTab = BrowserTab(
                                            id = UUID.randomUUID().toString(),
                                            title = "Image",
                                            url = targetMediaUrl
                                        )
                                        tabs.add(newTab)
                                        activeTabId = newTab.id
                                        urlInput = targetMediaUrl
                                        currentUrl = targetMediaUrl
                                        webView?.loadUrl(targetMediaUrl)
                                        longPressTargetUrl = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Open in New Tab", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        if (targetLink != null) {
                            // Open link in new tab
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF334155))
                                    .clickable {
                                        val newTab = BrowserTab(
                                            id = UUID.randomUUID().toString(),
                                            title = "New Tab",
                                            url = targetLink
                                        )
                                        tabs.add(newTab)
                                        activeTabId = newTab.id
                                        urlInput = targetLink
                                        currentUrl = targetLink
                                        webView?.loadUrl(targetLink)
                                        longPressLinkUrl = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Open Link in New Tab", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Download linked file
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        downloadToVault(targetLink)
                                        longPressLinkUrl = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Download Linked File to Vault", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Copy URL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    val clip = targetMediaUrl ?: targetLink ?: ""
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("URL", clip))
                                    showToast("Copied to clipboard")
                                    longPressTargetUrl = null
                                    longPressLinkUrl = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Copy Link Address", color = Color.White, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        longPressTargetUrl = null
                        longPressLinkUrl = null
                    }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A),
                tonalElevation = 8.dp
            )
        }

        // -------------------------------------------------------------
        // TAB SWITCHER BOTTOM SHEET
        // -------------------------------------------------------------
        if (showTabsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTabsSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tabs (${tabs.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        TextButton(onClick = {
                            val newTab = BrowserTab(
                                id = UUID.randomUUID().toString(),
                                title = "Google",
                                url = defaultHomeUrl
                            )
                            tabs.add(newTab)
                            activeTabId = newTab.id
                            urlInput = defaultHomeUrl
                            currentUrl = defaultHomeUrl
                            webView?.loadUrl(defaultHomeUrl)
                            showTabsSheet = false
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Tab", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabs) { tab ->
                            val isSelected = tab.id == activeTabId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF0F172A))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        activeTabId = tab.id
                                        urlInput = tab.url
                                        currentUrl = tab.url
                                        webView?.loadUrl(tab.url)
                                        showTabsSheet = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tab.url,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (tabs.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val idx = tabs.indexOf(tab)
                                            tabs.remove(tab)
                                            if (tab.id == activeTabId) {
                                                val nextTab = tabs.getOrNull(idx) ?: tabs.last()
                                                activeTabId = nextTab.id
                                                urlInput = nextTab.url
                                                currentUrl = nextTab.url
                                                webView?.loadUrl(nextTab.url)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Tab", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // -------------------------------------------------------------
        // MEDIA SNIFFER & DOWNLOADER BOTTOM SHEET
        // -------------------------------------------------------------
        if (showMediaDetectorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMediaDetectorSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Media on Page (${detectedMedia.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (detectedMedia.isNotEmpty()) {
                                TextButton(onClick = {
                                    showToast("Downloading all ${detectedMedia.size} items to Vault...")
                                    detectedMedia.forEach { m ->
                                        downloadToVault(m.url, m.name)
                                    }
                                    showMediaDetectorSheet = false
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download All", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = { scanPageMedia() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rescan", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = "Download videos and photos straight into your isolated Vault. They will never appear in your main phone's gallery.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (detectedMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No video or photo sources detected on this page.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(detectedMedia) { media ->
                                val isVideo = media.type == "video"
                                val isAudio = media.type == "audio"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isVideo -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                        isAudio -> Color(0xFFA855F7).copy(alpha = 0.2f)
                                                        else -> Color(0xFF0284C7).copy(alpha = 0.2f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isVideo -> Icons.Default.Movie
                                                    isAudio -> Icons.Default.VideoLibrary
                                                    else -> Icons.Default.Image
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    isVideo -> Color(0xFFF87171)
                                                    isAudio -> Color(0xFFC084FC)
                                                    else -> Color(0xFF38BDF8)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = media.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when {
                                                    isVideo -> "Video File"
                                                    isAudio -> "Audio Stream"
                                                    else -> "Photo / Graphic"
                                                },
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            downloadToVault(media.url, media.name)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0284C7))
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // -------------------------------------------------------------
        // IN-VAULT DOWNLOADS SHEET
        // -------------------------------------------------------------
        if (showDownloadsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDownloadsSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Vault In-App Downloads (${downloadedItems.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Stored strictly inside your private vault. Not visible to external Android apps.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (downloadedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No files downloaded into vault yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(downloadedItems) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (item.type) {
                                                VaultItemType.PHOTO -> Icons.Default.Image
                                                VaultItemType.VIDEO -> Icons.Default.Movie
                                                else -> Icons.Default.FileDownload
                                            },
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(item.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(ImageCompressor.formatFileSize(item.sizeBytes), color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteItem(item)
                                                showToast("Deleted from vault")
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // -------------------------------------------------------------
        // BOOKMARKS SHEET
        // -------------------------------------------------------------
        if (showBookmarksSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBookmarksSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bookmarks (${bookmarks.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = {
                            bookmarks.add(0, pageTitle to currentUrl)
                            showToast("Added current page to bookmarks")
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Current", color = Color(0xFFF59E0B), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bookmarks) { (title, url) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .clickable {
                                        navigateTo(url)
                                        showBookmarksSheet = false
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(url, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = { bookmarks.removeAll { it.second == url } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // -------------------------------------------------------------
        // HISTORY SHEET
        // -------------------------------------------------------------
        if (showHistorySheet) {
            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Browsing History (${history.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (history.isNotEmpty()) {
                            TextButton(onClick = {
                                history.clear()
                                showToast("History cleared")
                            }) {
                                Text("Clear All", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history entries yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(history) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .clickable {
                                            navigateTo(entry.url)
                                            showHistorySheet = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.url, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(timeFormat.format(Date(entry.timestamp)), color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
