package com.chessapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.chessapp.model.GameRecord;
import com.chessapp.model.PlayerProfile;

/**
 * Room database for the Chess application.
 */
@Database(entities = {PlayerProfile.class, GameRecord.class}, version = 1, exportSchema = false)
public abstract class ChessDatabase extends RoomDatabase {

    private static volatile ChessDatabase INSTANCE;

    public abstract ProfileDao profileDao();
    public abstract GameHistoryDao gameHistoryDao();

    public static ChessDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ChessDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ChessDatabase.class, "chess_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
