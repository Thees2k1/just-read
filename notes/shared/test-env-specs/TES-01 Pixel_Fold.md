---
id: TES-01
device: Pixel Fold (API 37 Emulator)
avd_name: Pixel_Fold
type: emulator
os: Android 17
api: 37
form_factor: foldable
abi: x86_64, arm64-v8a
page_size_kb: 16
ram_gb: 2.9
status: active
focus: Fold postures, Android 17 (API 37), 16 KB page size
updated: 2026-11-27
tags:
  - tes
  - android/api-37
  - form/foldable
  - env/emulator
  - page-size/16kb
---

# TES-01: Pixel_Fold (Emulator)

## Quick Run & Tools Used
- **AVD Name:** `Pixel_Fold` (Display name: `Pixel Fold`)
- **AVD Path:** `C:\Users\<user>\.android\avd\Pixel_Fold.avd`
- **Headless Start:** `android emulator start --headless Pixel_Fold`
- **Stop:** `android emulator stop Pixel_Fold`
- **Run App:** `android run` (or `android run --device=emulator-5554`)

### CLI & Config Sources Used for This Spec
1. **`ammo android doctor --json`** — SDK, JDK, AGP/Kotlin/Compose project stack, and tool path discovery
2. **Android CLI (`android.EXE` `1.1.16919734`)** — `android info`, `android emulator start --headless Pixel_Fold`, `android emulator stop Pixel_Fold`
3. **Android Debug Bridge (`adb.EXE` `1.0.41` / `36.0.0-13206524`)** — `adb devices -l`, `getprop`, `getconf PAGESIZE`, `/proc/meminfo`, `nproc`, `wm size`, `wm density`, `dumpsys device_state`
4. **AVD Manager (`avdmanager.bat`)** — `avdmanager list avd`
5. **AVD Config (`config.ini`)** — `C:\Users\<user>\.android\avd\Pixel_Fold.avd\config.ini`

---

## Hardware & System Spec (`config.ini` vs. `adb` Observed)

| Parameter | `config.ini` (AVD Config) | Observed (`adb` on `emulator-5554`) | Dev Notes |
| :--- | :--- | :--- | :--- |
| **OS / API Level** | `target=android-37.0` | Android `17` (`REL`), SDK `37`, Patch `2026-03-05` | Matches `compileSdk = 37.0`, `targetSdk = 37` |
| **System Image** | `system-images\android-37.0\google_apis_playstore_ps16k\x86_64\` | `google/sdk_gphone16k_x86_64/emu64xa16k:17/CP21.260306.017.A1/15087165:user/dev-keys` | Play Store enabled, 16 KB page-size image |
| **Model / Device** | `hw.device.name=pixel_fold` (`Google`) | `sdk_gphone16k_x86_64` | Emulator generic Pixel Fold profile |
| **CPU / ABI** | `x86_64`, `hw.cpu.ncore=2` | `x86_64,arm64-v8a`, `nproc=2` | Supports `x86_64` native + `arm64-v8a` translation |
| **RAM / VM Heap** | `hw.ramSize=2048` MB, `vm.heapSize=336` MB | `MemTotal: 3043480 kB` (~2.90 GiB), `dalvik.vm.heapsize=576m` | Lower RAM than real hardware; watch large PDF bitmaps |
| **Memory Page Size** | `tag.ids=...,page_size_16kb,...` | `16384` bytes (`16 KB`) via `getconf PAGESIZE` | Enforces 16 KB ELF alignment for native `.so` libs |
| **Inner / Main Display** | `2208x1840` px @ `420` dpi (`landscape`) | `Physical size: 2208x1840`, `Physical density: 420` | ~841 × 701 dp (`Expanded` width class when unfolded) |
| **Outer / Cover Region** | `hw.displayRegion.0.1`: `1080x2092` (offset `0,0`) | Not separately queried via `wm` | ~411 × 797 dp (`Compact` width class when folded) |
| **Hinge / Fold Sensor** | `hw.sensor.hinge=yes`, `ranges=0-180`, `defaults=180`, postures `1, 2, 3` (`0-30`, `30-150`, `150-180`), `areas=1080-0-0-1840` | `dumpsys device_state`: `OPENED` (`id=2`), `Goldfish hinge sensor0: [180.0]` | Supports `CLOSED`, `HALF_OPENED`, `OPENED` postures |
| **GPU / EGL** | `hw.gpu.enabled=yes`, `hw.gpu.mode=auto` | `ro.hardware.egl=emulation` | Host-backed emulated GLES/Vulkan |
| **Storage** | `disk.dataPartition.size=6G`, `sdcard.size=512M` | — | Virtual disk / SD card |
| **Cameras & Sensors** | Back: `virtualscene`, Front: `emulated`; accel, gyro, light, mag, orient, pressure, proximity, GPS | — | Virtual sensors only |

---

## Developer Constraints & Notes
- **16 KB Page Size (`16384` bytes):** Any native `.so` dependencies (including transitive PDF/imaging libraries) must be 16 KB ELF-aligned. Older projects (e.g., AGP 8.x without 16 KB alignment support or older NDKs) will warn or fail on this image.
- **Foldable Breakpoints & Hinge:** Unfolded (`2208x1840` @ `420` dpi ≈ `841 dp` wide) triggers `Expanded` layouts; folded (`1080x2092` @ `420` dpi ≈ `411 dp` wide) triggers `Compact`. Hinge occlusion area is defined at `1080-0-0-1840` with half-open posture between `30°–150°`.
- **Toolchain Note (`ammo android doctor`):** Project uses `AGP 9.1.0`, `Gradle 9.3.1`, `Kotlin 2.3.20`, `compileSdk 37.0`, `targetSdk 37`, `jvmToolchain(17)`. Active system `JAVA_HOME` is JDK 25 (differs from `jvmToolchain(17)`).

---

## Out-of-Reach / Unverified Specifications
The following items were **not** available from `config.ini`, `android` CLI, `avdmanager`, or the `adb` checks run in this session and need either web lookup (e.g., Google Store / GSMArena) or a physical device:

1. **Real Pixel Fold Hardware Specs (Web Lookup Needed):**
   - Physical OLED panel sizes (inches), pixel-per-inch (PPI), and LTPO refresh rate (e.g., 120 Hz).
   - Real SoC (Google Tensor generation), NPU/GPU performance, real RAM (12 GB / 16 GB), UFS storage speed, battery capacity, and thermal throttling behavior.
2. **Emulator / Runtime Items Not Queried in This Session:**
   - Actual emulated refresh rate / vsync (`dumpsys SurfaceFlinger` / `dumpsys display`).
   - Exact `WindowInsets` / display cutout bounds in folded vs. unfolded postures (`dumpsys window displays`).
   - `avdmanager` XML parse warning observed in `C:\Users\<user>\.android\devices.xml` (`cvc-elt.1.a: Cannot find the declaration of element 'd:devices'`, backed up to `devices.xml.old`).

---

## Re-verify Commands
```powershell
android emulator start --headless Pixel_Fold
adb -s emulator-5554 shell getprop ro.build.version.sdk
adb -s emulator-5554 shell getconf PAGESIZE
adb -s emulator-5554 shell wm size
adb -s emulator-5554 shell wm density
adb -s emulator-5554 shell dumpsys device_state
android emulator stop Pixel_Fold
```
