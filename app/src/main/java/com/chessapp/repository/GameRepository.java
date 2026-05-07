package com.chessapp.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.chessapp.database.ChessDatabase;
import com.chessapp.database.GameHistoryDao;
import com.chessapp.database.ProfileDao;
import com.chessapp.model.GameRecord;
import com.chessapp.model.PlayerProfile;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth for persisted user preferences and database records.
 */
public class GameRepository {

    private static final String PREFS_NAME          = "chess_prefs";
    private static final String KEY_SOUND           = "sound_enabled";
    private static final String KEY_ANIMATIONS      = "animations_enabled";
    private static final String KEY_THEME           = "theme";
    private static final String KEY_P1_NAME         = "last_p1_name";
    private static final String KEY_P2_NAME         = "last_p2_name";
    private static final String KEY_DIFFICULTY      = "last_difficulty";
    private static final String KEY_ACTIVE_PROFILE  = "active_profile_id";

    private static final String DEFAULT_P1          = "Player 1";
    private static final String DEFAULT_P2          = "Player 2";
    private static final String DEFAULT_DIFFICULTY  = "INTERMEDIATE";
    private static final String DEFAULT_THEME       = "light";

    private final SharedPreferences prefs;
    private final ChessDatabase database;
    private final ProfileDao profileDao;
    private final GameHistoryDao gameHistoryDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GameRepository(Application app) {
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        database = ChessDatabase.getDatabase(app);
        profileDao = database.profileDao();
        gameHistoryDao = database.gameHistoryDao();
    }

    // ── Active Profile Session ───────────────────────────────────────
    public long getActiveProfileId() {
        return prefs.getLong(KEY_ACTIVE_PROFILE, -1L);
    }

    public void setActiveProfileId(long id) {
        prefs.edit().putLong(KEY_ACTIVE_PROFILE, id).apply();
    }

    // ── Profile Operations ───────────────────────────────────────────
    public LiveData<List<PlayerProfile>> getAllProfiles() {
        return profileDao.getAllProfiles();
    }

    public void insertProfile(PlayerProfile profile, OnProfileInsertedListener listener) {
        executor.execute(() -> {
            long id = profileDao.insertProfile(profile);
            if (listener != null) listener.onInserted(id);
        });
    }

    public interface OnProfileInsertedListener {
        void onInserted(long id);
    }

    public void updateProfile(PlayerProfile profile) {
        executor.execute(() -> profileDao.updateProfile(profile));
    }

    public void deleteProfile(PlayerProfile profile) {
        executor.execute(() -> profileDao.deleteProfile(profile));
    }

    public PlayerProfile getProfileByIdSync(long id) {
        return profileDao.getProfileById(id);
    }

    public void incrementProfileStats(long profileId, String result) {
        executor.execute(() -> {
            switch (result) {
                case "WIN":  profileDao.incrementWins(profileId); break;
                case "LOSS": profileDao.incrementLosses(profileId); break;
                case "DRAW": profileDao.incrementDraws(profileId); break;
            }
        });
    }

    // ── Game Record Operations ───────────────────────────────────────
    public void insertGameRecord(GameRecord record) {
        executor.execute(() -> gameHistoryDao.insertGameRecord(record));
    }

    public LiveData<List<GameRecord>> getGameHistoryForProfile(long profileId) {
        return gameHistoryDao.getGameHistoryForProfile(profileId);
    }

    // ── Sound ────────────────────────────────────────────────────────
    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    // ── Animations ───────────────────────────────────────────────────
    public boolean isAnimationsEnabled() {
        return prefs.getBoolean(KEY_ANIMATIONS, true);
    }
    public void setAnimationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ANIMATIONS, enabled).apply();
    }

    // ── Theme ────────────────────────────────────────────────────────
    public String getTheme() {
        return prefs.getString(KEY_THEME, DEFAULT_THEME);
    }
    public void setTheme(String theme) {
        if (theme == null) theme = DEFAULT_THEME;
        prefs.edit().putString(KEY_THEME, theme).apply();
    }
    public boolean isDarkTheme() {
        return "dark".equalsIgnoreCase(getTheme());
    }

    // ── Player names ─────────────────────────────────────────────────
    public String getLastPlayer1Name() {
        return prefs.getString(KEY_P1_NAME, DEFAULT_P1);
    }
    public void savePlayer1Name(String name) {
        if (name == null || name.trim().isEmpty()) name = DEFAULT_P1;
        prefs.edit().putString(KEY_P1_NAME, name.trim()).apply();
    }

    public String getLastPlayer2Name() {
        return prefs.getString(KEY_P2_NAME, DEFAULT_P2);
    }
    public void savePlayer2Name(String name) {
        if (name == null || name.trim().isEmpty()) name = DEFAULT_P2;
        prefs.edit().putString(KEY_P2_NAME, name.trim()).apply();
    }

    // ── Difficulty ───────────────────────────────────────────────────
    public String getLastDifficulty() {
        return prefs.getString(KEY_DIFFICULTY, DEFAULT_DIFFICULTY);
    }
    public void saveLastDifficulty(String difficulty) {
        if (difficulty == null || difficulty.trim().isEmpty()) difficulty = DEFAULT_DIFFICULTY;
        prefs.edit().putString(KEY_DIFFICULTY, difficulty.trim().toUpperCase()).apply();
    }

    // ── Reset ────────────────────────────────────────────────────────
    /** Clears all saved preferences. Call only for dev/debug resets. */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
