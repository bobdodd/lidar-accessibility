package com.bobdodd.lidaraccessibility.core.platform

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android actual for [WakeLock].
 *
 * NOTE: This uses PARTIAL_WAKE_LOCK to keep the CPU running during
 * voice sessions even when the screen is off. The architecture spec
 * also calls for `Window.addFlags(FLAG_KEEP_SCREEN_ON)` via the
 * Activity lifecycle (release on onPause, reacquire on onResume).
 * That screen-on wakelock will be added in the Activity wiring step;
 * this class handles the voice-processing CPU wakelock only.
 */
class AndroidWakeLock(context: Context) : WakeLock {

    private val wakeLock: PowerManager.WakeLock =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lidar-accessibility::voice-session")

    private val _isHeld = MutableStateFlow(false)
    override val isHeld = _isHeld.asStateFlow()

    override fun acquire() {
        if (!_isHeld.value) {
            wakeLock.acquire()
            _isHeld.value = true
        }
    }

    override fun release() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        _isHeld.value = false
    }
}
