package com.bobdodd.lidaraccessibility.android.ui.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bobdodd.lidaraccessibility.core.chat.ChatController
import com.bobdodd.lidaraccessibility.core.chat.ChatState
import com.bobdodd.lidaraccessibility.core.chat.ConversationLogEntry
import com.bobdodd.lidaraccessibility.core.chat.ConversationLogKind
import com.bobdodd.lidaraccessibility.core.chat.ShushAction
import com.bobdodd.lidaraccessibility.core.heading.HeadingReading
import com.bobdodd.lidaraccessibility.core.heading.HeadingSource
import com.bobdodd.lidaraccessibility.core.location.FollowMe
import com.bobdodd.lidaraccessibility.core.location.FollowMeEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * v1 surface: no visible map. Shows heading readout, Follow Me toggle,
 * mic button, text input, and a scrollable conversation log with all
 * user messages and TTS announcements (newest at top).
 */
@Composable
fun KnowledgeMapScreen(
    chat: ChatController,
    heading: StateFlow<HeadingReading>,
    followMeEvent: StateFlow<FollowMeEvent?>,
    isFollowing: StateFlow<Boolean>,
) {
    val state by chat.state.collectAsState()
    val headingReading by heading.collectAsState()
    val followMe by followMeEvent.collectAsState()
    val following by isFollowing.collectAsState()
    val log by chat.log.collectAsState()

    var draft by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Knowledge Map",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            // Heading + FollowMe readout
            Text(
                text = headingLabel(headingReading),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = followMeLabel(followMe),
                style = MaterialTheme.typography.bodySmall,
            )

            // Status line
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state is ChatState.Thinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = "Waiting for server response"
                        },
                    )
                }
                Text(
                    text = stateLabel(state),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = stateDescription(state)
                    },
                )
            }

            // Button row: Mic + Follow Me toggle
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                OutlinedButton(
                    onClick = { chat.setFollowMeEnabled(!following) },
                    modifier = Modifier.semantics {
                        contentDescription = if (following) {
                            "Stop Follow Me button. Double tap to stop following."
                        } else {
                            "Start Follow Me button. Double tap to start following."
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (following) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
                        contentDescription = null,
                    )
                    Text(if (following) "Stop Follow Me" else "Follow Me")
                }
            }

            // Text input row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Type a message") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        if (draft.isNotBlank()) {
                            chat.submitText(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }

            // Conversation log — newest at top
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(log, key = { it.id }) { entry ->
                    val prefix = when (entry.kind) {
                        ConversationLogKind.USER -> "You: "
                        ConversationLogKind.ANNOUNCEMENT -> ""
                        ConversationLogKind.ERROR -> "Error: "
                    }
                    Text(
                        text = prefix + entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (entry.kind) {
                            ConversationLogKind.USER -> MaterialTheme.colorScheme.onSurface
                            ConversationLogKind.ANNOUNCEMENT -> MaterialTheme.colorScheme.primary
                            ConversationLogKind.ERROR -> MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
    }
}

private fun headingLabel(reading: HeadingReading): String {
    val yaw = reading.yawDeg?.let { "%.1f".format(it) } ?: "—"
    val trust = if (reading.trusted) "trusted" else "untrusted"
    val src = when (reading.source) {
        HeadingSource.ORIENTATION_FUSED -> "sensor"
        HeadingSource.GPS_COURSE -> "GPS"
        HeadingSource.NONE -> "none"
    }
    return "Heading: ${yaw}° ($src, $trust)"
}

private fun followMeLabel(event: FollowMeEvent?): String {
    return when (event) {
        is FollowMeEvent.Update -> {
            val bearing = event.bearingDeg?.let { "%.0f°".format(it) } ?: "—"
            "Follow: %.4f, %.4f (bearing $bearing)".format(event.fix.lat, event.fix.lon)
        }
        is FollowMeEvent.TurnCallout -> {
            val direction = FollowMe.headingToCompassDirection(event.toDeg)
            "Facing: $direction"
        }
        null -> "Follow: inactive"
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
    is ChatState.Idle -> "Listen"
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
