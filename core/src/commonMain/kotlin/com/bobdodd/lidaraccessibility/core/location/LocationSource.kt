package com.bobdodd.lidaraccessibility.core.location

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic location provider.
 *
 * Android actual wraps `FusedLocationProviderClient`; iOS actual wraps
 * `CLLocationManager`. Course-over-ground is optional and only present
 * when the platform's speed estimate exceeds ~1 m/s (see the heading
 * pipeline's GPS-override logic).
 */
interface LocationSource {
    val updates: Flow<Fix>
    suspend fun getCurrent(timeoutMs: Long = 5_000L): Fix?
    fun start()
    fun stop()
}

data class Fix(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val bearingDeg: Float?,
    val timestampMs: Long,
)
