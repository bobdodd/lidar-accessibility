# 0004 — Personal memory scope

**Date:** 2026-08-15
**Status:** Accepted

## The choice

**Personal memory is per-device, per-install. It does not sync between
the web version and the mobile apps, or between a user's Android and
iOS installs.**

The mobile apps preserve the web version's principle: the user's
personal places and notes stay as close to the user as possible, and
are cleared only by an explicit "forget" or by uninstalling the app /
clearing the browser's site data.

## Storage

- **Android:** Room database (SQLite under the hood), local to the app
  sandbox.
- **iOS:** SwiftData (SQLite under the hood), local to the app sandbox.
- **Web:** `localStorage` (unchanged from the existing implementation).

Each surface persists memory items independently. The item shape is
shared via the KMP core so the same fields exist on every platform
(name, kind, coordinates, notes, timestamp).

## How memory travels

Following the web version's pattern:

- Memory items ride along in every `POST /api/knowledge-chat` request
  body, so the LLM can answer *from* them (e.g. "how far is my front
  door?").
- The server returns the updated memory in the response body when a
  remember / forget happened; the client persists what the server
  returns.
- No memory items are ever stored server-side.

## Rejected

- **Optional cloud sync behind an account.** Rejected because the web
  version deliberately has no account model, no sign-in, no cloud data,
  and adding one on mobile would break the "sits close to the user"
  principle. Also a much larger security and privacy surface.
- **Optional user-controlled export/import (share sheet / QR code /
  file).** Not rejected on principle; deferred. If field testing shows
  users setting up personal memory on one device and wanting it on
  another, we add an explicit export step. Not required for v1.

## Accessibility implications

- **Zero-friction first launch.** No sign-in, no OAuth, no confusion —
  the app is usable in seconds, which is what a BLV user needs when
  first opening it.
- **Users own their data unambiguously.** No question of "is this
  synced?", "who else can see this?", or "what happens if I close my
  account?".
- **Loss on uninstall / device replacement is accepted.** Personal
  memory in v1 is expected to be low-volume ("my front door", "my
  regular bus stop", handful of items), not a large archive. If usage
  patterns change, revisit.

## References

- Existing web implementation: `public/demos/knowledge-map/src/knowledge-map.js`
  (MEM_KEY = `km-memory-v1`, localStorage-only, sent with every
  question, updated store returned by the server).
- ADR 0001 — Mobile stack (dual-native + KMP; memory model lives in
  the KMP core, persistence in the platform layer).
- ADR 0002 — Knowledge Map port strategy (memory is one of the
  interaction concerns that must port).
