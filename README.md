# Memora

**Memora** is an offline-first personal memory app for Android. It helps you
remember where your physical belongings are, what is inside boxes and cupboards,
where document originals live, and what you lent to whom — all through simple
typing or optional voice input, with no cloud, no accounts and no internet.

Core principles:

- **Offline-first.** Every essential feature works with no connection. The app
  requests **no permissions at all** in Phase 1 — there is deliberately no
  INTERNET permission.
- **Physical object identity.** Every physical object gets its own internal
  UUID. Three different car keys can all be named `Car key` — Memora never
  merges records that just share a name, and never forces unique names.
- **Lightweight.** No background services, no always-listening microphone, no
  continuous AI. Work happens only while you use the app.
- **You stay in control.** Suggestions (descriptions, aliases, OCR results)
  are always optional and never overwrite confirmed data.

## Development status

The project is built incrementally, phase by phase. **This build implements
Phase 1 only** — later phases are not implemented yet and their screens say so
honestly.

| Phase | Scope | Status |
|------:|-------|--------|
| 1 | Project foundation: Gradle + Kotlin + Compose, navigation, theme, screens | ✅ done |
| 2 | Room database, entities, DAOs, repositories, indexes | ⬜ not started |
| 3 | Objects, locations, containers, detail screens | ⬜ not started |
| 4 | Identity resolution, movement history, undo | ⬜ not started |
| 5 | Documents, attachments, OCR, duplicate detection | ⬜ not started |
| 6 | Products, purchases, warranties, repairs | ⬜ not started |
| 7 | Natural-language understanding (typed input first) | ⬜ not started |
| 8 | On-demand offline voice input + TTS | ⬜ not started |
| 9 | Backup/restore, app lock, integrity checks | ⬜ not started |
| 10 | Optimization, regression and release preparation | ⬜ not started |

## Requirements

- **JDK 17** (bundled with recent Android Studio versions)
- **Android Studio** (Ladybug or newer recommended) with **Android SDK 35**
- Gradle 8.10.2 via the included wrapper — no separate Gradle install needed

## Build and run

**Android Studio:** open this folder, let it sync, pick a device or emulator
(API 26+), press Run.

**Command line:**

```bash
./gradlew :app:assembleDebug          # build the debug APK
./gradlew :app:testDebugUnitTest      # run JVM unit tests
./gradlew :app:connectedDebugAndroidTest   # run on a connected device/emulator
```

The first build downloads dependencies (Google Maven + Maven Central) and the
Android SDK — that happens on the development machine, never inside the app.

## Project structure

```text
app/src/main/java/com/memora/app/
├── app/                    # Application entry points
│   ├── MainActivity.kt
│   └── MemoraApplication.kt
├── core/
│   ├── common/             # Shared UI (PlaceholderScreen)
│   ├── navigation/         # Routes (pure Kotlin) + NavHost scaffold
│   └── ui/theme/           # Material 3 theme, colours, typography
├── domain/model/           # Pure domain models (AddMode, …)
└── feature/                # One package per screen
    ├── home/
    ├── search/
    ├── add/                # Add screen + AddViewModel (MVVM pattern)
    ├── documents/
    └── more/
```

Planned additions (see the phase table): `core/database`, `core/backup`,
`core/security`, `data/` (entities, DAOs, repositories), `intelligence/`
(normalization → intent → entity extraction → identity resolution pipeline),
`workers/ReminderWorker.kt`.

Navigation destinations live in `core/navigation/MemoraDestinations.kt` as pure
Kotlin (no Android imports), so route logic is unit-tested on the JVM.

## Testing

- `app/src/test/` — JVM unit tests (route integrity, AddViewModel state).
- `app/src/androidTest/` — instrumented smoke tests for navigation.

Mandatory acceptance scenarios (duplicate object names, container movement,
timestamp integrity, …) are introduced phase by phase alongside the features
they cover.

## Privacy

All data stays on the device (from Phase 2 onward: a local Room/SQLite
database). Local backup/restore arrives in Phase 9. Memora contains no
analytics, no trackers and no network code.
