package com.bobdodd.lidaraccessibility.android.ui.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer as AndroidSystemSpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bobdodd.lidaraccessibility.android.di.AppComponent
import com.bobdodd.lidaraccessibility.core.tts.TtsPriority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class DiagnosticResult(
    val name: String,
    val status: String, // PASS, FAIL, SKIP
    val detail: String,
) {
    val icon get() = when (status) {
        "PASS" -> "[PASS]"
        "FAIL" -> "[FAIL]"
        "SKIP" -> "[SKIP]"
        else -> "[...]"
    }
}

@Composable
fun DiagnosticsScreen(
    component: AppComponent,
    context: Context,
    onContinue: () -> Unit,
) {
    var results by remember { mutableStateOf(listOf<DiagnosticResult>()) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runDiagnostics() {
        scope.launch {
            running = true
            results = emptyList()

            // 1. TTS — try to speak and check it doesn't crash
            results = results + DiagnosticResult("TTS", "...", "Testing")
            try {
                component.tts.speak("Diagnostics starting", TtsPriority.INTERRUPTING)
                delay(1500)
                results = results.dropLast(1) + DiagnosticResult("TTS", "PASS", "Speak call succeeded")
            } catch (e: Exception) {
                results = results.dropLast(1) + DiagnosticResult("TTS", "FAIL", e.message ?: "Unknown error")
            }

            // 2. STT — check on-device availability
            results = results + DiagnosticResult("STT", "...", "Checking")
            try {
                val available = AndroidSystemSpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                results = results.dropLast(1) + DiagnosticResult(
                    "STT",
                    if (available) "PASS" else "FAIL",
                    if (available) "On-device recognition available" else "On-device NOT available"
                )
            } catch (e: Exception) {
                results = results.dropLast(1) + DiagnosticResult("STT", "FAIL", e.message ?: "Unknown error")
            }

            // 3. Location — try to get a fix
            results = results + DiagnosticResult("Location", "...", "Getting fix")
            try {
                val hasPermission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    results = results.dropLast(1) + DiagnosticResult("Location", "SKIP", "Permission not granted")
                } else {
                    val fix = component.location.getCurrent(timeoutMs = 5000)
                    if (fix != null) {
                        results = results.dropLast(1) + DiagnosticResult(
                            "Location",
                            "PASS",
                            "Lat %.4f, Lon %.4f, Acc %.0fm".format(fix.lat, fix.lon, fix.accuracyMeters)
                        )
                    } else {
                        results = results.dropLast(1) + DiagnosticResult("Location", "FAIL", "No fix within 5s")
                    }
                }
            } catch (e: Exception) {
                results = results.dropLast(1) + DiagnosticResult("Location", "FAIL", e.message ?: "Unknown error")
            }

            // 4. Orientation — start sensor, read one sample
            results = results + DiagnosticResult("Orientation", "...", "Reading sensor")
            try {
                component.orientation.start()
                val first = withTimeoutOrNull(3000) {
                    component.orientation.updates.first()
                }
                component.orientation.stop()
                if (first != null) {
                    results = results.dropLast(1) + DiagnosticResult(
                        "Orientation",
                        "PASS",
                        "Yaw %.1f°, Accuracy: %s".format(first.yawDeg, first.accuracy)
                    )
                } else {
                    results = results.dropLast(1) + DiagnosticResult("Orientation", "FAIL", "No reading within 3s")
                }
            } catch (e: Exception) {
                component.orientation.stop()
                results = results.dropLast(1) + DiagnosticResult("Orientation", "FAIL", e.message ?: "Unknown error")
            }

            // 5. WakeLock — acquire, check, release
            results = results + DiagnosticResult("WakeLock", "...", "Testing")
            try {
                component.wakeLock.acquire()
                val held = component.wakeLock.isHeld.value
                component.wakeLock.release()
                results = results.dropLast(1) + DiagnosticResult(
                    "WakeLock",
                    if (held) "PASS" else "FAIL",
                    if (held) "Acquired and released" else "Failed to acquire"
                )
            } catch (e: Exception) {
                results = results.dropLast(1) + DiagnosticResult("WakeLock", "FAIL", e.message ?: "Unknown error")
            }

            // 6. API — ping a11ybob.com
            results = results + DiagnosticResult("API", "...", "Calling a11ybob.com")
            try {
                val searchResults = component.api.mapSearch("coffee", limit = 1)
                results = results.dropLast(1) + DiagnosticResult(
                    "API",
                    "PASS",
                    "Got ${searchResults.size} result(s) from a11ybob.com"
                )
            } catch (e: Exception) {
                results = results.dropLast(1) + DiagnosticResult("API", "FAIL", e.message ?: "Unknown error")
            }

            // Final TTS announcement
            val passCount = results.count { it.status == "PASS" }
            component.tts.speak(
                "Diagnostics complete. $passCount of 6 tests passed.",
                TtsPriority.INTERRUPTING
            )

            running = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        runDiagnostics()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Step 1 Validation",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            Text(
                text = "Tests the six Android platform actuals.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    )
                },
                enabled = !running,
            ) {
                Text(if (running) "Running..." else "Run Diagnostics")
            }

            results.forEach { result ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = result.icon,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Column {
                        Text(
                            text = result.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = result.detail,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            if (!running && results.isNotEmpty()) {
                TextButton(onClick = onContinue) {
                    Text("Continue to Knowledge Map >")
                }
            }
        }
    }
}
