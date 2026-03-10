package com.eterna.kee

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.eterna.kee.media.LocalMediaItem
import com.eterna.kee.media.LocalMediaType
import com.eterna.kee.media.loadRecentMedia
import com.eterna.kee.ui.chat.*
import com.eterna.kee.ui.theme.EternaColors
import com.eterna.kee.ui.theme.EternaTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UnityBridge.init(this)
        UnityBridge.getView()

        setContent {
            EternaTheme {
                var showUnity by remember { mutableStateOf(false) }
                if (showUnity) UnityScreen()
                else LauncherScreen(onStart = { showUnity = true })
            }
        }
    }

    override fun onResume() { super.onResume(); UnityBridge.resume() }
    override fun onPause() { super.onPause(); UnityBridge.pause() }
    override fun onDestroy() { super.onDestroy(); UnityBridge.destroy() }
}

// ── 런처 ──

@Composable
private fun LauncherScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(EternaColors.Background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Compose-Unity-Bridge (PoC)", style = MaterialTheme.typography.headlineMedium, color = EternaColors.TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = EternaColors.Accent)) {
            Text("3D 캐릭터 보기")
        }
    }
}

// ── Unity + 채팅 화면 ──

@Composable
private fun UnityScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 채팅 상태
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }

    // 미디어 상태
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaType by remember { mutableStateOf<MediaType?>(null) }
    var isGalleryOpen by remember { mutableStateOf(false) }
    val galleryItems = remember { mutableStateListOf<LocalMediaItem>() }

    // 마이크 상태
    var isRecording by remember { mutableStateOf(false) }

    // 카메라 촬영용 임시 URI
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraVideoUri by remember { mutableStateOf<Uri?>(null) }

    // ── 갤러리 로드 함수 ──

    fun loadGallery() {
        scope.launch {
            val items = loadRecentMedia(context, limit = 50)
            galleryItems.clear()
            galleryItems.addAll(items)
        }
    }

    // ── 런처 & 권한 ──

    // 카메라 사진
    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            pendingMediaUri = cameraPhotoUri
            pendingMediaType = MediaType.Image
        }
    }

    // 카메라 동영상
    val recordVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && cameraVideoUri != null) {
            pendingMediaUri = cameraVideoUri
            pendingMediaType = MediaType.Video
        }
    }

    // CAMERA 권한
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempUri(context, "photo_", ".jpg")
            cameraPhotoUri = uri
            takePhotoLauncher.launch(uri)
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // RECORD_AUDIO 권한
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecording = true
            Toast.makeText(context, "녹음 시작 (PoC)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 갤러리 읽기 권한 (Android 13+: READ_MEDIA_IMAGES + READ_MEDIA_VIDEO)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 하나라도 승인되면 갤러리 열기 (부분 접근 포함)
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            isGalleryOpen = true
            focusManager.clearFocus()
            loadGallery()
        } else {
            Toast.makeText(context, "사진/동영상 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // ── 갤러리 토글 핸들러 ──

    fun handleAttachToggle() {
        if (isGalleryOpen) {
            isGalleryOpen = false
            return
        }

        // 권한 체크 후 갤러리 열기
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: 부분 접근 포함
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            // Android 12 이하
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        mediaPermissionLauncher.launch(permissions)

    }

    // ── 갤러리 아이템 선택 핸들러 ──

    fun handleGalleryItemClick(item: LocalMediaItem) {
        pendingMediaUri = item.uri
        pendingMediaType = when (item.type) {
            LocalMediaType.Image -> MediaType.Image
            LocalMediaType.Video -> MediaType.Video
        }
        // 갤러리는 열어둔 채로 미리보기 표시 (카카오톡 동작)
    }

    // ── 카메라 바로가기 핸들러 ──

    fun handleCameraClick() {
        isGalleryOpen = false
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── 전송 핸들러 ──

    fun handleSend() {
        val text = inputText.trim()
        val mediaUri = pendingMediaUri
        val mediaType = pendingMediaType
        if (text.isEmpty() && mediaUri == null) return

        messages.add(
            ChatMessage(
                text = text, isUser = true,
                mediaUri = mediaUri, mediaType = mediaType,
            )
        )

        if (text.isNotEmpty()) {
            UnityBridge.sendMessage("PocCharacter", "ShowBubble", text)
        }

        inputText = ""
        pendingMediaUri = null
        pendingMediaType = null

        // PoC 더미 응답
        val replyText = when {
            mediaUri != null && text.isNotEmpty() -> "이미지와 함께 「$text」… 감사해요."
            mediaUri != null -> "멋진 ${if (mediaType == MediaType.Video) "영상" else "사진"}이네요!"
            else -> "「$text」… 흥미로운 이야기네요."
        }
        messages.add(ChatMessage(text = replyText, isUser = false))
    }

    // ── Unity → Compose 콜백 ──

    DisposableEffect(Unit) {
        UnityBridge.onMessageFromUnity = { _, method, msg ->
            when (method) {
                "ChatReply" -> messages.add(ChatMessage(text = msg, isUser = false))
            }
        }
        onDispose { UnityBridge.onMessageFromUnity = null }
    }

    // ── UI ──

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    isGalleryOpen = false
                })
            }
    ) {
        AndroidView(
            factory = { UnityBridge.getView()!! },
            modifier = Modifier.fillMaxSize(),
        )

        EternaChatOverlay(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter),
            messages = messages,
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = ::handleSend,
            onEmotion = { emotion ->
                UnityBridge.sendMessage("PocCharacter", "PlayEmotion", emotion)
                messages.add(ChatMessage(text = "[$emotion 감정 재생]", isUser = false))
            },
            // 갤러리
            onAttachToggle = ::handleAttachToggle,
            isGalleryOpen = isGalleryOpen,
            galleryItems = galleryItems,
            onGalleryItemClick = ::handleGalleryItemClick,
            onCameraClick = ::handleCameraClick,
            // 마이크
            onMicClick = {
                if (isRecording) {
                    isRecording = false
                    Toast.makeText(context, "녹음 중지 (PoC)", Toast.LENGTH_SHORT).show()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            isRecording = isRecording,
            pendingMediaUri = pendingMediaUri,
            pendingMediaType = pendingMediaType,
            onClearPendingMedia = {
                pendingMediaUri = null
                pendingMediaType = null
            },
        )
    }
}

// ── FileProvider URI 헬퍼 ──

private fun createTempUri(context: android.content.Context, prefix: String, suffix: String): Uri {
    val file = File.createTempFile(prefix, suffix, context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}