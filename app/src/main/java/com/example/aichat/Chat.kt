package com.example.aichat

import java.util.UUID


data class Chat(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "新对话",
    val timestamp: Long = System.currentTimeMillis(),
    val messages: MutableList<Message> = mutableListOf()
) {
    fun getLastMessageContent(): String {
        return messages.lastOrNull()?.content ?: ""
    }

    fun updateLastMessage(content: String) {
        messages.lastOrNull()?.content = content
    }
}