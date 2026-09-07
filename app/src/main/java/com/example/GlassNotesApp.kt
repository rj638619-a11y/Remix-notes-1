package com.example

import android.app.Application
import android.util.Log
import java.io.File

class GlassNotesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureWebViewCacheDirectories()
    }

    /**
     * Pre-create the Chromium / WebView disk cache and code cache directory structure.
     * This prevents Chromium's simple_file_enumerator and simple_index_file from failing with
     * ENOENT (No such file or directory) when initializing HTTP Cache / Code Cache (js/wasm).
     */
    private fun ensureWebViewCacheDirectories() {
        try {
            val webViewCache = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(webViewCache, "js").mkdirs()
            File(webViewCache, "wasm").mkdirs()
        } catch (e: Throwable) {
            Log.w("GlassNotesApp", "Unable to pre-create WebView code cache directories", e)
        }
    }
}
