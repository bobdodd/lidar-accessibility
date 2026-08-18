package com.bobdodd.lidaraccessibility.core.location

import com.bobdodd.lidaraccessibility.core.heading.HeadingSmoother
import com.bobdodd.lidaraccessibility.core.util.AngleMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * "Follow Me" navigation mode.
 *
 * User-initiated toggle (via UI button or voice command). While
 * active, monitors heading and position:
 *
 * - Turn detection: when the user's heading has changed by more
 *   than [turnThresholdDegrees] (30° = 360/12) since the last
 *   announcement, a [FollowMeEvent.TurnCallout] is emitted with the
 *   new heading. The caller announces "You are facing [clock] o'clock."
 *
 * - [FollowMeEvent.Update] — periodic position/bearing context
 *   (every 15m or 8s, whichever comes first).
 *
 * FollowMe is a measurement layer — it does not speak. The
 * ChatController collects events and announces them via TTS.
 */
class FollowMe(
    private val locations: LocationSource,
    private val heading: HeadingSmoother,
    private val distanceMeters: Double = 15.0,
    private val timeMs: Long = 8_000L,
    private val turnThresholdDegrees: Double = 30.0, // 360 / 12
    private val scope: CoroutineScope,
) {
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = _isFollowing.asStateFlow()

    private val _events = MutableSharedFlow<FollowMeEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val events = _events.asSharedFlow()

    private val _lastEvent = MutableStateFlow<FollowMeEvent?>(null)
    val lastEvent = _lastEvent.asStateFlow()

    private val mutex = Mutex()

    // Shared state — guarded by mutex
    private var latestFix: Fix? = null
    private var lastEmittedFix: Fix? = null
    private var latestTrustedHeading: Double? = null
    private var lastAnnouncedHeading: Double? = null

    private var locationJob: Job? = null
    private var headingJob: Job? = null
    private var timerJob: Job? = null

    fun start() {
        if (_isFollowing.value) return
        _isFollowing.value = true

        // Reset state
        latestFix = null
        lastEmittedFix = null
        latestTrustedHeading = null
        lastAnnouncedHeading = null

        locations.start()
        locationJob = scope.launch { collectLocations() }
        headingJob = scope.launch { collectHeading() }
    }

    fun stop() {
        if (!_isFollowing.value) return
        _isFollowing.value = false

        locationJob?.cancel()
        headingJob?.cancel()
        timerJob?.cancel()
        locations.stop()
    }

    private suspend fun collectLocations() {
        locations.updates.collect { fix ->
            if (!_isFollowing.value) return@collect

            val event: FollowMeEvent? = mutex.withLock {
                latestFix = fix

                val shouldEmit = lastEmittedFix == null ||
                    haversineMeters(lastEmittedFix!!, fix) >= distanceMeters

                if (shouldEmit) {
                    lastEmittedFix = fix
                    FollowMeEvent.Update(fix, latestTrustedHeading)
                } else {
                    null
                }
            }

            if (event != null) {
                _events.tryEmit(event)
                _lastEvent.value = event
            }

            armTimer()
        }
    }

    private suspend fun collectHeading() {
        heading.heading.collect { reading ->
            if (!_isFollowing.value) return@collect

            val event: FollowMeEvent? = mutex.withLock {
                if (reading.trusted && reading.yawDeg != null) {
                    val yaw = reading.yawDeg
                    latestTrustedHeading = yaw

                    if (lastAnnouncedHeading == null) {
                        // First trusted heading — announce immediately
                        lastAnnouncedHeading = yaw
                        FollowMeEvent.TurnCallout(yaw, yaw)
                    } else {
                        val change = AngleMath.absDelta(yaw, lastAnnouncedHeading!!)
                        if (change >= turnThresholdDegrees) {
                            val from = lastAnnouncedHeading!!
                            lastAnnouncedHeading = yaw
                            FollowMeEvent.TurnCallout(from, yaw)
                        } else {
                            null
                        }
                    }
                } else {
                    latestTrustedHeading = null
                    null
                }
            }

            if (event != null) {
                _events.tryEmit(event)
                _lastEvent.value = event
            }
        }
    }

    private fun armTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(timeMs)
            if (!_isFollowing.value) return@launch

            val event: FollowMeEvent? = mutex.withLock {
                val fix = latestFix ?: return@withLock null
                lastEmittedFix = fix
                FollowMeEvent.Update(fix, latestTrustedHeading)
            }

            if (event != null) {
                _events.tryEmit(event)
                _lastEvent.value = event
            }

            armTimer()
        }
    }

    companion object {
        /**
         * Convert an absolute heading (0-360°) to a compass direction.
         * 12 directions at 30° intervals (360/12):
         * 0° = north, 90° = east, 180° = south, 270° = west.
         */
        fun headingToCompassDirection(headingDeg: Double): String {
            val normalized = AngleMath.norm360(headingDeg)
            val sector = ((normalized + 15.0) / 30.0).toInt() % 12
            return when (sector) {
                0 -> "north"
                1 -> "north-northeast"
                2 -> "east-northeast"
                3 -> "east"
                4 -> "east-southeast"
                5 -> "south-southeast"
                6 -> "south"
                7 -> "south-southwest"
                8 -> "west-southwest"
                9 -> "west"
                10 -> "west-northwest"
                11 -> "north-northwest"
                else -> "north"
            }
        }

        /** Haversine distance in meters between two fixes. */
        private fun haversineMeters(a: Fix, b: Fix): Double {
            val r = 6_371_000.0
            val lat1 = Math.toRadians(a.lat)
            val lat2 = Math.toRadians(b.lat)
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLon = Math.toRadians(b.lon - a.lon)

            val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            val clamped = h.coerceIn(0.0, 1.0)
            val c = 2 * atan2(sqrt(clamped), sqrt(1 - clamped))
            return r * c
        }
    }
}

sealed interface FollowMeEvent {
    data class Update(val fix: Fix, val bearingDeg: Double?) : FollowMeEvent
    data class TurnCallout(val fromDeg: Double, val toDeg: Double) : FollowMeEvent
}
