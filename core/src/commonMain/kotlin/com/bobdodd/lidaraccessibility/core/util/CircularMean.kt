package com.bobdodd.lidaraccessibility.core.util

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Wrap-safe helpers for angles in degrees.
 *
 * Ports the sin/cos-space maths from `HeadingProvider.js` so the same
 * algorithms run identically on Android and iOS.
 */
object AngleMath {

    /** Normalise an angle to [0, 360). */
    fun norm360(deg: Double): Double {
        val m = deg % 360.0
        return if (m < 0) m + 360.0 else m
    }

    /** Signed shortest angular difference `a - b` in (-180, 180]. */
    fun delta(a: Double, b: Double): Double {
        var d = (a - b + 540.0) % 360.0 - 180.0
        if (d == -180.0) d = 180.0
        return d
    }

    /** Absolute shortest angular difference in [0, 180]. */
    fun absDelta(a: Double, b: Double): Double = kotlin.math.abs(delta(a, b))

    /**
     * Circular mean of a list of angles (degrees). Returns null on empty.
     */
    fun circularMean(anglesDeg: List<Double>): Double? {
        if (anglesDeg.isEmpty()) return null
        var sx = 0.0
        var sy = 0.0
        for (a in anglesDeg) {
            val r = a * PI / 180.0
            sx += cos(r)
            sy += sin(r)
        }
        val mean = atan2(sy, sx) * 180.0 / PI
        return norm360(mean)
    }

    /**
     * Wrap-safe low-pass in sin/cos space.
     *
     * `alpha` is the weight on the previous value (0..1). The web
     * `HeadingProvider` uses 0.82 for orientation, so the previous
     * value dominates and the low-pass smooths jitter without lagging
     * significantly on real turns.
     */
    fun lowPass(prev: Double?, next: Double, alpha: Double = 0.82): Double {
        if (prev == null) return norm360(next)
        val pr = prev * PI / 180.0
        val nr = next * PI / 180.0
        val sx = alpha * cos(pr) + (1.0 - alpha) * cos(nr)
        val sy = alpha * sin(pr) + (1.0 - alpha) * sin(nr)
        return norm360(atan2(sy, sx) * 180.0 / PI)
    }
}
