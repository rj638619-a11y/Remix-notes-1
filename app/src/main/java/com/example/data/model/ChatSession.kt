package com.example.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString().take(8),
    val sender: String, // "user" or "ai"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null
)

data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val lastUpdated: Long = System.currentTimeMillis()
)
