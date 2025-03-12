package com.example.aichat

data class DeepSeekResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: AIMessage
)

data class AIMessage(
    val content: String
)
