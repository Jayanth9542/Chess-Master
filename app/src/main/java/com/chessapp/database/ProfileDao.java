package com.chessapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.chessapp.model.PlayerProfile;

import java.util.List;

/**
 * DAO for the profiles table.
 */
@Dao
public interface ProfileDao {

    @Insert
    long insertProfile(PlayerProfile profile);

    @Update
    void updateProfile(PlayerProfile profile);

    @Delete
    void deleteProfile(PlayerProfile profile);

    @Query("SELECT * FROM profiles ORDER BY displayName ASC")
    LiveData<List<PlayerProfile>> getAllProfiles();

    @Query("SELECT * FROM profiles WHERE id = :id")
    PlayerProfile getProfileById(long id);

    @Query("UPDATE profiles SET totalWins = totalWins + 1, totalGames = totalGames + 1 WHERE id = :profileId")
    void incrementWins(long profileId);

    @Query("UPDATE profiles SET totalLosses = totalLosses + 1, totalGames = totalGames + 1 WHERE id = :profileId")
    void incrementLosses(long profileId);

    @Query("UPDATE profiles SET totalDraws = totalDraws + 1, totalGames = totalGames + 1 WHERE id = :profileId")
    void incrementDraws(long profileId);
}
