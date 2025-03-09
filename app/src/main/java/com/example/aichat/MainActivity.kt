package com.example.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Message(val sender: String, val text: String)

@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var showSidebar by remember { mutableStateOf(true) }
    val conversations = listOf("对话 1", "对话 2", "对话 3")

    Row(modifier = Modifier.fillMaxSize()) {
        if (showSidebar) {
            ConversationList(conversations) { showSidebar = false }
        }

        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            ChatMessages(messages)
            ChatInput(inputText, onTextChange = { inputText = it }) {
                messages = messages + Message("我", inputText.text)
                inputText = TextFieldValue("")
            }
        }
    }
}

@Composable
fun ConversationList(conversations: List<String>, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
            .background(Color.Gray)
    ) {
        Text("对话列表", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        LazyColumn {
            items(conversations) { convo ->
                Text(
                    text = convo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { /* 切换对话 */ }
                )
            }
        }
        Button(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("关闭")
        }
    }
}

@Composable
fun ChatMessages(messages: List<Message>) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = if (message.sender == "我") Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = "${message.sender}: ${message.text}",
                        modifier = Modifier
                            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInput(text: TextFieldValue, onTextChange: (TextFieldValue) -> Unit, onSend: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onSend, modifier = Modifier.padding(start = 8.dp)) {
            Text("发送")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChatScreen() }
    }
}
