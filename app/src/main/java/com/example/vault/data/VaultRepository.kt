package com.example.vault.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import java.util.UUID

data class MoveResult(
    val item: VaultItem?,
    val wasHiddenFromMainDevice: Boolean,
    val errorMessage: String? = null
)

class VaultRepository(private val context: Context) {
    private val vaultRoot: File = File(context.filesDir, "secret_vault").apply { mkdirs() }
    private val metaFile: File = File(vaultRoot, "vault_meta.json")

    private val photosDir: File = File(vaultRoot, "photos").apply { mkdirs() }
    private val videosDir: File = File(vaultRoot, "videos").apply { mkdirs() }
    private val audioDir: File = File(vaultRoot, "audio").apply { mkdirs() }
    private val docsDir: File = File(vaultRoot, "documents").apply { mkdirs() }
    private val notesDir: File = File(vaultRoot, "notes").apply { mkdirs() }

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

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext MoveResult(null, false, "Could not open file input stream")

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
            val wasHidden = tryDeleteSource(uri, fileName)

            val current = _itemsFlow.value.toMutableList()
            current.add(0, newItem)
            _itemsFlow.value = current
            saveMetadata()

            MoveResult(newItem, wasHidden)
        } catch (e: Exception) {
            MoveResult(null, false, e.message ?: "Failed to move file to vault")
        }
    }

    private fun tryDeleteSource(uri: Uri, fileName: String?): Boolean {
        var deleted = false
        // 1. Try DocumentsContract if applicable
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            }
        } catch (_: Exception) {}

        // 2. Try direct contentResolver delete
        if (!deleted) {
            try {
                deleted = context.contentResolver.delete(uri, null, null) > 0
            } catch (_: Exception) {}
        }

        // 3. Try deleting via direct file scheme
        if (!deleted && uri.scheme == "file") {
            try {
                val f = File(uri.path ?: "")
                if (f.exists()) {
                    deleted = f.delete()
                }
            } catch (_: Exception) {}
        }

        // 4. Try querying MediaStore _data column
        if (!deleted) {
            try {
                val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns._ID)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataIdx != -1) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrBlank()) {
                                val file = File(path)
                                if (file.exists()) {
                                    deleted = file.delete()
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. Try querying external storage collections by filename
        if (!deleted && !fileName.isNullOrBlank()) {
            try {
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
                            deleted = true
                            break
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        return deleted
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
