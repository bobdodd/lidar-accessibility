package com.bobdodd.lidaraccessibility.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Android actual for [SpeechSynthesizer].
 *
 * Wraps `android.speech.tts.TextToSpeech`. The [isSpeaking] StateFlow is
 * driven by [UtteranceProgressListener] — this replaces the web version's
 * unreliable `synth.speaking` polling (see ADR 0002).
 */
class AndroidSpeechSynthesizer(
    context: Context,
    private val scope: CoroutineScope,
) : SpeechSynthesizer {

    private val _events = MutableSharedFlow<TtsEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    /** Maps utterance IDs back to their text for TtsEvent emission. */
    private val utteranceTexts = ConcurrentHashMap<String, String>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
                ready = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        val text = utteranceTexts[utteranceId].orEmpty()
                        scope.launch { _events.emit(TtsEvent.Started(text)) }
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        val text = utteranceTexts.remove(utteranceId).orEmpty()
                        scope.launch { _events.emit(TtsEvent.Finished(text)) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        val text = utteranceTexts.remove(utteranceId).orEmpty()
                        scope.launch { _events.emit(TtsEvent.Error("TTS error for: $text")) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        val text = utteranceTexts.remove(utteranceId).orEmpty()
                        scope.launch { _events.emit(TtsEvent.Error("TTS error $errorCode for: $text")) }
                    }
                })
            }
        }
    }

    override suspend fun speak(utterance: String, priority: TtsPriority) {
        if (!ready) return

        when (priority) {
            TtsPriority.INTERRUPTING -> tts?.stop()
            TtsPriority.ANNOUNCE_ONLY -> if (_isSpeaking.value) return
            TtsPriority.NORMAL -> { /* queue naturally via QUEUE_ADD */ }
        }

        val utteranceId = UUID.randomUUID().toString()
        utteranceTexts[utteranceId] = utterance
        tts?.speak(utterance, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    override fun cancel() {
        tts?.stop()
        _isSpeaking.value = false
    }

    override fun cancelAndAnnounce(utterance: String) {
        tts?.stop()
        _isSpeaking.value = false
        utteranceTexts.clear()
        val utteranceId = UUID.randomUUID().toString()
        utteranceTexts[utteranceId] = utterance
        tts?.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /** Call from Activity.onDestroy to release TTS resources. */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        utteranceTexts.clear()
    }
}
