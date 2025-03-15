package com.example.aichat

import com.example.aichat.dataclass.Message

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object AIResponseHelper {

    //设置 LLM 密匙和链接
    private const val API_KEY = "sk-caa8d044547341b288b84829bfa817f4"
    private const val API_URL = "https://api.deepseek.com/v1/chat/completions"

    // 构建聊天消息的 JSON 数组
    fun buildMessages(userInput: String, chatMessages: List<Message>): JSONArray {
        val messages = JSONArray().apply {
            chatMessages.forEach { message ->
                put(JSONObject().apply {
                    put("role", if (message.isUser) "user" else "assistant")
                    put("content", message.content.removePrefix("AI: "))
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userInput)
            })
        }
        return messages
    }

    // 创建请求体
    fun createRequestBody(messages: JSONArray): RequestBody {
        val jsonBody = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", messages)
            put("stream", true)
        }
        return jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
    }

    // 发送请求
    fun sendRequest(requestBody: RequestBody, onResponse: (String) -> Unit, onFailure: (String) -> Unit) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_FAILURE", "Network error", e)
                onFailure("网络请求失败 - ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onFailure("请求失败，错误码：${response.code}")
                    return
                }

                val source = response.body?.source()
                source?.use { bufferedSource ->
                    var fullResponse = ""
                    while (!bufferedSource.exhausted()) {
                        val line = bufferedSource.readUtf8Line() ?: continue
                        if (line.startsWith("data: ")) {
                            val json = line.removePrefix("data: ").trim()
                            try {
                                val delta = JSONObject(json)
                                    .optJSONArray("choices")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("delta")
                                    ?.optString("content", "")

                                if (!delta.isNullOrEmpty()) {
                                    fullResponse += delta
                                    onResponse(fullResponse)
                                }
                            } catch (e: JSONException) {
                                Log.e("STREAM_ERROR", "Failed to parse stream", e)
                            }
                        }
                    }
                }
            }
        })
    }
}
