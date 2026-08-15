package com.bobdodd.lidaraccessibility.core.chat

import com.bobdodd.lidaraccessibility.core.api.A11yBobApi
import com.bobdodd.lidaraccessibility.core.location.FollowMe
import com.bobdodd.lidaraccessibility.core.memory.MemoryStore
import com.bobdodd.lidaraccessibility.core.stt.SpeechRecognizer
import com.bobdodd.lidaraccessibility.core.tts.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Turn-taking state machine.
 *
 * Owns the STT lifecycle, the TTS speak queue, the [Backstops] timers,
 * and the three shush actions from the web version:
 *
 * 1. In-flight abort: cancel an outstanding request; announce
 *    "Aborted".
 * 2. Speech cancel: stop the current utterance without announcing.
 * 3. Listening restart: cancel the current STT session and open a new
 *    one.
 *
 * Subscribes to [SpeechSynthesizer.isSpeaking] and opens the mic on
 * the `true -> false` transition (replaces the web's synth.speaking
 * poll).
 *
 * Snapshots [MemoryStore] before every [A11yBobApi] request and calls
 * [MemoryStore.replace] on every response.
 */
class ChatController(
    private val api: A11yBobApi,
    private val memory: MemoryStore,
    private val stt: SpeechRecognizer,
    private val tts: SpeechSynthesizer,
    private val followMe: FollowMe,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<ChatState>
        get() = TODO("v1 scaffolding: implementation in the next pass")

    fun startListening(): Unit = TODO("v1 scaffolding")
    fun stopListening(): Unit = TODO("v1 scaffolding")
    fun shush(action: ShushAction): Unit = TODO("v1 scaffolding")
}

/**
 * Coarse-grained UI-facing state. The Android composables read this,
 * dispatch user gestures back through [ChatController], and never
 * touch STT/TTS directly.
 */
sealed interface ChatState {
    data object Idle : ChatState
    data object Listening : ChatState
    data class Heard(val text: String) : ChatState
    data object Thinking : ChatState
    data class Speaking(val text: String) : ChatState
    data class Failed(val message: String) : ChatState
}

enum class ShushAction {
    ABORT_IN_FLIGHT,
    CANCEL_SPEECH,
    RESTART_LISTENING,
}
