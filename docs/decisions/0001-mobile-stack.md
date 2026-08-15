# 0001 — Mobile stack

**Date:** 2026-08-15
**Status:** Accepted

## Choices

- **App architecture:** Dual-native — Kotlin + Jetpack Compose on Android,
  Swift + SwiftUI on iOS.
- **Shared algorithm core:** Kotlin Multiplatform (KMP). Spatial reasoning
  (clock-position bearings, distance / nearest queries), follow-me cadence
  (15 m / 8 s, opening vs terse updates, compass-driven turn call-outs),
  conversation state machine, personal-memory model, and the STT-answer-STT
  hands-free loop live here.
- **AR / depth:** ARCore Depth API on Android; ARKit Scene Reconstruction
  on iOS. Wrapped behind a `DepthProvider` interface in the KMP core.
- **Voice I/O:** Native per platform — Android `SpeechRecognizer` +
  platform TTS; iOS `SFSpeechRecognizer` + `AVSpeechSynthesizer`.
- **Backend:** Reuse the existing a11ybob Next.js API surface
  (`/api/map-search`, `/api/map-nearby`, `/api/place-knowledge`,
  `/api/knowledge-chat`, `/api/context-chat`, `/api/context-stt`,
  `/api/context-stt-token`, `/api/search`). The mobile app is a client of
  the same endpoints the web Knowledge Map uses. Mobile-specific endpoint
  variants are added only where mobile diverges (e.g. LIDAR-derived
  queries).
- **Language for shared code:** Kotlin (targets JVM for Android and Kotlin
  Native for iOS via KMP).
- **Target device order:** Pixel 10 Pro XL first (development device and
  initial target); LiDAR-capable iPhone Pro / iPad Pro second.
- **Development workstation:** Windows 10 desktop, no hardware
  virtualization available — Android + KMP work runs natively; iOS work
  requires a Mac (or a Mac-in-cloud service) and is deferred until an
  Android prototype is proven.

## Rejected

- **Kotlin + ARCore native (Android-only)** — forces a parallel Swift +
  ARKit codebase for iOS, duplicating every spatial-reasoning and
  conversation algorithm. iOS is a first-class target because blind and
  low-vision users skew heavily iOS; this option is not acceptable.
- **Unity + AR Foundation (ARCore XR + ARKit XR plugins)** — Unity's
  strengths are 3D scenes and sonification middleware; the Knowledge Map
  is a voice-first conversation app with almost no visual UI and no
  spatial audio scene, so those strengths do not apply. Unity's
  TalkBack / VoiceOver story and native STT / TTS access are both weaker
  than dual-native. Heavier toolchain on a no-virtualization workstation.
- **Flutter + `arcore_flutter_plugin`** — depth APIs are thin and
  community-maintained; accessibility integration is weaker than native.
  Reasonable for visual-UI-heavy apps; this is not one.
- **Hybrid native shell + WebView hosting the Knowledge Map UI** —
  proposed early, rejected once the Knowledge Map was read carefully.
  Its accessibility lives in the interaction model (hands-free loop,
  clock-position bearings, source-cited answers, no-expiry memory,
  disclaimer-first flow), not in ARIA / keyboard treatment of a
  rendered widget. There is no visual map surface to host in a WebView,
  so the pattern adds a JS ↔ native bridge without solving a real
  problem.
- **Wrapped web app (Cordova / Capacitor)** — requirement is a native
  app with first-class LIDAR access and native accessibility
  integration.
- **"Write it twice" (no KMP, duplicate algorithm code in Kotlin and
  Swift)** — the spatial reasoning, follow-me cadence, memory model, and
  conversation state constitute a body of algorithm code that must remain
  identical across platforms as the design evolves. Duplicating it in two
  languages is the maintenance trap that drives teams to cross-platform
  engines in the first place. KMP shares that code without giving up
  native surfaces.

## Accessibility implications

- **First-class TalkBack (Android) and VoiceOver (iOS) integration** is
  a hard requirement, satisfied by staying native on both platforms.
- **Native STT / TTS** is used because the voice loop is the app's
  primary interface, not a nice-to-have. WebView or cross-platform
  engine STT wrappers introduce latency and reduce control over the
  hands-free heuristics (voice separation, first-speaker lock, gap-based
  end-of-utterance detection).
- **Disclaimer gate parity with the web version** is important because
  the disclaimer is a safety and legal artefact. Open question tracked
  in `concepts/mobile-stack` (project wiki): rebuild natively per
  platform, or host the web disclaimer in a WebView to guarantee text
  parity.
- **"Map measures, model speaks"** is preserved: LIDAR reports obstacle
  and geometry facts, computed on-device; the LLM only phrases them,
  server-side, as it already does for map-derived and Wikipedia-derived
  facts.
- **Personal memory stays on-device** on both platforms, matching the
  web version's principle. No cross-device sync in v1.

## Open questions

Tracked in `concepts/mobile-stack` in the project wiki; to be resolved
during the code-reading pass over the Knowledge Map and Next.js API
routes.

## References

- Project wiki: `concepts/mobile-stack`
- a11ybob-website Knowledge Map:
  <https://github.com/bobdodd/a11ybob-website>
- Tiled Toronto map (data pipeline):
  <https://github.com/bobdodd/tiled-toronto-map>
