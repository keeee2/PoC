package com.eterna.kee

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.safeDrawingPadding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var showUnity by remember { mutableStateOf(false) }

            if (showUnity) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Unity 3D View
                    AndroidView<View>(
                        factory = {
                            UnityBridge.init(this@MainActivity)
                            UnityBridge.getView()!!
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 하단 UI
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // 감정 버튼
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = {
                                UnityBridge.sendMessage("PocCharacter", "PlayEmotion", "Happy")
                            }) { Text("Happy") }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(onClick = {
                                UnityBridge.sendMessage("PocCharacter", "PlayEmotion", "Sad")
                            }) { Text("Sad") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 텍스트 입력 + 전송 → Unity 말풍선
                        var message by remember { mutableStateOf("") }

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
                                        UnityBridge.sendMessage(
                                            "PocCharacter",
                                            "ShowBubble",
                                            message.trim()
                                        )
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