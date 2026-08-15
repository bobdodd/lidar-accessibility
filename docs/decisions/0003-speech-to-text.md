# 0003 — Speech-to-text

**Date:** 2026-08-15
**Status:** Accepted (v1); v2 cloud opt-in deferred

## Choices

- **Android:** platform `SpeechRecognizer` with
  `createOnDeviceSpeechRecognizer()` (Android 13+). Offline, on-device,
  no per-request cost. On Pixel-class hardware with Gemini Nano, this
  is fast (~400 ms median latency) and within ~2 percentage points of
  cloud accuracy on clean speech.
- **iOS:** `SpeechAnalyzer` on iOS 26+ (Apple's on-device replacement
  for `SFSpeechRecognizer`). Independently benchmarked at **2.12% WER
  on clean speech and 4.56% WER on noisy speech** — better than
  Deepgram Nova-3 in cloud, running fully on-device with no
  per-minute cost, no bundled model, no network round-trip. On iOS 25
  and earlier, fall back to `SFSpeechRecognizer`.
- **No paid cloud STT in v1.** Deepgram, AssemblyAI, and Google
  Cloud Speech are all viable but incur a per-minute cost that the
  project is not willing to accept as a default.

## Rejected (for v1)

- **Deepgram Nova-3 (parity with web).** The web Knowledge Map streams
  audio directly to Deepgram over WebSocket for diarisation + voice-lock.
  Rejected as the default because (a) it costs per minute of audio, and
  (b) the native platform APIs are now accurate enough (Android on-device
  ~94% word accuracy, iOS SpeechAnalyzer 2.12% WER) that the accuracy
  argument for cloud no longer holds. Deferred as a v2 opt-in for users
  in noisy environments who want voice-lock.
- **whisper.cpp on-device (Android).** Technically viable at sub-100 ms
  perceived latency with a ring-buffer + Vulkan GPU backend, ~2–6% WER
  with int8-quantised models. Rejected for v1 because it requires a
  40–460 MB model download, careful JNI streaming architecture, and
  offers no diarisation. If Android's `SpeechRecognizer` proves
  inadequate in field testing, whisper.cpp becomes the fallback before
  cloud.
- **SFSpeechRecognizer as the primary iOS API (iOS 25 and earlier).**
  Its ~9% WER on clean speech is 4× worse than SpeechAnalyzer's. Kept
  only as a fallback for older iOS versions.

## What we lose without cloud STT

The web version's Deepgram-based **voice-lock** — after 3 words, lock
onto the first diarised speaker and drop other speakers — is not
available from any native mobile STT API. On a bus, on a busy sidewalk,
or in a café with other conversations, cross-talk can hijack a query.

This matters more for a BLV navigation app than for a typical dictation
app, so it is captured as a **known gap in v1**, not a rejected concern.
If field testing confirms it is a real problem, v2 adds an opt-in cloud
STT toggle. See the "Open questions" section of the project wiki
concept `concepts/mobile-stack` for the deferred decision on which
provider.

## Accessibility implications

- **Free and offline by default** is the right default for a BLV
  accessibility tool: users on limited data plans, or in areas with poor
  connectivity, still get the voice interface.
- **No account required** matches the web version's zero-friction
  disclaimer-to-Speak flow. Adding a paid cloud service in v1 would
  require accounts, quotas, or API-key configuration, none of which
  belong in a first launch.
- **VoiceOver / TalkBack are the only voice UI a BLV user hears from the
  OS.** Native STT integrates cleanly with them; a third-party STT layer
  can accidentally duck platform screen-reader speech.

## References

- [Apple SpeechAnalyzer vs Whisper benchmark](https://www.developersdigest.tech/blog/apple-speechanalyzer-vs-whisper-benchmark)
  — 2.12% / 4.56% WER (clean / noisy) benchmarked July 2026.
- [Apple SpeechAnalyzer API review](https://loopwire.tech/apple-speechanalyzer-api-review-features-pricing-and-how-it-compares-to-whisper/)
  — long-form, no 1-minute cap, on-device only.
- [Android SpeechRecognizer offline (Pixel 7, Android 14)](https://lifetips.alibaba.com/tech-efficiency/automate-your-home-via-voice-commands-with-tasker-and-v)
  — 94.1% word accuracy, 420 ms median latency.
- [Cross-platform STT in React Native](https://www.jocheojeda.com/2026/07/04/cross-platform-speech-to-text-in-react-native/)
  — Android 13+ `createOnDeviceSpeechRecognizer` overview.
- [Whisper.cpp on Android — sub-100 ms on-device ASR](https://mvpfactory.io/blog/wiring-whisper-cpp-to-android-s-audiorecord-api-building-a-sub-100ms-on-device)
  — reference for the fallback path.
- ADR 0001 — Mobile stack (dual-native + KMP).
