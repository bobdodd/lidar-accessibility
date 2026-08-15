# LIDAR-based Accessibility

Prototype tools that use LIDAR to help blind and low-vision users
understand and navigate their environment — in both mapped and unmapped
spaces.

**Repository:** <https://github.com/bobdodd/lidar-accessibility>

## Project scope

This repository is a working space for a series of prototype tools
exploring digital navigation and wayfinding via LIDAR-equipped consumer
devices (phones, tablets, and dedicated sensors). Research notes and
landscape scans live alongside the code as it evolves.

## Development environment

- **Target device:** Google Pixel 10 Pro XL (initial target for all
  prototypes).
- **Workstation:** Windows 10, Ryzen 9 3900X, 32 GB RAM, RTX 2060 (6 GB),
  2.73 TB storage. Hardware virtualization is not available on this
  machine, so WSL2, Hyper-V, Docker Desktop, and the hardware-accelerated
  Android Emulator are not part of the local toolchain — Android work
  runs directly on the Pixel 10 Pro XL over ADB.

Full details and toolchain decisions:
[`docs/development-environment.md`](./docs/development-environment.md).

## Decisions

Meaningful technical, design, and accessibility decisions are recorded as
numbered ADRs in [`docs/decisions/`](./docs/decisions/):

- [0001 — Mobile stack](./docs/decisions/0001-mobile-stack.md): dual-native
  Android + iOS with a Kotlin Multiplatform core; reuses the existing
  a11ybob Next.js backend.
- [0002 — Knowledge Map port strategy](./docs/decisions/0002-knowledge-map-port-strategy.md):
  the native rebuild uses the JS Knowledge Map as its design spec; LIDAR
  is a new fact source, not an AR overlay.
- [0003 — Speech-to-text](./docs/decisions/0003-speech-to-text.md):
  native on-device STT on both platforms; no paid cloud service in v1.
- [0004 — Personal memory scope](./docs/decisions/0004-personal-memory-scope.md):
  per-device, per-install; no cross-device sync in v1.
- [0005 — Disclaimer gate](./docs/decisions/0005-disclaimer-gate.md):
  rebuilt natively per platform with the same text as the web version.

## Architecture

- [Architecture spec (v1)](./docs/architecture.md) — module layout,
  KMP core interfaces, and the port matrix from the web Knowledge Map
  reading pass.

Format modelled on the
[a11ybob-website decision log](https://github.com/bobdodd/a11ybob-website/tree/main/docs/decisions).

## Repository layout

- `core/` — Kotlin Multiplatform module with Android and iOS targets;
  holds every algorithm that is not a platform primitive (chat
  controller, heading smoother, follow-me, memory model, Ktor API
  client, backstop timers). Namespace `com.bobdodd.lidaraccessibility.core`.
- `androidApp/` — Android application module (Jetpack Compose,
  Material 3, Room, Play Services location). Namespace
  `com.bobdodd.lidaraccessibility.android`.
- `gradle/libs.versions.toml` — single source of dependency versions.
- `docs/` — project documentation (development environment, decisions,
  architecture spec).
- `research/` — device and platform capability notes (e.g. Pixel 10
  LIDAR).
- `lidar-accessibility-landscape-research.md` — landscape scan of the
  LIDAR-for-accessibility field.
- `commercial-lidar-accessibility-research.md` — survey of commercial
  offerings.

### Toolchain (Aug 2026 stable set)

- Kotlin 2.4.10, AGP 9.3.1, Gradle 9.5.0, JDK 17.
- compileSdk 37, minSdk 33 (`createOnDeviceSpeechRecognizer` floor),
  targetSdk 37.
- Jetpack Compose BOM 2026.08.00 (Compose 1.12).
- Ktor 3.5.2, Room 2.8.0, coroutines 1.9.0.

### Building

Open the repository in Android Studio Quail (`2026.1.x`) or run from
the command line:

```
./gradlew :core:build
./gradlew :androidApp:installDebug   # deploys to the Pixel 10 Pro XL over ADB
```

The Gradle wrapper is not committed yet — initialise it in the local
clone with `gradle wrapper --gradle-version=9.5.0` before the first
build.

## License

This project is **dual-licensed** by artifact type:

### Code — GNU General Public License v3.0

All source code in this repository is licensed under the **GNU General
Public License, version 3.0 (GPL-3.0)**. See [LICENSE](./LICENSE) for
the full text.

    Copyright (C) 2026  Bob Dodd and contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

### Documentation — Creative Commons Attribution-ShareAlike 4.0

All documentation in this repository — Markdown files, research notes,
guides, specifications, diagrams, and other written or visual materials
that are not source code — is licensed under the **Creative Commons
Attribution-ShareAlike 4.0 International License (CC BY-SA 4.0)**. See
[LICENSE-docs.md](./LICENSE-docs.md) for the full notice.

[![License: CC BY-SA 4.0](https://img.shields.io/badge/License-CC_BY--SA_4.0-lightgrey.svg)](https://creativecommons.org/licenses/by-sa/4.0/)

To reuse documentation from this project, please attribute similar to:

> "LIDAR-based Accessibility documentation" by Bob Dodd and
> contributors, licensed under
> [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

### Why dual-licensed?

GPL-3.0 is a strong copyleft license chosen so that forks and
derivatives of the assistive technology remain free software and
improvements flow back to users. CC BY-SA 4.0 is the
documentation-appropriate copyleft analogue, requiring attribution and
share-alike redistribution for the accompanying written materials.
