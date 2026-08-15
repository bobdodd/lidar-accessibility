package com.bobdodd.lidaraccessibility.core

import com.bobdodd.lidaraccessibility.core.util.AngleMath
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AngleMathTest {

    @Test
    fun norm360_wraps_negative_and_over_360() {
        assertEquals(0.0, AngleMath.norm360(360.0), 1e-9)
        assertEquals(350.0, AngleMath.norm360(-10.0), 1e-9)
        assertEquals(10.0, AngleMath.norm360(370.0), 1e-9)
    }

    @Test
    fun delta_returns_signed_shortest_path() {
        assertEquals(10.0, AngleMath.delta(10.0, 0.0), 1e-9)
        assertEquals(-20.0, AngleMath.delta(350.0, 10.0), 1e-9)
        assertEquals(180.0, AngleMath.delta(180.0, 0.0), 1e-9)
    }

    @Test
    fun circular_mean_survives_wrap() {
        // Mean of 350 and 10 should be 0, not 180.
        val m = AngleMath.circularMean(listOf(350.0, 10.0))!!
        assertTrue(abs(AngleMath.delta(m, 0.0)) < 1e-6, "expected ~0, got $m")
    }

    @Test
    fun low_pass_prev_null_returns_normalised_next() {
        assertEquals(45.0, AngleMath.lowPass(null, 45.0), 1e-9)
        assertEquals(350.0, AngleMath.lowPass(null, -10.0), 1e-9)
    }

    @Test
    fun low_pass_biased_towards_prev() {
        // Previous 0 deg, next 90 deg, alpha 0.82 -> result should be
        // much closer to 0 than to 90 and never wrap.
        val out = AngleMath.lowPass(0.0, 90.0, alpha = 0.82)
        assertTrue(out in 0.0..45.0, "expected 0-45, got $out")
    }
}
