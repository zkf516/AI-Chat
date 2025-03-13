package com.example.aichat

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.content.SharedPreferences
import org.json.JSONException

//设置 LLM 密匙和链接
const val API_KEY = "sk-caa8d044547341b288b84829bfa817f4"
const val API_URL = "https://api.deepseek.com/v1/chat/completions"

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private val messageList = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)

        // 设置系统栏透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_chat)

        // 初始化时根据白色背景设置黑色图标
        setSystemBarIconColor(Color.WHITE)

        initViews()
        setupChat()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messageList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
        loadMessages() // 加载历史消息

        sendButton.setOnClickListener {
            val userMessage = messageInput.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, isUser = true)
                messageInput.text.clear()
                fetchAIResponse(userMessage)
            }
        }
    }

    private fun loadMessages() {
        val jsonString = sharedPreferences.getString("messages", null)
        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                messageList.clear()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val content = jsonObject.getString("content")
                    val isUser = jsonObject.getBoolean("isUser")
                    messageList.add(Message(content, isUser))
                }
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messageList.size - 1)
            } catch (e: JSONException) {
                Log.e("MainActivity", "Error parsing messages", e)
            }
        }
    }

    private fun saveMessages() {
        val jsonArray = JSONArray()
        for (message in messageList) {
            val jsonObject = JSONObject()
            jsonObject.put("content", message.content)
            jsonObject.put("isUser", message.isUser)
            jsonArray.put(jsonObject)
        }
        sharedPreferences.edit()
            .putString("messages", jsonArray.toString())
            .apply()
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val message = Message(text, isUser)
        messageList.add(message)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)

        // 根据最新消息背景色调整系统栏图标
        val bgColor = if (isUser) Color.WHITE else Color.BLACK
        setSystemBarIconColor(bgColor)

        // 保存消息
        saveMessages()
    }

    private fun fetchAIResponse(userInput: String) {
        val client = OkHttpClient()

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", userInput)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", messages)
            put("stream", true)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_FAILURE", "Network error", e)
                runOnUiThread {
                    addMessage("AI: 网络请求失败 - ${e.message}", false)
                    saveMessages() // 失败时保存
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        addMessage("AI: 请求失败，错误码：${response.code}", false)
                        saveMessages() // 错误时保存
                    }
                    return
                }

                val source = response.body?.source()
                source?.use { bufferedSource ->
                    var fullResponse = ""
                    runOnUiThread { addMessage("", false) }
                    while (!bufferedSource.exhausted()) {
                        val line = bufferedSource.readUtf8Line() ?: continue
                        if (line.startsWith("data: ")) {
                            val json = line.removePrefix("data: ").trim()
                            try {
                                val responseObject = JSONObject(json)
                                val delta = responseObject.optJSONArray("choices")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("delta")
                                    ?.optString("content", "")

                                if (!delta.isNullOrEmpty()) {
                                    fullResponse += delta
                                    runOnUiThread {
                                        messageList.last().content = "AI: $fullResponse"
                                        chatAdapter.notifyItemChanged(messageList.size - 1)
                                        recyclerView.scrollToPosition(messageList.size - 1)
                                        saveMessages() // 每次更新后保存
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("STREAM_ERROR", "Failed to parse stream", e)
                            }
                        }
                    }
                    runOnUiThread { saveMessages() } // 流结束后保存
                }
            }
        })
    }

    // 系统栏图标颜色切换函数
    @Suppress("DEPRECATION")
    private fun setSystemBarIconColor(backgroundColor: Int) {
        val isLight = ColorUtils.calculateLuminance(backgroundColor) > 0.5

        // 状态栏处理
        window.decorView.systemUiVisibility = if (isLight) {
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        // 导航栏处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = if (isLight) {
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }
}