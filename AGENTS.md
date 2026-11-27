# AGENTS.md

Guidance for AI coding agents working in **Just Read** (`com.justread.app`), a native Android PDF reading application ported from the UWP reference app in `archive/BookReaderApp/`.

## Android Build & Version Stack

Always use the Gradle wrapper (`./gradlew` on Linux/macOS, `.\gradlew.bat` on Windows) rather than a global `gradle` or `kotlinc` binary.

| Layer | Version / Setting | Source / Notes |
| :--- | :--- | :--- |
| **Application ID / Namespace** | `com.justread.app` | `app/build.gradle.kts` |
| **Gradle Wrapper** | `9.4.1-bin` | `gradle/wrapper/gradle-wrapper.properties` |
| **Android Gradle Plugin (AGP)** | `9.2.1` | `gradle/libs.versions.toml` |
| **Kotlin (KGP)** | `2.3.20` | `gradle/libs.versions.toml` |
| **Jetpack Compose BOM** | `2026.11.01` | `gradle/libs.versions.toml` |
| **SDK Targets** | `minSdk = 24`, `compileSdk = 37.1`, `targetSdk = 37` | `app/build.gradle.kts` |
| **JVM Toolchain** | `17` (`jvmToolchain(17)`) | `app/build.gradle.kts` |

## Discovering the Local Environment

Do not hardcode or assume local SDK, JDK, ADB, or emulator paths—this repository is developed across both Windows and Ubuntu/Linux machines.

Whenever you need to inspect the active machine's Android toolchain (SDK location, JDK version, `android` CLI, `adb`, connected devices, or available AVDs), run the project's doctor script:

- **Linux / macOS (Bash):** `./.script/android-doctor.sh`
- **Windows (PowerShell):** `.\.script\android-doctor.ps1`

## Common Commands

```bash
# Linux / macOS
./.script/android-doctor.sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

```powershell
# Windows (PowerShell)
.\.script\android-doctor.ps1
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

## Codebase Map

- `app/src/main/kotlin/com/justread/app/`
  - `MainActivity.kt`, `Navigation.kt`, `NavigationKeys.kt` — App entry point and navigation routes
  - `data/` — `DataRepository.kt`, `model/Book.kt`, `pdf/PdfDocumentManager.kt`
  - `ui/main/` — Library/home screen (`MainScreen.kt`, `MainScreenViewModel.kt`)
  - `ui/create/` — Create/import book flow (`CreateBookScreen.kt`, `CreateBookViewModel.kt`)
  - `ui/reader/` — PDF reader (`ReaderScreen.kt`, `ReaderViewModel.kt`, `SlidingWindowCalculator.kt`)
  - `ui/theme/` — Material 3 theme & reader styles (`Color.kt`, `Type.kt`, `Theme.kt`, `ReaderStyles.kt`)
- `app/src/test/` — JVM unit tests mirroring `com.justread.app` packages
- `app/src/androidTest/` — Compose UI instrumented tests
- `notes/` — Architecture notes, specs, and environment setup guides
  - `notes/shared/` — Shared templates and per-device Test Environment Specs (`test-env-specs/TES-XX <Name>.md`)
- `website/src/content/docs/` — Public-facing Astro documentation site
- `archive/BookReaderApp/` — Legacy UWP C#/XAML reference implementation (read-only reference)
