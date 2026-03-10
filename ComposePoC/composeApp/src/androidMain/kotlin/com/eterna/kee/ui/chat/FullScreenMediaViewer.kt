package com.eterna.kee.ui.chat

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 풀스크린 미디어 뷰어.
 * - 이미지: 핀치 줌 + 패닝
 * - 동영상: ExoPlayer 풀스크린 재생
 * - 탭 또는 X 버튼으로 닫기
 */
@Composable
fun FullScreenMediaViewer(
    uri: Uri?,
    mediaType: MediaType?,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = uri != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (uri != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .systemBarsPadding(),
            ) {
                when (mediaType) {
                    MediaType.Video -> FullScreenVideoContent(uri = uri)
                    else -> FullScreenImageContent(uri = uri, onTapDismiss = onDismiss)
                }

                // X 닫기 버튼 (우상단)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clickable(onClick = onDismiss),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── 이미지: 핀치 줌 + 드래그 ──

@Composable
private fun FullScreenImageContent(uri: Uri, onTapDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // 더블탭: 줌 토글
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = {
                        // 줌 안 된 상태에서 탭하면 닫기
                        if (scale <= 1f) onTapDismiss()
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    if (scale > 1f) {
                        offset = Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y,
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .build(),
            contentDescription = "이미지",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

// ── 동영상: 풀스크린 ExoPlayer ──

@Composable
private fun FullScreenVideoContent(uri: Uri) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}