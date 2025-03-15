package com.example.aichat.adapter

import com.example.aichat.R
import com.example.aichat.TextStyleParser
import com.example.aichat.dataclass.Message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MessageAdapter(private val messageList: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 子类 ViewHolder
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.message_text)
    }

    companion object {
        private const val VIEW_TYPE_AI = 0
        private const val VIEW_TYPE_USER = 1
    }

    // 根据消息类型（用户/AI）返回不同值
    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    // 根据消息类型创建不同 layout 的 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = if (viewType == VIEW_TYPE_USER) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
        }
        else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        }
        return ViewHolder(view)
    }

    // 将 message 绑定到 ViewHolder，设置消息文本内容
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        val processedText = TextStyleParser.processContent(message.content)
        (holder as ViewHolder).messageText.text = processedText
    }

    override fun getItemCount(): Int = messageList.size

}

