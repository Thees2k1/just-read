# Just Read

> A native Android PDF reading application ported from the original UWP [BookReaderApp](./archive/BookReaderApp/).

---

## Overview

**Just Read** aims to bring a clean, modern PDF reading experience to Android with:

- **Native Reading Experience** — Smooth Jetpack Compose UI with Material 3 theming (including system-synced dark/light mode) and responsive layouts across screen sizes.
- **Local Library & Search** — Fast local storage with full-text search, bookmarks, reading progress, and favorites.
- **Accessibility & Localization** — Built with Compose Semantics and multi-language support (`en`, `vi`).
- **Cloud & Open Resources *(Planned)*** — Optional open-library exploration (e.g., Internet Archive), cloud library sync, and AI-assisted features.

---

## Tech Stack & Version Stack

| Component | Version / Value |
| :--- | :--- |
| **Application ID / Namespace** | `com.justread.app` |
| **Language & KGP** | Kotlin `2.3.20` (Coroutines, Flow) |
| **UI** | Jetpack Compose (BOM `2026.11.01`), Material 3 |
| **Android Gradle Plugin (AGP)** | `9.2.1` |
| **Gradle Wrapper** | `9.4.1-bin` |
| **Android SDK Targets** | `minSdk 24` \| `compileSdk 37` (`37.1`) \| `targetSdk 37` |
| **JVM Toolchain** | Java `17` (`jvmToolchain(17)`) |
| **Host JDK** | OpenJDK `25.0.3` (Android Studio JBR) |
| **Android Studio** | `2026.2.1` (Canary `2026.2.3`) |
| **Android CLI / ADB / Emulator** | `android` `1.1.16919734` \| ADB `36.0.0` \| Emulator `36.5.10.0` |

---

## Repository Structure

```text
just-read/
├── app/                 # Android application module (Kotlin + Jetpack Compose)
├── archive/
│   └── BookReaderApp/   # Original UWP reference implementation
├── gradle/              # Gradle wrapper & libs.versions.toml version catalog
├── notes/               # Development specs, roadmap, and engineering notes
├── .script/             # Environment diagnostics (android-doctor.ps1 / .sh)
├── AGENTS.md            # AI agent context, toolchain stack, and commands
└── README.md
```

---

## Documentation

- [Development Specs & Roadmap](./notes/roadmap-and-specs.md)
- [Fastest Demo Deployment Scheme](./notes/how/the-fastest-deployment-scheme.md)
- [Shell & CLI Learning Verdict](./notes/why/prequesite-shell-script-knowledge.md)

---

## Getting Started

1. **Check your local Android environment:**
   ```powershell
   .\.script\android-doctor.ps1
   ```
2. **Build the debug APK:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
3. **Run unit tests:**
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
