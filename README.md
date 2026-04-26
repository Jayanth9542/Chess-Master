<div align="center">

# ♛ ChessMaster

### A fully-featured Android chess app powered by the Stockfish engine via JNI

[![Android](https://img.shields.io/badge/Platform-Android-brightgreen?logo=android)](https://developer.android.com)
[![API](https://img.shields.io/badge/Min%20SDK-API%2028%20(Android%209)-blue)](https://developer.android.com/about/versions/pie)
[![Java](https://img.shields.io/badge/Language-Java%208-orange?logo=openjdk)](https://www.java.com)
[![NDK](https://img.shields.io/badge/NDK-C%2B%2B17%20%7C%20JNI-red)](https://developer.android.com/ndk)
[![Engine](https://img.shields.io/badge/Engine-Stockfish%20(UCI)-blueviolet)](https://stockfishchess.org)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-teal)](https://developer.android.com/topic/architecture)

<br/>

> *Play locally with a friend or go head-to-head against one of the world's strongest chess engines at five difficulty levels — from casual to Magnus-level.*

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Stockfish Integration](#-stockfish-integration)
- [Difficulty Levels](#-difficulty-levels)
- [Configuration](#-configuration)
- [Troubleshooting](#-troubleshooting)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)
- [Acknowledgements](#-acknowledgements)

---

## ✨ Features

### Gameplay
- ♟ **Player vs Player** — two players on the same device with custom names
- 🤖 **Player vs Bot** — challenge the Stockfish engine at five difficulty levels
- ✅ **Full rule enforcement** — castling (king-side & queen-side), en passant, pawn promotion, check, checkmate, stalemate, 50-move rule
- ↩ **Undo** — take back moves (undoes both your move and the bot's response in PvB mode)
- ⏱ **Elapsed timers** — per-player elapsed time display

### Board UI
- 🎨 **Custom Canvas board** — drawn entirely with Android `Canvas`, no external libraries
- 🟢 **Legal move highlights** — green dots on valid destination squares
- 🔴 **Check indicator** — red tint on the king's square when in check
- 🟡 **Last move highlight** — yellow tint on the previous move's from/to squares
- 🏷 **Rank & file labels** — a–h and 1–8 around the board edges
- 👆 **Touch selection** — tap to select, tap again to move, tap own piece to re-select

### App
- ⚙ **Settings** — toggle sound, toggle animations, switch Light/Dark theme
- 💾 **Persistent preferences** — names, difficulty, and settings saved via `SharedPreferences`
- 🌙 **Dark mode** — full Material Design dark theme with `values-night/` support
- 📱 **Portrait locked** — optimised for phone portrait layout
- 🔄 **Rotation-safe** — `ViewModel` survives configuration changes

---

## 📸 Screenshots

| Main Menu | Game Mode | PvP Setup | Game Screen | Settings |
|:---------:|:---------:|:---------:|:-----------:|:--------:|
| *(add screenshot)* | *(add screenshot)* | *(add screenshot)* | *(add screenshot)* | *(add screenshot)* |

> To add screenshots: take them on a device or emulator, place them in `docs/screenshots/`, and update the table above.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 8 |
| **UI** | XML Layouts, Material Design Components 1.12 |
| **Custom View** | Android `Canvas` (ChessBoardView) |
| **Architecture** | MVVM — `ViewModel`, `LiveData`, Repository |
| **Engine** | Stockfish (UCI protocol) |
| **Native Bridge** | Android NDK, JNI (C++17) |
| **IPC** | POSIX pipes + `dup2` (stdin/stdout redirection) |
| **Threading** | `ExecutorService` (Java), `pthread` (C++) |
| **Persistence** | `SharedPreferences` |
| **Build** | Gradle 8.4, CMake 3.22.1, AGP 8.2.2 |
| **Min SDK** | API 28 (Android 9 Pie) |
| **Target SDK** | API 35 (Android 15) |

---

## 🏗 Architecture

ChessMaster follows **MVVM (Model-View-ViewModel)** clean architecture:

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│  Activities ──► ChessBoardView (Canvas touch/draw)      │
│       │                  │                              │
│       └──────────────────┘                              │
│              observes LiveData / fires callbacks         │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    ViewModel Layer                       │
│  GameViewModel                                          │
│  ├── onHumanMove(Move)   → updates ChessBoard           │
│  ├── requestBotMove()    → background ExecutorService   │
│  ├── undoMove()                                         │
│  └── LiveData: boardLD, statusLD, gameOverLD, ...       │
└──────────────┬──────────────────────┬───────────────────┘
               │                      │
┌──────────────▼──────┐  ┌────────────▼────────────────────┐
│   Model Layer       │  │      Engine Layer               │
│  ChessBoard.java    │  │  EngineBridge.java (Java)        │
│  ChessPiece.java    │  │  ↕ JNI (System.loadLibrary)     │
│  Move.java          │  │  native-lib.cpp                 │
│  GameState.java     │  │  engine_wrapper.cpp (C++)        │
└─────────────────────┘  │  ↕ POSIX pipes (stdin/stdout)   │
                         │  stockfish_main() [pthread]      │
┌────────────────────┐   └─────────────────────────────────┘
│  Repository Layer  │
│  GameRepository    │
│  (SharedPrefs)     │
└────────────────────┘
```

### Data Flow — Human Move to Bot Response

```
User taps square
      │
ChessBoardView.onTouchEvent()
      │
OnMoveSelectedListener.onMoveSelected(Move)
      │
GameViewModel.onHumanMove(Move)
      │
ChessBoard.makeMove(Move)   ──►  boardLD.setValue()  ──►  Activity redraws board
      │
[if PvB & black to move]
      │
ExecutorService.execute( requestBotMove() )
      │
EngineBridge.getBestMove(fen, depth, skill)
      │                              [background thread]
native getBestMove() via JNI
      │
engine_wrapper.cpp: sendCommand("position fen …")
                    sendCommand("go depth N")
      │
Stockfish UCI loop (pthread) ──► pipe ──► readLine() ──► "bestmove e7e5"
      │
parseBotMove("e7e5") → Move object
      │
ChessBoard.makeMove(botMove)
      │
boardLD.postValue()  ──►  Activity redraws board
```

---

## 📁 Project Structure

```
ChessApp/
├── app/
│   ├── build.gradle                         # App-level Gradle (NDK, CMake, deps)
│   ├── proguard-rules.pro                   # Keep JNI + ViewModel classes
│   └── src/main/
│       ├── AndroidManifest.xml              # 6 activities declared
│       │
│       ├── cpp/                             # Native (NDK) code
│       │   ├── CMakeLists.txt               # CMake build — auto-collects Stockfish *.cpp
│       │   ├── native-lib.cpp               # JNI entry points (6 native methods)
│       │   ├── engine_wrapper.h             # C++ UCI bridge — header
│       │   ├── engine_wrapper.cpp           # C++ UCI bridge — POSIX pipes, pthread
│       │   └── stockfish/                   # ← PASTE STOCKFISH SOURCE HERE (see below)
│       │
│       ├── java/com/chessapp/
│       │   ├── engine/
│       │   │   └── EngineBridge.java        # Java singleton — loads .so, exposes native API
│       │   ├── model/
│       │   │   ├── ChessPiece.java          # Piece type/colour + Unicode + FEN char
│       │   │   ├── Move.java                # Move data + UCI string + undo state
│       │   │   ├── ChessBoard.java          # Full chess rules, FEN generation, undo stack
│       │   │   └── GameState.java           # Session state, timers, player names
│       │   ├── repository/
│       │   │   └── GameRepository.java      # SharedPreferences abstraction
│       │   ├── viewmodel/
│       │   │   └── GameViewModel.java       # MVVM ViewModel, LiveData, bot dispatch
│       │   ├── views/
│       │   │   └── ChessBoardView.java      # Custom Canvas board widget
│       │   └── ui/
│       │       ├── MainMenuActivity.java
│       │       ├── GameModeActivity.java
│       │       ├── PvPSetupActivity.java
│       │       ├── PvBotSetupActivity.java
│       │       ├── GameActivity.java
│       │       ├── SettingsActivity.java
│       │       └── AboutActivity.java
│       │
│       └── res/
│           ├── layout/                      # 7 XML layouts
│           ├── values/                      # strings, colors, themes (light)
│           ├── values-night/                # Dark theme overrides
│           ├── drawable/                    # bg_card.xml, ic_launcher_foreground.xml
│           └── mipmap-hdpi/                 # ic_launcher.xml
│
├── gradle/wrapper/
│   └── gradle-wrapper.properties           # Gradle 8.4
├── build.gradle                            # Root Gradle (AGP 8.2.2)
├── settings.gradle
├── .gitignore
└── README.md
```

---

## ✅ Prerequisites

Before building, make sure you have the following installed:

| Tool | Version | Where to get it |
|---|---|---|
| **Android Studio** | Hedgehog (2023.1.1) or later | [developer.android.com/studio](https://developer.android.com/studio) |
| **Android SDK** | API 35 | Android Studio SDK Manager |
| **NDK (Side by side)** | 27.x | Android Studio → SDK Manager → SDK Tools |
| **CMake** | 3.22.1 | Android Studio → SDK Manager → SDK Tools |
| **Build Tools** | 35.0.0 | Android Studio → SDK Manager → SDK Tools |
| **JDK** | 8 or higher | Bundled with Android Studio |

> **Install NDK and CMake via Android Studio:**
> `Tools → SDK Manager → SDK Tools tab` → check **NDK (Side by side)** and **CMake** → Apply

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Jayanth9542/Chess-Master.git
cd ChessMaster
```

### 2. Open in Android Studio

```
File → Open → select the ChessMaster/ folder → OK
```

### 3. Add Stockfish source (required for bot play)

See the [Stockfish Integration](#-stockfish-integration) section below.

### 4. Sync Gradle

```
File → Sync Project with Gradle Files
```

### 5. Build and run

```
Build → Make Project   (Ctrl+F9 / Cmd+F9)
Run   → Run 'app'      (Shift+F10 / Ctrl+R)
```

---

## ⚙ Stockfish Integration

> The Stockfish engine source is **not included** in this repository because it is licensed separately under the GNU GPL v3. You must download it yourself and place it in the correct folder. This takes about 2 minutes.

### Step 1 — Download Stockfish source

```bash
git clone https://github.com/official-stockfish/Stockfish.git
```

Or download the ZIP from [github.com/official-stockfish/Stockfish](https://github.com/official-stockfish/Stockfish) → **Code → Download ZIP**

> Stockfish 16 and 17 are both supported.

### Step 2 — Copy source files into the project

Copy **all files** from `Stockfish/src/` (the `src` subfolder, not the root) into:

```
app/src/main/cpp/stockfish/
```

**Using terminal:**
```bash
cp Stockfish/src/*.cpp  ChessMaster/app/src/main/cpp/stockfish/
cp Stockfish/src/*.h    ChessMaster/app/src/main/cpp/stockfish/
```

After copying, the folder should look like:

```
app/src/main/cpp/stockfish/
├── bitboard.cpp         ├── bitboard.h
├── evaluate.cpp         ├── evaluate.h
├── main.cpp             ├── misc.h
├── movegen.cpp          ├── movegen.h
├── position.cpp         ├── position.h
├── search.cpp           ├── search.h
├── uci.cpp              ├── uci.h
└── ... (all other .cpp and .h files)
```

> ⚠️ **Important:** All files must be placed **flat** in the `stockfish/` folder — no sub-directories. The `CMakeLists.txt` uses `GLOB_RECURSE` to collect them automatically.

> ⚠️ **Do NOT place the `Stockfish/src/` folder itself inside** `stockfish/` — copy the *contents* of `src/`, not the folder.

### Step 3 — Sync and build

```
File → Sync Project with Gradle Files
Build → Make Project
```

### Step 4 — Verify via Logcat

After launching the app and starting a **Player vs Bot** game, filter Logcat by `ChessEngine`:

```
I/ChessEngine: Stockfish thread starting
I/ChessEngine: -> Engine: uci
I/ChessEngine: <- Engine: uciok
I/ChessEngine: -> Engine: isready
I/ChessEngine: <- Engine: readyok
I/ChessEngine: Engine initialised successfully
```

If you see these lines, the engine is working correctly.

### How the integration works

```
Java EngineBridge.getBestMove()
        │  JNI
        ▼
native-lib.cpp  →  engine_wrapper.cpp
                        │
              POSIX pipe (write UCI command)
                        │
              Stockfish pthread (reads from pipe, writes output to pipe)
                        │
              POSIX pipe (read "bestmove e2e4")
                        │
        ▼  JNI
Java String returned to GameViewModel
```

Stockfish's `main()` function is renamed to `stockfish_main()` at compile time via the CMake flag `-Dmain=stockfish_main`. This allows the engine to be launched as a thread rather than a subprocess, with its `stdin`/`stdout` redirected through POSIX pipes using `dup2`.

---

## 🎯 Difficulty Levels

| Level | Stockfish Depth | Skill Level (0–20) | Approx. ELO |
|---|:---:|:---:|---|
| 🟢 **Easy** | 1 | 0 | ~800 |
| 🟡 **Intermediate** | 5 | 8 | ~1200 |
| 🟠 **Hard** | 10 | 14 | ~1800 |
| 🔴 **Grandmaster** | 15 | 18 | ~2400 |
| 💀 **Magnus** | 20 | 20 | ~3500 |

Difficulty is configured via Stockfish UCI options:
```
setoption name Skill Level value N
go depth N
```

---

## 🔧 Configuration

### NDK Version

Open `app/build.gradle` and add your exact NDK version string (visible in SDK Manager):

```groovy
android {
    defaultConfig {
        // Add this line — replace with your exact NDK version
        ndkVersion "27.0.12077973"

        ndk {
            abiFilters "arm64-v8a", "armeabi-v7a", "x86_64"
        }
    }
}
```

### ABI Filters

By default the project builds for three ABIs. To speed up debug builds, restrict to your test device's ABI:

```groovy
ndk {
    abiFilters "arm64-v8a"   // most modern Android phones
}
```

### CMake flags

Key compile definitions in `CMakeLists.txt`:

| Flag | Purpose |
|---|---|
| `-Dmain=stockfish_main` | Renames Stockfish's `main()` so it can be called as a thread |
| `IS_64BIT` | Enables 64-bit optimisations in Stockfish |
| `USE_PTHREADS` | Required for Stockfish's internal threading |
| `NDEBUG` | Disables debug assertions in release builds |

---

## 🐛 Troubleshooting

### `UnsatisfiedLinkError: chess_engine`
**Cause:** NDK not installed or library failed to compile.
**Fix:** Install NDK via `Tools → SDK Manager → SDK Tools → NDK (Side by side)`. Check the Build output for CMake errors.

---

### `No uciok received` in Logcat
**Cause:** Stockfish `.cpp` files are missing from `stockfish/`.
**Fix:** Re-do [Step 2](#step-2--copy-source-files-into-the-project) of the Stockfish integration. Ensure the folder is not empty.

---

### `CMake Error: stockfish_main undefined reference`
**Cause:** The `-Dmain=stockfish_main` flag is not being applied.
**Fix:** Clean the project (`Build → Clean Project`) and rebuild. If persists, check `target_compile_definitions` in `CMakeLists.txt`.

---

### `fatal error: 'immintrin.h' file not found`
**Cause:** Stockfish is trying to include x86 SSE/AVX intrinsics for an ARM build.
**Fix:** Add to `target_compile_definitions` in `CMakeLists.txt`:
```cmake
target_compile_definitions(chess_engine PRIVATE
    USE_POPCNT=0
    NO_PREFETCH
)
```

---

### App crashes with `SIGABRT` on engine start
**Cause:** Stockfish's thread stack size may be too small on some devices.
**Fix:** In `engine_wrapper.cpp`, add before `pthread_create`:
```cpp
pthread_attr_setstacksize(&attr, 8 * 1024 * 1024); // 8 MB stack
```

---

### `CMake version 3.22.1 not found`
**Fix:** `Tools → SDK Manager → SDK Tools → CMake` → install version 3.22.1.

---

### Engine thinking but never returning a move
**Cause:** Depth 20 (Magnus) can take a very long time on low-end devices.
**Fix:** The default timeout in `getBestMove()` is 15 seconds. Lower it, or reduce the Magnus depth in `EngineBridge.getDifficultyParams()`.

---

### Gradle sync fails with `Plugin version mismatch`
**Fix:** Ensure your Android Studio is Hedgehog (2023.1.1) or later. The project uses AGP 8.2.2.

---

## 🗺 Roadmap

- [x] Player vs Player (same device)
- [x] Player vs Stockfish bot (5 difficulty levels)
- [x] Full chess rule enforcement (castling, en passant, promotion)
- [x] Legal move highlights
- [x] Check / checkmate / stalemate detection
- [x] Undo moves
- [x] Elapsed timers
- [x] Light / Dark theme
- [x] Settings persistence
- [ ] 🔊 Sound effects (SoundPool — toggle exists, implementation pending)
- [ ] ✨ Piece move animations (ObjectAnimator — toggle exists, pending)
- [ ] ⏳ Countdown clock mode (e.g. 10+0, 3+2 blitz)
- [ ] 🏳 Draw offer and resignation
- [ ] 📡 Peer-to-Peer multiplayer (Bluetooth / Wi-Fi Direct)
- [ ] 📖 Move history sidebar (algebraic notation)
- [ ] 🔁 Board flip for black's perspective
- [ ] 💾 Save / load game (PGN format)
- [ ] 📊 Post-game analysis with engine evaluation
- [ ] 🌐 Online multiplayer

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Commit** your changes
   ```bash
   git commit -m "feat: add piece move animations"
   ```
4. **Push** to your fork
   ```bash
   git push origin feature/your-feature-name
   ```
5. **Open** a Pull Request

### Commit message style

This project loosely follows [Conventional Commits](https://www.conventionalcommits.org):

| Prefix | Use for |
|---|---|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `refactor:` | Code restructure, no behaviour change |
| `docs:` | README or documentation only |
| `chore:` | Build config, dependencies |

### Code style

- Java: follow the existing code style (4-space indent, no wildcard imports)
- C++: match the existing formatting in `engine_wrapper.cpp`
- No business logic in Activities — use ViewModel
- All Stockfish/engine calls must run off the main thread

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 [Your Name]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

> **Note on Stockfish:** The Stockfish engine itself is licensed under the **GNU General Public License v3**. It is not included in this repository. When you download and build it, you are subject to its license terms. See [Stockfish's license](https://github.com/official-stockfish/Stockfish/blob/master/Copying.txt).

---

## 🙏 Acknowledgements

- [**Stockfish**](https://stockfishchess.org) — The world's strongest open-source chess engine, without which the bot play feature would not exist.
- [**UCI Protocol**](https://www.shredderchess.com/chess-features/uci-universal-chess-interface.html) — Universal Chess Interface specification used to communicate with the engine.
- [**Android NDK**](https://developer.android.com/ndk) — Enabling C++17 native code execution on Android.
- [**Material Design Components**](https://github.com/material-components/material-components-android) — Google's Material Design library for Android.
- [**Chess Unicode Symbols**](https://en.wikipedia.org/wiki/Chess_symbols_in_Unicode) — ♔♕♖♗♘♙♚♛♜♝♞♟ used to render pieces on the board canvas.

---

<div align="center">

Made with ♟ and Java

**[⬆ Back to top](#-chessmaster)**

</div>
