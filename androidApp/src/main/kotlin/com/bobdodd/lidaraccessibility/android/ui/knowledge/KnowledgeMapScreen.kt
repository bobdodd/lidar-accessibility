package com.bobdodd.lidaraccessibility.android.ui.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bobdodd.lidaraccessibility.android.R
import com.bobdodd.lidaraccessibility.core.chat.ChatController
import com.bobdodd.lidaraccessibility.core.chat.ChatState
import com.bobdodd.lidaraccessibility.core.chat.ShushAction

/**
 * v1 surface: no visible map, just status + transcript + mic control.
 *
 * The transcript is a polite live region so TalkBack announces new
 * turns without pulling focus. See docs/architecture.md § "Android
 * surface".
 */
@Composable
fun KnowledgeMapScreen(chat: ChatController) {
    val state by chat.state.collectAsState()
    val transcript by chat.transcript.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.knowledge_map_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            // Status area — live region so TalkBack announces state changes
            Text(
                text = stateLabel(state),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = stateDescription(state)
                },
            )

            // Mic button — behaviour depends on current state
            Button(
                onClick = { handleMicAction(chat, state) },
                modifier = Modifier.semantics {
                    contentDescription = micButtonDescription(state)
                },
            ) {
                Icon(
                    imageVector = if (state is ChatState.Listening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                )
                Text(micButtonLabel(state))
            }

            // Transcript — scrollable, polite live region
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
            ) {
                items(transcript) { turn ->
                    val isUser = turn.role == "user"
                    Text(
                        text = if (isUser) "You: ${turn.content}" else "Assistant: ${turn.content}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun stateLabel(state: ChatState): String = when (state) {
    is ChatState.Idle -> "Tap Listen to start"
    is ChatState.Listening -> "Listening..."
    is ChatState.Heard -> "Heard: ${state.text}"
    is ChatState.Thinking -> "Thinking..."
    is ChatState.Speaking -> "Speaking..."
    is ChatState.Failed -> "Error: ${state.message}"
}

private fun stateDescription(state: ChatState): String = when (state) {
    is ChatState.Idle -> "Ready. Tap the Listen button to begin."
    is ChatState.Listening -> "Listening. Tap Stop to cancel."
    is ChatState.Heard -> "Heard: ${state.text}"
    is ChatState.Thinking -> "Thinking. Tap to abort."
    is ChatState.Speaking -> "Speaking. Tap to stop."
    is ChatState.Failed -> "Error: ${state.message}. Tap Listen to retry."
}

private fun micButtonLabel(state: ChatState): String = when (state) {
    is ChatState.Idle -> stringListen()
    is ChatState.Listening -> "Stop"
    is ChatState.Heard -> "..."
    is ChatState.Thinking -> "Abort"
    is ChatState.Speaking -> "Stop"
    is ChatState.Failed -> "Retry"
}

private fun micButtonDescription(state: ChatState): String = when (state) {
    is ChatState.Idle -> "Listen button. Double tap to start listening."
    is ChatState.Listening -> "Stop button. Double tap to stop listening."
    is ChatState.Heard -> "Processing."
    is ChatState.Thinking -> "Abort button. Double tap to cancel the request."
    is ChatState.Speaking -> "Stop button. Double tap to stop speaking."
    is ChatState.Failed -> "Retry button. Double tap to try again."
}

private fun handleMicAction(chat: ChatController, state: ChatState) {
    when (state) {
        is ChatState.Idle -> chat.startListening()
        is ChatState.Listening -> chat.stopListening()
        is ChatState.Thinking -> chat.shush(ShushAction.ABORT_IN_FLIGHT)
        is ChatState.Speaking -> chat.shush(ShushAction.CANCEL_SPEECH)
        is ChatState.Failed -> chat.startListening()
        is ChatState.Heard -> { /* brief transitional state, no action */ }
    }
}

// These are inline because we can't use stringResource outside @Composable
private fun stringListen() = "Listen"
