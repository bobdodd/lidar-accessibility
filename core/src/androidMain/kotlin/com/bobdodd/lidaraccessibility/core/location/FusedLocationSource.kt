package com.bobdodd.lidaraccessibility.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Android actual for [LocationSource].
 *
 * Wraps `FusedLocationProviderClient` with high-accuracy priority.
 * Course-over-ground (bearing) is only populated when the platform
 * reports it (typically when speed > 1 m/s).
 */
class FusedLocationSource(private val context: Context) : LocationSource {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _updates = MutableSharedFlow<Fix>(extraBufferCapacity = 64)
    override val updates = _updates.asSharedFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _updates.tryEmit(
                    Fix(
                        lat = loc.latitude,
                        lon = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        speedMps = if (loc.hasSpeed()) loc.speed else null,
                        bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                        timestampMs = loc.time,
                    )
                )
            }
        }
    }

    private fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun getCurrent(timeoutMs: Long): Fix? {
        if (!hasPermission()) return null

        // Try a fresh GPS fix first
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(
                                Fix(
                                    lat = loc.latitude,
                                    lon = loc.longitude,
                                    accuracyMeters = loc.accuracy,
                                    speedMps = if (loc.hasSpeed()) loc.speed else null,
                                    bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                                    timestampMs = loc.time,
                                )
                            )
                        } else {
                            cont.resume(null)
                        }
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
        if (fresh != null) return fresh

        // Fall back to last known cached location
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                client.lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(
                                Fix(
                                    lat = loc.latitude,
                                    lon = loc.longitude,
                                    accuracyMeters = loc.accuracy,
                                    speedMps = if (loc.hasSpeed()) loc.speed else null,
                                    bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                                    timestampMs = loc.time,
                                )
                            )
                        } else {
                            cont.resume(null)
                        }
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!hasPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()
        client.requestLocationUpdates(request, callback, context.mainLooper)
    }

    override fun stop() {
        client.removeLocationUpdates(callback)
    }
}
