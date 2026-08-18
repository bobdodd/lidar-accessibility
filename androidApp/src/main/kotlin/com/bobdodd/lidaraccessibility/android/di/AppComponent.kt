package com.bobdodd.lidaraccessibility.android.di

import android.content.Context
import com.bobdodd.lidaraccessibility.core.api.A11yBobApi
import com.bobdodd.lidaraccessibility.core.api.A11yBobApiImpl
import com.bobdodd.lidaraccessibility.core.api.LocationHint
import com.bobdodd.lidaraccessibility.core.chat.ChatController
import com.bobdodd.lidaraccessibility.core.heading.HeadingSmoother
import com.bobdodd.lidaraccessibility.core.location.FollowMe
import com.bobdodd.lidaraccessibility.core.location.FusedLocationSource
import com.bobdodd.lidaraccessibility.core.location.LocationSource
import com.bobdodd.lidaraccessibility.core.memory.InMemoryMemoryStore
import com.bobdodd.lidaraccessibility.core.memory.MemoryStore
import com.bobdodd.lidaraccessibility.core.platform.AndroidWakeLock
import com.bobdodd.lidaraccessibility.core.platform.AndroidBusyCue
import com.bobdodd.lidaraccessibility.core.platform.BusyCue
import com.bobdodd.lidaraccessibility.core.platform.DeviceOrientationSource
import com.bobdodd.lidaraccessibility.core.platform.RotationVectorOrientationSource
import com.bobdodd.lidaraccessibility.core.platform.WakeLock
import com.bobdodd.lidaraccessibility.core.stt.AndroidSpeechRecognizer
import com.bobdodd.lidaraccessibility.core.stt.SpeechRecognizer
import com.bobdodd.lidaraccessibility.core.tts.AndroidSpeechSynthesizer
import com.bobdodd.lidaraccessibility.core.tts.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Manual composition root. Constructor-injected, no framework.
 *
 * Wires the six platform actuals + ChatController, HeadingSmoother,
 * and FollowMe. See docs/architecture.md § "DI / composition root".
 */
class AppComponent(
    private val appContext: Context,
) {
    private val applicationJob = SupervisorJob()
    private val applicationScope = CoroutineScope(applicationJob + Dispatchers.Default)

    val api: A11yBobApi = A11yBobApiImpl()

    val memory: MemoryStore = InMemoryMemoryStore()

    val location: LocationSource = FusedLocationSource(appContext)

    val orientation: DeviceOrientationSource = RotationVectorOrientationSource(appContext)

    val wakeLock: WakeLock = AndroidWakeLock(appContext)

    val busyCue: BusyCue = AndroidBusyCue()

    val tts: SpeechSynthesizer = AndroidSpeechSynthesizer(appContext, applicationScope)

    val stt: SpeechRecognizer = AndroidSpeechRecognizer(appContext, applicationScope)

    val heading = HeadingSmoother(orientation, location, scope = applicationScope)

    val followMe = FollowMe(location, heading, scope = applicationScope)

    val chat = ChatController(
        api = api,
        memory = memory,
        stt = stt,
        tts = tts,
        followMe = followMe,
        scope = applicationScope,
        locationHintProvider = {
            val headingReading = heading.heading.value
            location.getCurrent(timeoutMs = 5_000)?.let { fix ->
                LocationHint(
                    lat = fix.lat,
                    lon = fix.lon,
                    heading = headingReading.yawDeg?.takeIf { headingReading.trusted },
                )
            }
        },
        busyCue = busyCue,
    )

    /** Start background sensors (heading only — FollowMe starts on demand). */
    fun startSensors() {
        heading.start()
    }

    /** Release resources that hold native objects (TTS engine, etc.). */
    fun shutdown() {
        followMe.stop()
        heading.stop()
        applicationJob.cancel()
        (tts as? AndroidSpeechSynthesizer)?.shutdown()
        (busyCue as? AndroidBusyCue)?.release()
        orientation.stop()
        location.stop()
    }
}
