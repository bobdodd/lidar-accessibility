package com.bobdodd.lidaraccessibility.core.stt

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic streaming speech-to-text.
 *
 * Android actual wraps `android.speech.SpeechRecognizer` (with
 * `createOnDeviceSpeechRecognizer` on API 33+). iOS actual wraps
 * `SpeechAnalyzer` (iOS 26+) with `SFSpeechRecognizer` as a fallback.
 *
 * See ADR 0003 (docs/decisions/0003-speech-to-text.md).
 */
interface SpeechRecognizer {
    val events: Flow<SttEvent>
    suspend fun start(config: SttConfig = SttConfig())
    fun stop()
    fun cancel()
}

/**
 * Config for a single STT session.
 *
 * `idleTimeoutMs` matches the web Knowledge Map's 10 s idle-and-restart
 * behaviour. `preferOnDevice` is a hint; platforms fall back if the
 * on-device engine is unavailable for the requested language.
 */
data class SttConfig(
    val languageTag: String = "en-US",
    val preferOnDevice: Boolean = true,
    val idleTimeoutMs: Long = 10_000L,
)

sealed interface SttEvent {
    data class Partial(val text: String) : SttEvent
    data class Final(val text: String) : SttEvent
    data object EndOfSpeech : SttEvent
    data object NoMatch : SttEvent
    data class Error(val kind: SttErrorKind, val message: String? = null) : SttEvent
}

enum class SttErrorKind {
    NETWORK,
    AUDIO,
    PERMISSION,
    SERVER,
    LANGUAGE_UNAVAILABLE,
    UNKNOWN,
}
