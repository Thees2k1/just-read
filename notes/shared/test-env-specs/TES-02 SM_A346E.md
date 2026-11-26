---
id: TES-02
device: Samsung Galaxy A34 5G (SM-A346E)
serial: <device-serial>
type: physical
os: Android 16 (One UI 8.0)
api: 36
form_factor: phone
abi: arm64-v8a, armeabi-v7a, armeabi
page_size_kb: 4
ram_gb: 7.3
status: active
focus: Real-world mid-range ARM64 perf, 120 Hz, One UI 8.0 (API 36)
updated: 2026-11-27
tags:
  - tes
  - android/api-36
  - form/phone
  - env/physical
  - oem/samsung
---

# TES-02: Samsung Galaxy A34 5G (`SM-A346E`)

## Quick Run
- **ADB Serial:** `<device-serial>` (`product:a34xdxx`, `device:a34x`)
- **Run App:** `android run --device=<device-serial>`

### CLI Sources Used for This Spec
- **Android Debug Bridge (`adb.EXE` `1.0.41` / `36.0.0-13206524`):** `adb devices -l`, `getprop`, `getconf PAGESIZE`, `/proc/meminfo`, `nproc`, `df -h /data`, `wm size`, `wm density`, `dumpsys display`

---

## Observed (`adb`) Hardware & System Spec

| Parameter | Observed (`adb` on `<device-serial>`) | Dev Notes |
| :--- | :--- | :--- |
| **OS / API Level** | Android `16`, SDK `36`, One UI `80000` (8.0), Patch `2026-04-05` | Within `compileSdk = 37.0` / `minSdk = 24` |
| **Build Fingerprint** | `samsung/a34xdxx/a34x:16/BP2A.250605.031.A3/A346EXXSEEZC7:user/release-keys` | Production release build |
| **Model / Board** | `samsung` `SM-A346E` (`a34x`), `ro.hardware=mt6877`, `ro.board.platform=mt6877` | MediaTek `mt6877` SoC |
| **CPU / ABI** | `arm64-v8a,armeabi-v7a,armeabi`, `nproc=8` | Real 64-bit ARMv8 + 32-bit ABI support |
| **RAM / VM Heap** | `MemTotal: 7648504 kB` (~7.29 GiB), `dalvik.vm.heapsize=512m` | 8 GB nominal RAM |
| **Memory Page Size** | `4096` bytes (`4 KB`) via `getconf PAGESIZE` | Standard 4 KB kernel page size |
| **Storage (`/data`)** | `225G` total | 256 GB nominal internal storage |
| **Display** | `1080x2340` px @ `450` dpi (`391.89 x 393.62` physical ppi, scale `2.8125`) | `384 × 832 dp` (`Compact` width class) |
| **Refresh Rate** | Supported: `60.0 Hz`, `120.0 Hz` (`defaultModeId=2` = 120 Hz; `hasArrSupport=false`) | Screen was `OFF` during dump (`renderFrameRate=60.0`) |
| **Cutout & Corners** | Top cutout `Rect(468, 0 - 612, 75)` (`insets=0,75,0,0`); corner radius `113` px | Test edge-to-edge top inset & rounded corner clearance |
| **GPU / EGL** | `ro.hardware.egl=meow`, `maxLuminance=500.0` nits (no HDR modes reported) | Real mobile GPU + thermal throttling |

---

## Developer Constraints & Quirks
- **One UI 8.0 / Samsung OEM Behavior:** Watch for Samsung background battery optimization (Sleeping / Deep Sleeping apps) and aggressive foreground-service restrictions if background work is added later.
- **384 dp Compact Width + Top Cutout:** At `450` dpi (`2.8125`x), `1080` px = `384 dp` wide with a `75 px` (`~26.7 dp`) top center notch—verify top app bars, reader controls, and edge-to-edge padding.
- **120 Hz Real-Device Jank Check:** Unlike the 60 Hz emulator, this device runs at `120 Hz` (`8.33 ms` frame budget), making it ideal for verifying PDF scroll/zoom smoothness and profile-build performance.
- **4 KB Page Size:** Unlike `TES-01` (16 KB), this device uses `4 KB` pages, so 16 KB alignment issues will *not* surface here—keep testing 16 KB on `TES-01`.

---

## Out-of-Reach / Unverified Specifications
- **Official / Physical Specs (Web Lookup Needed):** Commercial marketing name for `mt6877` (Dimensity 1080), battery capacity (mAh), charging wattage, IP rating, and Gorilla Glass / panel type.
- **Runtime Items Not Queried:** Battery health/temperature (`dumpsys battery`), thermal throttling state (`dumpsys thermalservice`), and live `120 Hz` frame timings with the screen `ON`.

---

## Re-verify Commands
```powershell
adb -s <device-serial> shell getprop ro.build.version.sdk
adb -s <device-serial> shell getprop ro.build.version.oneui
adb -s <device-serial> shell getconf PAGESIZE
adb -s <device-serial> shell wm size
adb -s <device-serial> shell wm density
```
