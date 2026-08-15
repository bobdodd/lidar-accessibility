package com.bobdodd.lidaraccessibility.core.tts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic streaming text-to-speech.
 *
 * The [isSpeaking] StateFlow is the whole point: the chat controller
 * subscribes to it and opens the mic on the `true -> false` transition,
 * fixing the web version's `onend`-unreliable bug (see the reading pass
 * findings in the project wiki).
 *
 * Android actual wraps `android.speech.tts.TextToSpeech`; iOS actual
 * wraps `AVSpeechSynthesizer`. See ADR 0002.
 */
interface SpeechSynthesizer {
    val events: Flow<TtsEvent>
    val isSpeaking: StateFlow<Boolean>

    suspend fun speak(
        utterance: String,
        priority: TtsPriority = TtsPriority.NORMAL,
    )

    fun cancel()

    /** Cancel current utterance and immediately announce [utterance]. */
    fun cancelAndAnnounce(utterance: String)
}

sealed interface TtsEvent {
    data class Started(val utterance: String) : TtsEvent
    data class Finished(val utterance: String) : TtsEvent
    data class Error(val message: String?) : TtsEvent
}

enum class TtsPriority {
    /** Preempts anything currently speaking. */
    INTERRUPTING,

    /** Queues if something is speaking; speaks immediately otherwise. */
    NORMAL,

    /** Announcement-only; drop if the synthesizer is already busy. */
    ANNOUNCE_ONLY,
}
