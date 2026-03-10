package com.eterna.kee.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eterna.kee.media.LocalMediaItem
import com.eterna.kee.media.LocalMediaType
import com.eterna.kee.ui.theme.EternaColors

/**
 * 카카오톡 스타일 인앱 갤러리 그리드.
 * 채팅 입력 바 아래에 슬라이드 업으로 표시된다.
 *
 * @param items MediaStore에서 로드된 최근 미디어
 * @param visible 그리드 표시 여부
 * @param onItemClick 미디어 아이템 선택 시
 * @param onCameraClick 카메라 촬영 바로가기
 */
@Composable
fun InlineGalleryGrid(
    items: List<LocalMediaItem>,
    visible: Boolean,
    onItemClick: (LocalMediaItem) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + expandVertically(),
        exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            color = EternaColors.Surface,
            tonalElevation = 0.dp,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // 첫 칸: 카메라 바로가기
                item {
                    CameraShortcutCell(onClick = onCameraClick)
                }

                items(items, key = { it.id }) { item ->
                    MediaThumbnailCell(
                        item = item,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}

// ── 카메라 바로가기 셀 ──

@Composable
private fun CameraShortcutCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(EternaColors.SurfaceBright)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📷", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "카메라",
                style = MaterialTheme.typography.labelMedium,
                color = EternaColors.TextDim,
            )
        }
    }
}

// ── 미디어 썸네일 셀 ──

@Composable
private fun MediaThumbnailCell(
    item: LocalMediaItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 동영상이면 좌하단에 시간 표시
        if (item.type == LocalMediaType.Video) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = formatDuration(item.durationMs),
                    fontSize = 10.sp,
                    color = Color.White,
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}