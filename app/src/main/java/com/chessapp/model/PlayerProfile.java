package com.chessapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a player profile.
 */
@Entity(tableName = "profiles")
public class PlayerProfile {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String displayName;

    @NonNull
    private String avatarEmoji = "♟";

    private long createdAt;

    private int totalWins = 0;
    private int totalLosses = 0;
    private int totalDraws = 0;
    private int totalGames = 0;

    public PlayerProfile(@NonNull String displayName, @NonNull String avatarEmoji) {
        this.displayName = displayName;
        this.avatarEmoji = avatarEmoji;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getDisplayName() { return displayName; }
    public void setDisplayName(@NonNull String displayName) { this.displayName = displayName; }

    @NonNull
    public String getAvatarEmoji() { return avatarEmoji; }
    public void setAvatarEmoji(@NonNull String avatarEmoji) { this.avatarEmoji = avatarEmoji; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }

    public int getTotalLosses() { return totalLosses; }
    public void setTotalLosses(int totalLosses) { this.totalLosses = totalLosses; }

    public int getTotalDraws() { return totalDraws; }
    public void setTotalDraws(int totalDraws) { this.totalDraws = totalDraws; }

    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }
}
