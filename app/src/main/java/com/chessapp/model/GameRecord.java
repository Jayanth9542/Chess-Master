package com.chessapp.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * Room entity representing a completed chess game.
 */
@Entity(tableName = "game_history",
        foreignKeys = @ForeignKey(entity = PlayerProfile.class,
                                 parentColumns = "id",
                                 childColumns = "profileId",
                                 onDelete = CASCADE),
        indices = {@Index("profileId")})
public class GameRecord {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long profileId;

    @NonNull
    private String opponentName;

    @NonNull
    private String gameMode; // "PVP" or "PVB"

    @Nullable
    private String difficulty;

    @NonNull
    private String result; // "WIN", "LOSS", or "DRAW"

    @NonNull
    private String playerColor; // "WHITE" or "BLACK"

    private int totalMoves;

    private long durationMs;

    private long playedAt;

    @Nullable
    private String openingFEN;

    public GameRecord(long profileId, @NonNull String opponentName, @NonNull String gameMode,
                      @NonNull String result, @NonNull String playerColor, int totalMoves,
                      long durationMs, @Nullable String openingFEN) {
        this.profileId = profileId;
        this.opponentName = opponentName;
        this.gameMode = gameMode;
        this.result = result;
        this.playerColor = playerColor;
        this.totalMoves = totalMoves;
        this.durationMs = durationMs;
        this.playedAt = System.currentTimeMillis();
        this.openingFEN = openingFEN;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProfileId() { return profileId; }
    public void setProfileId(long profileId) { this.profileId = profileId; }

    @NonNull
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(@NonNull String opponentName) { this.opponentName = opponentName; }

    @NonNull
    public String getGameMode() { return gameMode; }
    public void setGameMode(@NonNull String gameMode) { this.gameMode = gameMode; }

    @Nullable
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(@Nullable String difficulty) { this.difficulty = difficulty; }

    @NonNull
    public String getResult() { return result; }
    public void setResult(@NonNull String result) { this.result = result; }

    @NonNull
    public String getPlayerColor() { return playerColor; }
    public void setPlayerColor(@NonNull String playerColor) { this.playerColor = playerColor; }

    public int getTotalMoves() { return totalMoves; }
    public void setTotalMoves(int totalMoves) { this.totalMoves = totalMoves; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getPlayedAt() { return playedAt; }
    public void setPlayedAt(long playedAt) { this.playedAt = playedAt; }

    @Nullable
    public String getOpeningFEN() { return openingFEN; }
    public void setOpeningFEN(@Nullable String openingFEN) { this.openingFEN = openingFEN; }
}
