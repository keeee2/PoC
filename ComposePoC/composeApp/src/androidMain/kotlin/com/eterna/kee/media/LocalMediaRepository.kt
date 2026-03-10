package com.eterna.kee.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LocalMediaType { Image, Video }

data class LocalMediaItem(
    val id: Long,
    val uri: Uri,
    val type: LocalMediaType,
    val dateAdded: Long,
    val durationMs: Long = 0,  // 동영상만
)

/**
 * 디바이스 갤러리에서 최근 미디어를 가져온다.
 * Android 13+: READ_MEDIA_IMAGES + READ_MEDIA_VIDEO
 * Android 12-: READ_EXTERNAL_STORAGE
 */
suspend fun loadRecentMedia(
    context: Context,
    limit: Int = 50,
): List<LocalMediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<LocalMediaItem>()

    // 이미지
    queryMedia(
        context = context,
        collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        type = LocalMediaType.Image,
        limit = limit,
        output = items,
    )

    // 동영상
    queryMedia(
        context = context,
        collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        type = LocalMediaType.Video,
        limit = limit,
        output = items,
    )

    // 최신순 정렬 후 limit
    items.sortedByDescending { it.dateAdded }.take(limit)
}

private fun queryMedia(
    context: Context,
    collection: Uri,
    type: LocalMediaType,
    limit: Int,
    output: MutableList<LocalMediaItem>,
) {
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DATE_ADDED,
    ) + if (type == LocalMediaType.Video) {
        arrayOf(MediaStore.Video.VideoColumns.DURATION)
    } else emptyArray()

    val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

    // Android Q+: LIMIT via Bundle, 이전: sortOrder에 LIMIT 추가
    val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bundle = android.os.Bundle().apply {
            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
            putStringArray(
                android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_ADDED),
            )
            putInt(
                android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,
                android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }
        context.contentResolver.query(collection, projection, bundle, null)
    } else {
        context.contentResolver.query(
            collection, projection, null, null,
            "$sortOrder LIMIT $limit",
        )
    }

    cursor?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val durationCol = if (type == LocalMediaType.Video)
            c.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
        else -1

        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            output.add(
                LocalMediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    type = type,
                    dateAdded = c.getLong(dateCol),
                    durationMs = if (durationCol >= 0) c.getLong(durationCol) else 0,
                )
            )
        }
    }
}