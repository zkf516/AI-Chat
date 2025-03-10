package com.example.aichat

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

class ChatActivity : AppCompatActivity() {
    private var apiService: DeepSeekApiService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AIChat)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 初始化 Retrofit 客户端
        apiService = RetrofitClient.client?.create(DeepSeekApiService::class.java)

        // 发送消息
        sendMessage("user123", "你好，DeepSeek！")

        // 接收消息
        receiveMessage("user123")
    }

    /**
     * 发送消息
     *
     * @param userId  用户 ID
     * @param message 消息内容
     */
    private fun sendMessage(userId: String, message: String) {
        val request = ChatRequest(userId, message)
        val call: Call<ChatResponse> = apiService!!.sendMessage("Bearer " + API_KEY, request)

        call.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("ChatActivity", "消息发送成功: " + response.body()?.message)
                } else {
                    Log.e("ChatActivity", "消息发送失败: " + response.message())
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.e("ChatActivity", "消息发送失败: " + t.message)
            }
        })
    }

    /**
     * 接收消息
     *
     * @param userId 用户 ID
     */
    private fun receiveMessage(userId: String) {
        val request = ChatRequest(userId, "")
        val call: Call<ChatResponse> = apiService!!.receiveMessage("Bearer " + API_KEY, request)

        call.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("ChatActivity", "收到消息: " + response.body()?.message)
                } else {
                    Log.e("ChatActivity", "接收消息失败: " + response.message())
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.e("ChatActivity", "接收消息失败: " + t.message)
            }
        })
    }

    companion object {
        private const val API_KEY = "your_api_key_here" // 替换为你的 DeepSeek API 密钥
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://api.deepseek.com/" // DeepSeek API 基础 URL
    private var retrofit: Retrofit? = null

    val client: Retrofit?
        get() {
            if (retrofit == null) {
                // 添加日志拦截器（用于调试）
                val logging = HttpLoggingInterceptor()
                logging.setLevel(HttpLoggingInterceptor.Level.BODY)

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()

                // 初始化 Retrofit
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }
}

class ChatRequest(
    var userId: String, // 用户 ID
    var message: String // 消息内容
)

class ChatResponse {
    var status: String? = null // 响应状态
    var message: String? = null // 返回的消息内容
}

interface DeepSeekApiService {
    // 发送消息
    @POST("chat/send")
    fun sendMessage(
        @Header("Authorization") apiKey: String?,  // API 密钥
        @Body request: ChatRequest? // 请求体
    ): Call<ChatResponse>

    // 接收消息
    @POST("chat/receive")
    fun receiveMessage(
        @Header("Authorization") apiKey: String?,  // API 密钥
        @Body request: ChatRequest? // 请求体
    ): Call<ChatResponse>
}
