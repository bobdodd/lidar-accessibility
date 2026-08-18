package com.bobdodd.lidaraccessibility.core.heading

import com.bobdodd.lidaraccessibility.core.location.LocationSource
import com.bobdodd.lidaraccessibility.core.platform.DeviceOrientationSource
import com.bobdodd.lidaraccessibility.core.platform.Orientation
import com.bobdodd.lidaraccessibility.core.platform.OrientationAccuracy
import com.bobdodd.lidaraccessibility.core.util.AngleMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Compass-heading pipeline.
 *
 * Consumes fused orientation (TYPE_ROTATION_VECTOR on Android) and
 * optional GPS course data, producing a single smoothed, trusted
 * heading for the rest of the app.
 *
 * Pipeline stages:
 * 1. Accuracy gate — only HIGH-accuracy sensor readings feed the
 *    low-pass filter. LOW/MEDIUM/UNRELIABLE readings are ignored.
 * 2. Low-pass filter — AngleMath.lowPass in sin/cos space (wrap-safe).
 * 3. GPS course override — when enabled and speed > threshold, GPS
 *    bearing replaces sensor yaw as the trusted heading. Sensor
 *    low-pass state is kept separate so it survives override.
 * 4. Silent-sensor retry — if no orientation update arrives within
 *    [HeadingSettings.sensorSilentRetryMs], restart the orientation
 *    source up to [HeadingSettings.sensorSilentRetryMax] times.
 *
 * Tilt compensation is deliberately NOT done here — both platforms
 * provide fused, tilt-compensated yaw via their rotation-vector APIs.
 */
class HeadingSmoother(
    private val orientation: DeviceOrientationSource,
    private val location: LocationSource,
    private val settings: HeadingSettings = HeadingSettings.default(),
    private val scope: CoroutineScope,
) {
    private val _heading = MutableStateFlow(
        HeadingReading(yawDeg = null, trusted = false, source = HeadingSource.NONE, timestampMs = 0)
    )
    val heading = _heading.asStateFlow()

    private val mutex = Mutex()

    // Shared mutable state — guarded by mutex
    private var sensorYaw: Double? = null   // low-passed sensor heading (never overwritten by GPS)
    private var gpsOverrideActive = false
    private var retryCount = 0

    private var orientationJob: Job? = null
    private var locationJob: Job? = null
    private var silentRetryJob: Job? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true

        // Reset state from any previous run
        gpsOverrideActive = false
        retryCount = 0

        orientation.start()
        orientationJob = scope.launch { collectOrientation() }

        if (settings.gpsOverrideEnabled) {
            location.start()
            locationJob = scope.launch { collectLocation() }
        }

        armSilentRetry()
    }

    fun stop() {
        running = false
        orientationJob?.cancel()
        locationJob?.cancel()
        silentRetryJob?.cancel()
        orientation.stop()
        if (settings.gpsOverrideEnabled) {
            location.stop()
        }

        // Reset state and mark heading as untrusted on stop
        gpsOverrideActive = false
        retryCount = 0
        val current = _heading.value
        if (current.trusted) {
            _heading.value = current.copy(trusted = false)
        }
    }

    private suspend fun collectOrientation() {
        orientation.updates.collect { reading ->
            // Cancel silent retry timer — we got an update
            silentRetryJob?.cancel()
            mutex.withLock { retryCount = 0 }

            processOrientation(reading)
            armSilentRetry()
        }
    }

    private suspend fun processOrientation(reading: Orientation) {
        mutex.withLock {
            // Don't let sensor readings override active GPS course
            if (gpsOverrideActive) return@withLock

            val rawYaw = AngleMath.norm360(reading.yawDeg)

            if (reading.accuracy == OrientationAccuracy.HIGH) {
                sensorYaw = AngleMath.lowPass(sensorYaw, rawYaw, settings.lowPassAlpha)
                _heading.value = HeadingReading(
                    yawDeg = sensorYaw,
                    trusted = true,
                    source = HeadingSource.ORIENTATION_FUSED,
                    timestampMs = reading.timestampMs,
                )
            } else {
                // Low/medium/unreliable — don't feed the filter.
                // Emit last known sensor yaw as untrusted so UI doesn't freeze.
                if (sensorYaw != null) {
                    _heading.value = HeadingReading(
                        yawDeg = sensorYaw,
                        trusted = false,
                        source = HeadingSource.ORIENTATION_FUSED,
                        timestampMs = reading.timestampMs,
                    )
                }
            }
        }
    }

    private suspend fun collectLocation() {
        location.updates.collect { fix ->
            mutex.withLock {
                val speed = fix.speedMps
                val bearing = fix.bearingDeg

                if (speed != null && bearing != null &&
                    speed > settings.gpsOverrideMps
                ) {
                    // Moving fast enough — GPS course is more reliable than compass.
                    // Do NOT overwrite sensorYaw — GPS bearing is direction of travel,
                    // not phone facing. They can differ.
                    gpsOverrideActive = true
                    val normalizedBearing = AngleMath.norm360(bearing.toDouble())
                    _heading.value = HeadingReading(
                        yawDeg = normalizedBearing,
                        trusted = true,
                        source = HeadingSource.GPS_COURSE,
                        timestampMs = fix.timestampMs,
                    )
                } else {
                    // Not moving fast enough — release override.
                    // Next HIGH-accuracy sensor reading will take over.
                    gpsOverrideActive = false
                }
            }
        }
    }

    private fun armSilentRetry() {
        if (!running) return
        silentRetryJob?.cancel()
        silentRetryJob = scope.launch {
            delay(settings.sensorSilentRetryMs)

            val shouldRetry = mutex.withLock {
                if (retryCount >= settings.sensorSilentRetryMax) {
                    // Exhausted retries — mark heading as untrusted
                    val current = _heading.value
                    if (current.trusted) {
                        _heading.value = current.copy(trusted = false)
                    }
                    false
                } else {
                    retryCount++
                    true
                }
            }

            if (!shouldRetry) return@launch

            // Restart orientation source
            orientation.stop()
            orientation.start()
            armSilentRetry()
        }
    }
}

data class HeadingSettings(
    val lowPassAlpha: Double,
    val gpsOverrideEnabled: Boolean,
    val gpsOverrideMps: Float,
    val sensorSilentRetryMs: Long,
    val sensorSilentRetryMax: Int,
) {
    companion object {
        fun default(): HeadingSettings = HeadingSettings(
            lowPassAlpha = 0.82,
            gpsOverrideEnabled = false,
            gpsOverrideMps = 1.0f,
            sensorSilentRetryMs = 1_500L,
            sensorSilentRetryMax = 4,
        )
    }
}

data class HeadingReading(
    val yawDeg: Double?,
    val trusted: Boolean,
    val source: HeadingSource,
    val timestampMs: Long,
)

enum class HeadingSource { ORIENTATION_FUSED, GPS_COURSE, NONE }
