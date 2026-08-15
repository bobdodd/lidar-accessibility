# Architecture spec — KMP core + Android app (v1)

**Status:** Draft, for review.
**Namespace:** `com.bobdodd.lidaraccessibility` (Kotlin), `com.bobdodd.lidaraccessibility.core.*` for the KMP core, `com.bobdodd.lidaraccessibility.android.*` for the Android surface.
**Related decisions:** ADRs [0001](./decisions/0001-mobile-stack.md), [0002](./decisions/0002-knowledge-map-port-strategy.md), [0003](./decisions/0003-speech-to-text.md), [0004](./decisions/0004-personal-memory-scope.md), [0005](./decisions/0005-disclaimer-gate.md).

## Goals

1. **Every algorithm that is not a platform primitive lives in shared code.** Chat orchestration, memory model, backstop timers, shush semantics, follow-me logic, and heading smoothing all belong in the KMP core so they cannot drift between Android and iOS.
2. **Every platform primitive is behind a small `expect`/`actual` interface.** STT, TTS, location, orientation, wake-lock, and persistence are named in shared code and implemented once per platform. The core never imports Android or iOS types.
3. **The UI is native.** No shared UI framework. Jetpack Compose on Android, SwiftUI on iOS. The core exposes state and events; the UI observes and dispatches.
4. **The Next.js backend is unchanged.** The core is a client of the existing a11ybob API endpoints; no new server work in v1.

## Repository layout

```
lidar-accessibility/
├── build.gradle.kts                  # root Gradle build (Kotlin DSL)
├── settings.gradle.kts               # includes :core and :androidApp
├── gradle/
│   └── libs.versions.toml            # single source of dependency versions
├── core/                             # Kotlin Multiplatform module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/bobdodd/lidaraccessibility/core/
│       │   ├── api/                  # HTTP client + a11ybob API DTOs
│       │   ├── chat/                 # ChatController, turn-taking state machine
│       │   ├── heading/              # HeadingSmoother, heading pipeline
│       │   ├── memory/               # MemoryStore interface + in-memory logic
│       │   ├── location/             # Location + FollowMe logic
│       │   ├── stt/                  # SpeechRecognizer interface + events
│       │   ├── tts/                  # SpeechSynthesizer interface + events
│       │   ├── platform/             # WakeLock, DeviceOrientation, Clock (expect)
│       │   ├── time/                 # Backstop timers, coroutine helpers
│       │   ├── util/                 # circular-mean, low-pass, dispatchers
│       │   └── AppComponent.kt       # top-level composition root (DI)
│       ├── commonTest/kotlin/...     # pure-Kotlin tests for logic
│       ├── androidMain/kotlin/com/bobdodd/lidaraccessibility/core/
│       │   ├── platform/             # actual WakeLock, DeviceOrientation, Clock
│       │   ├── memory/               # actual Room-backed MemoryStore
│       │   ├── stt/                  # actual SpeechRecognizer (Android)
│       │   ├── tts/                  # actual SpeechSynthesizer (Android TTS)
│       │   └── location/             # actual FusedLocationProvider adapter
│       ├── androidUnitTest/kotlin/...
│       ├── iosMain/kotlin/com/bobdodd/lidaraccessibility/core/
│       │   ├── platform/             # (deferred until iOS work starts)
│       │   ├── memory/               # SwiftData bridge
│       │   ├── stt/                  # SpeechAnalyzer / SFSpeechRecognizer
│       │   ├── tts/                  # AVSpeechSynthesizer
│       │   └── location/             # CLLocationManager adapter
│       └── iosTest/kotlin/...
├── androidApp/                       # Android application module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/bobdodd/lidaraccessibility/android/
│       │   ├── LidarAccessibilityApp.kt         # Application class
│       │   ├── MainActivity.kt
│       │   ├── ui/
│       │   │   ├── disclaimer/                   # DisclaimerGate composables
│       │   │   ├── knowledge/                    # Knowledge Map surface
│       │   │   ├── theme/                        # AAA colours, type scale (3:1 cap)
│       │   │   └── a11y/                         # focus helpers, live-region utils
│       │   ├── voice/
│       │   │   ├── AndroidSpeechRecognizerImpl.kt
│       │   │   └── AndroidTextToSpeechImpl.kt
│       │   ├── sensors/
│       │   │   ├── RotationVectorHeadingSource.kt
│       │   │   └── FusedLocationSource.kt
│       │   └── di/                              # composition root wiring
│       └── res/
│           ├── values/                          # strings (English)
│           ├── values-fr/                       # French (deferred)
│           └── xml/                             # accessibility service metadata
├── docs/                                        # existing
└── ...
```

**Design points:**

- `core/` has an Android target and an iOS target but **no Android UI dependency**. Anything that needs a `Context` lives in `androidApp/` or is passed in through a small adapter interface.
- `androidApp/` is the only module that depends on Compose, Jetpack, Room, and Android Speech / TTS APIs.
- The core is deliberately split by concern (`chat/`, `heading/`, `memory/`, ...) so that a single concern can be moved between shared and platform code without churning the whole tree.
- Room lives in `core/androidMain/` because Room's generated code is Android-only, but its **interface** (`MemoryStore`) is in `commonMain`. iOS gets a SwiftData actual behind the same interface.

## The KMP core surface

The core exposes six small interfaces. The Android app implements the six platform actuals and observes the state the core emits.

### 1. `SpeechRecognizer` (STT)

```kotlin
package com.bobdodd.lidaraccessibility.core.stt

interface SpeechRecognizer {
    val events: Flow<SttEvent>
    suspend fun start(config: SttConfig)
    fun stop()
    fun cancel()
}

sealed interface SttEvent {
    data class Partial(val text: String) : SttEvent
    data class Final(val text: String) : SttEvent
    data object EndOfSpeech : SttEvent
    data object NoMatch : SttEvent
    data class Error(val kind: SttErrorKind, val message: String?) : SttEvent
}

data class SttConfig(
    val languageTag: String = "en-US",
    val preferOnDevice: Boolean = true,
    val idleTimeoutMs: Long = 10_000L,     // matches web
)
```

- **Android actual:** wraps `SpeechRecognizer.createOnDeviceSpeechRecognizer()` on API 33+, falls back to online recognizer on older devices, translates `RecognitionListener` callbacks into `SttEvent`s.
- **iOS actual (deferred):** wraps `SpeechAnalyzer` on iOS 26+, `SFSpeechRecognizer` earlier.
- **No diarisation in v1.** ADR 0003 records this as a known gap.

### 2. `SpeechSynthesizer` (TTS)

```kotlin
interface SpeechSynthesizer {
    val events: Flow<TtsEvent>
    val isSpeaking: StateFlow<Boolean>
    suspend fun speak(utterance: String, priority: TtsPriority = TtsPriority.NORMAL)
    fun cancel()
    fun cancelAndAnnounce(utterance: String)   // "shush" pattern from web
}

sealed interface TtsEvent {
    data class Started(val utterance: String) : TtsEvent
    data class Finished(val utterance: String) : TtsEvent
    data class Error(val message: String?) : TtsEvent
}

enum class TtsPriority { INTERRUPTING, NORMAL, ANNOUNCE_ONLY }
```

- **Android actual:** wraps `android.speech.tts.TextToSpeech`; `isSpeaking` derived from `UtteranceProgressListener`.
- **iOS actual (deferred):** wraps `AVSpeechSynthesizer`; `isSpeaking` from `AVSpeechSynthesizerDelegate`.
- The `isSpeaking` StateFlow solves the web version's speech-vs-mic race — the chat controller subscribes rather than polling.

### 3. `LocationSource`

```kotlin
interface LocationSource {
    val updates: Flow<Fix>
    suspend fun getCurrent(timeoutMs: Long = 5_000L): Fix?
    fun start()
    fun stop()
}

data class Fix(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val bearingDeg: Float?,     // course-over-ground when speedMps > 1
    val timestampMs: Long,
)
```

- **Android actual:** `FusedLocationProviderClient` with high-accuracy priority.
- **iOS actual:** `CLLocationManager`.
- Course-over-ground is optional; the heading pipeline decides whether to use it.

### 4. `DeviceOrientationSource`

```kotlin
interface DeviceOrientationSource {
    val updates: Flow<Orientation>
    fun start()
    fun stop()
}

data class Orientation(
    val yawDeg: Double,             // 0 = magnetic north, clockwise
    val pitchDeg: Double,
    val rollDeg: Double,
    val accuracy: OrientationAccuracy,
    val timestampMs: Long,
)

enum class OrientationAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH }
```

- **Android actual:** `SensorManager` with `SENSOR_TYPE_ROTATION_VECTOR` — this is already tilt-compensated and fused, so the platform hands us a clean yaw.
- **iOS actual:** `CLLocationManager.heading` with `CMHeadingFilter`, using `trueHeading` when available.
- The **HeadingSmoother** (see below) applies retry/accuracy-gate/GPS-override on top; it does not need to redo tilt compensation because both platforms provide it.

### 5. `WakeLock`

```kotlin
interface WakeLock {
    fun acquire()
    fun release()
    val isHeld: StateFlow<Boolean>
}
```

- **Android actual:** `Window.addFlags(FLAG_KEEP_SCREEN_ON)` on the current activity, plus release on `onPause` and reacquire on `onResume` (matches web `visibilitychange` behaviour).
- **iOS actual:** `UIApplication.shared.isIdleTimerDisabled`.

### 6. `MemoryStore`

```kotlin
interface MemoryStore {
    val items: Flow<List<MemoryItem>>
    suspend fun snapshot(): List<MemoryItem>          // for chat requests
    suspend fun replace(items: List<MemoryItem>)      // apply server-returned memory
    suspend fun clear()
}

data class MemoryItem(
    val id: String,
    val name: String,
    val kind: MemoryKind,               // PLACE, NOTE, PERSON, OTHER
    val lat: Double?,
    val lon: Double?,
    val notes: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
```

- **Android actual:** Room database, single-user, app-sandbox-local. See ADR 0004.
- **iOS actual (deferred):** SwiftData.
- **The chat controller calls `snapshot()` before every request** and `replace(response.memory)` on every response — matches the web's "memory travels in/out of the payload" pattern.

## Shared logic in `core/commonMain`

### `HeadingSmoother` (in `heading/`)

Ports the interesting parts of `HeadingProvider.js`:

- Low-pass in sin/cos space (0.82 / 0.18 mix) — carries over unchanged; the wrap-safe math is the point.
- Retry-on-silent-sensor loop (up to 4×, 1.5 s each) — carries over.
- Accuracy gate on iOS (`webkitCompassAccuracy > 30` → don't trust). On Android the equivalent is `Orientation.accuracy == UNRELIABLE`.
- GPS-course-override at speed > 1 m/s — **kept off by default in v1**, matching the current web state (marked TEMP in the JS). Wired as a toggle so a later ADR can flip it.
- **Tilt compensation is dropped from this layer.** Platforms hand us fused, tilt-compensated yaw. This is the main simplification the reading pass identified.

Signature:

```kotlin
class HeadingSmoother(
    private val orientation: DeviceOrientationSource,
    private val location: LocationSource,
    private val settings: HeadingSettings = HeadingSettings.default(),
    scope: CoroutineScope,
) {
    val heading: StateFlow<HeadingReading>
    fun start()
    fun stop()
}

data class HeadingReading(
    val yawDeg: Double?,
    val trusted: Boolean,
    val source: HeadingSource,
    val timestampMs: Long,
)

enum class HeadingSource { ORIENTATION_FUSED, GPS_COURSE, NONE }
```

### `ChatController` (in `chat/`)

The turn-taking state machine. Ports the web version's chat loop:

- Owns the `TtsPriority`-aware speak queue and the STT lifecycle.
- Owns the **backstops**: 75 s chat, 25 s per call, 60 s request budget, 20 s per tool, 180 s runaway speech. All named constants in a single `Backstops` object so they can be reviewed and tuned centrally.
- Owns the **shush** semantics — three distinct actions from the web:
  - **In-flight abort:** an outstanding request is cancelled, and the app announces "Aborted".
  - **Speech cancel:** the synthesizer is stopped mid-utterance, no announcement.
  - **Listening restart:** current STT session is cancelled and a fresh one is started.
- Subscribes to `SpeechSynthesizer.isSpeaking` and only opens the mic on the `true → false` transition. Fixes the web version's `onend`-unreliable bug in a place the core owns.
- Idle timer arms on state entry, re-arms on every `SttEvent.Partial` or `SttEvent.Final`.

The controller exposes a `StateFlow<ChatState>` for the UI to render — the UI does not drive turn-taking, it only shows what state the core says we're in.

### `FollowMe` (in `location/`)

Ports the 15 m / 8 s cadence and the turn-callout rule from the web:

- Emits `FollowMeUpdate` events when the user has moved ≥ 15 m *or* 8 s have passed since the last update, whichever comes first.
- Turn call-outs at ≥ 45° heading change, with a 700 ms settle window, using the older-vs-newer-half circular-mean detector. Ported unchanged; the maths is the whole point.
- Consumes `LocationSource.updates` and `HeadingSmoother.heading`; emits into a `Flow<FollowMeEvent>` the chat controller can subscribe to.

### `A11yBobApi` (in `api/`)

Ktor client, one function per endpoint from the reading pass:

| Endpoint | Function | Notes |
| --- | --- | --- |
| `POST /api/knowledge-chat` | `chat(request: ChatRequest): ChatResponse` | Includes location, history, memory, `canShowMap`, `modality` |
| `GET /api/map-search` | `search(q, access?, near?, limit)` | Accessibility-tag filter is first-class |
| `GET /api/map-nearby` | `nearby(lat, lng, categoriesOff?, categoriesOn?)` | Significance-weighted; nearest-point |
| `GET /api/place-knowledge` | `placeKnowledge(lat, lng)` / `placeKnowledge(q)` | Cached blurbs |
| `POST /api/context-stt-token` | `mintSttToken()` | **Not used in v1** — kept for a v2 cloud-STT opt-in |

Base URL is a constructor parameter; production points at the a11ybob site, tests use a mock.

## Android surface (`androidApp/`)

### Screens

Only two screens in v1:

1. **DisclaimerGate** — the only entry into the app. Rebuilds the web disclaimer as native Compose; on accept, requests permissions and hands control to the map surface. See ADR 0005.
2. **KnowledgeMap** — the voice-first surface. No visible map; a status area, a mic control, a follow-me toggle, and a scrollable transcript. Renders `ChatController.state` and dispatches user gestures back to the core.

### Theming

- **AAA contrast** enforced via a `LidarAccessibilityTheme` composable. Colour tokens are the a11ybob "maps" zone tint (155° / 0.045 forest green, OKLCH L=95% for light / L=20% for dark) plus AAA-checked foregrounds.
- **Type scale capped at 3:1.** Text sizes scale with system font size, but the ratio between the largest and smallest text in a single view is clamped at 3:1 to protect magnifier users (Bob's CNIB constraint from `<user_background>` context).
- **Focus indicators:** 2 px outline + 3 px offset + halo box-shadow equivalent, on every focusable composable.

### Accessibility integration

- Every action button has `contentDescription` and `role = Role.Button`.
- The transcript is a **live region** (`liveRegion = LiveRegionMode.Polite`) so TalkBack announces new turns without pulling focus.
- Focus moves to the app title on entering the map surface — matches the web version's on-accept focus move.
- No custom accessibility widgets in v1. Every interactive element is a platform primitive.

### DI / composition root

Manual constructor injection through `AppComponent`. No Dagger, no Hilt, no Koin in v1 — the object graph is small enough that manual wiring is clearer than any framework. If the graph grows we revisit.

```kotlin
class AppComponent(
    context: Context,
    applicationScope: CoroutineScope,
) {
    val api: A11yBobApi = ...
    val memory: MemoryStore = RoomMemoryStore(context)
    val location: LocationSource = FusedLocationSource(context)
    val orientation: DeviceOrientationSource = RotationVectorOrientationSource(context)
    val wakeLock: WakeLock = AndroidWakeLock()
    val tts: SpeechSynthesizer = AndroidSpeechSynthesizer(context)
    val stt: SpeechRecognizer = AndroidSpeechRecognizer(context)
    val heading = HeadingSmoother(orientation, location, scope = applicationScope)
    val followMe = FollowMe(location, heading, scope = applicationScope)
    val chat = ChatController(api, memory, stt, tts, followMe, scope = applicationScope)
}
```

## Port matrix (reading pass → target module)

| Web Knowledge Map concern | Target module | Notes |
| --- | --- | --- |
| Deepgram Nova-3 WebSocket | dropped in v1 | Replaced by native STT — ADR 0003 |
| 30 s server-minted JWT | `api/A11yBobApi.mintSttToken` | Endpoint kept, unused in v1 |
| PCM AudioWorklet | dropped | Platform STT owns audio capture |
| Diarisation + first-speaker lock | dropped in v1 | Known gap; deferred to v2 |
| 10 s idle timer | `chat/ChatController` | Arms on state entry, re-arms on every partial/final |
| Speech-vs-mic race (`synth.speaking` polling) | `tts/SpeechSynthesizer.isSpeaking` | StateFlow instead of polling |
| Backstops (75/25/60/20/180 s) | `chat/Backstops` | Named constants, one file |
| Soft-click busy tone | `androidApp/voice/BusyTone.kt` | `SoundPool` on Android; not shared |
| Shush semantics | `chat/ChatController` | Three explicit paths: abort / cancel / restart |
| Wake-lock + visibility | `platform/WakeLock` + Activity lifecycle | Release on `onPause`, reacquire on `onResume` |
| Follow-me 15 m / 8 s | `location/FollowMe` | Ported |
| Turn call-out ≥ 45° / 700 ms | `location/FollowMe` | Circular-mean detector ported |
| Heading low-pass (sin/cos, 82/18) | `heading/HeadingSmoother` | Ported |
| Tilt compensation | dropped | Platform-fused heading covers this |
| Screen-orientation compensation | dropped | Platform-fused heading covers this |
| Retry-on-silent-sensor (4×, 1.5 s) | `heading/HeadingSmoother` | Ported |
| Accuracy gate | `heading/HeadingSmoother` | Uses `OrientationAccuracy` enum |
| GPS course override > 1 m/s | `heading/HeadingSmoother` | Off by default, toggleable |
| Personal memory (localStorage) | `memory/MemoryStore` (Room) | See ADR 0004 |
| Memory in/out of chat payload | `chat/ChatController` + `api/A11yBobApi` | Snapshot before request, replace on response |
| Disclaimer text and gesture unlock | `androidApp/ui/disclaimer` | Native, see ADR 0005 |
| AAA contrast + 3:1 type scale | `androidApp/ui/theme` | Enforced in composables |
| Zonal tint (maps zone, forest green) | `androidApp/ui/theme` | Design token |
| Native platform widgets first | `androidApp/ui/*` | No custom a11y widgets in v1 |

## What is not in the core

- **AR / camera preview.** LIDAR facts feed the map, they do not overlay the world. ADR 0002.
- **Networking policy** (retry, offline queueing, cache expiry). Deliberately deferred — we ship the simplest Ktor client that works on-network, and add resilience once field testing shows what actually breaks.
- **Analytics.** No telemetry in v1. If we add any, it needs its own ADR because it changes the "sits close to the user" principle.
- **Localisation.** English only in v1. `values-fr` folder exists as a placeholder for the eventual French Canadian port; strings are already externalised.

## Open questions before scaffolding

1. **Minimum Android SDK.** `createOnDeviceSpeechRecognizer` is API 33 (Android 13). Sensor rotation-vector is much older. Room + Compose are fine well below. Proposed `minSdk = 33`, `targetSdk = 35`. This drops < 15% of active devices as of mid-2026 and is the right trade for a BLV accessibility tool that assumes recent OS accessibility fixes. **Confirm or push back.**
2. **Kotlin, AGP, and Compose versions.** Locked into `libs.versions.toml`. I'll propose the current stable set (Kotlin 2.x, AGP 8.x, Compose Multiplatform not used — we use plain Jetpack Compose on Android only). No preview channels.
3. **Testing shape.** `commonTest` for pure logic (heading maths, follow-me detector, chat state machine). Android instrumentation tests only for the four Android actuals, not for logic. **Confirm.**
4. **Icon set.** Material Icons Extended is the pragmatic pick for v1. Custom iconography can come later once we know which glyphs are actually load-bearing for a BLV user.
