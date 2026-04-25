cmake_minimum_required(VERSION 3.22.1)
project("chess_engine" CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ─────────────────────────────────────────────────────────────────────────────
# Collect ALL Stockfish source files recursively.
# Place Stockfish sources in: app/src/main/cpp/stockfish/
# Supports Stockfish 14, 15, 16, 17 layouts (all have .cpp in src/ subfolders).
# ─────────────────────────────────────────────────────────────────────────────
file(GLOB_RECURSE STOCKFISH_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/stockfish/*.cpp"
)

if(NOT STOCKFISH_SOURCES)
    message(WARNING
        "⚠  No Stockfish .cpp files found in stockfish/. "
        "The JNI bridge will compile but initEngine() will fail at runtime. "
        "See integration steps in the README.")
endif()

# ─────────────────────────────────────────────────────────────────────────────
# Build the shared library
# ─────────────────────────────────────────────────────────────────────────────
add_library(
    chess_engine
    SHARED
    native-lib.cpp
    engine_wrapper.cpp
    ${STOCKFISH_SOURCES}
)

# ─────────────────────────────────────────────────────────────────────────────
# KEY: rename Stockfish's main() to stockfish_main() via preprocessor.
# This lets us call it from a background thread in engine_wrapper.cpp.
# ─────────────────────────────────────────────────────────────────────────────
target_compile_definitions(chess_engine PRIVATE
    main=stockfish_main
    NDEBUG
    IS_64BIT
    USE_PTHREADS
    NNUE_EMBEDDING_OFF
)

target_compile_options(chess_engine PRIVATE
    -O2
    -fPIC
    -fno-rtti
    -Wall
    -Wno-unused-variable
    -Wno-unused-function
    -Wno-missing-braces
)

# Stockfish headers live in the stockfish/ directory itself
target_include_directories(chess_engine PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish
)

# ─────────────────────────────────────────────────────────────────────────────
# Link Android system libraries
# ─────────────────────────────────────────────────────────────────────────────
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(chess_engine
    ${log-lib}
    ${android-lib}
)
