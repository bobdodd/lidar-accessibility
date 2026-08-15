package com.bobdodd.lidaraccessibility.android.di

import android.content.Context

/**
 * Manual composition root. Constructor-injected, no framework.
 *
 * Only the type sketch is present in v1 scaffolding; the actual
 * platform actuals are wired in the next pass, alongside the
 * `androidMain` implementations in :core.
 */
class AppComponent(
    private val appContext: Context,
) {
    // Wiring is intentionally deferred until the platform actuals land.
    // See docs/architecture.md § "DI / composition root".
}
