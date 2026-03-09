package com.eterna.kee

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showUnity by remember { mutableStateOf(false) }
            if (showUnity) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView<View>(
                        factory = {
                            UnityBridge.init(this@MainActivity)
                            UnityBridge.getView()!!
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = {
                            UnityBridge.sendMessage("PocCharacter", "PlayEmotion", "Happy")
                        }) { Text("Happy") }

                        Button(onClick = {
                            UnityBridge.sendMessage("PocCharacter", "PlayEmotion", "Sad")
                        }) { Text("Sad") }
                    }
                }
            }else {
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