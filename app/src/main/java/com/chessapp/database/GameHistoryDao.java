package com.chessapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.chessapp.model.GameRecord;

import java.util.List;

/**
 * DAO for the game_history table.
 */
@Dao
public interface GameHistoryDao {

    @Insert
    long insertGameRecord(GameRecord record);

    @Query("SELECT * FROM game_history WHERE profileId = :profileId ORDER BY playedAt DESC")
    LiveData<List<GameRecord>> getGameHistoryForProfile(long profileId);

    @Query("SELECT * FROM game_history WHERE profileId = :profileId ORDER BY playedAt DESC LIMIT :limit")
    LiveData<List<GameRecord>> getRecentGames(long profileId, int limit);

    @Query("SELECT COUNT(*) FROM game_history WHERE profileId = :profileId")
    int getTotalGamesPlayed(long profileId);
}
