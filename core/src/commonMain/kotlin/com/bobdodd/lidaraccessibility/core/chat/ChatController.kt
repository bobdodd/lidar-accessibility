package com.bobdodd.lidaraccessibility.core.chat

import com.bobdodd.lidaraccessibility.core.api.A11yBobApi
import com.bobdodd.lidaraccessibility.core.api.ChatRequest
import com.bobdodd.lidaraccessibility.core.api.ChatTurn
import com.bobdodd.lidaraccessibility.core.api.LocationHint
import com.bobdodd.lidaraccessibility.core.location.FollowMe
import com.bobdodd.lidaraccessibility.core.location.FollowMeEvent
import com.bobdodd.lidaraccessibility.core.memory.MemoryStore
import com.bobdodd.lidaraccessibility.core.platform.BusyCue
import com.bobdodd.lidaraccessibility.core.platform.NoOpBusyCue
import com.bobdodd.lidaraccessibility.core.stt.SpeechRecognizer
import com.bobdodd.lidaraccessibility.core.stt.SttConfig
import com.bobdodd.lidaraccessibility.core.stt.SttEvent
import com.bobdodd.lidaraccessibility.core.tts.SpeechSynthesizer
import com.bobdodd.lidaraccessibility.core.tts.TtsEvent
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
 * Also coordinates Follow Me mode:
 * - Intercepts "follow me" / "stop following me" voice commands
 * - Collects FollowMe events and announces turn callouts as
 *   clockface directions via TTS
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
    private val busyCue: BusyCue = NoOpBusyCue,
) {
    private val _state = MutableStateFlow<ChatState>(ChatState.Idle)
    val state = _state.asStateFlow()

    private val _transcript = MutableStateFlow<List<ChatTurn>>(emptyList())
    val transcript = _transcript.asStateFlow()

    private val _log = MutableStateFlow<List<ConversationLogEntry>>(emptyList())
    val log = _log.asStateFlow()
    private var logSeq = 0L

    private val history = mutableListOf<ChatTurn>()

    private var sttJob: Job? = null
    private var apiJob: Job? = null
    private var idleJob: Job? = null
    private var speakingObserver: Job? = null
    private var chatTimeoutJob: Job? = null
    private var followMeEventJob: Job? = null
    private var followMeAnnouncementJob: Job? = null
    private var ttsLogJob: Job? = null
    private var busyCueJob: Job? = null
    @Volatile private var conversationActive = false

    init {
        // Collect FollowMe events and announce turn callouts
        followMeEventJob = scope.launch {
            followMe.events.collect { event ->
                when (event) {
                    is FollowMeEvent.TurnCallout -> {
                        announceFollowMeWhereAmI(event.toDeg)
                    }
                    is FollowMeEvent.Update -> { /* silent — position updates don't speak */ }
                }
            }
        }

        // Log all TTS output — captures chat replies, FollowMe announcements,
        // voice command confirmations, abort announcements
        ttsLogJob = scope.launch {
            tts.events.collect { event ->
                when (event) {
                    is TtsEvent.Started -> addLog(ConversationLogKind.ANNOUNCEMENT, event.utterance)
                    is TtsEvent.Error -> addLog(ConversationLogKind.ERROR, event.message ?: "Speech error")
                    is TtsEvent.Finished -> Unit
                }
            }
        }
    }

    private fun addLog(kind: ConversationLogKind, text: String) {
        val entry = ConversationLogEntry(++logSeq, kind, text)
        _log.value = listOf(entry) + _log.value.take(199)
    }

    /** Start the audible busy cursor — a click every 2.5 seconds. */
    private fun startBusyCue() {
        busyCueJob?.cancel()
        busyCueJob = scope.launch {
            while (true) {
                busyCue.click()
                delay(2_500L)
            }
        }
    }

    /** Stop the audible busy cursor. */
    private fun stopBusyCue() {
        busyCueJob?.cancel()
        busyCueJob = null
    }

    /**
     * On a FollowMe turn callout, ask the backend "Where am I?" with
     * the current heading and location. The response is spoken with
     * INTERRUPTING priority so navigation updates cut through.
     */
    private fun announceFollowMeWhereAmI(headingDeg: Double) {
        followMeAnnouncementJob?.cancel()
        followMeAnnouncementJob = scope.launch {
            val direction = FollowMe.headingToCompassDirection(headingDeg)
            val locationHint = locationHintProvider()

            if (locationHint == null) {
                tts.speak("You are facing $direction. I do not have your location yet.", TtsPriority.INTERRUPTING)
                return@launch
            }

            val request = ChatRequest(
                message = "Where am I? This is an automatic Follow Me update. Start exactly with: You are facing $direction. Then give a brief useful location update for a blind traveler. Do not use clockface directions unless describing where a place or object is relative to me.",
                location = LocationHint(
                    lat = locationHint.lat,
                    lon = locationHint.lon,
                    heading = headingDeg,
                ),
                history = emptyList(),
                memory = memory.snapshot(),
                modality = "voice",
            )

            startBusyCue()
            val response = withTimeoutOrNull(Backstops.CALL_MS) {
                api.chat(request)
            }
            stopBusyCue()

            val reply = response?.reply
            if (!reply.isNullOrBlank()) {
                tts.speak(reply, TtsPriority.INTERRUPTING)
            } else {
                tts.speak("You are facing $direction.", TtsPriority.INTERRUPTING)
            }
        }
    }

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

    /** Toggle Follow Me mode on/off. */
    fun setFollowMeEnabled(enabled: Boolean) {
        scope.launch {
            if (enabled) {
                followMe.start()
                tts.speak("Following.", TtsPriority.INTERRUPTING)
            } else {
                followMe.stop()
                tts.speak("Stopped following.", TtsPriority.INTERRUPTING)
            }
        }
    }

    /** Submit a typed text message (from the text input field). */
    fun submitText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        scope.launch {
            addLog(ConversationLogKind.USER, trimmed)

            val command = parseFollowMeCommand(trimmed.lowercase())
            if (command != null) {
                setFollowMeEnabled(command)
                return@launch
            }

            _state.value = ChatState.Thinking
            processUserMessage(trimmed)
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
                if (_state.value !is ChatState.Listening) return@collect

                when (event) {
                    is SttEvent.Partial -> armIdleTimer()

                    is SttEvent.Final -> {
                        val text = event.text.trim()

                        addLog(ConversationLogKind.USER, text)

                        // Intercept Follow Me voice commands
                        val command = parseFollowMeCommand(text.lowercase())
                        if (command != null) {
                            idleJob?.cancel()
                            stt.stop()
                            setFollowMeEnabled(command)
                            // Restart listening after voice command
                            if (conversationActive) startListening()
                            return@collect
                        }

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

    /**
     * Check if the spoken text is a Follow Me voice command.
     * Returns true (start), false (stop), or null (not a command).
     */
    private fun parseFollowMeCommand(text: String): Boolean? {
        // Stop commands — check first so "stop following me" doesn't
        // match the "follow me" start pattern
        val stopPatterns = listOf(
            "stop following me",
            "stop follow me",
            "stop follow",
            "turn off follow me",
            "turn off following",
            "stop following",
        )
        if (stopPatterns.any { text.contains(it) }) return false

        // Start commands
        val startPatterns = listOf(
            "follow me",
            "start following me",
            "start follow me",
            "start following",
            "turn on follow me",
            "turn on following",
        )
        if (startPatterns.any { text.contains(it) }) return true

        return null
    }

    private fun processUserMessage(text: String) {
        history.add(ChatTurn("user", text))
        _transcript.value = history.toList()

        _state.value = ChatState.Thinking
        startBusyCue()

        apiJob = scope.launch {
            try {
                val response = withTimeoutOrNull(Backstops.CALL_MS) {
                    val locationHint = locationHintProvider()
                    val memoryItems = memory.snapshot()

                    val request = ChatRequest(
                        message = text,
                        location = locationHint,
                        history = history.dropLast(1),
                        memory = memoryItems,
                        modality = "voice",
                    )
                    api.chat(request)
                }

                if (response == null) {
                    stopBusyCue()
                    endConversation(ChatState.Failed("Request timed out"))
                    return@launch
                }

                if (response.error != null) {
                    stopBusyCue()
                    endConversation(ChatState.Failed(response.error))
                    return@launch
                }

                response.memory?.let { memory.replace(it) }

                history.add(ChatTurn("assistant", response.reply))
                _transcript.value = history.toList()

                stopBusyCue()
                _state.value = ChatState.Speaking(response.reply)
                tts.speak(response.reply, TtsPriority.NORMAL)

            } catch (e: Exception) {
                stopBusyCue()
                endConversation(ChatState.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    private fun startSpeakingObserver() {
        speakingObserver?.cancel()
        speakingObserver = scope.launch {
            var wasSpeaking = false
            tts.isSpeaking.collect { speaking ->
                if (wasSpeaking && !speaking && _state.value is ChatState.Speaking) {
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
            cancelAllJobs()
            stt.cancel()
            tts.cancel()
            endConversation(ChatState.Failed("Chat turn timed out"))
        }
    }

    private fun endConversation(finalState: ChatState) {
        idleJob?.cancel()
        chatTimeoutJob?.cancel()
        speakingObserver?.cancel()
        stopBusyCue()
        _state.value = finalState
        conversationActive = false
    }

    private fun cancelAllJobs() {
        sttJob?.cancel()
        apiJob?.cancel()
        idleJob?.cancel()
        chatTimeoutJob?.cancel()
        stopBusyCue()
    }

    /** Reset the conversation: clear history, return to Idle. */
    fun reset() {
        scope.launch {
            cancelAllJobs()
            speakingObserver?.cancel()
            stt.cancel()
            tts.cancel()
            followMe.stop()
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

data class ConversationLogEntry(
    val id: Long,
    val kind: ConversationLogKind,
    val text: String,
)

enum class ConversationLogKind {
    USER,
    ANNOUNCEMENT,
    ERROR,
}
