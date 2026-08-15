# LIDAR-Based Accessibility for Blind and Low-Vision Navigation: A Comprehensive Landscape Report

*Version 2.0 — 15 August 2026. Integrates academic research from [a11ybob.com](https://a11ybob.com) (2,662 reviewed papers) with commercial product research across 40+ products.*

---

## Executive Summary

This report maps the full landscape of LIDAR-based and depth-sensing accessibility solutions for blind and low-vision (BLV) navigation, combining detailed analysis of 25+ key academic papers from [a11ybob.com](https://a11ybob.com) with a commercial survey of 40+ products across six categories. The research reveals a field at an inflection point — but also one where the commercial and academic frontiers are diverging.

**On the academic side**, consumer-grade LIDAR (iPhone Pro since 2020) has made real-time, map-free indoor navigation practical for the first time. The CaBot/PathFinder/WanderGuide lineage (2019–2026) has progressed from laboratory prototypes to 3-week longitudinal field deployments with GPT-4o integration. NavCog3 remains the most extensively evaluated system (53 participants, 21,000 m²), using LIDAR for map creation rather than user-facing sensing.

**On the commercial side**, true LIDAR is rare and mostly not on the user's body. It plays four distinct roles: (1) Apple's built-in Door Detection uses phone LIDAR for door/people/furniture detection; (2) indoor platforms like GoodMaps, Waymap, and Lazarillo use LIDAR as a survey instrument to map venues, then localise users with camera VPS or inertial dead reckoning; (3) the leading new hardware (Glide, .lumen, biped NOA) uses stereo/IR depth or multi-camera rigs, not LIDAR; (4) ultrasonic remains the dominant shipping obstacle sensor on smart canes. Only one commercial product — Strap Tech's **Ara** — combines LIDAR and ultrasonic on the body.

**The decisive market split** for this project is: **mapped** environments (GoodMaps/Waymap-style point-cloud survey + phone localisation) versus **unmapped** environments (Apple LiDAR Detection, Ara, Glide, Milo — all doing live geometry without a prior map). Milo's explicit claim of navigating "without a prior 3D scan" represents the unmapped frontier.

**Critical gaps** that no system — academic or commercial — credibly fills: unmapped indoor wayfinding, glass/mirror surface detection, open-lobby navigation beyond 5m phone LIDAR range, drop-offs and descending stairs, and last-few-meters (door-level) precision. Business-model fragility (Sunu insolvency, Soundscape discontinued, Google Glass EE2 support ended, Glide schedule slips) argues for designing prototypes on durable, open platforms.

---

## Part I: Academic Research Landscape

### 1. Smartphone LIDAR-Based Systems

The arrival of LIDAR on consumer iPhones (iPhone 12 Pro, 2020) opened a new paradigm: real-time indoor navigation assistance using only a phone, with no wearable rig, no building-scale BLE-beacon retrofit, and no pre-built digital map.

#### Corridor-Walker (2022)

**Paper:** Kuribayashi, Kayukawa, Vongkulbhisal, Asakawa, Sato, Takagi, Morishima. *Corridor-Walker: Mobile Indoor Walking Assistance for Blind People to Avoid Obstacles and Recognize Intersections.* MobileHCI, 2022.
**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e1a6df745c772d6dc36749)

| Property | Details |
|---|---|
| **Device** | Single off-the-shelf iPhone 12 Pro |
| **LiDAR use** | Continuously builds a 2D occupancy grid map of the corridor |
| **LiDAR range** | 5 m (fails in open lobbies) |
| **Floor detection** | RANSAC floor-plane detection labels cells as walkable, non-walkable, or unknown |
| **Path planning** | A* path planner generates obstacle-avoiding routes |
| **Intersection detection** | YOLOv3 detector trained on 9,940 grid-map images; classifies L, T, rotated-T, and X shapes |
| **Feedback** | Bone-conducting headphones (spatialised audio veering + TTS); phone vibration (continuous for collision, pulses for intersections) |
| **Evaluation** | 14 blind participants; 3 tasks |
| **Key results** | Intersection ID: T-shaped 21.4% → 92.9% (p<0.01); wall contacts on L-turns: 3.86 → 0.14 (p=0.004); SUS: 80.5 |
| **Limitations** | Only perpendicular intersections; 5m LiDAR range fails in open lobbies; phone-holding fatigue (11/14); system-aided walks 50–70% slower |

#### Snap&Nav (2024)

**Paper:** *Snap&Nav: Smartphone-based Indoor Navigation System For Blind People via Floor Map Analysis and Intersection Detection.* MobileHCI, 2024.
**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e189b20aa2707c55b144cb)

| Property | Details |
|---|---|
| **Device** | iPhone 12 Pro |
| **Innovation** | Photographs a physical floor map at building entrance to extract a navigation graph — no pre-built digital map required |
| **Map analysis** | Connected-component extraction, skeletonisation, Harris corner detection, OCR → node map |
| **Navigation** | Phone LiDAR builds local 2D occupancy grid; YOLOv7 detects intersection shapes; matching against floor-map graph |
| **Evaluation** | 20 sighted assistants (map capture) + 12 blind participants (navigation) |
| **Key results** | 99% correct position annotation; blind SUS: 92.5; all 6 Likert items favoured system vs. cane-only (p<0.05) |
| **Limitations** | Requires sighted assistant for initial map capture; only simple 90° corridors; no last-few-meters (specific door) resolution |

#### RASSAR (2023)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69b09be5f5c3998cc1c4e89b)

RASSAR uses iPhone LiDAR with Apple's RoomPlan API to construct real-time 3D parametric models of rooms, combined with YOLOv5, to identify 20 types of accessibility and safety issues. F1: 0.83 across 8 home spaces. Notably, **the system is currently inaccessible to blind users** — a gap directly relevant to this project.

#### LineChaser (2021)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e1a803f841aa898e41f266)

LineChaser addresses a specific micro-navigation task: finding the end of a line, joining it, and shuffling forward. Uses smartphone camera-based line detection and pedestrian detection with AR-marker-based localisation.

---

### 2. Robotic Navigation Platforms

The CaBot research programme, led by Chieko Asakawa's team at IBM Research / Carnegie Mellon / Waseda University, represents the most sustained effort in LIDAR-based robotic navigation for blind users, spanning 2019–2026.

#### CaBot (2019)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69bc5c4e8252003f7476400d)

| Property | Details |
|---|---|
| **Form factor** | Suitcase-shaped autonomous robot (25.2 kg) |
| **LiDAR** | Used for localisation (with wheel odometry via ROS) |
| **Stereo camera** | ZED stereo camera with YOLOv2 for dynamic obstacle detection |
| **Mapping** | Pre-mapped floorplan required |
| **Feedback** | 3 vibrators on handle; bone-conducting headphones for speech; user-controlled speed (0.05 m/s increments, max 1.0 m/s) |
| **Evaluation** | 10 blind participants; routes 41–89m |
| **Key results** | Zero navigation errors; SUS: 88; confidence 6.7/7, safety 6.3/7, trust 6.4/7 |

#### PathFinder (2023)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e1ab4df05a954f3b6e153a)

| Property | Details |
|---|---|
| **Platform** | CaBot-derived robot with PC (NVIDIA RTX 3080) + iPhone 12 Pro |
| **LiDAR** | 360° LiDAR with **Cartographer SLAM** |
| **Sign recognition** | iOS Vision OCR (real-time) + EasyOCR (on-demand) + YOLOv5 (arrow detection) |
| **Innovation** | **Map-less navigation**: reads the building in real-time rather than requiring pre-built maps |
| **Evaluation** | 7 blind participants; routes 46m and 166m |
| **Key results** | All 7 reached every destination; "Take-me-back" return function unanimously praised |
| **Limitations** | Transparent surfaces (glass bridges/doors) cause LiDAR false positives; sign-recognition accuracy ~44% correct-and-relevant |

#### WanderGuide (2025)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e1819deaf2c878f3054251)

Extends PathFinder's map-less approach from goal-directed navigation to **recreational, open-ended exploration** — wandering, browsing, window-shopping. Uses 360° LiDAR and 3 RGB-D cameras.

#### Beyond Omakase: Shared Control (2024)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e18837c45fdadfc32a857f)

Introduces three autonomy modes — **Omakase** (passive following), **Monitor** (information-seeking dialogue), and **Boss** (active command issuing). All 13 participants wanted all three modes. Key finding: **autonomy is a design variable, not an end goal.**

#### Robot-Assisted Group Tours (2026)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e1837a3eddfa342b5f1827)

| Property | Details |
|---|---|
| **Platform** | CaBot-based robot; LiDAR for collision avoidance; UWB for guide localization; GPT-4o for scene descriptions |
| **Innovation** | Extends from solo navigation to **mixed-visual group tours** |
| **Evaluation** | 8 blind participants at Miraikan; SUS: 90.6 |
| **Key issue** | LiDAR sometimes detected user's white cane or feet as obstacles, causing robot to freeze |

#### How Does Delegation Evolve? (2026)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e17fbef9b9f807a5dceee2)

3-week longitudinal study; 6 blind participants; AI Suitcase robot with LiDAR + 3 RGB-D cameras. Delegation is heterogeneous and dynamic: some participants delegated 83–100% from Week 1; others increased from 0–20% to 54.5–100% over three weeks.

#### Other Robot Systems

- **BlindPilot** (CHI EA 2020): Hokuyo LiDAR + ROS gmapping SLAM; leads user to landmark. [Review](https://a11ybob.com/writing/reviews/69e1b057021fa853c3a61bf1)
- **Guiding Blind Pedestrians** (IMWUT 2020): ROS Cartographer SLAM with LiDAR + IMU. [Review](https://a11ybob.com/writing/reviews/69e1a9abf878074c11cd14b4)
- **Field Trials** (CHI EA 2025): Real-world deployment of CaBot-derived robot. [Review](https://a11ybob.com/writing/reviews/69e1872537c1b53df2cc43d2)
- **Museum Robot** (CHI 2023): Autonomous navigation robot in museum. [Review](https://a11ybob.com/writing/reviews/69e1a3de3624507234db7dbf)

---

### 3. NavCog and BLE Beacon Systems

#### NavCog3 (2017)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69c6d649b26f8b68187f758b)

| Property | Details |
|---|---|
| **Localisation** | Hybrid: BLE beacon fingerprinting + pedestrian dead reckoning (PDR) |
| **Infrastructure** | ~220 BLE beacons across 3 connected buildings + subway station |
| **LiDAR role** | LIDAR-based fingerprinting machine for **map creation** (reduced time to 1/20th of manual) — not user-facing |
| **Accuracy** | 1.65 m average |
| **Evaluation** | 53 visually impaired participants; 21,000 m² shopping mall |
| **Key results** | Study 1: 10/10 completed all 3 routes (450m); Study 2: 188 trips, 52 unique POIs, avg 152m/trip |
| **Limitations** | Requires extensive BLE beacon infrastructure; single-session design |

---

### 4. Depth-Sensing Wearables and Alternative Form Factors

#### Eyes on the Palm: Ring Camera (2026)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e17e172d4c4de6f084c00c)

6.4g ring with 920×736 camera; YOLOv8 + Depth Anything V2 + GPT-4o. Ring camera: Raw-TLX median 35 vs. smartphone 52 (p<.001). Preserves tactile anchoring: 78.9% of trials kept one hand on exhibit (vs. 21% with smartphone).

#### NURing: Fingertip Deflection (2026)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69f000980aeaf08ccf5b22f7)

120° camera on tendon-driven ring; ArUco tags at 60 Hz for 3D pose estimation. Design philosophy: **"bias the body, do not narrate directions."** Authors position this as a stepping stone toward **markerless guidance via SLAM**.

#### BBeep: Sonic Collision Avoidance

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69e2163f9e8fd93f8a7b3096)

ZED stereo camera + YOLOv2 for pedestrian detection. Key finding: **broadcasting alerts to nearby pedestrians was more effective than private bone-conducting alerts** (mean collision risk: 0.41 vs. 2.00, p=0.005).

#### SoundSpace: Spatial Audio (2026)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69ebafb86d1d3b98053e535e)

YOLO-World + MiDaS monocular depth estimation — **no LIDAR** — maps depth to stereo panning, pitch, and loudness. Demonstrates that monocular depth can serve a similar function to LIDAR for spatial audio encoding.

#### SocialCue: Social Wayfinding (2026)

**Review:** [a11ybob.com](https://a11ybob.com/writing/reviews/69ee6f9ad885c2670ae37f84)

Meta Quest 3 with YOLOv10 + GPT-4o mini. Addresses **social navigation** — not where to walk, but how to read social scenes (gaze, expression, turn-taking).

---

### 5. Complementary Research

- **Beyond the Cane** (TACCESS 2022): Three-level info hierarchy for urban mobility. Intersection-related objects rated more useful than collision hazards. [Review](https://a11ybob.com/writing/reviews/69cbfd8324f831a53841ce7a)
- **ThermalCane** (ASSETS 2020): Peltier thermal directional cues on cane grip. Thermal > vibrotactile for directional accuracy. [Review](https://a11ybob.com/writing/reviews/69b994c70987446936d5a905)
- **All the Way There and Back** (TACCESS 2024): Inertial sensors only (phone in pocket) for dead reckoning — no LIDAR, no beacons.
- **SceneScout** (CHI 2026): GPT-4o makes Google Street View accessible to screen-reader users. [Review](https://a11ybob.com/writing/reviews/69e517228e71efdc3bf957cf)
- **Resilience to Disruption** (CHI 2026): Interoperability between apps, O&M skills, and remote helpers is a first-class resilience mechanism. [Review](https://a11ybob.com/writing/reviews/69e4e7e850f2bb64f23b4d11)
- **Haptic Feedback Review** (TACCESS 2025): 132 papers; 26 on guidance/navigation. Haptic and audio serve complementary roles. [Review](https://a11ybob.com/writing/reviews/69caa71de5d1adca5b1d94e7)

---

## Part II: Commercial Landscape

*Full product-by-product detail with source citations is in the companion file: `commercial-lidar-accessibility-research.md`*

### Headline Finding: True LIDAR Is Rare and Mostly Not on the User's Body

LIDAR/depth sensing plays four distinct commercial roles:

1. **Phone LIDAR (on-device)** — Apple's Door Detection explicitly uses "LiDAR, camera, and on-device machine learning," gated to iPhone/iPad Pro LiDAR models ([Apple Newsroom](https://www.apple.com/newsroom/2022/05/apple-previews-innovative-accessibility-features/)). Four third-party iOS apps (Super Lidar, Obstacle Detector, AI Guide Dog, EyeGuide Vision) piggyback on the same hardware — all free, all tiny (843 KB–16 MB), all thinly documented; max stated range 5 m ([AppleVis](https://www.applevis.com/apps/ios/navigation/obstacle-detector-blind)).

2. **LIDAR as a survey instrument, not a user sensor** — GoodMaps maps with "state-of-the-art LiDAR imaging" ([GoodMaps](https://goodmaps.com/)); Waymap's team "walks your venue with LiDAR scanners, capturing a precise 3D point cloud" then localises users with camera VPS + inertial dead reckoning at 100 Hz, needing "no GPS, no WiFi, no Bluetooth, no mobile signal" ([Waymap](https://www.waymapnav.com/our-tech)); Lazarillo lists "3D lidar scanning" as a mapping option ([Lazarillo](https://lazarillo.app/business/)).

3. **Depth ≠ LIDAR on leading new hardware** — Glide uses "dual infrared-enhanced stereo depth cameras" with cliff detection and 50 ft range ([Glidance](https://www.glidance.io/product)); .lumen uses six cameras, two IR laser projectors, three IMUs ([New Atlas](https://newatlas.com/wearables/dotlumen-ai-glasses-blind-independence/)); biped NOA uses three 170° cameras + GPS, no LIDAR ([biped FAQ](https://biped.ai/faq)).

4. **Ultrasonic still dominates shipping obstacle detection** — WeWALK Smart Cane 2 uses "ultrasonic time-of-flight (ToF) sensors" plus a 6-axis IMU ([Electronics360](https://electronics360.globalspec.com/article/21809/wewalk-unveils-smarter-tdk-powered-cane-at-ces-2025)).

**Only one commercial product combines LIDAR + ultrasonic on the body:** Strap Tech's **Ara** — "an array of LiDar and ultrasonic sensors," obstacles classified at three body heights, distinct vibration patterns for ascending/descending stairs and "recessed obstacles, like gaps and holes," limited to 250 units ([Strap Tech FAQ](https://www.strap.tech/frequently-asked-questions)).

### Commercial Products by Category

#### Phone-Based LIDAR / Depth Apps

| Product | Sensing | Price | Status |
|---|---|---|---|
| **Apple Magnifier Detection Mode** | LiDAR + camera + on-device ML | Free (bundled, LiDAR-model-gated) | Active; iOS 26 adds VoiceOver Live Recognition |
| **Super Lidar** | iPhone LiDAR + camera + AI | Free | Listed; residual COVID-era framing |
| **Obstacle Detector** | LiDAR scanner + TrueDepth camera | Freemium | Listed; 5m max range |
| **AI Guide Dog** | iPhone LiDAR or front camera | Free | Listed; liability disclaimer |
| **EyeGuide Vision** | iPhone LiDAR + AR scanning | Free | Active |
| **Seeing AI** (Microsoft) | Camera, OCR — **no LiDAR** | Free | Active; Android updated May 2026 |
| **Be My Eyes / Be My AI** | Live video + AI — **no depth** | Free | Active; out of beta |
| **Google Lookout** | Camera + sensors | Free | Active; Explore mode beta |

#### Smart Canes

| Product | Sensing | Price | Status |
|---|---|---|---|
| **WeWALK Smart Cane 2** | Ultrasonic ToF + 6-axis IMU + MEMS mics | $1,225–$1,250; voice assistant £4.99/mo | Shipping; CES 2025 |
| **SmartCane** (IIT Delhi) | Ultrasonic; 3m outdoor, 1.8m indoor | ~$90 | 20,000+ units distributed (India) |
| **BAWA Cane** | "Sensor-fusion" (unspecified) | n.a. | **Discontinued** (component EOL) |
| **Ray** (Caretec) | Ultrasonic + light sensor | n.a. | Listed |
| **Miniguide** | Ultrasonic pulse/echo | n.a. | Listed; positioned as secondary aid |

#### Wearables (Depth, LIDAR, Camera)

| Product | Sensing | Price | Status |
|---|---|---|---|
| **Ara** (Strap Tech) | **LiDAR + ultrasonic array** | Financing from $283/6mo | Limited: 250 units; ships worldwide |
| **.lumen glasses** | 6 cameras, 2 IR laser projectors, 3 IMUs, GPS | €9,999 (~$11,800) | CE-certified; 1,500+ preorders; free in Romania via state program |
| **biped NOA** | 3× 170° cameras + GPS — **no LiDAR** | $2,899 + $49/mo or $4,990 | Shipping; 25+ countries |
| **Glide** (Glidance) | Dual IR-enhanced stereo depth cameras | $1,499 + $30/mo | Pilot rollout began Jul 2026; Spring 2026 production |
| **Envision Glasses** | 8 MP camera — **no depth** | $1,899–$3,499 | Still sold; underlying Google Glass EE2 support ended Sep 2023 |
| **Ally Solos** (Envision) | HD cameras — **no depth** | $399 launch | Shipping Oct 2025 |
| **OrCam MyEye 3 Pro** | Smart camera — **no depth** | $4,250–$4,490 | Active; Orientation feature is beta |
| **eSight Go** | 12 MP camera + dual OLED | $4,950 | Active; for residual-vision users |
| **Sunu Band** | Sonar/echolocation to 5.5m | ~£380 historic | **Discontinued** (insolvency, June 2023) |
| **BuzzClip** | Ultrasonic, ~30° | $249 | Listed |

#### Indoor Navigation Platforms

| Product | LIDAR Role | User Localisation | Price | Status |
|---|---|---|---|---|
| **GoodMaps** | LIDAR-scanned 3D point clouds for mapping | Camera-based (ARKit/ARCore) | Free | Active; Walmart Mexico, airports, campuses |
| **Waymap** | LIDAR scanners for venue survey | SmartStep dead reckoning (100 Hz) + camera VPS | n.a. | Live in Washington DC (98 rail stations, 11,000+ bus stops) |
| **NaviLens** | None (visual codes) | Phone camera reads proprietary markers | Free app | Active; NYC MTA, Barcelona, Heathrow; 1M+ downloads |
| **Evelity** (Okeenea) | Not disclosed | "Like a GPS" | Free | Active; requires venue installation |
| **RightHear** | None | BLE beacons + GPS/OSM | Beacon subscription | Active; venue-side hardware model |
| **BlindSquare** | None | GPS + Foursquare/OSM; BLE beacons indoors | $39.99 | Active; 125M+ POIs globally |
| **Lazarillo** | "3D lidar scanning" as mapping option | Multiple options | Free consumer app | Active; piloting 5G indoor geolocation |
| **Clew** | None (AR camera) | Camera-based AR | Free | Listed; records one route at a time |
| **Aira** | None | Human visual interpreters + AI | Tiered subscription | Active; free at participating locations |
| **Microsoft Soundscape** | None | 3D binaural audio + GPS/OSM | Free | **Discontinued** Jan 2023; open-sourced |

#### Robotics / Autonomous Mobility

| Product | Sensing | Price | Status |
|---|---|---|---|
| **Glide** (Glidance) | Dual IR-enhanced stereo depth cameras; cliff detection; 50 ft range | $1,499 + $30/mo | Pilot rollout Jul 2026; Directed Navigation coming post-launch OTA |
| **Milo** (Mila) | Onboard sensors + voxel mapping + SAM segmentation; RL in BEV simulator | ~$2,000 (open-source) | Research prototype; open-source hardware designs |
| **RoboGuide** (Glasgow) | "Sophisticated sensors" + LLM | n.a. | Prototype in development |
| **Lysa** (Vixsystem, Brazil) | Unspecified sensors + voice | n.a. | Second generation in development |

### Key Commercial Findings

**Pricing spectrum:** Free (all OS features, Seeing AI, Be My Eyes, GoodMaps, NaviLens, Evelity) → $39.99 (BlindSquare) → $399 (Ally Solos) → $1,499 + $30/mo (Glide) → $2,899–$4,990 (biped NOA) → $4,950 (eSight Go) → $4,490 (OrCam MyEye 3 Pro) → €9,999 (.lumen).

**Business-model fragility is a material risk:** Soundscape discontinued 3 Jan 2023 (open-sourced); Sunu ceased operations June 2023 (no refunds); BAWA Cane out of production (component EOL); Google Glass EE2 support ended 15 Sep 2023 (stranding Envision Glasses buyers at up to $3,499); Glide slipped from 2025 to "Spring 2026 production timeline." Notably, Glidance founder Amos Miller previously led Microsoft Soundscape — direct lineage from the discontinued app to the leading new robotic aid ([Glidance FAQ](https://glidance.io/frequently-asked-questions/)).

**Cost disruption signal:** Mila's **Milo** — "the first fully autonomous robot guide dog," fully on-device, needing "no a priori knowledge of the environment," open-sourced, buildable for ~$2,000 vs ~$50,000 for a guide dog ([arXiv:2607.19530](https://arxiv.org/abs/2607.19530), [Mila](https://mila.quebec/en/article/milo-the-first-fully-autonomous-robot-guide-dog)).

---

## Part III: Comparative Analysis

### Research vs. Commercial: Where They Diverge

| Dimension | Academic Frontier | Commercial Frontier |
|---|---|---|
| **LIDAR on user's body** | Phone LIDAR (Corridor-Walker, Snap&Nav) | Only Ara (chest harness); Apple Door Detection (phone) |
| **LIDAR for mapping** | NavCog3 fingerprinting machine | GoodMaps, Waymap, Lazarillo (venue survey) |
| **Map-less navigation** | PathFinder, WanderGuide (360° LIDAR + SLAM) | Milo (open-source, ~$2,000); Apple Detection Mode (limited) |
| **Depth sensing (non-LIDAR)** | SoundSpace (MiDaS), Eyes on the Palm (Depth Anything V2) | Glide (stereo), .lumen (IR laser), biped NOA (cameras) |
| **Feedback grammar** | Spatialised audio, vibration patterns, thermal cues | Pitch-mapped audio (Super Lidar), 3D spatial beeps (biped), forehead haptics (.lumen), physical steering (Glide) |
| **Scale of evaluation** | Up to 53 users (NavCog3), 3-week longitudinal (Delegation) | 1,500+ preorders (.lumen), 20,000+ units (SmartCane), 300+ users (biped) |
| **Price point** | Research prototypes (no cost to participants) | $0–€9,999 |

### By Sensing Technology (Combined)

| Approach | Academic Systems | Commercial Systems | Strengths | Weaknesses |
|---|---|---|---|---|
| **Phone LIDAR** | Corridor-Walker, Snap&Nav, RASSAR | Apple Door Detection, Super Lidar, Obstacle Detector, AI Guide Dog, EyeGuide | No infrastructure; consumer hardware; real-time | 5m range; hardware-gated to Pro models; phone-holding fatigue |
| **Robot 360° LIDAR + SLAM** | CaBot, PathFinder, WanderGuide, BlindPilot | Milo (open-source) | Full SLAM; autonomous; handles complex environments | Robot size/weight; cost; social acceptability; glass-surface failures |
| **BLE Beacons + PDR** | NavCog3 | BlindSquare, RightHear, Evelity | High accuracy (1.65m); proven at scale | Extensive beacon infrastructure; maintenance burden |
| **Stereo/IR Depth** | BBeep (ZED), CaBot obstacle | Glide, .lumen | Real-time depth; longer range than phone LIDAR | Lighting dependent; no mapping |
| **Monocular Depth** | SoundSpace (MiDaS), Eyes on the Palm (Depth Anything V2) | None shipping | No depth sensor needed; works on commodity hardware | No metric depth; less accurate than LIDAR |
| **Ultrasonic** | — | WeWALK, SmartCane, BuzzClip, Miniguide, Ray | Proven; cheap; chest-height detection | No ground-level; no mapping; limited range |
| **LIDAR + Ultrasonic** | — | **Ara** (Strap Tech) | Multi-height; drop-off/stair detection | Limited to 250 units; pricing opaque |

---

## Part IV: Timeline of Notable Work

| Year | Academic | Commercial |
|---|---|---|
| 2012 | — | BlindSquare released |
| 2014 | — | SmartCane launched (IIT Delhi) |
| 2016 | NavCog (MobileHCI) | Toyota Project BLAID announced (never shipped) |
| 2017 | NavCog3 (ASSETS) — 53 users, 21,000 m² | — |
| 2019 | CaBot (ASSETS) — first fully evaluated nav robot | GoodMaps established at APH |
| 2020 | iPhone 12 Pro with LIDAR; BlindPilot; BBeep; ThermalCane | — |
| 2021 | LineChaser (CHI) | — |
| 2022 | Corridor-Walker (MobileHCI) — phone LIDAR nav | Apple Door Detection previewed (May) |
| 2023 | PathFinder (CHI) — map-less 360° LIDAR SLAM; RASSAR | Soundscape discontinued (Jan); Sunu ceased ops (Jun); Google Glass EE2 support ended (Sep) |
| 2024 | Snap&Nav (MobileHCI); Beyond Omakase | Glide preorders opened (Jul); Envision app made free |
| 2025 | WanderGuide (CHI); Field Trials | WeWALK Smart Cane 2 (CES); Ally Solos announced; Waymap live in DC |
| 2026 | Robot-Assisted Group Tours; Delegation (3-week); SoundSpace; Eyes on the Palm; SocialCue; NURing | Glide pilot rollout (Jul); Milo open-sourced (Jul); .lumen 1,500+ preorders |

---

## Part V: Key Research and Market Themes

### 1. No Single Tool Is Enough
Blind travelers combine multiple navigation technologies. AI-based systems are more reliable as on-demand consultation tools than as continuous navigation aids. ([a11ybob.com navigation essay](https://a11ybob.com/writing/research-essays/navigation-wayfinding-accessibility))

### 2. Map-less Navigation Is the Frontier
The academic trajectory: pre-built maps → BLE beacons → floor-map photography → real-time SLAM → phone LIDAR only. Commercially, every indoor platform requires prior survey, markers, or beacons — except Apple's Detection Mode (limited) and Milo (open-source research). 59.4% of blind people ask a sighted person to accompany them to unfamiliar buildings. ([PathFinder review](https://a11ybob.com/writing/reviews/69e1ab4df05a954f3b6e153a))

### 3. LIDAR's Commercial Role Is Mapping, Not Mobility
Three of four leading indoor platforms (GoodMaps, Waymap, Lazarillo) use LIDAR to survey venues, then localise users with camera VPS or inertial dead reckoning. The user never touches a LIDAR sensor. This is the decisive market split: **mapped** (survey + phone localisation) vs. **unmapped** (live geometry without a prior map).

### 4. Phone LIDAR Is Commercially Underexploited
The four third-party iOS LIDAR apps are all free, tiny (843 KB–16 MB), single-purpose proximity buzzers with limited documentation. Apple's own implementation is materially better (semantic door attributes, sign reading, haptic gradient) but hardware-gated. Range is the ceiling: 5m for phone LIDAR vs. 15m for Glide's stereo vision.

### 5. Autonomy Is a Design Variable
Beyond Omakase and How Does Delegation Evolve both demonstrate that fully autonomous robots can strip users of agency. Mode preferences are situational and evolve over time. ([Beyond Omakase](https://a11ybob.com/writing/reviews/69e18837c45fdadfc32a857f), [Delegation](https://a11ybob.com/writing/reviews/69e17fbef9b9f807a5dceee2))

### 6. LIDAR Has Known Failure Modes
- **Transparent surfaces** (glass bridges, doors, windows) cause false-positive intersection detection (PathFinder)
- **Open lobbies** exceed 5m phone LIDAR range (Corridor-Walker)
- **White canes and feet** misdetected as obstacles by robot LIDAR (Robot-Assisted Group Tours)
- **No commercial product addresses glass/mirror surfaces** — this appears only as MIT Media Lab research

### 7. Feedback Grammar Is Where Products Differentiate
Distinct approaches: pitch-mapped audio (Super Lidar), increasing-frequency haptics (Apple, BuzzClip), 3D spatial beeps with left/right/up/down (biped NOA), obstacle-type-specific vibration at three body heights (Ara), forehead-pull haptics mimicking guide dog harness (.lumen), physical steering of the user's hand (Glide), "bias the body, do not narrate directions" (NURing). Ara's "haptic language" and .lumen's guide-dog metaphor are the most transferable interaction models.

### 8. Price Is the Binding Constraint
Effective mobility-grade hardware sits at $1,499–€9,999. Only three mechanisms have broken that: state procurement (.lumen free in Romania), India's ADIP subsidy (SmartCane 20,000+ units), and open-source (Milo ~$2,000).

### 9. Business-Model Fragility Is a Real Risk
Sunu (insolvency), BAWA (component EOL), Soundscape (discontinued), Google Glass EE2 (platform withdrawal stranding Envision Glasses buyers), Glide (schedule slip from 2025 to Spring 2026). This argues for designing prototypes on durable platforms — mainstream phones, open standards (Wayfindr), or open-source stacks (Soundscape code, Milo).

---

## Part VI: Gaps and Opportunities

### Gaps No System — Academic or Commercial — Credibly Fills

1. **Unmapped indoor wayfinding**: Every commercial indoor platform requires prior survey, markers, or beacons. Clew's self-recorded routes (one at a time) are the only user-generated alternative. Academic systems (PathFinder, WanderGuide) demonstrate map-less navigation but require a robot. **Phone LIDAR map-free navigation in unmapped buildings remains an open frontier.**

2. **Glass and mirror surfaces**: No commercial product addresses transparent surfaces. PathFinder documents the failure but does not solve it. MIT Media Lab has researched it but no shipping product exists.

3. **Open-lobby / open-space navigation**: Phone LIDAR's 5m range fails in open lobbies (Corridor-Walker). No system handles large open indoor spaces without pre-built maps or beacons.

4. **Drop-offs and descending stairs**: Only Ara ("stairs going down," "recessed obstacles, like gaps and holes") and Glide ("cliff detection") claim this. No phone-based system offers ground-plane hazard detection.

5. **Last-few-meters precision**: Snap&Nav explicitly does not locate a specific door. NavCog3 achieves 1.65m accuracy — insufficient for door-level precision. No system provides reliable sub-meter navigation to a specific destination point.

6. **Outdoor LIDAR wayfinding**: All LIDAR-based systems operate indoors. Outdoor LIDAR navigation for blind users is essentially unexplored.

7. **Accessibility auditing accessible to blind users**: RASSAR uses LIDAR for accessibility auditing but is itself inaccessible to blind users.

8. **Long-term deployment studies**: Most systems are evaluated in single sessions (max 3 weeks). Long-term adoption and technology abandonment are understudied.

9. **Crowdsourced LIDAR mapping**: No system leverages multiple users' phone LIDAR scans to collaboratively build indoor maps (analogous to OpenStreetMap for indoor spaces).

10. **Deep LLM + LIDAR integration**: GPT-4o appears in recent systems but always as a bolt-on scene description tool. No system deeply integrates LIDAR depth data with LLM reasoning for spatial understanding.

### Commercial Gaps

11. **No commercial phone-LIDAR navigation app**: Despite Corridor-Walker and Snap&Nav proving the concept, no commercial app delivers map-free indoor navigation using phone LIDAR for blind users.

12. **Wearable LIDAR is nascent**: Only Ara combines LIDAR + ultrasonic on the body, limited to 250 units. No glasses, chest-mount, or cane-mounted LIDAR device exists at scale.

13. **Platform durability risk**: Multiple products have been stranded by platform withdrawal (Google Glass EE2, Sunu insolvency, Soundscape discontinuation).

---

## Part VII: Recommendations for Prototype Development

Based on the combined academic and commercial landscape analysis, the following prototype directions are recommended, prioritised by gap significance, technical feasibility, and alignment with the project's focus on mapped and unmapped LIDAR environments:

### Priority 1: Phone-LIDAR Map-Free Indoor Navigation (Extending Corridor-Walker / Snap&Nav)

Build on proven academic approaches, addressing their limitations:
- Extend beyond 5m range by fusing phone LIDAR with monocular depth estimation (MiDaS / Depth Anything V2)
- Handle open lobbies and non-perpendicular intersections
- Add last-few-meters navigation (door detection, leveraging Apple's Door Detection API)
- Address phone-holding fatigue with alternative mounting (chest harness, lanyard)
- Integrate with LLMs for semantic environmental understanding
- **Commercial gap addressed**: No commercial app delivers this; Apple's Detection Mode is limited to proximity alerts, not navigation

### Priority 2: LIDAR-Based Environmental Description and Auditing (Extending RASSAR)

Make RASSAR's accessibility auditing accessible to blind users:
- Non-visual output of room scans (spatial audio scene description)
- Real-time obstacle and hazard detection using phone LIDAR
- Door, counter, and furniture height measurement
- Integration with navigation for "scan and navigate" workflows
- **Commercial gap addressed**: No commercial product offers LIDAR-based environmental auditing for blind users

### Priority 3: Ground-Plane Hazard Detection (Extending Ara / Glide)

Address the critical gap in drop-off and descending stair detection:
- Phone LIDAR floor-plane segmentation for step-down detection
- Curb and drop-off detection for outdoor transitions
- Hole and gap detection in flooring
- Haptic feedback grammar for ground-level hazards (learning from Ara's three-height vibration patterns)
- **Commercial gap addressed**: Only Ara and Glide claim this; no phone-based solution exists

### Priority 4: Collaborative LIDAR Mapping

Develop a system where multiple users' phone LIDAR scans collaboratively build indoor maps:
- Anonymous occupancy grid sharing
- Map stitching from multiple traversals
- Crowdsourced accessibility annotations
- Open data approach (analogous to OpenStreetMap for indoor spaces)
- **Commercial gap addressed**: No commercial system offers user-generated indoor mapping; all require professional survey

### Priority 5: Outdoor LIDAR Wayfinding

Explore phone LIDAR for outdoor obstacle detection and path finding:
- Curb detection and drop-off warning
- Sidewalk edge tracking
- Obstacle classification (pole, tree, sign, person, bicycle)
- Integration with GPS for seamless indoor-outdoor transitions
- **Commercial gap addressed**: All LIDAR systems reviewed operate indoors; outdoor LIDAR is unexplored

### Priority 6: Social Navigation with LIDAR

Use LIDAR's precise distance measurement for social awareness:
- Personal space violation detection
- Conversation group identification
- Queue detection and line-following (extending LineChaser)
- Pedestrian trajectory prediction (extending BBeep)
- **Commercial gap addressed**: SocialCue addresses this with cameras, not LIDAR; no commercial product offers LIDAR-based social navigation

---

## Source Bibliography

### Academic Papers (from a11ybob.com)

**LIDAR-Specific Papers:**
- [Corridor-Walker](https://a11ybob.com/writing/reviews/69e1a6df745c772d6dc36749) — Phone LiDAR indoor navigation (MobileHCI 2022)
- [Snap&Nav](https://a11ybob.com/writing/reviews/69e189b20aa2707c55b144cb) — Floor-map + phone LiDAR (MobileHCI 2024)
- [PathFinder](https://a11ybob.com/writing/reviews/69e1ab4df05a954f3b6e153a) — 360° LiDAR + Cartographer SLAM (CHI 2023)
- [WanderGuide](https://a11ybob.com/writing/reviews/69e1819deaf2c878f3054251) — Map-less robotic exploration (CHI 2025)
- [CaBot](https://a11ybob.com/writing/reviews/69bc5c4e8252003f7476400d) — Autonomous navigation robot (ASSETS 2019)
- [NavCog3](https://a11ybob.com/writing/reviews/69c6d649b26f8b68187f758b) — BLE + LIDAR mapping (ASSETS 2017)
- [BlindPilot](https://a11ybob.com/writing/reviews/69e1b057021fa853c3a61bf1) — Hokuyo LiDAR + ROS gmapping (CHI EA 2020)
- [Field Trials](https://a11ybob.com/writing/reviews/69e1872537c1b53df2cc43d2) — Real-world deployment (CHI EA 2025)
- [Museum Robot](https://a11ybob.com/writing/reviews/69e1a3de3624507234db7dbf) — Museum autonomous navigation (CHI 2023)
- [Robot-Assisted Group Tours](https://a11ybob.com/writing/reviews/69e1837a3eddfa342b5f1827) — Mixed-visual tours (CHI 2026)
- [How Does Delegation Evolve](https://a11ybob.com/writing/reviews/69e17fbef9b9f807a5dceee2) — Longitudinal study (CHI 2026)
- [RASSAR](https://a11ybob.com/writing/reviews/69b09be5f5c3998cc1c4e89b) — LIDAR accessibility auditing (ASSETS 2023)
- [Guiding Blind Pedestrians](https://a11ybob.com/writing/reviews/69e1a9abf878074c11cd14b4) — Cartographer SLAM (IMWUT 2020)
- [LineChaser](https://a11ybob.com/writing/reviews/69e1a803f841aa898e41f266) — Smartphone line-following (CHI 2021)
- [Beyond Omakase](https://a11ybob.com/writing/reviews/69e18837c45fdadfc32a857f) — Shared control modes
- [BBeep](https://a11ybob.com/writing/reviews/69e2163f9e8fd93f8a7b3096) — Stereo camera collision avoidance

**Depth-Sensing and Related:**
- [Eyes on the Palm](https://a11ybob.com/writing/reviews/69e17e172d4c4de6f084c00c) — Ring camera + Depth Anything V2 (CHI 2026)
- [SoundSpace](https://a11ybob.com/writing/reviews/69ebafb86d1d3b98053e535e) — Monocular depth + spatial audio
- [SocialCue](https://a11ybob.com/writing/reviews/69ee6f9ad885c2670ae37f84) — Social wayfinding (CHI EA 2026)
- [NURing](https://a11ybob.com/writing/reviews/69f000980aeaf08ccf5b22f7) — Fingertip deflection (CHI EA 2026)
- [Beyond the Cane](https://a11ybob.com/writing/reviews/69cbfd8324f831a53841ce7a) — Urban scene description (TACCESS 2022)
- [ThermalCane](https://a11ybob.com/writing/reviews/69b994c70987446936d5a905) — Thermal directional cues (ASSETS 2020)
- [SceneScout](https://a11ybob.com/writing/reviews/69e517228e71efdc3bf957cf) — AI street-level imagery (CHI 2026)
- [Resilience to Disruption](https://a11ybob.com/writing/reviews/69e4e7e850f2bb64f23b4d11) — Navigation resilience (CHI 2026)
- [Haptic Feedback Review](https://a11ybob.com/writing/reviews/69caa71de5d1adca5b1d94e7) — 132-paper systematic review (TACCESS 2025)

**Research Essays:**
- [Navigation and Wayfinding Essay](https://a11ybob.com/writing/research-essays/navigation-wayfinding-accessibility) — Synthesis of 64 papers (2020–2025)

### Commercial Sources (detailed in companion file `commercial-lidar-accessibility-research.md`)

- [Apple Newsroom — Accessibility features](https://www.apple.com/newsroom/2022/05/apple-previews-innovative-accessibility-features/)
- [GoodMaps](https://goodmaps.com/)
- [Waymap — Technology](https://www.waymapnav.com/our-tech)
- [Strap Tech — Ara FAQ](https://www.strap.tech/frequently-asked-questions)
- [Glidance — Glide product](https://www.glidance.io/product)
- [.lumen glasses — New Atlas](https://newatlas.com/wearables/dotlumen-ai-glasses-blind-independence/)
- [biped.ai — FAQ](https://biped.ai/faq)
- [WeWALK — CES 2025, Electronics360](https://electronics360.globalspec.com/article/21809/wewalk-unveils-smarter-tdk-powered-cane-at-ces-2025)
- [NaviLens](https://www.navilens.com/en)
- [BlindSquare — Indoor](https://www.blindsquare.com/indoor/)
- [Lazarillo — Business](https://lazarillo.app/business/)
- [Mila — Milo](https://mila.quebec/en/article/milo-the-first-fully-autonomous-robot-guide-dog)
- [arXiv:2607.19530 — Milo paper](https://arxiv.org/abs/2607.19530)
- [Microsoft Soundscape — Support](https://www.microsoft.com/en-us/research/product/soundscape/support/)
- [Sunu CEO letter](https://milled.com/sunu-band/an-important-update-from-sunus-ceo-n7V4WEajj9Q9VYbH)
- [AppleVis — Obstacle Detector](https://www.applevis.com/apps/ios/navigation/obstacle-detector-blind)
- [a11ybob.com — Maps page](https://a11ybob.com/maps)

---

*Report compiled from 2,662 paper reviews on a11ybob.com (25+ papers analysed in detail) and commercial research across 40+ products in 6 categories. Academic sources span ACM ASSETS, W4A, CHI, MobileHCI, IMWUT, and TACCESS (2010–2026). Commercial sources include manufacturer pages, App Store/Google Play listings, press coverage, and independent reviews (accessed August 2026). Full commercial product-by-product detail with inline source citations is in the companion file: `commercial-lidar-accessibility-research.md`.*
