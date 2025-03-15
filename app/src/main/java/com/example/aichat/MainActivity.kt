package com.example.aichat

import com.example.aichat.adapter.MessageAdapter
import com.example.aichat.dataclass.Chat
import com.example.aichat.dataclass.Message
import com.example.aichat.fragment.ChatDialogFragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
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


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var moreButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var messageAdapter: MessageAdapter
    private var currentChatId = ""
    private val chats = mutableListOf<Chat>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        initViews()

        // 初始化适配器
        initMessageAdapter()

        // 初始化 SharedPreferences
        initSharedPreferences()

        // 设置监听器
        sendButton.setOnClickListener { sendMessage() }
        moreButton.setOnClickListener { showChatDialog() }
    }

    // 初始化 Views
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
        moreButton = findViewById(R.id.more_button)
    }

    // 初始化消息适配器
    private fun initMessageAdapter(){
        messageAdapter = MessageAdapter(mutableListOf())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }
    }

    // 初始化 SharedPreferences
    private fun initSharedPreferences(){
        sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        loadChats()  // 加载所有对话
    }

    // 发送消息
    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        addMessage(text, true)
        messageInput.text.clear()
        fetchAIResponse(text)
    }

    // 显示对话管理窗口
    private fun showChatDialog() {
        ChatDialogFragment.newInstance(chats).apply {
            // 处理新建回调
            onNewChat = { createNewChat() }

            // 处理选择回调
            onChatSelected = { chatId ->
                currentChatId = chatId
                saveChats()
                refreshChatDisplay()
            }

            // 处理删除回调
            onChatDeleted = { deletedChatId ->
                handleChatDelete(deletedChatId)
            }
        }.show(supportFragmentManager, "chat_dialog")
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val message = Message(text, isUser)
        getCurrentChat().messages.add(message)
        messageAdapter.notifyItemChanged(messageAdapter.itemCount - 1)
        recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
        updateChatTitle(text)

        saveChats()
    }

    // 获得当前对话id
    private fun getCurrentChat(): Chat {
        return chats.first { it.id == currentChatId }
    }

    // 更新标题
    private fun updateChatTitle(newMessage: String) {
        if (getCurrentChat().title == "新对话" && !newMessage.startsWith("AI:")) {
            getCurrentChat().title = newMessage.take(20)
            saveChats()
        }
    }

    // 创建新对话
    private fun createNewChat() {
        val newChat = Chat().apply {
            messages.add(Message("AI: 您好！有什么可以帮您？", false))
        }
        chats.add(0, newChat)
        currentChatId = newChat.id
        saveChats()
        refreshChatDisplay()
    }

    //刷新对话页面
    @SuppressLint("NotifyDataSetChanged")
    private fun refreshChatDisplay() {
        messageAdapter = MessageAdapter(getCurrentChat().messages)
        messageAdapter.notifyDataSetChanged()
        recyclerView.adapter = messageAdapter
        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
    }

    //保存对话
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

    // 加载对话
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

        if (chats.isEmpty() || chats.none { it.id == currentChatId })
            createNewChat()

        refreshChatDisplay()
    }

    //
    private fun handleChatDelete(deletedChatId: String) {
        // 从列表中移除
        val iterator = chats.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().id == deletedChatId) {
                iterator.remove()
                break
            }
        }

        // 如果删除的是当前对话
        if (currentChatId == deletedChatId) {
            currentChatId = chats.firstOrNull()?.id ?: ""
            if (currentChatId.isEmpty()) {
                createNewChat()
            }
            refreshChatDisplay()
        }
        saveChats()
    }

    // 解析消息
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
        val messages = AIResponseHelper.buildMessages(userInput, getCurrentChat().messages)
        val requestBody = AIResponseHelper.createRequestBody(messages)

        runOnUiThread { addMessage("AI：思考中……", false) }
        AIResponseHelper.sendRequest(
            requestBody,
            onResponse = { responseText ->
                runOnUiThread {
                    getCurrentChat().messages.last().content = "AI: $responseText"
                    messageAdapter.notifyItemChanged(messageAdapter.itemCount - 1)
                    saveChats()
                }
            },
            onFailure = { errorMessage ->
                runOnUiThread {
                    getCurrentChat().messages.last().content = "AI: $errorMessage"
                    messageAdapter.notifyItemChanged(messageAdapter.itemCount - 1)
                    saveChats()
                }
            }
        )
    }
}