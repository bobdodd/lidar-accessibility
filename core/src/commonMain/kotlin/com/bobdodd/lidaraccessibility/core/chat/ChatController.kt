package com.bobdodd.lidaraccessibility.core.chat

import com.bobdodd.lidaraccessibility.core.api.A11yBobApi
import com.bobdodd.lidaraccessibility.core.api.ChatRequest
import com.bobdodd.lidaraccessibility.core.api.ChatTurn
import com.bobdodd.lidaraccessibility.core.api.LocationHint
import com.bobdodd.lidaraccessibility.core.location.FollowMe
import com.bobdodd.lidaraccessibility.core.memory.MemoryStore
import com.bobdodd.lidaraccessibility.core.stt.SpeechRecognizer
import com.bobdodd.lidaraccessibility.core.stt.SttConfig
import com.bobdodd.lidaraccessibility.core.stt.SttEvent
import com.bobdodd.lidaraccessibility.core.tts.SpeechSynthesizer
import com.bobdodd.lidaraccessibility.core.tts.TtsPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Turn-taking state machine. Ports the web Knowledge Map chat loop.
 *
 * State flow: Idle → Listening → Heard → Thinking → Speaking →
 * (auto-restart) Listening, or → Failed/Idle on errors/timeouts.
 *
 * Owns the STT lifecycle, TTS speak queue, backstop timers, and
 * shush actions. Subscribes to [SpeechSynthesizer.isSpeaking] and
 * opens the mic on the true→false transition.
 *
 * Backstops implemented: CHAT_MS (75s turn ceiling), CALL_MS (25s
 * per LLM call), STT_IDLE_MS (10s silence). REQUEST_MS, TOOL_MS,
 * and SPEECH_MS are deferred to a later step.
 */
class ChatController(
    private val api: A11yBobApi,
    private val memory: MemoryStore,
    private val stt: SpeechRecognizer,
    private val tts: SpeechSynthesizer,
    private val followMe: FollowMe,
    private val scope: CoroutineScope,
    private val locationHintProvider: suspend () -> LocationHint? = { null },
) {
    private val _state = MutableStateFlow<ChatState>(ChatState.Idle)
    val state = _state.asStateFlow()

    private val _transcript = MutableStateFlow<List<ChatTurn>>(emptyList())
    val transcript = _transcript.asStateFlow()

    private val history = mutableListOf<ChatTurn>()

    private var sttJob: Job? = null
    private var apiJob: Job? = null
    private var idleJob: Job? = null
    private var speakingObserver: Job? = null
    private var chatTimeoutJob: Job? = null
    @Volatile private var conversationActive = false

    /**
     * Begin a listening session. If called from Idle or Failed,
     * starts a fresh conversation turn.
     */
    fun startListening() {
        scope.launch {
            conversationActive = true
            cancelAllJobs()
            _state.value = ChatState.Listening

            startSttCollection()
            armIdleTimer()
            startSpeakingObserver()
            startChatTimeout()
        }
    }

    /** Stop listening and return to Idle. */
    fun stopListening() {
        scope.launch {
            sttJob?.cancel()
            stt.stop()
            idleJob?.cancel()
            if (_state.value is ChatState.Listening) {
                endConversation(ChatState.Idle)
            }
        }
    }

    /**
     * Three shush actions from the web version:
     * 1. ABORT_IN_FLIGHT — cancel outstanding request, announce "Aborted"
     * 2. CANCEL_SPEECH — stop TTS, no announcement
     * 3. RESTART_LISTENING — cancel everything, start fresh STT session
     */
    fun shush(action: ShushAction) {
        when (action) {
            ShushAction.ABORT_IN_FLIGHT -> {
                apiJob?.cancel()
                tts.cancelAndAnnounce("Aborted")
                endConversation(ChatState.Idle)
            }
            ShushAction.CANCEL_SPEECH -> {
                tts.cancel()
                endConversation(ChatState.Idle)
            }
            ShushAction.RESTART_LISTENING -> {
                stt.cancel()
                apiJob?.cancel()
                tts.cancel()
                startListening()
            }
        }
    }

    private fun startSttCollection() {
        sttJob = scope.launch {
            try {
                stt.start(SttConfig())
            } catch (e: Exception) {
                endConversation(ChatState.Failed("STT start failed: ${e.message}"))
                return@launch
            }
            stt.events.collect { event ->
                // Guard against stale events after state has moved on
                if (_state.value !is ChatState.Listening) return@collect

                when (event) {
                    is SttEvent.Partial -> armIdleTimer()

                    is SttEvent.Final -> {
                        idleJob?.cancel()
                        stt.stop()
                        _state.value = ChatState.Heard(event.text)
                        processUserMessage(event.text)
                    }

                    is SttEvent.EndOfSpeech -> { /* keep collecting */ }

                    is SttEvent.NoMatch -> {
                        endConversation(ChatState.Idle)
                    }

                    is SttEvent.Error -> {
                        endConversation(ChatState.Failed(event.message ?: "Speech recognition error"))
                    }
                }
            }
        }
    }

    private fun processUserMessage(text: String) {
        history.add(ChatTurn("user", text))
        _transcript.value = history.toList()

        _state.value = ChatState.Thinking

        apiJob = scope.launch {
            try {
                val response = withTimeoutOrNull(Backstops.CALL_MS) {
                    val locationHint = locationHintProvider()
                    val memoryItems = memory.snapshot()

                    val request = ChatRequest(
                        message = text,
                        location = locationHint,
                        history = history.dropLast(1), // exclude the just-added user turn
                        memory = memoryItems,
                        modality = "voice",
                    )
                    api.chat(request)
                }

                if (response == null) {
                    endConversation(ChatState.Failed("Request timed out"))
                    return@launch
                }

                if (response.error != null) {
                    endConversation(ChatState.Failed(response.error))
                    return@launch
                }

                // Update memory from server response
                response.memory?.let { memory.replace(it) }

                // Add assistant reply to history
                history.add(ChatTurn("assistant", response.reply))
                _transcript.value = history.toList()

                // Speak the reply
                _state.value = ChatState.Speaking(response.reply)
                tts.speak(response.reply, TtsPriority.NORMAL)

            } catch (e: Exception) {
                endConversation(ChatState.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Observe TTS isSpeaking. On the true→false transition, if we're
     * in Speaking state, automatically restart listening.
     */
    private fun startSpeakingObserver() {
        speakingObserver?.cancel()
        speakingObserver = scope.launch {
            var wasSpeaking = false
            tts.isSpeaking.collect { speaking ->
                if (wasSpeaking && !speaking && _state.value is ChatState.Speaking) {
                    // TTS finished — auto-restart listening
                    if (conversationActive) {
                        startListening()
                    } else {
                        endConversation(ChatState.Idle)
                    }
                }
                wasSpeaking = speaking
            }
        }
    }

    private fun armIdleTimer() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(Backstops.STT_IDLE_MS)
            // Idle timeout — stop listening
            sttJob?.cancel()
            stt.stop()
            if (_state.value is ChatState.Listening) {
                endConversation(ChatState.Idle)
            }
        }
    }

    private fun startChatTimeout() {
        chatTimeoutJob?.cancel()
        chatTimeoutJob = scope.launch {
            delay(Backstops.CHAT_MS)
            // Overall chat turn timeout — cancel everything
            cancelAllJobs()
            stt.cancel()
            tts.cancel()
            endConversation(ChatState.Failed("Chat turn timed out"))
        }
    }

    /**
     * Centralized terminal-state transition. Cancels all timers and
     * observers, sets the final state, and marks the conversation inactive.
     */
    private fun endConversation(finalState: ChatState) {
        idleJob?.cancel()
        chatTimeoutJob?.cancel()
        speakingObserver?.cancel()
        _state.value = finalState
        conversationActive = false
    }

    private fun cancelAllJobs() {
        sttJob?.cancel()
        apiJob?.cancel()
        idleJob?.cancel()
        chatTimeoutJob?.cancel()
    }

    /** Reset the conversation: clear history, return to Idle. */
    fun reset() {
        scope.launch {
            cancelAllJobs()
            speakingObserver?.cancel()
            stt.cancel()
            tts.cancel()
            history.clear()
            _transcript.value = emptyList()
            _state.value = ChatState.Idle
            conversationActive = false
        }
    }
}

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
