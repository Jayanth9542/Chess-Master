#pragma once
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
// ═══════════════════════════════════════════════════════════════════════
//  engine_wrapper.h  –  UCI bridge between JNI and Stockfish
//  Uses POSIX pipes + dup2 to redirect Stockfish stdin/stdout.
// ═══════════════════════════════════════════════════════════════════════
#include <string>
#include <pthread.h>
#include <unistd.h>
#include <ctime>
#include <atomic>
#include <android/log.h>

#define ENGINE_LOG_TAG "ChessEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  ENGINE_LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  ENGINE_LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, ENGINE_LOG_TAG, __VA_ARGS__)

namespace chess {

class EngineWrapper {
public:
    // Singleton access
    static EngineWrapper& getInstance();

    // Lifecycle
    bool  initialize();
    void  shutdown();

    // UCI I/O
    bool        sendCommand(const std::string& cmd);
    std::string readLine(int timeoutMs = 3000);

    // High-level helpers
    bool        waitForToken(const std::string& token, int timeoutMs = 8000);
    std::string getBestMove(const std::string& fenPosition,
                            int depth, int skillLevel,
                            int timeLimitMs = 15000);
    void        stopSearch();
    bool        isReady();

    bool isRunning()     const { return running_; }
    bool isInitialized() const { return initialized_; }

private:
    EngineWrapper();
    ~EngineWrapper();
    EngineWrapper(const EngineWrapper&)            = delete;
    EngineWrapper& operator=(const EngineWrapper&) = delete;

    // Pipes:
    //   toEngine_[0]   = read end  (Stockfish reads its stdin from here)
    //   toEngine_[1]   = write end (JNI writes UCI commands here)
    //   fromEngine_[0] = read end  (JNI reads Stockfish output here)
    //   fromEngine_[1] = write end (Stockfish writes its stdout here)
    int toEngine_[2];
    int fromEngine_[2];

    pthread_t      engineThread_;
    pthread_mutex_t writeMutex_;
    std::atomic<bool> initialized_;
    std::atomic<bool> running_;

    static void* engineThreadFunc(void* arg);

    void pthread_timedjoin_np(pthread_t thread, void *pVoid, timespec *pTimespec);
};

} // namespace chess
