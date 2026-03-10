package com.eterna.kee.media

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.FileProvider
import java.io.File

/**
 * 음성 녹음 + STT 동시 처리.
 *
 * MediaRecorder로 오디오 파일 저장,
 * SpeechRecognizer로 STT 텍스트 추출 — 둘을 병렬로 실행한다.
 */
class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var outputFile: File? = null
    private var sttResult: String = ""
    private var isRecording = false

    var onSttPartialResult: ((String) -> Unit)? = null

    data class RecordingResult(
        val audioUri: Uri,
        val durationMs: Long,
        val sttText: String,
    )

    fun startRecording(onError: (String) -> Unit = {}) {
        if (isRecording) return
        sttResult = ""

        // ── MediaRecorder ──
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        try {
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            onError("녹음 시작 실패: ${e.message}")
            cleanup()
            return
        }

        // ── SpeechRecognizer (STT) ──
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        // STT 실패해도 녹음은 계속 — 파일만이라도 전송
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            sttResult = matches[0]
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onSttPartialResult?.invoke(matches[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                startListening(intent)
            }
        }

        isRecording = true
    }

    fun stopRecording(): RecordingResult? {
        if (!isRecording) return null
        isRecording = false

        // MediaRecorder 중지
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }
        mediaRecorder = null

        // SpeechRecognizer 중지
        try {
            speechRecognizer?.apply {
                stopListening()
                destroy()
            }
        } catch (_: Exception) { }
        speechRecognizer = null

        // 결과 생성
        val file = outputFile ?: return null
        if (!file.exists() || file.length() == 0L) return null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        // 대략적인 duration 추출
        val durationMs = getAudioDuration(file)

        return RecordingResult(
            audioUri = uri,
            durationMs = durationMs,
            sttText = sttResult,
        )
    }

    fun cancelRecording() {
        isRecording = false
        try { mediaRecorder?.apply { stop(); release() } } catch (_: Exception) { }
        try { speechRecognizer?.apply { stopListening(); destroy() } } catch (_: Exception) { }
        mediaRecorder = null
        speechRecognizer = null
        outputFile?.delete()
        outputFile = null
    }

    private fun cleanup() {
        mediaRecorder = null
        speechRecognizer = null
        outputFile = null
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (_: Exception) { 0L }
    }
}