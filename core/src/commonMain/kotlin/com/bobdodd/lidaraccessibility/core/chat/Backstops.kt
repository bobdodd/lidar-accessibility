package com.bobdodd.lidaraccessibility.core.chat

/**
 * Turn-taking backstop timers. Values match the web Knowledge Map so
 * that any tuning happens in one place and lives with the reading-pass
 * notes.
 */
object Backstops {
    /** Overall per-chat-turn ceiling. */
    const val CHAT_MS: Long = 75_000L

    /** Ceiling per LLM call. */
    const val CALL_MS: Long = 25_000L

    /** Ceiling for a whole outbound request budget. */
    const val REQUEST_MS: Long = 60_000L

    /** Ceiling per tool invocation inside a turn. */
    const val TOOL_MS: Long = 20_000L

    /** Runaway-speech ceiling: cancel TTS if it exceeds this. */
    const val SPEECH_MS: Long = 180_000L

    /** STT idle timeout. Matches the web's re-arm-on-word-recognised loop. */
    const val STT_IDLE_MS: Long = 10_000L
}
