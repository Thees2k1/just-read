---
name: android-emulator-tes
description: >-
  Diagnoses an Android emulator in headless mode via android CLI, adb,
  avdmanager, and AVD config.ini, and writes an Obsidian-friendly Test
  Environment Spec under notes/shared/test-env-specs/. Use when creating or
  refreshing a TES document for an AVD without Android Studio.
---

# Android Emulator TES Diagnostics

Use this skill to inspect a local AVD in headless mode and record a `TES-XX <AVD>.md` file in `notes/shared/test-env-specs/`.

## Steps

1. **Discover exact tool paths (never hardcode):**
   Run `ammo android doctor --json` (or `.\.script\android-doctor.ps1` / `./.script/android-doctor.sh` if `ammo` is unavailable) to get `android_cli_path`, `adb_path`, `avdmanager_path`, and `avds`.

2. **Read static AVD configuration:**
   - Read `~/.android/avd/<AVD>.avd/config.ini`.
   - Run `<avdmanager_path> list avd`.

3. **Start headless, diagnose via `adb`, and stop in ONE `run_command` call:**
   On Windows, `android emulator start --headless <AVD>`'s child QEMU process exits when the `run_command` task ends. Always chain start, `adb` queries, and stop in the same command:
   ```powershell
   <android_cli_path> emulator start --headless <AVD>
   if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
   <adb_path> devices -l
   <adb_path> -s <serial> shell getprop ro.build.version.release
   <adb_path> -s <serial> shell getprop ro.build.version.sdk
   <adb_path> -s <serial> shell getprop ro.build.version.codename
   <adb_path> -s <serial> shell getprop ro.build.version.security_patch
   <adb_path> -s <serial> shell getprop ro.build.fingerprint
   <adb_path> -s <serial> shell getprop ro.product.model
   <adb_path> -s <serial> shell getprop ro.product.cpu.abilist
   <adb_path> -s <serial> shell getprop ro.hardware.egl
   <adb_path> -s <serial> shell getprop dalvik.vm.heapsize
   <adb_path> -s <serial> shell getconf PAGESIZE
   <adb_path> -s <serial> shell "head -n 3 /proc/meminfo"
   <adb_path> -s <serial> shell nproc
   <adb_path> -s <serial> shell wm size
   <adb_path> -s <serial> shell wm density
   <adb_path> -s <serial> shell dumpsys device_state
   <android_cli_path> emulator stop <AVD>
   ```

4. **Write `notes/shared/test-env-specs/TES-XX <AVD>.md`:**
   - Include Obsidian YAML frontmatter (`id`, `device`, `avd_name`, `type`, `os`, `api`, `form_factor`, `abi`, `page_size_kb`, `ram_gb`, `status`, `focus`, `updated`, `tags`).
   - Record only directly observed `config.ini` and `adb` values; list any unqueried runtime metrics or real-hardware specs under **Out-of-Reach / Unverified Specifications**.
   - **Sanitize before saving:** Never include your local OS username or absolute home-directory path (use `~/.android/...` or `C:\Users\<user>\.android\...`), and never record physical device serials or personal storage usage.
