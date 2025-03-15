package com.example.aichat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.R
import com.example.aichat.adapter.ChatAdapter
import com.example.aichat.dataclass.Chat

class ChatDialogFragment : DialogFragment() {
    private lateinit var adapter: ChatAdapter
    var onNewChat: (() -> Unit)? = null
    var onChatSelected: ((String) -> Unit)? = null
    var onChatDeleted: ((String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_chats, container, false)
        val chats = arguments?.getSerializable("chats") as? List<Chat> ?: emptyList()

        view.findViewById<RecyclerView>(R.id.rv_chats).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ChatAdapter(chats.toMutableList()).also {
                this@ChatDialogFragment.adapter = it

                //设置选择监听
                it.setOnItemClickListener { position ->
                    onChatSelected?.invoke(chats[position].id)
                    dismiss()
                }

                // 设置删除监听
                it.setOnDeleteClickListener { position ->
                    val deletedChat = chats[position]
                    it.removeItem(position)
                    onChatDeleted?.invoke(deletedChat.id)
                }
            }
        }

        view.findViewById<Button>(R.id.btn_new_chat).setOnClickListener {
            onNewChat?.invoke()
            dismiss()
        }
        return view
    }

    companion object {
        fun newInstance(chats: List<Chat>): ChatDialogFragment {
            return ChatDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("chats", ArrayList(chats))
                }
            }
        }
    }
}