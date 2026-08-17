package com.bobdodd.lidaraccessibility.core.location

import com.bobdodd.lidaraccessibility.core.heading.HeadingSmoother
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Ports the web Knowledge Map "follow me" cadence.
 *
 * v1 minimal implementation: exposes an empty event flow.
 * Full implementation (15m/8s cadence, 45° turn callouts,
 * circular-mean detector) comes in step 4.
 */
class FollowMe(
    private val locations: LocationSource,
    private val heading: HeadingSmoother,
    private val distanceMeters: Double = 15.0,
    private val timeMs: Long = 8_000L,
    private val turnDegrees: Double = 45.0,
    private val turnSettleMs: Long = 700L,
    scope: CoroutineScope,
) {
    val events: Flow<FollowMeEvent> = emptyFlow()

    fun start() {
        // Step 4: collect location + heading updates, emit Update
        // when moved >= 15m or 8s passed, emit TurnCallout on
        // >= 45° heading change after settle window.
    }

    fun stop() {
        locations.stop()
    }
}

sealed interface FollowMeEvent {
    data class Update(val fix: Fix, val bearingDeg: Double?) : FollowMeEvent
    data class TurnCallout(val fromDeg: Double, val toDeg: Double) : FollowMeEvent
}
