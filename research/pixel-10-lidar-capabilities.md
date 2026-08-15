# Pixel 10 LiDAR & Depth Sensing Capabilities: Research Memo

## Bottom Line

**The Google Pixel 10 does NOT have a hardware LiDAR sensor.** No Pixel 10 variant — Pixel 10, Pixel 10 Pro, Pixel 10 Pro XL, or Pixel 10 Pro Fold — includes a dedicated LiDAR, time-of-flight (ToF), or depth sensor.

However, all Pixel 10 variants **do support the ARCore Depth API**, which provides software-based depth sensing using depth-from-motion algorithms with the phone's standard RGB camera. This is the primary depth-sensing pathway available to developers on the Pixel 10.

The project's "LIDAR" framing should be understood as **depth sensing via ARCore** (visual-inertial odometry + optional ML), not true hardware LiDAR scanning.

---

## Pixel 10 Sensing Reality

### What the Pixel 10 Has

| Sensor / Capability | Pixel 10 | Pixel 10 Pro / Pro XL | Notes |
|---|---|---|---|
| **Hardware LiDAR** | No | No | Not listed in any official spec |
| **ToF depth sensor** | No | No | Not listed in any official spec |
| **Dedicated depth sensor** | No | No | Not listed in any official spec |
| **ARCore Depth API** | Yes | Yes | Software depth-from-motion using RGB camera |
| **Laser Detect AF (LDAF)** | Not verified here | Yes (multi-zone) | Laser autofocus — for camera focus only, NOT depth mapping. Base Pixel 10 specs not independently verified. |
| **Ultra-Wideband (UWB)** | Not verified here | Yes | "Accurate ranging and spatial orientation" chip on Pro models. Base Pixel 10 specs not independently verified. |
| **Accelerometer** | Yes | Yes | |
| **Gyrometer** | Yes | Yes | |
| **Magnetometer** | Yes | Yes | |
| **Barometer** | Yes | Yes | |
| **Proximity sensor** | Yes | Yes | |
| **Ambient light sensor** | Yes | Yes | |

Sources: [Google Pixel 10 Pro official specs](https://store.google.com/product/pixel_10_pro_specs?hl=en-US), [ARCore supported devices](https://developers.google.com/ar/devices)

### Camera System (Pixel 10 Pro)

| Camera | Resolution | Aperture | FOV | Sensor Size |
|---|---|---|---|---|
| Wide (main) | 50 MP Octa PD | f/1.68 | 82° | 1/1.3" |
| Ultrawide | 48 MP Quad PD | f/1.7 | 123° | 1/2.51" |
| Telephoto (5x) | 48 MP Quad PD | f/2.8 | 22° | 1/2.51" |
| Front | 42 MP Dual PD | f/2.2 | 103° | — |

The base Pixel 10 has a reduced camera system: 48MP wide, 13MP ultrawide, 10.8MP telephoto.

Sources: [GSMArena Pixel 10 Pro review](https://www.gsmarena.com/google_pixel_10_pro-review-2877p5.php), [Android Central Pixel 10 camera review](https://www.androidcentral.com/phones/google-pixel-10-series-camera-review)

### Critical Implication

The LDAF (laser detect auto focus) sensor on the Pixel 10 Pro is sometimes confused with a LiDAR sensor, but it is a **focused infrared laser beam used only for autofocus distance measurement** — it does not produce a depth map of the environment and is not accessible to developers for spatial sensing.

---

## ARCore Depth API

The ARCore Depth API is the primary depth-sensing mechanism available on the Pixel 10. It generates depth maps computationally from camera motion.

### How It Works

- Uses **depth-from-motion algorithms** with a single RGB camera
- Combines visual data with **visual-inertial odometry** (accelerometer + gyroscope)
- Can optionally use hardware ToF sensors if present (not applicable on Pixel 10)
- Does NOT require a hardware depth sensor

Source: [ARCore Depth API developer guide](https://developers.google.com/ar/develop/java/depth/developer-guide)

### Key API Methods

| Method | Description |
|---|---|
| `frame.acquireDepthImage16Bits()` | Returns depth image (16-bit, millimeters) for the current frame. Range up to 65,535 mm (~65.5 m) |
| `frame.acquireRawDepthImage16Bits()` | Returns raw, mostly unfiltered depth image |
| `frame.acquireRawDepthConfidenceImage()` | Returns per-pixel confidence values (0–255) for raw depth |
| `session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)` | Checks device support |
| `config.setDepthMode(Config.DepthMode.AUTOMATIC)` | Enables depth mode in session |

### Depth Image Specifications

| Property | Value |
|---|---|
| Pixel format | 16-bit unsigned integer |
| Unit | Millimeters |
| Typical resolution | ~160×120 pixels |
| Max resolution | Up to 640×480 on some devices |
| Max representable range (16-bit) | 65,535 mm (~65.5 m) — this is the encoding maximum, not the reliable local sensing range |
| Max representable range (8-bit, deprecated) | 8,191 mm (~8.2 m) — encoding maximum |
| Confidence range | 0–255 (255 = highest confidence) |
| Zero value | Means no depth data available at that pixel |

Source: [ARCore Frame API reference](https://developers.google.com/ar/reference/java/com/google/ar/core/Frame)

### Practical Depth Range vs. Encoding Maximum

The 16-bit depth values can represent distances up to 65,535 mm (~65.5 m), but this is the **encoding maximum**, not the reliable local sensing range. In practice:

- **Without Geospatial Depth**: Typical local depth range is ~20–30 meters, with density and accuracy degrading beyond that range.
- **With Geospatial Depth** (in VPS-covered areas): Dense depth values can reach the 65m encoding maximum by merging Streetscape Geometry with local observations.

Source: [ARCore Geospatial Depth](https://developers.google.com/ar/develop/java/depth/geospatial-depth)

### Limitations

1. **Motion required**: Depth data is only available after the user moves the device. No depth is generated when standing still.
2. **Tracking-dependent**: If the camera loses tracking (poor lighting, featureless surfaces), depth data becomes unavailable.
3. **Disabled by default**: Must be explicitly enabled in the ARCore session configuration.
4. **Resolution is low**: 160×120 is typical — sufficient for obstacle detection but not fine-detail spatial mapping.
5. **Not all frames have new depth**: Check timestamps to determine if a raw depth image contains new data or is a reprojection.

### Enabling Depth Mode (Kotlin)

```kotlin
val config = session.config
val isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
if (isDepthSupported) {
    config.depthMode = Config.DepthMode.AUTOMATIC
}
session.configure(config)
```

### AndroidManifest (if depth is required)

```xml
<uses-feature android:name="com.google.ar.core.depth" />
```

Source: [ARCore Depth developer guide](https://developers.google.com/ar/develop/java/depth/developer-guide)

---

## Geospatial Depth (Extended Range)

Geospatial Depth is an extension of the ARCore Depth API that dramatically increases depth range and speed in VPS-covered areas.

| Property | Value |
|---|---|
| Max range | 65 meters (vs. ~20–30 m without it) |
| Data source | Merges local camera depth with Streetscape Geometry (from Google Street View) |
| Requirement | VPS coverage + Streetscape Geometry enabled |
| Hardware sensor needed | No |

### How to Enable

Requires enabling three modes in the ARCore session:
1. Depth mode (`Config.DepthMode.AUTOMATIC`)
2. Geospatial mode
3. Streetscape Geometry mode

Source: [ARCore Geospatial Depth](https://developers.google.com/ar/develop/java/depth/geospatial-depth)

### Implications for Blind Navigation

- In mapped urban areas (VPS-covered), the phone can "see" building and terrain geometry up to 65m away
- This provides **wayfinding context** — knowing where buildings, roads, and paths are
- In unmapped areas, depth range drops to ~20–30m with lower density beyond that range
- This dual-mode behavior (mapped vs. unmapped) aligns directly with the project's goal of supporting "mapped and unmapped LIDAR environments"

---

## Scene Semantics API

The Scene Semantics API uses an on-device ML model to classify each pixel in the camera feed into semantic categories — highly relevant for obstacle identification and environmental understanding.

### Semantic Labels (11 categories + unlabeled)

| Quality Tier | Labels |
|---|---|
| Main scene components | sky, building, tree, road, vehicle |
| Major scene details | sidewalk, terrain, structure, water |
| Minor scene details | object, person |
| Unclassified | unlabeled (pixels that could not be classified) |

### Key API Methods

| Method | Description |
|---|---|
| `frame.acquireSemanticImage()` | Returns per-pixel semantic label image |
| `frame.acquireSemanticConfidenceImage()` | Returns per-pixel confidence values |
| `frame.getSemanticLabelFraction(SemanticLabel)` | Returns percentage of frame containing a specific label |

### Limitations

- **Outdoor scenes only** — not designed for indoor use
- **Portrait orientation only** — quality not guaranteed in landscape
- Prediction quality varies: larger/common objects classified better than small/rare ones
- Shares the same supported device list as the Depth API (Pixel 10 is supported)

Source: [ARCore Scene Semantics](https://developers.google.com/ar/develop/scene-semantics)

### Implications for Blind Navigation

The Scene Semantics labels map almost directly to navigation-relevant categories:
- **sidewalk** + **road** → path following and boundary detection
- **person** + **vehicle** → dynamic obstacle awareness
- **building** + **structure** → landmark identification and spatial anchors
- **terrain** → off-path detection (grass, dirt)
- **water** → hazard avoidance

---

## Streetscape Geometry API

Provides 3D polygon meshes of terrain and buildings from Google Street View data.

- Useful for **building-level navigation context** in urban environments
- Can be used for raycasting (hit-testing against real-world geometry)
- Data quality varies by location
- Source: [ARCore Streetscape Geometry](https://developers.google.com/ar/develop/unity-arf/geospatial/streetscape-geometry)

---

## Geospatial API

Enables global-scale location-based AR using Google's Visual Positioning System (VPS) + GPS.

- Determines device position and orientation with high accuracy in VPS-covered areas
- Enables content anchoring to real-world coordinates
- Source: [ARCore Geospatial API](https://developers.google.com/ar/develop/geospatial)

---

## Object Detection & Machine Learning Tools

### ML Kit Object Detection & Tracking

| Property | Value |
|---|---|
| Platform | On-device (offline capable) |
| Max objects per frame | 5 (with tracking IDs) |
| Classification | Built-in coarse classifier OR custom TensorFlow Lite model |
| Min Android API | 21 |
| Latency | Real-time on modern devices |

Key API: `ObjectDetection.getClient(options)` → `process(image)`

Source: [ML Kit Object Detection](https://developers.google.com/ml-kit/vision/object-detection)

### TensorFlow Lite

- Run custom-trained models on-device for specialized obstacle detection
- Supports GPU acceleration via delegate
- Can detect custom object classes (stairs, doors, wet floor signs, etc.)
- Model format: `.tflite` files bundled in app assets
- Source: [TensorFlow Lite Android quickstart](https://android.googlesource.com/platform/external/tensorflow/+/upstream-master/tensorflow/lite/g3doc/android/quickstart.md)

### ARCore + ML Pipeline

ARCore provides camera frames that can be fed into ML Kit or TensorFlow Lite models for real-time object detection and classification, while simultaneously providing depth information for distance estimation.

Source: [Machine learning with ARCore](https://developers.google.com/ar/develop/machine-learning)

---

## Android Accessibility & Feedback APIs

### TalkBack (Screen Reader)

- Pre-installed Android accessibility service
- Speaks UI content aloud
- Apps should use `android:contentDescription` and accessibility framework APIs
- Source: [Android Accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing)

### Text-to-Speech (TTS)

- `android.speech.tts` package
- Programmatic spoken feedback for obstacle alerts, distance announcements, navigation instructions

### Haptic Feedback

- `View.performHapticFeedback()` for simple vibration cues
- `VibrationEffect` API for custom vibration patterns
- Can encode distance/direction through vibration intensity and rhythm
- Source: [Android haptic feedback](https://developer.android.com/develop/ui/views/haptics/haptic-feedback)

### Speech Recognition

- `android.speech.RecognizerIntent` or SpeechRecognizer API
- Enables voice-activated controls (start/stop navigation, query surroundings)

---

## Ultra-Wideband (UWB) — Pixel 10 Pro Only

The Pixel 10 Pro and Pro XL include a UWB chip described as providing "accurate ranging and spatial orientation." This could potentially be used for:
- Short-range object detection (via UWB ranging to tagged objects)
- Spatial orientation assistance
- Note: UWB is primarily designed for device-to-device ranging and digital key use cases, not environmental depth sensing

Source: [Google Pixel 10 Pro specs](https://store.google.com/product/pixel_10_pro_specs?hl=en-US)

---

## Prior Art: ARCore-Based Blind Navigation

Several academic and prototype projects have combined ARCore depth sensing with audio/haptic feedback for blind navigation:

1. **3rDi 4 All** — Android app using ARCore Depth API for real-time obstacle detection with voice warnings (published on Google Play)
2. **Zhang & Yao (2019)** — ARCore-based user-centric assistive navigation system with time-stamped map Kalman filter for obstacle detection
3. **JETIR study** — TensorFlow Lite + ARCore depth maps + TTS + haptic feedback pipeline

Sources: [3rDi 4 All](https://uralstech.github.io/2023/07/06/3rDi-4-All.html), [Semantic Scholar paper](https://www.semanticscholar.org/paper/An-ARCore-Based-User-Centric-Assistive-Navigation-Zhang-Yao/6d38d4c64c1b0f56c317e3309969f218c8004b1f), [JETIR paper](https://www.jetir.org/papers/JETIR2411605.pdf)

---

## Implications for the Prototype Architecture

### What the Pixel 10 Can Do

| Capability | Available? | Quality |
|---|---|---|
| Local obstacle depth detection (0–8m) | Yes | Good (depth-from-motion) |
| Extended depth range (up to 65m, mapped areas) | Yes (urban) | Good with VPS |
| Outdoor scene classification (11 labels) | Yes | Good for major features |
| Object detection & classification | Yes (ML Kit/TFLite) | Depends on model |
| Building/terrain mesh (urban) | Yes | Good in VPS areas |
| GPS + VPS positioning | Yes | High accuracy in VPS areas |
| Spoken feedback | Yes (TTS) | Full |
| Haptic feedback | Yes | Full |
| Voice input | Yes | Full |

### What the Pixel 10 Cannot Do

| Limitation | Impact |
|---|---|
| No hardware LiDAR | Depth is computed, not directly measured — lower precision and resolution |
| Depth requires motion | Cannot detect obstacles while standing completely still (first frame) |
| Scene Semantics outdoor only | Indoor navigation needs alternative object detection (ML Kit/TFLite) |
| Low depth resolution (160×120) | Fine obstacles (e.g., thin poles, steps) may be missed |
| No dedicated depth sensor | No infrared night-vision depth; depth quality drops in low light |

---

## Recommended Prototype Path

### Architecture: "ARCore Depth + ML + Audio/Haptic Feedback"

**Layer 1 — Spatial Sensing**
- ARCore Depth API (`acquireDepthImage16Bits()`) for obstacle distance
- ARCore Scene Semantics for environment classification (outdoor)
- ML Kit / TFLite custom model for indoor obstacle detection (stairs, doors, furniture)
- Geospatial Depth + Streetscape Geometry for urban wayfinding context

**Layer 2 — Processing**
- Obstacle detection: scan depth image center column for closest objects within threshold (e.g., 2m)
- Path finding: use Scene Semantics sidewalk/road labels to identify walkable areas
- Distance estimation: read millimeter depth values at detected obstacle locations
- Semantic labeling: combine ML Kit object labels with depth for "chair, 1.5m ahead"

**Layer 3 — Feedback**
- Spatial audio cues: directional beeps or voice announcements for obstacle location
- Haptic patterns: vibration intensity proportional to proximity (closer = stronger)
- TTS narration: spoken descriptions ("Obstacle ahead, 1.2 meters. Stairs down, 2 meters left.")
- Voice commands: speech recognition for user queries ("What's ahead of me?")

**Layer 4 — Wayfinding (mapped areas)**
- Geospatial API for GPS-anchored positioning
- Streetscape Geometry for building/landmark context
- Pre-loaded route data combined with real-time obstacle avoidance

### Recommended First Prototype

Start with a **minimal obstacle detector**: ARCore depth image → scan center region for nearest object within 2m → trigger haptic + TTS alert. This validates the core depth-from-motion pipeline before adding scene semantics, ML, and geospatial features.

---

## Development Environment

| Tool | Purpose |
|---|---|
| Android Studio | IDE |
| ARCore SDK for Android (Java/Kotlin) | Depth, Semantics, Geospatial APIs |
| ML Kit (via Google Maven) | Object detection & tracking |
| TensorFlow Lite | Custom obstacle models |
| CameraX | Camera feed management |
| Android TTS API | Spoken feedback |
| Android Haptic API | Vibration feedback |
| Android SpeechRecognizer | Voice input |

Min SDK: 24 (for AR Required apps), 21 (for ML Kit)

---

*Research date: August 15, 2026*
*Project: LIDAR based accessibility*
