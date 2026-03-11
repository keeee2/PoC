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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eterna.kee.media.LocalMediaItem
import com.eterna.kee.ui.theme.EternaColors
import java.text.SimpleDateFormat
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
    val audioUri: Uri? = null,
    val audioDurationMs: Long = 0,
    val sttText: String? = null,
)

// ── 채팅 화면 ──

@Composable
fun EternaChatOverlay(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmotion: (String) -> Unit,
    onAttachToggle: () -> Unit,
    isGalleryOpen: Boolean,
    galleryItems: List<LocalMediaItem>,
    onGalleryItemClick: (LocalMediaItem) -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    isRecording: Boolean = false,
    sttPartialText: String = "",
    pendingMediaUri: Uri? = null,
    pendingMediaType: MediaType? = null,
    onClearPendingMedia: () -> Unit = {},
    onMediaClick: (Uri, MediaType) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (messages.isEmpty()) {
                item { EmptyState() }
            }
            items(messages, key = { it.id }) { message ->
                ChatBubble(message, onMediaClick = onMediaClick)
            }
        }

        ChatBottomBar(
            text = inputText, onTextChange = onInputChange, onSend = onSend,
            onEmotion = onEmotion, onAttachToggle = onAttachToggle,
            isGalleryOpen = isGalleryOpen, onMicClick = onMicClick,
            isRecording = isRecording, sttPartialText = sttPartialText,
            pendingMediaUri = pendingMediaUri, pendingMediaType = pendingMediaType,
            onClearPendingMedia = onClearPendingMedia,
        )

        InlineGalleryGrid(
            items = galleryItems, visible = isGalleryOpen,
            onItemClick = onGalleryItemClick, onCameraClick = onCameraClick,
        )
    }
}

// ── 빈 상태 ──

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Stars, contentDescription = null, tint = EternaColors.Accent, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Mirror에게 말을 걸어보세요", style = MaterialTheme.typography.bodyMedium, color = EternaColors.TextDim)
        }
    }
}

// ── 채팅 버블 ──

@Composable
private fun ChatBubble(message: ChatMessage, onMediaClick: (Uri, MediaType) -> Unit) {
    val timeFormat = remember { SimpleDateFormat("a h:mm", Locale.KOREAN) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
    ) {
        if (!message.isUser) {
            Text("Mirror", style = MaterialTheme.typography.labelMedium, color = EternaColors.Accent,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
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
                tonalElevation = 0.dp, shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.widthIn(max = 260.dp)) {
                    if (message.mediaUri != null) {
                        MediaThumbnailBubble(
                            uri = message.mediaUri,
                            mediaType = message.mediaType ?: MediaType.Image,
                            onClick = { onMediaClick(message.mediaUri, message.mediaType ?: MediaType.Image) },
                        )
                    }
                    if (message.audioUri != null) {
                        VoiceMessageBubble(
                            audioUri = message.audioUri,
                            durationMs = message.audioDurationMs,
                            sttText = message.sttText,
                        )
                    }
                    if (message.text.isNotBlank() && message.audioUri == null) {
                        Text(message.text, style = MaterialTheme.typography.bodyLarge, color = EternaColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
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

// ── 미디어 썸네일 (탭하면 풀스크린) ──

@Composable
private fun MediaThumbnailBubble(uri: Uri, mediaType: MediaType, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 200.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
            contentDescription = if (mediaType == MediaType.Video) "동영상" else "이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
        )
        if (mediaType == MediaType.Video) {
            Box(
                modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "재생", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

// ── 음성 메시지 버블 ──

@Composable
private fun VoiceMessageBubble(audioUri: Uri, durationMs: Long, sttText: String?) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showStt by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUri))
            prepare()
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    LaunchedEffect(exoPlayer) {
        while (true) {
            kotlinx.coroutines.delay(300)
            if (isPlaying && !exoPlayer.isPlaying && exoPlayer.currentPosition > 0) {
                isPlaying = false
                exoPlayer.seekTo(0)
            }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 160.dp)) {
            IconButton(
                onClick = {
                    if (isPlaying) { exoPlayer.pause(); isPlaying = false }
                    else { exoPlayer.play(); isPlaying = true }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "정지" else "재생",
                    tint = EternaColors.Accent, modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(4.dp)).background(EternaColors.SurfaceBright))
            Spacer(modifier = Modifier.width(8.dp))
            Text(formatDuration(durationMs), style = MaterialTheme.typography.bodySmall, color = EternaColors.TextDim)
        }
        if (!sttText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (showStt) sttText else "텍스트 보기",
                style = MaterialTheme.typography.bodySmall,
                color = if (showStt) EternaColors.TextSecondary else EternaColors.Accent,
                modifier = Modifier.clickable { showStt = !showStt }.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun BubbleTimestamp(time: String) {
    Text(time, style = MaterialTheme.typography.bodySmall, color = EternaColors.TextDim, modifier = Modifier.padding(bottom = 2.dp))
}

// ── 하단 바 ──

@Composable
private fun ChatBottomBar(
    text: String, onTextChange: (String) -> Unit, onSend: () -> Unit,
    onEmotion: (String) -> Unit, onAttachToggle: () -> Unit,
    isGalleryOpen: Boolean, onMicClick: () -> Unit, isRecording: Boolean,
    sttPartialText: String, pendingMediaUri: Uri?, pendingMediaType: MediaType?,
    onClearPendingMedia: () -> Unit,
) {
    Surface(color = EternaColors.SurfaceGlass, tonalElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                EmotionChip(label = "Happy", onClick = { onEmotion("Happy") })
                Spacer(modifier = Modifier.width(10.dp))
                EmotionChip(label = "Sad", onClick = { onEmotion("Sad") })
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (isRecording) {
                RecordingIndicator(sttPartialText = sttPartialText)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (pendingMediaUri != null) {
                PendingMediaPreview(uri = pendingMediaUri, mediaType = pendingMediaType ?: MediaType.Image, onClear = onClearPendingMedia)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = onAttachToggle, modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGalleryOpen) EternaColors.Accent else EternaColors.SurfaceBright,
                        contentColor = if (isGalleryOpen) Color.White else EternaColors.TextSecondary,
                    ), shape = CircleShape,
                ) { Icon(if (isGalleryOpen) Icons.Filled.Close else Icons.Filled.Add, contentDescription = if (isGalleryOpen) "닫기" else "첨부") }

                Spacer(modifier = Modifier.width(6.dp))

                TextField(
                    value = text, onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지 입력...", color = EternaColors.TextDim) },
                    singleLine = false, maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = EternaColors.SurfaceBright, unfocusedContainerColor = EternaColors.SurfaceBright,
                        cursorColor = EternaColors.Accent, focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent, focusedTextColor = EternaColors.TextPrimary,
                        unfocusedTextColor = EternaColors.TextPrimary,
                    ), shape = RoundedCornerShape(20.dp),
                )

                Spacer(modifier = Modifier.width(6.dp))

                val hasContent = text.isNotBlank() || pendingMediaUri != null
                if (hasContent) {
                    FilledIconButton(
                        onClick = onSend, modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = EternaColors.Accent, contentColor = Color.White),
                        shape = CircleShape,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송") }
                } else {
                    FilledIconButton(
                        onClick = onMicClick, modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) EternaColors.Error else EternaColors.SurfaceBright,
                            contentColor = if (isRecording) Color.White else EternaColors.TextSecondary,
                        ), shape = CircleShape,
                    ) { Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = if (isRecording) "녹음 중지" else "녹음") }
                }
            }
        }
    }
}

@Composable
private fun RecordingIndicator(sttPartialText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(EternaColors.Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = EternaColors.Error, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (sttPartialText.isNotBlank()) sttPartialText else "녹음 중...",
            style = MaterialTheme.typography.bodySmall,
            color = if (sttPartialText.isNotBlank()) EternaColors.TextPrimary else EternaColors.TextDim,
            maxLines = 1, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PendingMediaPreview(uri: Uri, mediaType: MediaType, onClear: () -> Unit) {
    Box(modifier = Modifier.height(80.dp).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
            contentDescription = "첨부 미리보기", contentScale = ContentScale.Crop,
            modifier = Modifier.height(80.dp).widthIn(min = 80.dp, max = 120.dp).clip(RoundedCornerShape(12.dp)),
        )
        if (mediaType == MediaType.Video) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("동영상", fontSize = 10.sp, color = Color.White)
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clickable(onClick = onClear),
            shape = CircleShape, color = Color.Black.copy(alpha = 0.6f),
        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = "삭제", tint = Color.White, modifier = Modifier.size(14.dp)) } }
    }
}

@Composable
private fun EmotionChip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = EternaColors.Accent),
        border = androidx.compose.foundation.BorderStroke(1.dp,
            Brush.linearGradient(listOf(EternaColors.Accent.copy(alpha = 0.5f), EternaColors.AccentCyan.copy(alpha = 0.5f)))),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    ) { Text(label, style = MaterialTheme.typography.labelMedium, color = EternaColors.TextPrimary) }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}