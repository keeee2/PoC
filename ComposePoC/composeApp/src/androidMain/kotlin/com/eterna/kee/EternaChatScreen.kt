package com.eterna.kee.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterna.kee.ui.theme.EternaColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── 데이터 모델 ──

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

// ── 채팅 화면 (Unity 위 오버레이용) ──

@Composable
fun EternaChatOverlay(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmotion: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 새 메시지 시 자동 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier) {
        // ── 채팅 버블 리스트 ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (messages.isEmpty()) {
                item { EmptyState() }
            }
            items(messages, key = { it.id }) { message ->
                ChatBubble(message)
            }
        }

        // ── 감정 버튼 + 입력 바 ──
        ChatBottomBar(
            text = inputText,
            onTextChange = onInputChange,
            onSend = onSend,
            onEmotion = onEmotion,
        )
    }
}

// ── 빈 상태 ──

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✦",
                fontSize = 32.sp,
                color = EternaColors.Accent,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mirror에게 말을 걸어보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = EternaColors.TextDim,
            )
        }
    }
}

// ── 채팅 버블 ──

@Composable
private fun ChatBubble(message: ChatMessage) {
    val timeFormat = remember { SimpleDateFormat("a h:mm", Locale.KOREAN) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
    ) {
        // 이름 라벨 (Mirror만)
        if (!message.isUser) {
            Text(
                text = "Mirror",
                style = MaterialTheme.typography.labelMedium,
                color = EternaColors.Accent,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        ) {
            if (message.isUser) {
                BubbleTimestamp(timeFormat.format(Date(message.timestamp)))
                Spacer(modifier = Modifier.width(6.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp,
                ),
                color = if (message.isUser) EternaColors.BubbleUser else EternaColors.BubbleOther,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = EternaColors.TextPrimary,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 260.dp),
                )
            }

            if (!message.isUser) {
                Spacer(modifier = Modifier.width(6.dp))
                BubbleTimestamp(timeFormat.format(Date(message.timestamp)))
            }
        }
    }
}

@Composable
private fun BubbleTimestamp(time: String) {
    Text(
        text = time,
        style = MaterialTheme.typography.bodySmall,
        color = EternaColors.TextDim,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

// ── 하단 바: 감정 버튼 + 입력 필드 ──

@Composable
private fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmotion: (String) -> Unit,
) {
    Surface(
        color = EternaColors.SurfaceGlass,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // 감정 버튼 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                EmotionChip(label = "Happy", onClick = { onEmotion("Happy") })
                Spacer(modifier = Modifier.width(10.dp))
                EmotionChip(label = "Sad", onClick = { onEmotion("Sad") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 입력 필드 + 전송
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("메시지 입력...", color = EternaColors.TextDim)
                    },
                    singleLine = false,
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = EternaColors.SurfaceBright,
                        unfocusedContainerColor = EternaColors.SurfaceBright,
                        cursorColor = EternaColors.Accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = EternaColors.TextPrimary,
                        unfocusedTextColor = EternaColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(20.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                val hasText = text.isNotBlank()
                FilledIconButton(
                    onClick = onSend,
                    enabled = hasText,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasText) EternaColors.Accent else EternaColors.SurfaceBright,
                        contentColor = Color.White,
                        disabledContainerColor = EternaColors.SurfaceBright,
                        disabledContentColor = EternaColors.TextDim,
                    ),
                    shape = CircleShape,
                ) {
                    Text("➤", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun EmotionChip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EternaColors.Accent,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(EternaColors.Accent.copy(alpha = 0.5f), EternaColors.AccentCyan.copy(alpha = 0.5f))
            )
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = EternaColors.TextPrimary)
    }
}