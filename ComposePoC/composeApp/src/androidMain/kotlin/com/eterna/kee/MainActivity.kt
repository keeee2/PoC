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
import com.eterna.kee.media.VoiceRecorder
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

@Composable
private fun LauncherScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EternaColors.Background),
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

@Composable
private fun UnityScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }

    // 실제 물리 화면 높이 (키보드 상태와 무관하게 고정)
    val overlayHeight = remember {
        val dm = context.resources.displayMetrics
        (dm.heightPixels / dm.density * 0.55f).dp
    }

    // 미디어
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaType by remember { mutableStateOf<MediaType?>(null) }
    var isGalleryOpen by remember { mutableStateOf(false) }
    val galleryItems = remember { mutableStateListOf<LocalMediaItem>() }

    // 풀스크린 뷰어
    var fullScreenUri by remember { mutableStateOf<Uri?>(null) }
    var fullScreenMediaType by remember { mutableStateOf<MediaType?>(null) }

    // 음성
    var isRecording by remember { mutableStateOf(false) }
    var sttPartialText by remember { mutableStateOf("") }
    val voiceRecorder = remember {
        VoiceRecorder(context).apply {
            onSttPartialResult = { partial -> sttPartialText = partial }
        }
    }

    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun loadGallery() {
        scope.launch {
            val items = loadRecentMedia(context, limit = 50)
            galleryItems.clear()
            galleryItems.addAll(items)
        }
    }

    // ── 런처 & 권한 ──

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraPhotoUri != null) { pendingMediaUri = cameraPhotoUri; pendingMediaType = MediaType.Image }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createTempUri(context, "photo_", ".jpg")
            cameraPhotoUri = uri
            takePhotoLauncher.launch(uri)
        } else Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            voiceRecorder.startRecording(onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() })
            isRecording = true
            sttPartialText = ""
        } else Toast.makeText(context, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) { isGalleryOpen = true; focusManager.clearFocus(); loadGallery() }
        else Toast.makeText(context, "사진/동영상 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }

    // ── 핸들러 ──

    fun handleAttachToggle() {
        if (isGalleryOpen) { isGalleryOpen = false; return }
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        mediaPermissionLauncher.launch(perms)
    }

    fun handleGalleryItemClick(item: LocalMediaItem) {
        pendingMediaUri = item.uri
        pendingMediaType = when (item.type) { LocalMediaType.Image -> MediaType.Image; LocalMediaType.Video -> MediaType.Video }
    }

    fun handleCameraClick() { isGalleryOpen = false; cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }

    fun handleMicClick() {
        if (isRecording) {
            isRecording = false
            val result = voiceRecorder.stopRecording()
            sttPartialText = ""
            if (result != null) {
                messages.add(ChatMessage(text = "", isUser = true, audioUri = result.audioUri, audioDurationMs = result.durationMs, sttText = result.sttText.ifBlank { null }))
                messages.add(ChatMessage(text = if (result.sttText.isNotBlank()) "「${result.sttText}」… 잘 들었어요." else "음성 메시지를 받았어요.", isUser = false))
            } else Toast.makeText(context, "녹음에 실패했습니다", Toast.LENGTH_SHORT).show()
        } else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun handleSend() {
        val text = inputText.trim(); val mediaUri = pendingMediaUri; val mediaType = pendingMediaType
        if (text.isEmpty() && mediaUri == null) return
        messages.add(ChatMessage(text = text, isUser = true, mediaUri = mediaUri, mediaType = mediaType))
        if (text.isNotEmpty()) UnityBridge.sendMessage("PocCharacter", "ShowBubble", text)
        inputText = ""; pendingMediaUri = null; pendingMediaType = null
        val reply = when {
            mediaUri != null && text.isNotEmpty() -> "이미지와 함께 「$text」… 감사해요."
            mediaUri != null -> "멋진 ${if (mediaType == MediaType.Video) "영상" else "사진"}이네요!"
            else -> "「$text」… 흥미로운 이야기네요."
        }
        messages.add(ChatMessage(text = reply, isUser = false))
    }

    DisposableEffect(Unit) {
        UnityBridge.onMessageFromUnity = { _, method, msg ->
            when (method) { "ChatReply" -> messages.add(ChatMessage(text = msg, isUser = false)) }
        }
        onDispose { UnityBridge.onMessageFromUnity = null }
    }

    // ── UI ──

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus(); isGalleryOpen = false })
            }
    ) {
        AndroidView(factory = { UnityBridge.getView()!! }, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            EternaChatOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(overlayHeight)
                    .align(Alignment.BottomCenter),
                messages = messages, inputText = inputText,
                onInputChange = { inputText = it }, onSend = ::handleSend,
                onEmotion = { emotion ->
                    UnityBridge.sendMessage("PocCharacter", "PlayEmotion", emotion)
                    messages.add(ChatMessage(text = "[$emotion 감정 재생]", isUser = false))
                },
                onAttachToggle = ::handleAttachToggle, isGalleryOpen = isGalleryOpen,
                galleryItems = galleryItems, onGalleryItemClick = ::handleGalleryItemClick,
                onCameraClick = ::handleCameraClick, onMicClick = ::handleMicClick,
                isRecording = isRecording, sttPartialText = sttPartialText,
                pendingMediaUri = pendingMediaUri, pendingMediaType = pendingMediaType,
                onClearPendingMedia = { pendingMediaUri = null; pendingMediaType = null },
                onMediaClick = { uri, type ->
                    fullScreenUri = uri
                    fullScreenMediaType = type
                },
            )
        }

        // 풀스크린 미디어 뷰어 (Unity 위에 오버레이)
        FullScreenMediaViewer(
            uri = fullScreenUri,
            mediaType = fullScreenMediaType,
            onDismiss = {
                fullScreenUri = null
                fullScreenMediaType = null
            },
        )
    }
}

private fun createTempUri(context: android.content.Context, prefix: String, suffix: String): Uri {
    val file = File.createTempFile(prefix, suffix, context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}