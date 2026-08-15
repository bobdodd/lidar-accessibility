# 0005 — Disclaimer gate

**Date:** 2026-08-15
**Status:** Accepted

## The choice

**Rebuild the disclaimer gate natively per platform.** Same text as the
web version, kept in sync by convention. The gate is shown every
launch, is the only path into the app, and is the user gesture that
unlocks location, compass, speech, and wake-lock.

## Rejected

- **WebView pointing at the web disclaimer.** Would guarantee legal
  text parity, but costs a network dep on first launch, an awkward
  bridge to signal "accepted" back to native, and lower native
  accessibility integration. First-launch offline would be broken.
- **Native with a build-time text sync from the web source.**
  Deferred, not rejected on principle. If the disclaimer text starts
  drifting between web and mobile, add a build-time script that
  extracts the disclaimer content from `a11ybob-website` and stamps
  bundled string resources. For v1 the text is small enough to keep in
  sync by hand.

## How the gate must work (from the web version)

- Shown every launch. No "don't show me this again" — legal safety
  overrides UX convenience.
- Body text explains: experimental unfinished software; can be wrong;
  facts come from Wikipedia; not a navigation or safety tool; user's
  words and location go to external services; depends on GPS, network,
  and third-party services that can fail; keep using your usual
  navigation methods.
- Consent checkbox + "Accept and start" button. The button is disabled
  until the checkbox is ticked.
- Accepting the disclaimer is the **single user gesture** that:
  - Grants location permission.
  - Grants compass / device-orientation permission (iOS requires this
    inside a user gesture).
  - Primes the OS speech engine (iOS requires speaking a line inside
    a user gesture before later async answers may speak).
  - Acquires the wake-lock.
- Focus moves to the app title on entry so a screen-reader user starts
  reading from the top of the app.

## Accessibility implications

- **Native rendering means native focus, native reading order, native
  contrast against system settings, native scaling for magnifier
  users, native VoiceOver / TalkBack behaviour.** All of which are
  weaker in a WebView.
- **Focus management on accept** must move focus to the first useful
  control in the app, matching the web version.
- **Screen reader must read the whole notice.** No collapsible
  sections, no "read more" — the notice is short enough to read
  linearly.
- **Text scales to the user's platform accessibility settings** (Dynamic
  Type on iOS, Font size + Display size on Android) automatically when
  rendered natively.

## References

- Web disclaimer: `public/demos/knowledge-map/viewer.html`
  (`cv-gate` section) at
  <https://github.com/bobdodd/a11ybob-website/blob/main/public/demos/knowledge-map/viewer.html>.
- ADR 0001 — Mobile stack.
- ADR 0002 — Knowledge Map port strategy.
