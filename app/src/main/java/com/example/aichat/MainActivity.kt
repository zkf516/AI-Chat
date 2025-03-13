package com.example.aichat

import android.annotation.SuppressLint
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
    private val chats = mutableListOf<Chat>()
    private var currentChatId = ""
    private lateinit var messageAdapter: MessageAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var moreButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        initViews()

        // 初始化适配器
        messageAdapter = MessageAdapter(mutableListOf())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        loadChats()  // 加载所有对话
        checkCurrentChat()

        // 设置系统栏透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 初始化时根据白色背景设置黑色图标
        setSystemBarIconColor(Color.WHITE)

        // 设置监听器
        sendButton.setOnClickListener { sendMessage() }
        moreButton.setOnClickListener { showChatDialog() }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
        moreButton = findViewById(R.id.more_button)
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        addMessage(text, true)
        messageInput.text.clear()
        fetchAIResponse(text)
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val message = Message(text, isUser)
        getCurrentChat().messages.add(message)
        messageAdapter.notifyItemChanged(messageAdapter.itemCount - 1)
        recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
        updateChatTitle(text)

        // 根据最新消息背景色调整系统栏图标
        val bgColor = if (isUser) Color.WHITE else Color.BLACK
        setSystemBarIconColor(bgColor)
        saveChats()
    }



    private fun updateChatTitle(newMessage: String) {
        if (getCurrentChat().title == "新对话" && !newMessage.startsWith("AI:")) {
            getCurrentChat().title = newMessage.take(20)
            saveChats()
        }
    }

    private fun getCurrentChat(): Chat {
        return chats.first { it.id == currentChatId }
    }

    private fun checkCurrentChat() {
        if (chats.none { it.id == currentChatId }) {
            createNewChat()
        }
        refreshChatDisplay()
    }

    private fun createNewChat() {
        val newChat = Chat().apply {
            messages.add(Message("AI: 您好！有什么可以帮您？", false))
        }
        chats.add(0, newChat)
        currentChatId = newChat.id
        saveChats()
        refreshChatDisplay()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshChatDisplay() {
        messageAdapter = MessageAdapter(getCurrentChat().messages)
        messageAdapter.notifyDataSetChanged()
        recyclerView.adapter = messageAdapter
        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
    }

    private fun showChatDialog() {
        ChatDialogFragment.newInstance(chats).apply {
            onNewChat = { createNewChat() }
            onChatSelected = { chatId ->
                currentChatId = chatId
                saveChats()
                refreshChatDisplay()
            }
        }.show(supportFragmentManager, "chat_dialog")
    }

    // 存储相关方法
    private fun saveChats() {
        val json = JSONArray().apply {
            chats.forEach { chat ->
                put(JSONObject().apply {
                    put("id", chat.id)
                    put("title", chat.title)
                    put("timestamp", chat.timestamp)
                    put("messages", JSONArray().apply {
                        chat.messages.forEach { message ->
                            put(JSONObject().apply {
                                put("content", message.content)
                                put("isUser", message.isUser)
                                put("timestamp", message.timestamp)
                            })
                        }
                    })
                })
            }
        }

        sharedPreferences.edit()
            .putString("chats", json.toString())
            .putString("current_chat_id", currentChatId)
            .apply()
    }

    private fun loadChats() {
        chats.clear()
        val jsonString = sharedPreferences.getString("chats", null)
        currentChatId = sharedPreferences.getString("current_chat_id", "") ?: ""

        jsonString?.let {
            try {
                val jsonArray = JSONArray(it)
                for (i in 0 until jsonArray.length()) {
                    val chatJson = jsonArray.getJSONObject(i)
                    val messages = parseMessages(chatJson.getJSONArray("messages"))
                    chats.add(
                        Chat(
                            id = chatJson.getString("id"),
                            title = chatJson.getString("title"),
                            timestamp = chatJson.getLong("timestamp"),
                            messages = messages.toMutableList()
                        )
                    )
                }
            } catch (e: JSONException) {
                Log.e("MainActivity", "Error loading chats", e)
            }
        }

        if (chats.isEmpty()) createNewChat()
    }

    private fun parseMessages(jsonArray: JSONArray): List<Message> {
        return List(jsonArray.length()) { j ->
            jsonArray.getJSONObject(j).run {
                Message(
                    content = getString("content"),
                    isUser = getBoolean("isUser"),
                    timestamp = getLong("timestamp")
                )
            }
        }
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
                    saveChats() // 失败时保存
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        addMessage("AI: 请求失败，错误码：${response.code}", false)
                        saveChats() // 错误时保存
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
                                        getCurrentChat().messages.last().content = "AI: $fullResponse"
                                        messageAdapter.notifyItemChanged(messageAdapter.itemCount - 1)
                                        recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                                        saveChats() // 每次更新后保存
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("STREAM_ERROR", "Failed to parse stream", e)
                            }
                        }
                    }
                    runOnUiThread { saveChats() } // 流结束后保存
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