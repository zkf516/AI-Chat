package com.example.aichat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.R
import com.example.aichat.dataclass.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val chats: MutableList<Chat>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private var onItemClickListener: ((Int) -> Unit)? = null
    private var onDeleteClickListener: ((Int) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_title)
        val time: TextView = view.findViewById(R.id.tv_time)
        val btnDelete: Button = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]

        holder.title.text = chat.title
        holder.time.text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(chat.timestamp))

        // 点击整个项选择对话
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(position)
        }

        // 删除按钮点击
        holder.btnDelete.setOnClickListener {
            onDeleteClickListener?.invoke(position)
        }
    }

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (Int) -> Unit) {
        onDeleteClickListener = listener
    }

    fun removeItem(position: Int) {
        chats.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount() = chats.size
}