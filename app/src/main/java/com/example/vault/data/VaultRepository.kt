package com.example.vault.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.example.util.ImageCompressor
import com.example.vault.model.VaultItem
import com.example.vault.model.VaultItemType
import com.example.vault.model.VaultStorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.UUID

data class MoveResult(
    val item: VaultItem?,
    val wasHiddenFromMainDevice: Boolean,
    val pendingDeleteSender: IntentSender? = null,
    val sourceMediaUri: Uri? = null,
    val errorMessage: String? = null
)

private data class InternalDeleteOutcome(
    val isDeleted: Boolean,
    val intentSender: IntentSender? = null,
    val mediaUri: Uri? = null
)

class VaultRepository(private val context: Context) {
    private val vaultRoot: File = File(context.filesDir, "secret_vault").apply { mkdirs() }
    private val metaFile: File = File(vaultRoot, "vault_meta.json")

    private val photosDir: File = File(vaultRoot, "photos").apply { mkdirs() }
    private val videosDir: File = File(vaultRoot, "videos").apply { mkdirs() }
    private val audioDir: File = File(vaultRoot, "audio").apply { mkdirs() }
    private val docsDir: File = File(vaultRoot, "documents").apply { mkdirs() }
    private val notesDir: File = File(vaultRoot, "notes").apply { mkdirs() }
    private val thumbnailsDir: File = File(vaultRoot, "thumbnails").apply { mkdirs() }

    private val _itemsFlow = MutableStateFlow<List<VaultItem>>(emptyList())
    val itemsFlow: StateFlow<List<VaultItem>> = _itemsFlow.asStateFlow()

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        try {
            if (!metaFile.exists()) {
                _itemsFlow.value = emptyList()
                return
            }
            val jsonStr = metaFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VaultItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("type", "DOCUMENT")
                val itemType = try {
                    VaultItemType.valueOf(typeStr)
                } catch (_: Exception) {
                    VaultItemType.DOCUMENT
                }
                list.add(
                    VaultItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Unnamed"),
                        relativePath = obj.optString("relativePath", ""),
                        type = itemType,
                        mimeType = obj.optString("mimeType", ""),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                        durationSec = obj.optLong("durationSec", 0L),
                        content = obj.optString("content", "")
                    )
                )
            }
            _itemsFlow.value = list.sortedByDescending { it.dateAdded }
        } catch (_: Exception) {
            _itemsFlow.value = emptyList()
        }
    }

    private fun saveMetadata() {
        try {
            val array = JSONArray()
            _itemsFlow.value.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("relativePath", item.relativePath)
                    put("type", item.type.name)
                    put("mimeType", item.mimeType)
                    put("sizeBytes", item.sizeBytes)
                    put("dateAdded", item.dateAdded)
                    put("durationSec", item.durationSec)
                    put("content", item.content)
                }
                array.put(obj)
            }
            metaFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFileForItem(item: VaultItem): File {
        return File(vaultRoot, item.relativePath)
    }

    fun getThumbnailForItem(item: VaultItem): File {
        val fileName = File(item.relativePath).name
        val thumb = File(thumbnailsDir, "$fileName.thumb")
        if (thumb.exists() && thumb.length() > 0) {
            return thumb
        }
        val orig = getFileForItem(item)
        if (item.type == VaultItemType.PHOTO && orig.exists()) {
            try {
                ImageCompressor.createThumbnail(orig, thumb, size = 360, quality = 75)
                if (thumb.exists() && thumb.length() > 0) return thumb
            } catch (_: Exception) {}
        }
        return orig
    }

    suspend fun optimizeVaultImages(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var count = 0
        var bytesSaved = 0L
        val currentItems = _itemsFlow.value.toMutableList()

        currentItems.forEachIndexed { index, item ->
            if (item.type == VaultItemType.PHOTO) {
                val file = getFileForItem(item)
                if (file.exists() && file.length() > 500 * 1024) { // Only compress if > 500KB
                    val originalSize = file.length()
                    val tempOut = File(file.parentFile, "${file.name}.opt")
                    val success = ImageCompressor.compressFile(file, tempOut, maxDimension = 1920, quality = 82)
                    if (success && tempOut.exists() && tempOut.length() < originalSize) {
                        val newSize = tempOut.length()
                        val diff = originalSize - newSize
                        file.delete()
                        tempOut.renameTo(file)

                        // Recreate thumbnail
                        val thumb = File(thumbnailsDir, "${file.name}.thumb")
                        ImageCompressor.createThumbnail(file, thumb, size = 360, quality = 75)

                        currentItems[index] = item.copy(sizeBytes = newSize)
                        count++
                        bytesSaved += diff
                    } else {
                        tempOut.delete()
                    }
                }
            }
        }

        if (count > 0) {
            _itemsFlow.value = currentItems
            saveMetadata()
        }
        Pair(count, bytesSaved)
    }

    /**
     * Downloads an internet URL or data URI directly into the isolated Vault sandbox.
     * The file is stored ONLY in private app files and never touches the main device's
     * public Downloads or MediaStore folders.
     */
    suspend fun downloadUrlToVault(
        url: String,
        suggestedFileName: String? = null,
        mimeTypeOverride: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): VaultItem? = withContext(Dispatchers.IO) {
        try {
            if (url.startsWith("data:", ignoreCase = true)) {
                val commaIndex = url.indexOf(',')
                if (commaIndex == -1) return@withContext null
                val header = url.substring(0, commaIndex)
                val base64Data = url.substring(commaIndex + 1)
                val mime = header.substringAfter("data:").substringBefore(";").trim().ifEmpty { "image/png" }
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "png"
                val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)

                val itemType = if (mime.startsWith("image/")) VaultItemType.PHOTO
                else if (mime.startsWith("video/")) VaultItemType.VIDEO
                else if (mime.startsWith("audio/")) VaultItemType.AUDIO
                else VaultItemType.DOCUMENT

                val targetDir = when (itemType) {
                    VaultItemType.PHOTO -> photosDir
                    VaultItemType.VIDEO -> videosDir
                    VaultItemType.AUDIO -> audioDir
                    else -> docsDir
                }

                val cleanName = (suggestedFileName?.trim()?.ifEmpty { null } ?: "vault_dl_${System.currentTimeMillis()}").let {
                    if (!it.contains(".")) "$it.$ext" else it
                }
                val safeName = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val uniqueFileName = "${UUID.randomUUID().toString().take(8)}_$safeName"
                val targetFile = File(targetDir, uniqueFileName)
                targetFile.writeBytes(rawBytes)

                if (itemType == VaultItemType.PHOTO) {
                    val thumbFile = File(thumbnailsDir, "$uniqueFileName.thumb")
                    ImageCompressor.createThumbnail(targetFile, thumbFile, size = 360, quality = 75)
                }

                val vaultItem = VaultItem(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    relativePath = targetFile.relativeTo(vaultRoot).path,
                    type = itemType,
                    mimeType = mime,
                    sizeBytes = targetFile.length(),
                    dateAdded = System.currentTimeMillis()
                )

                _itemsFlow.value = listOf(vaultItem) + _itemsFlow.value
                saveMetadata()
                return@withContext vaultItem
            }

            var currentUrl = url
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
                val u = URL(currentUrl)
                connection = (u.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    setRequestProperty("Accept", "*/*")
                }
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(u, location).toString()
                    }
                    redirects++
                } else {
                    break
                }
            }

            val conn = connection ?: return@withContext null
            if (conn.responseCode !in 200..299) {
                return@withContext null
            }

            val rawContentType = conn.contentType ?: ""
            val cleanMime = (mimeTypeOverride ?: rawContentType.substringBefore(";").trim()).ifEmpty {
                URLConnection.guessContentTypeFromName(currentUrl) ?: "application/octet-stream"
            }
            val contentDisposition = conn.getHeaderField("Content-Disposition")

            var resolvedName: String = if (!suggestedFileName.isNullOrBlank()) {
                suggestedFileName.trim()
            } else {
                URLUtil.guessFileName(currentUrl, contentDisposition, cleanMime)
            }
            if (resolvedName.isBlank() || resolvedName == "downloadfile.bin") {
                val pathEnd = currentUrl.substringBefore("?").substringAfterLast("/")
                if (pathEnd.isNotBlank()) resolvedName = pathEnd
            }
            resolvedName = resolvedName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            if (!resolvedName.contains(".")) {
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime) ?: "bin"
                resolvedName = "$resolvedName.$ext"
            }

            val ext = resolvedName.substringAfterLast('.', "").lowercase()
            val itemType = when {
                cleanMime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "svg") -> VaultItemType.PHOTO
                cleanMime.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "webm", "avi", "3gp", "flv", "ts") -> VaultItemType.VIDEO
                cleanMime.startsWith("audio/") || ext in listOf("mp3", "m4a", "wav", "aac", "flac", "ogg", "opus", "wma") -> VaultItemType.AUDIO
                else -> VaultItemType.DOCUMENT
            }

            val targetDir = when (itemType) {
                VaultItemType.PHOTO -> photosDir
                VaultItemType.VIDEO -> videosDir
                VaultItemType.AUDIO -> audioDir
                else -> docsDir
            }

            val uniqueFileName = "${UUID.randomUUID().toString().take(8)}_$resolvedName"
            val targetFile = File(targetDir, uniqueFileName)

            val contentLength = conn.contentLengthLong
            var bytesReadTotal = 0L

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        bytesReadTotal += bytes
                        if (contentLength > 0 && onProgress != null) {
                            onProgress(bytesReadTotal.toFloat() / contentLength)
                        }
                    }
                }
            }

            if (itemType == VaultItemType.PHOTO && targetFile.length() > 0) {
                val thumbFile = File(thumbnailsDir, "$uniqueFileName.thumb")
                ImageCompressor.createThumbnail(targetFile, thumbFile, size = 360, quality = 75)
            }

            val vaultItem = VaultItem(
                id = UUID.randomUUID().toString(),
                name = resolvedName,
                relativePath = targetFile.relativeTo(vaultRoot).path,
                type = itemType,
                mimeType = cleanMime,
                sizeBytes = targetFile.length(),
                dateAdded = System.currentTimeMillis()
            )

            _itemsFlow.value = listOf(vaultItem) + _itemsFlow.value
            saveMetadata()
            vaultItem
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun moveFileToVault(uri: Uri, forcedType: VaultItemType? = null): MoveResult = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_${System.currentTimeMillis()}"
            var mimeType = context.contentResolver.getType(uri) ?: ""

            // Query display name
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            fileName = name
                        }
                    }
                }
            } catch (_: Exception) {
                uri.lastPathSegment?.let { seg ->
                    val clean = seg.substringAfterLast('/')
                    if (clean.isNotBlank()) fileName = clean
                }
            }

            // Determine item type
            val type = forcedType ?: when {
                mimeType.startsWith("image/") || isImageExtension(fileName) -> VaultItemType.PHOTO
                mimeType.startsWith("video/") || isVideoExtension(fileName) -> VaultItemType.VIDEO
                mimeType.startsWith("audio/") || isAudioExtension(fileName) -> VaultItemType.AUDIO
                else -> VaultItemType.DOCUMENT
            }

            if (mimeType.isBlank()) {
                mimeType = when (type) {
                    VaultItemType.PHOTO -> "image/jpeg"
                    VaultItemType.VIDEO -> "video/mp4"
                    VaultItemType.AUDIO -> "audio/mp3"
                    VaultItemType.DOCUMENT -> "application/octet-stream"
                    VaultItemType.NOTE -> "text/plain"
                }
            }

            val targetDir = when (type) {
                VaultItemType.PHOTO -> photosDir
                VaultItemType.VIDEO -> videosDir
                VaultItemType.AUDIO -> audioDir
                VaultItemType.DOCUMENT -> docsDir
                VaultItemType.NOTE -> notesDir
            }

            val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(targetDir, "${UUID.randomUUID()}_$sanitizedName")

            if (type == VaultItemType.PHOTO) {
                // Compress image to crisp 1080p/2K resolution (~300-600KB instead of 15-30MB raw)
                val compressed = ImageCompressor.compressUri(
                    context = context,
                    uri = uri,
                    outputFile = targetFile,
                    maxDimension = 1920,
                    quality = 82
                )
                if (!compressed) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@withContext MoveResult(null, false, errorMessage = "Could not open file input stream")
                }

                // Generate lightweight 360px thumbnail for lag-free gallery rendering
                try {
                    val thumbFile = File(thumbnailsDir, "${targetFile.name}.thumb")
                    ImageCompressor.createThumbnail(targetFile, thumbFile, size = 360, quality = 75)
                } catch (_: Exception) {}
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext MoveResult(null, false, errorMessage = "Could not open file input stream")
            }

            val sizeBytes = targetFile.length()
            val relativePath = targetFile.relativeTo(vaultRoot).path

            val newItem = VaultItem(
                id = UUID.randomUUID().toString(),
                name = fileName,
                relativePath = relativePath,
                type = type,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                dateAdded = System.currentTimeMillis()
            )

            // Attempt to automatically delete the source file from main device to hide it
            val deleteOutcome = tryDeleteSourceDetailed(uri, fileName, sizeBytes, type)
            val wasHidden = deleteOutcome.isDeleted

            val current = _itemsFlow.value.toMutableList()
            current.add(0, newItem)
            _itemsFlow.value = current
            saveMetadata()

            MoveResult(
                item = newItem,
                wasHiddenFromMainDevice = wasHidden,
                pendingDeleteSender = deleteOutcome.intentSender,
                sourceMediaUri = deleteOutcome.mediaUri
            )
        } catch (e: Exception) {
            MoveResult(null, false, errorMessage = e.message ?: "Failed to move file to vault")
        }
    }

    /**
     * Resolves modern PhotoPicker, Document, or generic content URIs into standard MediaStore URIs
     * that MediaStore.createDeleteRequest accepts on Android 11+.
     */
    fun resolveToStandardMediaStoreUri(uri: Uri, itemType: VaultItemType? = null): Uri? {
        val uriStr = uri.toString()
        // 1. Check if it is already a standard MediaStore external URI
        if (uri.authority == "media" && (
            uriStr.contains("/external/images/media/") ||
            uriStr.contains("/external/video/media/") ||
            uriStr.contains("/external/audio/media/") ||
            uriStr.contains("/external/file/")
        )) {
            return uri
        }

        // 2. Modern Android PhotoPicker URI: content://...photopicker.../media/<numeric_id>
        if (uriStr.contains("photopicker") || uriStr.contains("picker")) {
            val numericId = uri.lastPathSegment?.toLongOrNull()
                ?: uri.pathSegments.lastOrNull { it.toLongOrNull() != null }?.toLongOrNull()
            if (numericId != null) {
                return when (itemType) {
                    VaultItemType.VIDEO -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, numericId)
                    VaultItemType.AUDIO -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, numericId)
                    VaultItemType.DOCUMENT -> ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), numericId)
                    else -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, numericId)
                }
            }
        }

        // 3. Document Provider URIs (SAF)
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.startsWith("image:")) {
                    val id = docId.substringAfter("image:").toLongOrNull()
                    if (id != null) return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else if (docId.startsWith("video:")) {
                    val id = docId.substringAfter("video:").toLongOrNull()
                    if (id != null) return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else if (docId.startsWith("audio:")) {
                    val id = docId.substringAfter("audio:").toLongOrNull()
                    if (id != null) return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                }
            }
        } catch (_: Exception) {}

        // 4. Any numeric ID at end of URI
        val endId = uri.lastPathSegment?.toLongOrNull()
        if (endId != null && uri.scheme == "content") {
            return when (itemType) {
                VaultItemType.VIDEO -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, endId)
                VaultItemType.AUDIO -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, endId)
                VaultItemType.DOCUMENT -> ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), endId)
                else -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, endId)
            }
        }

        return null
    }

    /**
     * Creates a batch delete prompt IntentSender for multiple MediaStore items (Android 11+)
     */
    fun createBatchDeleteSender(mediaUris: List<Uri>, itemType: VaultItemType? = null): IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mediaUris.isNotEmpty()) {
            try {
                val standardUris = mediaUris.mapNotNull { 
                    resolveToStandardMediaStoreUri(it, itemType) ?: it 
                }.filter { 
                    it.authority == "media" && it.scheme == "content" 
                }.distinct()

                if (standardUris.isNotEmpty()) {
                    return MediaStore.createDeleteRequest(context.contentResolver, standardUris).intentSender
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun tryDeleteSourceDetailed(
        uri: Uri,
        fileName: String?,
        sourceSizeBytes: Long,
        itemType: VaultItemType? = null
    ): InternalDeleteOutcome {
        // 1. If it's a SAF Document URI, try deleteDocument
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                    return InternalDeleteOutcome(isDeleted = true)
                }
            }
        } catch (_: Exception) {}

        // 2. Try direct contentResolver delete on original uri
        try {
            val count = context.contentResolver.delete(uri, null, null)
            if (count > 0) {
                return InternalDeleteOutcome(isDeleted = true)
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                return InternalDeleteOutcome(
                    isDeleted = false,
                    intentSender = e.userAction.actionIntent.intentSender,
                    mediaUri = uri
                )
            }
        } catch (_: Exception) {}

        // 3. Try direct file delete if file scheme or raw path
        try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                val f = File(uri.path!!)
                if (f.exists() && f.delete()) {
                    return InternalDeleteOutcome(isDeleted = true)
                }
            } else if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.startsWith("raw:")) {
                    val f = File(docId.substringAfter("raw:"))
                    if (f.exists() && f.delete()) return InternalDeleteOutcome(isDeleted = true)
                } else if (docId.startsWith("primary:")) {
                    val f = File(Environment.getExternalStorageDirectory(), docId.substringAfter("primary:"))
                    if (f.exists() && f.delete()) return InternalDeleteOutcome(isDeleted = true)
                }
            }
        } catch (_: Exception) {}

        // 4. Resolve standard MediaStore URI (handles PhotoPicker, Document, etc.)
        var resolvedMediaUri: Uri? = resolveToStandardMediaStoreUri(uri, itemType)

        // 5. Query MediaStore by display name or size if still not resolved
        if (resolvedMediaUri == null && !fileName.isNullOrBlank()) {
            val collections = when (itemType) {
                VaultItemType.PHOTO -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Files.getContentUri("external"))
                VaultItemType.VIDEO -> listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Files.getContentUri("external"))
                VaultItemType.AUDIO -> listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Files.getContentUri("external"))
                else -> listOf(MediaStore.Files.getContentUri("external"), MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }
            for (collection in collections) {
                try {
                    val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(fileName)
                    context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val id = cursor.getLong(idIdx)
                            resolvedMediaUri = ContentUris.withAppendedId(collection, id)
                        }
                    }
                    if (resolvedMediaUri != null) break
                } catch (_: Exception) {}
            }
        }

        val targetUri = resolvedMediaUri ?: uri

        // 6. Try deleting standard MediaStore URI with contentResolver
        if (targetUri != uri) {
            try {
                val count = context.contentResolver.delete(targetUri, null, null)
                if (count > 0) {
                    return InternalDeleteOutcome(isDeleted = true)
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    return InternalDeleteOutcome(
                        isDeleted = false,
                        intentSender = e.userAction.actionIntent.intentSender,
                        mediaUri = targetUri
                    )
                }
            } catch (_: Exception) {}
        }

        // 7. On Android 11+ (API 30+), create system delete request with targetUri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && resolvedMediaUri != null) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(resolvedMediaUri))
                return InternalDeleteOutcome(
                    isDeleted = false,
                    intentSender = pendingIntent.intentSender,
                    mediaUri = resolvedMediaUri
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 8. Try fallback deletion across collections by display name
        if (!fileName.isNullOrBlank()) {
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Files.getContentUri("external")
            )
            for (collection in collections) {
                try {
                    val deletedRows = context.contentResolver.delete(
                        collection,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(fileName)
                    )
                    if (deletedRows > 0) {
                        return InternalDeleteOutcome(isDeleted = true)
                    }
                } catch (_: Exception) {}
            }
        }

        return InternalDeleteOutcome(isDeleted = false, mediaUri = resolvedMediaUri)
    }

    suspend fun saveSecretNote(title: String, content: String, existingId: String? = null): VaultItem = withContext(Dispatchers.IO) {
        val current = _itemsFlow.value.toMutableList()
        val item = if (existingId != null) {
            val idx = current.indexOfFirst { it.id == existingId }
            val existing = if (idx >= 0) current[idx] else null
            val updated = existing?.copy(
                name = title.ifBlank { "Secret Note" },
                content = content,
                dateAdded = System.currentTimeMillis()
            ) ?: VaultItem(
                id = existingId,
                name = title.ifBlank { "Secret Note" },
                type = VaultItemType.NOTE,
                content = content,
                dateAdded = System.currentTimeMillis()
            )
            if (idx >= 0) current[idx] = updated else current.add(0, updated)
            updated
        } else {
            val created = VaultItem(
                id = UUID.randomUUID().toString(),
                name = title.ifBlank { "Secret Note" },
                type = VaultItemType.NOTE,
                content = content,
                dateAdded = System.currentTimeMillis()
            )
            current.add(0, created)
            created
        }
        _itemsFlow.value = current
        saveMetadata()
        item
    }

    suspend fun saveRecordedAudio(tempFile: File, title: String): VaultItem = withContext(Dispatchers.IO) {
        val destFile = File(audioDir, "${UUID.randomUUID()}_${title.replace(' ', '_')}.m4a")
        tempFile.copyTo(destFile, overwrite = true)
        tempFile.delete()

        val item = VaultItem(
            id = UUID.randomUUID().toString(),
            name = "$title.m4a",
            relativePath = destFile.relativeTo(vaultRoot).path,
            type = VaultItemType.AUDIO,
            mimeType = "audio/mp4",
            sizeBytes = destFile.length(),
            dateAdded = System.currentTimeMillis()
        )
        val current = _itemsFlow.value.toMutableList()
        current.add(0, item)
        _itemsFlow.value = current
        saveMetadata()
        item
    }

    suspend fun deleteItem(item: VaultItem) = withContext(Dispatchers.IO) {
        if (item.relativePath.isNotBlank()) {
            val file = File(vaultRoot, item.relativePath)
            if (file.exists()) {
                file.delete()
            }
            val thumb = File(thumbnailsDir, "${file.name}.thumb")
            if (thumb.exists()) {
                thumb.delete()
            }
        }
        val current = _itemsFlow.value.toMutableList()
        current.removeAll { it.id == item.id }
        _itemsFlow.value = current
        saveMetadata()
    }

    suspend fun exportItemToDevice(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(vaultRoot, item.relativePath)
            if (!srcFile.exists()) return@withContext false

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativeDir = when (item.type) {
                        VaultItemType.PHOTO -> Environment.DIRECTORY_PICTURES + "/Restored_From_Vault"
                        VaultItemType.VIDEO -> Environment.DIRECTORY_MOVIES + "/Restored_From_Vault"
                        VaultItemType.AUDIO -> Environment.DIRECTORY_MUSIC + "/Restored_From_Vault"
                        else -> Environment.DIRECTORY_DOWNLOADS + "/Restored_From_Vault"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val tableUri = when (item.type) {
                VaultItemType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                VaultItemType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                VaultItemType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }

            val targetUri = resolver.insert(tableUri, contentValues) ?: return@withContext false

            resolver.openOutputStream(targetUri)?.use { outStream ->
                srcFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportAllToDevice(): Int = withContext(Dispatchers.IO) {
        var count = 0
        _itemsFlow.value.forEach { item ->
            if (item.type != VaultItemType.NOTE && item.relativePath.isNotBlank()) {
                if (exportItemToDevice(item)) count++
            }
        }
        count
    }

    suspend fun wipeVault() = withContext(Dispatchers.IO) {
        vaultRoot.deleteRecursively()
        vaultRoot.mkdirs()
        photosDir.mkdirs()
        videosDir.mkdirs()
        audioDir.mkdirs()
        docsDir.mkdirs()
        notesDir.mkdirs()
        thumbnailsDir.mkdirs()
        _itemsFlow.value = emptyList()
        saveMetadata()
    }

    fun getStats(): VaultStorageStats {
        val items = _itemsFlow.value
        var pCount = 0
        var pBytes = 0L
        var vCount = 0
        var vBytes = 0L
        var aCount = 0
        var aBytes = 0L
        var dCount = 0
        var dBytes = 0L
        var nCount = 0
        var total = 0L

        items.forEach { item ->
            when (item.type) {
                VaultItemType.PHOTO -> {
                    pCount++
                    pBytes += item.sizeBytes
                }
                VaultItemType.VIDEO -> {
                    vCount++
                    vBytes += item.sizeBytes
                }
                VaultItemType.AUDIO -> {
                    aCount++
                    aBytes += item.sizeBytes
                }
                VaultItemType.DOCUMENT -> {
                    dCount++
                    dBytes += item.sizeBytes
                }
                VaultItemType.NOTE -> {
                    nCount++
                }
            }
            total += item.sizeBytes
        }
        return VaultStorageStats(
            photoCount = pCount,
            photoBytes = pBytes,
            videoCount = vCount,
            videoBytes = vBytes,
            audioCount = aCount,
            audioBytes = aBytes,
            docCount = dCount,
            docBytes = dBytes,
            noteCount = nCount,
            totalBytes = total
        )
    }

    private fun isImageExtension(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
    }

    private fun isVideoExtension(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp")
    }

    private fun isAudioExtension(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "m4a", "wav", "aac", "flac", "ogg", "opus")
    }
}
