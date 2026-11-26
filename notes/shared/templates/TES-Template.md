---
id: TES00
device: ""
type: emulator # emulator | physical
os: Android 17
api: 37
form_factor: foldable # phone | foldable | tablet
abi: x86_64
page_size_kb: 16 # 4 | 16
ram_gb: 8
status: active # active | retired
focus: "" # e.g. "Fold postures, API 37, 16KB"
updated: {{date:YYYY-MM-DD}}
tags:
  - tes
  - android/api-37
  - form/foldable
  - env/emulator
---

# {{title}}

## Quick Run
- **AVD / Serial:** `<avd-name-or-serial>`
- **Start / Run:** `android emulator start <avd-name>` → `android run`

## Spec vs. Observed (`adb`)
> For emulators, compare **Observed (AVD)** against **Real Hardware**. For physical devices, compare **Observed (`adb`)** against **Official Spec**.

| Parameter | Observed (`adb` / AVD) | Real / Official Spec | Dev Notes |
| :--- | :--- | :--- | :--- |
| **Primary / Outer Display** | | | `wm size` / `wm density` |
| **Inner Display** *(if foldable)* | | | `sw<N>dp` breakpoint |
| **CPU / ABI** | | | `getprop ro.product.cpu.abilist` |
| **RAM / VM Heap** | | | |
| **Page Size** | | | `getconf PAGESIZE` (`16384` = 16 KB) |
| **Refresh Rate** | | | |

## Developer Constraints & Quirks
- **Build / Native:** *(e.g., 16 KB ELF alignment required; warns on old AGP 8.x/unaligned `.so`)*
- **API / OS Behavior:** *(e.g., API 37 edge-to-edge / large-screen resizability)*
- **Form Factor / OEM:** *(e.g., `dumpsys device_state` postures, or Samsung One UI battery limits)*
- **Known Gaps:** *(e.g., emulated 60 Hz GPU vs. real 120 Hz + thermal throttling)*

## Re-verify Commands
```powershell
adb shell getprop ro.build.version.sdk
adb shell getconf PAGESIZE
adb shell wm size
adb shell wm density
```