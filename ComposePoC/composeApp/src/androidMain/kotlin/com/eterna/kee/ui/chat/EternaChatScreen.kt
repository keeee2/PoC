package com.eterna.kee.ui.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eterna.kee.media.LocalMediaItem
import com.eterna.kee.ui.theme.EternaColors
import java.text.SimpleDateFormat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop

import java.util.*

// ── 데이터 모델 ──

enum class MediaType { Image, Video }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaUri: Uri? = null,
    val mediaType: MediaType? = null,
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
    // 미디어 관련
    onAttachToggle: () -> Unit,
    isGalleryOpen: Boolean,
    galleryItems: List<LocalMediaItem>,
    onGalleryItemClick: (LocalMediaItem) -> Unit,
    onCameraClick: () -> Unit,
    // 마이크
    onMicClick: () -> Unit,
    isRecording: Boolean = false,
    // 첨부 미리보기
    pendingMediaUri: Uri? = null,
    pendingMediaType: MediaType? = null,
    onClearPendingMedia: () -> Unit = {},
) {
    val listState = rememberLazyListState()

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

        // ── 감정 + 입력 바 ──
        ChatBottomBar(
            text = inputText,
            onTextChange = onInputChange,
            onSend = onSend,
            onEmotion = onEmotion,
            onAttachToggle = onAttachToggle,
            isGalleryOpen = isGalleryOpen,
            onMicClick = onMicClick,
            isRecording = isRecording,
            pendingMediaUri = pendingMediaUri,
            pendingMediaType = pendingMediaType,
            onClearPendingMedia = onClearPendingMedia,
        )

        // ── 인라인 갤러리 (키보드 자리에 표시) ──
        InlineGalleryGrid(
            items = galleryItems,
            visible = isGalleryOpen,
            onItemClick = onGalleryItemClick,
            onCameraClick = onCameraClick,
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
            Text("✦", fontSize = 32.sp, color = EternaColors.Accent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Mirror에게 말을 걸어보세요",
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
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp,
                ),
                color = if (message.isUser) EternaColors.BubbleUser else EternaColors.BubbleOther,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.widthIn(max = 260.dp)) {
                    if (message.mediaUri != null) {
                        MediaBubbleContent(
                            uri = message.mediaUri,
                            mediaType = message.mediaType ?: MediaType.Image,
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = EternaColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            if (!message.isUser) {
                Spacer(modifier = Modifier.width(6.dp))
                BubbleTimestamp(timeFormat.format(Date(message.timestamp)))
            }
        }
    }
}

// ── 미디어 버블 ──

@Composable
private fun MediaBubbleContent(uri: Uri, mediaType: MediaType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .widthIn(max = 260.dp)
            .heightIn(max = 200.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = if (mediaType == MediaType.Video) "동영상" else "이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp),
        )
        if (mediaType == MediaType.Video) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("▶", fontSize = 20.sp, color = Color.White)
                    }
                }
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

// ── 하단 바 ──

@Composable
private fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmotion: (String) -> Unit,
    onAttachToggle: () -> Unit,
    isGalleryOpen: Boolean,
    onMicClick: () -> Unit,
    isRecording: Boolean,
    pendingMediaUri: Uri?,
    pendingMediaType: MediaType?,
    onClearPendingMedia: () -> Unit,
) {
    Surface(color = EternaColors.SurfaceGlass, tonalElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // 감정 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                EmotionChip(label = "Happy", onClick = { onEmotion("Happy") })
                Spacer(modifier = Modifier.width(10.dp))
                EmotionChip(label = "Sad", onClick = { onEmotion("Sad") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 첨부 미리보기
            if (pendingMediaUri != null) {
                PendingMediaPreview(
                    uri = pendingMediaUri,
                    mediaType = pendingMediaType ?: MediaType.Image,
                    onClear = onClearPendingMedia,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // [+/✕] [TextField] [🎤/➤]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // + 토글 (열림 시 ✕)
                FilledIconButton(
                    onClick = onAttachToggle,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGalleryOpen) EternaColors.Accent else EternaColors.SurfaceBright,
                        contentColor = if (isGalleryOpen) Color.White else EternaColors.TextSecondary,
                    ),
                    shape = CircleShape,
                ) {
                    Text(if (isGalleryOpen) "✕" else "+", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지 입력...", color = EternaColors.TextDim) },
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

                Spacer(modifier = Modifier.width(6.dp))

                val hasContent = text.isNotBlank() || pendingMediaUri != null
                if (hasContent) {
                    FilledIconButton(
                        onClick = onSend,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = EternaColors.Accent,
                            contentColor = Color.White,
                        ),
                        shape = CircleShape,
                    ) { Text("➤", fontSize = 18.sp) }
                } else {
                    FilledIconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) EternaColors.Error else EternaColors.SurfaceBright,
                            contentColor = if (isRecording) Color.White else EternaColors.TextSecondary,
                        ),
                        shape = CircleShape,
                    ) { Text(if (isRecording) "■" else "🎤", fontSize = 18.sp) }
                }
            }
        }
    }
}

// ── 첨부 미리보기 ──

@Composable
private fun PendingMediaPreview(uri: Uri, mediaType: MediaType, onClear: () -> Unit) {
    Box(modifier = Modifier.height(80.dp).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
            contentDescription = "첨부 미리보기",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(80.dp)
                .widthIn(min = 80.dp, max = 120.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        if (mediaType == MediaType.Video) {
            Box(
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) { Text("▶ 동영상", fontSize = 10.sp, color = Color.White) }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clickable(onClick = onClear),
            shape = CircleShape, color = Color.Black.copy(alpha = 0.6f),
        ) { Box(contentAlignment = Alignment.Center) { Text("✕", fontSize = 12.sp, color = Color.White) } }
    }
}

// ── 감정 칩 ──

@Composable
private fun EmotionChip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = EternaColors.Accent),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    EternaColors.Accent.copy(alpha = 0.5f),
                    EternaColors.AccentCyan.copy(alpha = 0.5f),
                )
            )
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    ) { Text(label, style = MaterialTheme.typography.labelMedium, color = EternaColors.TextPrimary) }
}