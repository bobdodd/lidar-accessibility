package com.bobdodd.lidaraccessibility.core.location

import com.bobdodd.lidaraccessibility.core.heading.HeadingSmoother
import kotlinx.coroutines.flow.Flow

/**
 * Ports the web Knowledge Map "follow me" cadence:
 *
 * - Emit a [FollowMeEvent.Update] whenever the user has moved
 *   >= [distanceMeters] OR [timeMs] has passed since the last update,
 *   whichever is sooner.
 * - Emit a [FollowMeEvent.TurnCallout] when the heading changes by
 *   >= [turnDegrees] and has held that new heading for
 *   [turnSettleMs]. Detected via the older-vs-newer-half circular-mean
 *   comparison from `knowledge-map.js`.
 */
class FollowMe(
    private val locations: LocationSource,
    private val heading: HeadingSmoother,
    private val distanceMeters: Double = 15.0,
    private val timeMs: Long = 8_000L,
    private val turnDegrees: Double = 45.0,
    private val turnSettleMs: Long = 700L,
) {
    val events: Flow<FollowMeEvent>
        get() = TODO("v1 scaffolding: implementation in the next pass")

    fun start(): Unit = TODO("v1 scaffolding: implementation in the next pass")
    fun stop(): Unit = TODO("v1 scaffolding: implementation in the next pass")
}

sealed interface FollowMeEvent {
    data class Update(val fix: Fix, val bearingDeg: Double?) : FollowMeEvent
    data class TurnCallout(val fromDeg: Double, val toDeg: Double) : FollowMeEvent
}
