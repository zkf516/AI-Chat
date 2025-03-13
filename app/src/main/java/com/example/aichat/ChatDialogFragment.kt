package com.example.aichat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatDialogFragment : DialogFragment() {
    private lateinit var adapter: ChatAdapter
    var onNewChat: (() -> Unit)? = null
    var onChatSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_chats, container, false)
        val chats = arguments?.getSerializable("chats") as? List<Chat> ?: emptyList()

        view.findViewById<RecyclerView>(R.id.rv_chats).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ChatAdapter(chats).also {
                it.setOnItemClickListener { position ->
                    onChatSelected?.invoke(chats[position].id)
                    dismiss()
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