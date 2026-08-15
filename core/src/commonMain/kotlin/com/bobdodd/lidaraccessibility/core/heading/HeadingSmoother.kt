package com.bobdodd.lidaraccessibility.core.heading

import com.bobdodd.lidaraccessibility.core.location.LocationSource
import com.bobdodd.lidaraccessibility.core.platform.DeviceOrientationSource
import com.bobdodd.lidaraccessibility.core.platform.OrientationAccuracy
import kotlinx.coroutines.flow.StateFlow

/**
 * Compass-heading pipeline.
 *
 * Ports what remains from `HeadingProvider.js` after both mobile
 * platforms are found to provide tilt-compensated fused yaw:
 *
 * - Low-pass in sin/cos space (default alpha = 0.82).
 * - Retry on silent sensor (up to 4 attempts, 1.5 s each).
 * - Accuracy gate: drop the value when [OrientationAccuracy] is
 *   `UNRELIABLE`.
 * - GPS course-over-ground override when the platform reports speed
 *   above [HeadingSettings.gpsOverrideMps]. Disabled by default in v1
 *   to match the current web behaviour (`gpsOverride = false`); a
 *   later ADR can flip this on once we validate against a Pixel 10.
 *
 * Tilt compensation is deliberately dropped from this layer.
 */
class HeadingSmoother(
    private val orientation: DeviceOrientationSource,
    private val location: LocationSource,
    private val settings: HeadingSettings = HeadingSettings.default(),
) {
    val heading: StateFlow<HeadingReading>
        get() = TODO("v1 scaffolding: implementation in the next pass")

    fun start(): Unit = TODO("v1 scaffolding: implementation in the next pass")
    fun stop(): Unit = TODO("v1 scaffolding: implementation in the next pass")
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
