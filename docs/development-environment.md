# Development Environment

This document records the project's initial development environment and
the toolchain decisions that follow from it. It is intended for
contributors setting up their own machine and for future decisions about
build, test, and CI infrastructure.

## Target device

- **Device:** Google Pixel 10 Pro XL
- **Role:** Primary development device and initial target platform for
  all prototypes. First-release builds and demos target this device;
  support for other LIDAR-equipped phones and tablets is deferred until
  the initial target is proven.
- **Sensor / capability notes:** see
  [`research/pixel-10-lidar-capabilities.md`](../research/pixel-10-lidar-capabilities.md).

## Development workstation

Initial development workstation specifications:

| Component        | Spec                                            |
|------------------|-------------------------------------------------|
| OS               | Windows 10                                      |
| CPU              | AMD Ryzen 9 3900X (12 cores / 24 threads)       |
| Memory           | 32 GB RAM                                       |
| GPU              | NVIDIA GeForce RTX 2060 (6 GB VRAM)             |
| Storage          | 2.73 TB total                                   |
| Virtualization   | **Not available on this machine**               |

## Toolchain decisions

The lack of hardware virtualization on the initial workstation is the
single most important constraint on the local toolchain. All decisions
below follow from it.

### Off the table (require virtualization)

- **WSL2** — requires Hyper-V / virtualization.
- **Hyper-V** — not available.
- **Docker Desktop (WSL2 or Hyper-V backends)** — not usable locally.
  Containerized workflows are deferred to CI rather than run on the
  workstation.
- **Android Emulator with hardware acceleration** — HAXM, Hyper-V, and
  WHPX all depend on virtualization. Software-only emulation is too slow
  to be useful for LIDAR/ARCore work.

### Chosen local toolchain

- **Native Windows tooling** — Android Studio, Android SDK/NDK, Gradle,
  Git for Windows, and any editor/IDE run natively on Windows 10.
- **On-device Android development** — Builds, installs, debugging, and
  automated tests run directly on the Pixel 10 Pro XL over ADB (USB or
  wireless). The physical device is the reference runtime; the emulator
  is not part of the loop.
- **WSL1 (optional)** — Acceptable as a POSIX shell layer if needed;
  does not require virtualization. Not required by the toolchain.
- **Git LFS** — Considered for large binary assets (point-cloud
  captures, recorded sensor logs) if any are committed to the repo.

### CI and containerized workflows

Anything that genuinely needs Linux containers, virtualization, or
matrix builds runs in **GitHub Actions**, not on the workstation. This
keeps the local machine focused on authoring and on-device testing.

## Contributor prerequisites

If you are contributing from your own machine:

1. Install Android Studio with the current stable Android SDK and NDK.
2. Enable Developer Options and USB debugging on a Pixel 10 Pro XL (or
   equivalent LIDAR-capable Pixel) and connect via ADB.
3. Clone this repository and open it in Android Studio.
4. Build and deploy directly to the connected device; do not rely on the
   emulator for LIDAR/ARCore code paths.

If your machine supports virtualization, WSL2 and the Android Emulator
are of course fine to use — but any workflow the project depends on must
also work on a non-virtualizing host.
