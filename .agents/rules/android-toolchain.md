---
trigger: always_on
---

# Android Toolchain & Docs Rules

## Environment Discovery
- When inspecting the active machine's Android toolchain (SDK, `android` CLI, `adb`, `avdmanager`, AVDs, or project build predictions), prefer `ammo android doctor --json` when `ammo` is available; fall back to `.\.script\android-doctor.ps1` (Windows) or `./.script/android-doctor.sh` (Linux/macOS).

## Documentation Boundary
- **Internal engineering & QA docs:** Store in `notes/` (and shared team templates/TES specs in `notes/shared/test-env-specs/`).
- **Public product docs:** Store in `website/src/content/docs/` (Astro site). Never place internal TES files, ADRs, or local AVD configs inside `website/src/content/docs/`.
- **Public repo sanitization:** Never write local OS usernames, absolute home-directory paths (e.g. `C:\Users\<name>\...` or `/home/<name>/...`), physical device ADB serial numbers, or personal disk usage figures into `notes/` or other tracked files. Use placeholders such as `~/.android/...`, `C:\Users\<user>\...`, and `<device-serial>`.
