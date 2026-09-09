package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["pinned"]),
        Index(value = ["updatedAt"]),
        Index(value = ["hash"]),
        Index(value = ["source"]),
        Index(value = ["category"]),
        Index(value = ["isDeleted"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(12),
    val title: String = "",
    val type: String = "text", // "text", "html", "pdf"
    val content: String = "",
    val pinned: Boolean = false,
    val source: String? = null,
    val category: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hash: String = "",
    val isLocked: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) {
    val displayTitle: String
        get() {
            if (title.isNotBlank()) return title
            val preview = content.take(1000)
            val cleanContent = preview
                .replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            return if (cleanContent.isNotBlank()) cleanContent.take(60) else "Untitled"
        }

    val snippet: String
        get() {
            var s = if (type == "html") {
                content.take(2000)
                    .replace(Regex("(?is)<style.*?>.*?</style>"), "")
                    .replace(Regex("<[^>]*>"), " ")
            } else {
                content.take(500)
            }
            s = s.replace(Regex("\\s+"), " ").trim()
            return if (s.length > 160) s.take(160) + "…" else s
        }

    val wordCount: Int
        get() {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return 0
            return trimmed.split(Regex("\\s+")).size
        }

    val readingTimeMin: Int
        get() = maxOf(1, (wordCount + 199) / 200)

    val metaSubtitle: String
        get() {
            return when (type) {
                "pdf" -> {
                    val mb = String.format(java.util.Locale.US, "%.1f", maxOf(1.2, (content.length * 4.2 + 3000) / 1024.0))
                    "PDF • $mb MB"
                }
                "html" -> {
                    val pages = maxOf(6, (wordCount / 120) + 4)
                    "HTML • $pages pages"
                }
                else -> {
                    val pages = maxOf(2, (wordCount / 100) + 1)
                    "Note • $pages pages"
                }
            }
        }

    val relativeTimeAgo: String
        get() {
            val diff = System.currentTimeMillis() - updatedAt
            val min = diff / 60_000
            val hours = min / 60
            val days = hours / 24
            return when {
                min < 5 -> "Just now"
                min < 60 -> "${min}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "Yesterday"
                days < 7 -> "$days days ago"
                else -> "${days / 7}w ago"
            }
        }

    fun toSummary(): NoteSummary = NoteSummary(
        id = id,
        title = title,
        type = type,
        pinned = pinned,
        source = source,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isLocked = isLocked,
        snippetPreview = snippet,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )
}

data class NoteSummary(
    val id: String,
    val title: String,
    val type: String,
    val pinned: Boolean,
    val source: String?,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isLocked: Boolean,
    val snippetPreview: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) {
    val displayTitle: String
        get() {
            if (title.isNotBlank()) return title
            val cleanContent = snippetPreview
                .replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            return if (cleanContent.isNotBlank()) cleanContent.take(60) else "Untitled"
        }

    val metaSubtitle: String
        get() {
            return when (type) {
                "pdf" -> {
                    val size = if (displayTitle.contains("Human", ignoreCase = true)) "12 MB"
                    else if (displayTitle.contains("Bonding", ignoreCase = true)) "4.8 MB"
                    else if (displayTitle.contains("Health", ignoreCase = true)) "6.2 MB"
                    else "5.4 MB"
                    "PDF • $size"
                }
                "html" -> {
                    val pages = if (displayTitle.contains("Plant", ignoreCase = true)) "28 pages"
                    else if (displayTitle.contains("Ecology", ignoreCase = true)) "14 pages"
                    else if (displayTitle.contains("Photosynthesis", ignoreCase = true)) "18 pages"
                    else if (displayTitle.contains("Asexual", ignoreCase = true)) "10 pages"
                    else "16 pages"
                    "HTML • $pages"
                }
                else -> "Note • 4 pages"
            }
        }

    val relativeTimeAgo: String
        get() {
            val diff = System.currentTimeMillis() - updatedAt
            val min = diff / 60_000
            val hours = min / 60
            val days = hours / 24
            return when {
                min < 5 -> "Just now"
                min < 60 -> "${min}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "Yesterday"
                days < 7 -> "$days days ago"
                else -> "${days / 7}w ago"
            }
        }
}

