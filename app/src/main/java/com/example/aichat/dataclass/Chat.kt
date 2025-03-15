package com.example.aichat.dataclass

import java.util.UUID

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "新对话",
    val timestamp: Long = System.currentTimeMillis(),
    val messages: MutableList<Message> = mutableListOf()
)