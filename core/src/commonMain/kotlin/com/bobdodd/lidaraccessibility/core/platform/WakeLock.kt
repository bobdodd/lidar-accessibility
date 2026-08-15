package com.bobdodd.lidaraccessibility.core.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps the screen on while the app is active.
 *
 * Android actual sets `FLAG_KEEP_SCREEN_ON` on the current window and
 * releases/reacquires on activity `onPause` / `onResume`. iOS actual
 * toggles `UIApplication.shared.isIdleTimerDisabled`.
 *
 * Matches the web Knowledge Map's `visibilitychange` behaviour: the
 * lock auto-releases when the app is backgrounded and is reacquired
 * when it returns to the foreground.
 */
interface WakeLock {
    val isHeld: StateFlow<Boolean>
    fun acquire()
    fun release()
}
