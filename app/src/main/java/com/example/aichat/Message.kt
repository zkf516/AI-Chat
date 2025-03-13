package com.example.aichat

data class Message(
    var content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)