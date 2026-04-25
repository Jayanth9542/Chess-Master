// ═══════════════════════════════════════════════════════════════════════
//  native-lib.cpp  –  JNI layer between Java EngineBridge and C++ wrapper
//  Java class: com.chessapp.engine.EngineBridge
// ═══════════════════════════════════════════════════════════════════════
#include <jni.h>
#include <string>
#include <stdexcept>
#include <android/log.h>
#include "engine_wrapper.h"

#define JNI_TAG "ChessJNI"
#define JNI_LOGI(...) __android_log_print(ANDROID_LOG_INFO,  JNI_TAG, __VA_ARGS__)
#define JNI_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, JNI_TAG, __VA_ARGS__)

// Helper: safely convert jstring → std::string
static std::string jstr(JNIEnv* env, jstring js) {
    if (!js) return "";
    const char* c = env->GetStringUTFChars(js, nullptr);
    if (!c) return "";
    std::string s(c);
    env->ReleaseStringUTFChars(js, c);
    return s;
}

extern "C" {

// ────────────────────────────────────────────────────────────────────────────
//  bool EngineBridge.initEngine()
// ────────────────────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_com_chessapp_engine_EngineBridge_initEngine(JNIEnv* env, jobject /*obj*/) {
    JNI_LOGI("initEngine called");
    try {
        return chess::EngineWrapper::getInstance().initialize()
               ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        JNI_LOGE("initEngine exception: %s", e.what());
    } catch (...) {
        JNI_LOGE("initEngine unknown exception");
    }
    return JNI_FALSE;
}

// ────────────────────────────────────────────────────────────────────────────
//  void EngineBridge.sendCommand(String cmd)
// ────────────────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_chessapp_engine_EngineBridge_sendCommand(JNIEnv* env, jobject /*obj*/,
                                                   jstring jCmd) {
    try {
        chess::EngineWrapper::getInstance().sendCommand(jstr(env, jCmd));
    } catch (...) {
        JNI_LOGE("sendCommand exception");
    }
}

// ────────────────────────────────────────────────────────────────────────────
//  String EngineBridge.getBestMove(String fen, int depth, int skillLevel)
// ────────────────────────────────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_chessapp_engine_EngineBridge_getBestMove(JNIEnv* env, jobject /*obj*/,
                                                   jstring jFen,
                                                   jint    depth,
                                                   jint    skillLevel) {
    std::string move;
    try {
        move = chess::EngineWrapper::getInstance()
                   .getBestMove(jstr(env, jFen), (int)depth, (int)skillLevel);
    } catch (...) {
        JNI_LOGE("getBestMove exception");
    }
    return env->NewStringUTF(move.c_str());
}

// ────────────────────────────────────────────────────────────────────────────
//  void EngineBridge.stopEngine()
// ────────────────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_chessapp_engine_EngineBridge_stopEngine(JNIEnv* env, jobject /*obj*/) {
    try {
        chess::EngineWrapper::getInstance().shutdown();
    } catch (...) {
        JNI_LOGE("stopEngine exception");
    }
}

// ────────────────────────────────────────────────────────────────────────────
//  boolean EngineBridge.isEngineReady()
// ────────────────────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_com_chessapp_engine_EngineBridge_isEngineReady(JNIEnv* env, jobject /*obj*/) {
    try {
        return chess::EngineWrapper::getInstance().isReady()
               ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

// ────────────────────────────────────────────────────────────────────────────
//  String EngineBridge.getEngineVersion()
// ────────────────────────────────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_chessapp_engine_EngineBridge_getEngineVersion(JNIEnv* env, jobject /*obj*/) {
    // We parse the "id name Stockfish X" line during uci handshake;
    // for now return a static placeholder.
    return env->NewStringUTF("Stockfish (Android NDK build)");
}

} // extern "C"
