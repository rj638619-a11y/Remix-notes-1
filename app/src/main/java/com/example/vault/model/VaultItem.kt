package com.example.vault.model

import java.util.UUID

enum class VaultItemType {
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT,
    NOTE
}

data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relativePath: String = "",
    val type: VaultItemType,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val durationSec: Long = 0L,
    val content: String = "" // For secret scratchpad notes or text snippets
)

data class VaultStorageStats(
    val photoCount: Int = 0,
    val photoBytes: Long = 0L,
    val videoCount: Int = 0,
    val videoBytes: Long = 0L,
    val audioCount: Int = 0,
    val audioBytes: Long = 0L,
    val docCount: Int = 0,
    val docBytes: Long = 0L,
    val noteCount: Int = 0,
    val totalBytes: Long = 0L
) {
    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
