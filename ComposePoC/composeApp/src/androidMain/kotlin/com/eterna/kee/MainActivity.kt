package com.eterna.kee

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

data class ChatMessage(
    val text: String,
    val isMe: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Unity 엔진 + 뷰 미리 초기화 (버튼 누를 때 딜레이 제거)
        UnityBridge.init(this)
        UnityBridge.getView()

        setContent {
            var showUnity by remember { mutableStateOf(false) }

            if (showUnity) {
                val messages = remember { mutableStateListOf<ChatMessage>() }
                var message by remember { mutableStateOf("") }
                val listState = rememberLazyListState()
                val focusManager = LocalFocusManager.current

                // Unity -> Compose 콜백
                DisposableEffect(Unit) {
                    UnityBridge.onMessageFromUnity = { _, method, msg ->
                        when (method) {
                            "ChatReply" -> messages.add(ChatMessage(msg, isMe = false))
                        }
                    }
                    onDispose { UnityBridge.onMessageFromUnity = null }
                }

                // 새 메시지 시 자동 스크롤
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    // Unity 3D View — 풀스크린 고정
                    AndroidView<View>(
                        factory = { UnityBridge.getView()!! },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 채팅 버블 — 상단 영역
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.4f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages) { msg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isMe)
                                    Arrangement.End else Arrangement.Start
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 15.sp,
                                    color = if (msg.isMe) Color.White else Color.Black,
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (msg.isMe) 16.dp else 4.dp,
                                                bottomEnd = if (msg.isMe) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (msg.isMe) Color(0xFF3B82F6)
                                            else Color.White.copy(alpha = 0.9f)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    // 하단 UI — 키보드만큼만 올라감
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // 감정 버튼
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                UnityBridge.sendMessage("PocCharacter", "PlayEmotion", "Happy")
                                messages.add(ChatMessage("[Happy 감정 재생]", isMe = false))
                            }) { Text("Happy") }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(onClick = {
                                UnityBridge.sendMessage("PocCharacter", "PlayEmotion", "Sad")
                                messages.add(ChatMessage("[Sad 감정 재생]", isMe = false))
                            }) { Text("Sad") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 텍스트 입력 + 전송
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("메시지 입력...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.9f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (message.isNotBlank()) {
                                        messages.add(ChatMessage(message.trim(), isMe = true))
                                        message = ""
                                    }
                                },
                                enabled = message.isNotBlank()
                            ) {
                                Text("전송")
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ETERNA PoC", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showUnity = true }) {
                        Text("3D 캐릭터 보기")
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); UnityBridge.resume() }
    override fun onPause() { super.onPause(); UnityBridge.pause() }
    override fun onDestroy() { super.onDestroy(); UnityBridge.destroy() }
}