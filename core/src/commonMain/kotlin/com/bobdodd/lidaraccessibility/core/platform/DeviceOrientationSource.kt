package com.bobdodd.lidaraccessibility.core.platform

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic device orientation source.
 *
 * Android actual uses `SensorManager` with `SENSOR_TYPE_ROTATION_VECTOR`
 * (already tilt-compensated and fused). iOS actual uses
 * `CLLocationManager.heading` with `CMHeadingFilter` (`trueHeading` when
 * available).
 *
 * The core [HeadingSmoother] applies retry, accuracy gating, and
 * GPS-course override on top; tilt compensation is deliberately NOT the
 * core's job because both platforms already do it.
 */
interface DeviceOrientationSource {
    val updates: Flow<Orientation>
    fun start()
    fun stop()
}

data class Orientation(
    /** Degrees clockwise from magnetic north (0 = north, 90 = east). */
    val yawDeg: Double,
    val pitchDeg: Double,
    val rollDeg: Double,
    val accuracy: OrientationAccuracy,
    val timestampMs: Long,
)

enum class OrientationAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH,
}
