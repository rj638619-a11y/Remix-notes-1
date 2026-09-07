package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class GlassNotesApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        ensureWebViewCacheDirectories()
    }

    /**
     * Optimized global Coil image loader configuration:
     * - 25% memory cache to prevent GC pressure and OutOfMemory errors
     * - 64MB disk cache in cacheDir to minimize disk footprint
     * - Hardware bitmaps enabled for zero-copy GPU textures on modern and low-end devices
     * - Subtle 150ms crossfade to eliminate UI stutter when loading images
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
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
