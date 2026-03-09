package com.eterna.kee

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.eterna.kee.ui.chat.ChatMessage
import com.eterna.kee.ui.chat.EternaChatOverlay
import com.eterna.kee.ui.theme.EternaColors
import com.eterna.kee.ui.theme.EternaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UnityBridge.init(this)
        UnityBridge.getView()

        setContent {
            EternaTheme {
                var showUnity by remember { mutableStateOf(false) }

                if (showUnity) {
                    UnityScreen()
                } else {
                    LauncherScreen(onStart = { showUnity = true })
                }
            }
        }
    }

    override fun onResume() { super.onResume(); UnityBridge.resume() }
    override fun onPause() { super.onPause(); UnityBridge.pause() }
    override fun onDestroy() { super.onDestroy(); UnityBridge.destroy() }
}

// ── 런처 화면 ──

@Composable
private fun LauncherScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EternaColors.Background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "ETERNA PoC",
            style = MaterialTheme.typography.headlineMedium,
            color = EternaColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = EternaColors.Accent,
            ),
        ) {
            Text("3D 캐릭터 보기")
        }
    }
}

// ── Unity + 채팅 화면 ──

@Composable
private fun UnityScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Unity → Compose 콜백
    DisposableEffect(Unit) {
        UnityBridge.onMessageFromUnity = { _, method, msg ->
            when (method) {
                "ChatReply" -> messages.add(ChatMessage(text = msg, isUser = false))
            }
        }
        onDispose { UnityBridge.onMessageFromUnity = null }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // Unity 3D View — 풀스크린
        AndroidView<View>(
            factory = { UnityBridge.getView()!! },
            modifier = Modifier.fillMaxSize()
        )

        // 채팅 오버레이 — 하단 절반, 반투명
        EternaChatOverlay(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter),
            messages = messages,
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = {
                val text = inputText.trim()
                if (text.isEmpty()) return@EternaChatOverlay

                messages.add(ChatMessage(text = text, isUser = true))
                UnityBridge.sendMessage("PocCharacter", "ShowBubble", text)
                inputText = ""

                // PoC용 더미 응답
                messages.add(ChatMessage(
                    text = "「$text」… 흥미로운 이야기네요.",
                    isUser = false,
                ))
            },
            onEmotion = { emotion ->
                UnityBridge.sendMessage("PocCharacter", "PlayEmotion", emotion)
                messages.add(ChatMessage(text = "[$emotion 감정 재생]", isUser = false))
            },
        )
    }
}