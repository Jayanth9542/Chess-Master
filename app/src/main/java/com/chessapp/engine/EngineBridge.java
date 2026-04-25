package com.chessapp.engine;

import android.util.Log;

/**
 * Java-side singleton that loads the native chess_engine library
 * and exposes UCI operations via JNI.
 *
 * Difficulty → (depth, skillLevel 0-20) mapping (5-Tier System):
 *   BEGINNER     → depth 2,  skill 2
 *   INTERMEDIATE → depth 5,  skill 8
 *   EXPERT       → depth 12, skill 17
 *   GRANDMASTER  → depth 18, skill 19
 *   MAGNUS       → depth 24, skill 20
 */
public class EngineBridge {

    private static final String TAG = "EngineBridge";

    static {
        try {
            System.loadLibrary("chess_engine");
            Log.i(TAG, "chess_engine library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load chess_engine: " + e.getMessage()
                     + " — JNI features disabled.");
        }
    }

    private static volatile EngineBridge INSTANCE;

    public static EngineBridge getInstance() {
        if (INSTANCE == null) {
            synchronized (EngineBridge.class) {
                if (INSTANCE == null) INSTANCE = new EngineBridge();
            }
        }
        return INSTANCE;
    }

    private EngineBridge() {}

    public native boolean initEngine();
    public native void sendCommand(String cmd);
    public native String getBestMove(String fenPosition, int depth, int skillLevel);
    public native void stopEngine();
    public native boolean isEngineReady();
    public native String getEngineVersion();

    /**
     * Maps the 6-tier difficulty string to Stockfish parameters.
     * @param difficulty one of BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, GRANDMASTER, MAGNUS
     * @return int[]{depth, skillLevel}
     */
    public static int[] getDifficultyParams(String difficulty) {
        if (difficulty == null) return new int[]{4, 7};
        switch (difficulty.toUpperCase()) {
            case "BEGINNER":     return new int[]{ 2,  2};
            case "INTERMEDIATE": return new int[]{ 5,  8};
            case "EXPERT":       return new int[]{12, 17};
            case "GRANDMASTER":  return new int[]{18, 19};
            case "MAGNUS":       return new int[]{24, 20};
            default:             return new int[]{ 5,  8};
        }
    }
}
