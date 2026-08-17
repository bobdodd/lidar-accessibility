package com.bobdodd.lidaraccessibility.core.heading

import com.bobdodd.lidaraccessibility.core.location.LocationSource
import com.bobdodd.lidaraccessibility.core.platform.DeviceOrientationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Compass-heading pipeline.
 *
 * v1 minimal implementation: exposes a StateFlow with a NONE reading.
 * Full implementation (low-pass filter, accuracy gate, retry-on-silent,
 * GPS override) comes in step 3.
 *
 * Tilt compensation is deliberately dropped — both platforms provide
 * fused, tilt-compensated yaw via TYPE_ROTATION_VECTOR.
 */
class HeadingSmoother(
    private val orientation: DeviceOrientationSource,
    private val location: LocationSource,
    private val settings: HeadingSettings = HeadingSettings.default(),
    scope: CoroutineScope,
) {
    private val _heading = MutableStateFlow(
        HeadingReading(yawDeg = null, trusted = false, source = HeadingSource.NONE, timestampMs = 0)
    )
    val heading = _heading.asStateFlow()

    fun start() {
        // Step 3: start collecting orientation.updates, apply low-pass,
        // accuracy gate, and emit HeadingReading values.
    }

    fun stop() {
        orientation.stop()
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
