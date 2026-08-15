# 0002 — Knowledge Map port strategy

**Date:** 2026-08-15
**Status:** Accepted

## Context

The existing Knowledge Map lives in
[`bobdodd/a11ybob-website`](https://github.com/bobdodd/a11ybob-website),
served at `/maps/knowledge-map` with the interactive demo at
`/demos/knowledge-map/viewer.html`. It is a voice-first, hands-free
conversational map that computes distances / bearings from OpenStreetMap
data and reads back source-cited facts fetched from Wikipedia,
Wikivoyage, and Wikidata. The LLM phrases answers but never measures or
recalls — the "map measures, model speaks" architecture.

The mobile version is not a wrapped web app. It is a native rebuild
using the JS Knowledge Map as the design specification, plus LIDAR-
derived environmental awareness that the web version cannot access.

## Choices

- **Rebuild the interaction natively; do not host the web UI in a
  WebView.** Justification: there is no visual map surface to preserve,
  and the app's accessibility lives in the interaction model, not in a
  rendered widget's ARIA.
- **Port the interaction design and the algorithms; treat the JS as the
  spec.** Concretely, this means:
  - Hands-free STT-answer-STT loop with rising / falling tones,
    "what did I say?" verification, tap-or-Escape interrupt, and the
    disclaimer gate every session.
  - Voice separation + first-speaker lock heuristic for noisy
    environments.
  - Spatial reasoning: clock-position bearings relative to device facing,
    distance and nearest-thing queries, follow-me cadence (15 m / 8 s
    throttling, opening description vs terse updates, compass-driven turn
    call-outs), house-number interpolation, barrier and accessibility-tag
    reporting.
  - Personal memory: remember / forget / list, no expiry, on-device only,
    stores substance (not pointers), never syncs.
- **Reuse the existing Next.js backend as a service.** Map lookups,
  transit schedule reads, place-knowledge fetchers, and the place-cell
  cache stay server-side. Do not re-implement them on-device or in a
  parallel service.
- **LIDAR is a new fact source, not an AR overlay.** ARCore Depth /
  ARKit Scene Reconstruction report obstacle geometry as structured
  facts, which enter the conversation via the same "map measures, model
  speaks" pattern. LIDAR does not drive a real-time visual overlay.
- **Where things live:**
  - KMP core: spatial reasoning, follow-me cadence, conversation state
    machine, memory model, `DepthProvider` interface.
  - Android-only: `DepthProvider` ARCore implementation, native STT /
    TTS, Compose UI, TalkBack integration.
  - iOS-only: `DepthProvider` ARKit implementation, native STT / TTS,
    SwiftUI UI, VoiceOver integration.
  - Server (unchanged): Next.js API routes, place-cell cache, Wikipedia /
    Wikivoyage / Wikidata fetchers, GTFS reads, LLM phrasing.

## Rejected

- **Preserving the browser-based demo UI wholesale in a WebView.** The
  demo's UI is not the thing worth preserving; the interaction is.
  Preserving the UI would keep browser-shaped affordances that the
  native shell can improve on (e.g. system-level TTS voice selection,
  proper wake-lock via platform APIs, background follow-me).
- **Reimplementing the map / transit / knowledge backend on-device.**
  Duplicates the server's place-cell cache, GTFS ingestion, and Wikimedia
  fetch pipeline; risks divergence from the web version's cited-answer
  guarantees; makes it harder to keep both clients in step.
- **Letting the LLM answer factual questions from training.** Already
  forbidden by the web version's design; not reopened. The model phrases
  fetched facts; it does not recall them.

## Open questions

Same open-questions list as ADR 0001 / `concepts/mobile-stack`:

- Disclaimer gate: native per platform vs WebView pointing at the web
  disclaimer, for legal-text parity.
- Personal memory: any sync between web and mobile installs, or between
  a user's Android and iOS installs? Default is no sync.
- Which compass smoothing / heading-provider values from
  `HeadingProvider.js` port unchanged versus need per-platform re-tuning.
- Exact request / response shapes of the reused Next.js endpoints, so
  KMP core data types match the wire format.

## Accessibility implications

- **Interaction design is the accessibility.** The port must preserve
  the disclaimer-first flow, the audible microphone-open / -close tones,
  the "what did I say?" verification, and the "silence means not mapped"
  discipline, or it stops being accessible.
- **Do not weaken safety framing.** The web version is explicit: "not
  for navigation or any safety decision". The native version must show
  the same disclaimer every session and enforce the same framing in
  spoken output.
- **Clock-position bearings depend on compass availability.** On
  devices where the magnetometer is unreliable, the app must degrade
  gracefully — bearings become cardinal ("north-east") or are omitted
  — rather than confidently misreport.

## References

- Web Knowledge Map (design source):
  <https://a11ybob.com/maps/knowledge-map>
- Knowledge Map JS (implementation source):
  <https://github.com/bobdodd/a11ybob-website/tree/main/public/demos/knowledge-map/src>
- Tiled Toronto map (data pipeline the API sits on top of):
  <https://github.com/bobdodd/tiled-toronto-map>
- ADR 0001 — Mobile stack
