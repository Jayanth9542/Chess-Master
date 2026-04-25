# ♛ ChessMaster — Android Chess App with Stockfish JNI

A complete Android chess application featuring:
- Full PvP (same device) and Player vs Stockfish bot modes
- Custom `ChessBoardView` drawn on Canvas with legal-move highlights
- MVVM architecture (ViewModel + LiveData + Repository)
- Five difficulty levels: Easy → Intermediate → Hard → Grandmaster → Magnus
- Full rule support: castling, en passant, pawn promotion, check/checkmate/stalemate
- Undo, timer display, dark/light theme

---

## 📁 Project Structure

```
ChessApp/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── cpp/
│       │   ├── CMakeLists.txt          ← CMake build config
│       │   ├── native-lib.cpp          ← JNI entry points
│       │   ├── engine_wrapper.h        ← C++ UCI bridge header
│       │   ├── engine_wrapper.cpp      ← C++ UCI bridge implementation
│       │   └── stockfish/              ← ⚠ PASTE STOCKFISH SOURCE HERE
│       ├── java/com/chessapp/
│       │   ├── engine/
│       │   │   └── EngineBridge.java   ← Java ↔ JNI interface
│       │   ├── model/
│       │   │   ├── ChessPiece.java
│       │   │   ├── ChessBoard.java     ← Full rule engine
│       │   │   ├── Move.java
│       │   │   └── GameState.java
│       │   ├── repository/
│       │   │   └── GameRepository.java ← SharedPreferences
│       │   ├── viewmodel/
│       │   │   └── GameViewModel.java  ← MVVM ViewModel
│       │   ├── views/
│       │   │   └── ChessBoardView.java ← Custom Canvas board
│       │   └── ui/
│       │       ├── MainMenuActivity.java
│       │       ├── GameModeActivity.java
│       │       ├── PvPSetupActivity.java
│       │       ├── PvBotSetupActivity.java
│       │       ├── GameActivity.java
│       │       ├── SettingsActivity.java
│       │       └── AboutActivity.java
│       └── res/
│           ├── layout/        ← All XML layouts
│           ├── values/        ← strings, colors, themes
│           ├── values-night/  ← Dark theme overrides
│           └── drawable/      ← bg_card, ic_launcher_foreground
├── build.gradle
├── settings.gradle
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

---

## 🔧 Stockfish Integration Steps

### Step 1 — Download Stockfish Source

Go to: https://github.com/official-stockfish/Stockfish

Click **Code → Download ZIP** (or clone with git):
```bash
git clone https://github.com/official-stockfish/Stockfish.git
```

### Step 2 — Copy Source Files

Copy the **contents of `Stockfish/src/`** into:
```
app/src/main/cpp/stockfish/
```

After copying you should have files like:
```
app/src/main/cpp/stockfish/
├── bitboard.cpp / .h
├── evaluate.cpp / .h
├── movegen.cpp / .h
├── position.cpp / .h
├── search.cpp / .h
├── uci.cpp / .h
├── main.cpp          ← contains main() — renamed to stockfish_main via CMake
└── ... (all other .cpp and .h files)
```

> ⚠️ **Do NOT place nested sub-folders**. All `.cpp` and `.h` files go flat in `stockfish/`.

### Step 3 — Sync Gradle

In Android Studio: **File → Sync Project with Gradle Files**

### Step 4 — Build

`Build → Make Project` or press `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (macOS).

The CMakeLists.txt uses `file(GLOB_RECURSE ...)` to pick up all Stockfish `.cpp` files automatically.

### Step 5 — Verify via Logcat

After launching the app and tapping **Player vs Bot**, filter Logcat by tag `ChessEngine`:
```
I/ChessEngine: Engine initialised successfully
I/ChessEngine: → Engine: uci
I/ChessEngine: ← Engine: uciok
I/ChessEngine: → Engine: isready
I/ChessEngine: ← Engine: readyok
```

---

## ⚙️ Difficulty Levels

| Level        | UCI Depth | Skill Level (0-20) |
|--------------|-----------|-------------------|
| Easy         | 1         | 0                 |
| Intermediate | 5         | 8                 |
| Hard         | 10        | 14                |
| Grandmaster  | 15        | 18                |
| Magnus       | 20        | 20                |

---

## 🐛 Troubleshooting

### `UnsatisfiedLinkError: chess_engine`
- NDK not installed. Open **SDK Manager → SDK Tools → NDK (Side by side)** → check installed.
- Ensure `local.properties` has `ndk.dir=...` OR use the NDK path set in the SDK.

### `No uciok received`
- Stockfish `.cpp` files are missing or in the wrong directory.
- Check `app/src/main/cpp/stockfish/` contains `*.cpp` files.
- Check Logcat for CMake build errors referencing missing headers.

### CMake Error: `stockfish_main undefined`
- The `-Dmain=stockfish_main` flag renames Stockfish's `main()`.
- If your Stockfish version uses `Stockfish::main()` namespace, update the `target_compile_definitions` in `CMakeLists.txt`.

### `fatal error: 'immintrin.h'`
- Remove SSE/AVX intrinsic flags. The `CMakeLists.txt` already avoids these.
- Add `-DUSE_POPCNT=0` to `target_compile_definitions` if the error persists.

### App crashes on device (SIGABRT in engine thread)
- Increase the JVM stack: add `pthread_attr_setstacksize(&attr, 8*1024*1024);` in `engineThreadFunc`.
- Try commenting out `NNUE_EMBEDDING_OFF` in CMakeLists and providing the NNUE `.nnue` file in assets.

### Gradle: `CMake version 3.22.1 not found`
- Open **SDK Manager → SDK Tools → CMake** and install version 3.22.1.

---

## 🏗 Architecture

```
UI Activities ──────► GameViewModel  ──────► ChessBoard (pure Java model)
     ▲                      │
     │  LiveData             │ background thread
     │                       ▼
ChessBoardView          EngineBridge (Java singleton)
(Canvas)                     │  System.loadLibrary("chess_engine")
                              ▼
                         native-lib.cpp  (JNI)
                              │
                         engine_wrapper.cpp (POSIX pipes)
                              │
                         stockfish_main()  (Stockfish UCI loop)
```

---

## 📋 Requirements

- Android Studio Hedgehog (2023.1.1) or later
- NDK r25 or later
- CMake 3.22.1
- minSdk 28 (Android 9 Pie)
- Java 8 compatibility

---

## 📄 License

App code: MIT License.
Stockfish engine: GNU GPL v3 — https://github.com/official-stockfish/Stockfish/blob/master/Copying.txt
