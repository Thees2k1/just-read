# Just Read

> A native Android PDF reading application ported from the original UWP [BookReaderApp](./archive/BookReaderApp/).

*Status: Early planning / pre-initialization — Android toolchain versions and concrete app architecture are not finalized yet.*

---

## Overview

**Just Read** aims to bring a clean, modern PDF reading experience to Android with:

- **Native Reading Experience** — Smooth Jetpack Compose UI with Material 3 theming (including system-synced dark/light mode) and responsive layouts across screen sizes.
- **Local Library & Search** — Fast local storage with full-text search, bookmarks, reading progress, and favorites.
- **Accessibility & Localization** — Built with Compose Semantics and multi-language support (`en`, `vi`).
- **Cloud & Open Resources *(Planned)*** — Optional open-library exploration (e.g., Internet Archive), cloud library sync, and AI-assisted features.

---

## Tech Stack (High-Level)

- **Language:** Kotlin (Coroutines, Flow)
- **UI:** Jetpack Compose, Material 3
- **Toolchain & Architecture:** *TBD*

---

## Repository Structure

```text
just-read/
├── archive/
│   └── BookReaderApp/   # Original UWP reference implementation
├── notes/               # Development specs, roadmap, and engineering notes
└── README.md
```

---

## Documentation

- [Development Specs & Roadmap](./notes/roadmap-and-specs.md)
- [Fastest Demo Deployment Scheme](./notes/how/the-fastest-deployment-scheme.md)
- [Shell & CLI Learning Verdict](./notes/why/prequesite-shell-script-knowledge.md)

---

## Getting Started

Build instructions and demo APK releases will be added here once the Android project is initialized.
