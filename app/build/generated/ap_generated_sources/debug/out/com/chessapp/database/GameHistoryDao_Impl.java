package com.chessapp.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.chessapp.model.GameRecord;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GameHistoryDao_Impl implements GameHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GameRecord> __insertionAdapterOfGameRecord;

  public GameHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGameRecord = new EntityInsertionAdapter<GameRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `game_history` (`id`,`profileId`,`opponentName`,`gameMode`,`difficulty`,`result`,`playerColor`,`totalMoves`,`durationMs`,`playedAt`,`openingFEN`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final GameRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getProfileId());
        if (entity.getOpponentName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getOpponentName());
        }
        if (entity.getGameMode() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getGameMode());
        }
        if (entity.getDifficulty() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDifficulty());
        }
        if (entity.getResult() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getResult());
        }
        if (entity.getPlayerColor() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPlayerColor());
        }
        statement.bindLong(8, entity.getTotalMoves());
        statement.bindLong(9, entity.getDurationMs());
        statement.bindLong(10, entity.getPlayedAt());
        if (entity.getOpeningFEN() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getOpeningFEN());
        }
      }
    };
  }

  @Override
  public long insertGameRecord(final GameRecord record) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfGameRecord.insertAndReturnId(record);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<GameRecord>> getGameHistoryForProfile(final long profileId) {
    final String _sql = "SELECT * FROM game_history WHERE profileId = ? ORDER BY playedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, profileId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"game_history"}, false, new Callable<List<GameRecord>>() {
      @Override
      @Nullable
      public List<GameRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProfileId = CursorUtil.getColumnIndexOrThrow(_cursor, "profileId");
          final int _cursorIndexOfOpponentName = CursorUtil.getColumnIndexOrThrow(_cursor, "opponentName");
          final int _cursorIndexOfGameMode = CursorUtil.getColumnIndexOrThrow(_cursor, "gameMode");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfPlayerColor = CursorUtil.getColumnIndexOrThrow(_cursor, "playerColor");
          final int _cursorIndexOfTotalMoves = CursorUtil.getColumnIndexOrThrow(_cursor, "totalMoves");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfPlayedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "playedAt");
          final int _cursorIndexOfOpeningFEN = CursorUtil.getColumnIndexOrThrow(_cursor, "openingFEN");
          final List<GameRecord> _result = new ArrayList<GameRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameRecord _item;
            final long _tmpProfileId;
            _tmpProfileId = _cursor.getLong(_cursorIndexOfProfileId);
            final String _tmpOpponentName;
            if (_cursor.isNull(_cursorIndexOfOpponentName)) {
              _tmpOpponentName = null;
            } else {
              _tmpOpponentName = _cursor.getString(_cursorIndexOfOpponentName);
            }
            final String _tmpGameMode;
            if (_cursor.isNull(_cursorIndexOfGameMode)) {
              _tmpGameMode = null;
            } else {
              _tmpGameMode = _cursor.getString(_cursorIndexOfGameMode);
            }
            final String _tmpResult;
            if (_cursor.isNull(_cursorIndexOfResult)) {
              _tmpResult = null;
            } else {
              _tmpResult = _cursor.getString(_cursorIndexOfResult);
            }
            final String _tmpPlayerColor;
            if (_cursor.isNull(_cursorIndexOfPlayerColor)) {
              _tmpPlayerColor = null;
            } else {
              _tmpPlayerColor = _cursor.getString(_cursorIndexOfPlayerColor);
            }
            final int _tmpTotalMoves;
            _tmpTotalMoves = _cursor.getInt(_cursorIndexOfTotalMoves);
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpOpeningFEN;
            if (_cursor.isNull(_cursorIndexOfOpeningFEN)) {
              _tmpOpeningFEN = null;
            } else {
              _tmpOpeningFEN = _cursor.getString(_cursorIndexOfOpeningFEN);
            }
            _item = new GameRecord(_tmpProfileId,_tmpOpponentName,_tmpGameMode,_tmpResult,_tmpPlayerColor,_tmpTotalMoves,_tmpDurationMs,_tmpOpeningFEN);
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            _item.setId(_tmpId);
            final String _tmpDifficulty;
            if (_cursor.isNull(_cursorIndexOfDifficulty)) {
              _tmpDifficulty = null;
            } else {
              _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            }
            _item.setDifficulty(_tmpDifficulty);
            final long _tmpPlayedAt;
            _tmpPlayedAt = _cursor.getLong(_cursorIndexOfPlayedAt);
            _item.setPlayedAt(_tmpPlayedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<GameRecord>> getRecentGames(final long profileId, final int limit) {
    final String _sql = "SELECT * FROM game_history WHERE profileId = ? ORDER BY playedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, profileId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return __db.getInvalidationTracker().createLiveData(new String[] {"game_history"}, false, new Callable<List<GameRecord>>() {
      @Override
      @Nullable
      public List<GameRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProfileId = CursorUtil.getColumnIndexOrThrow(_cursor, "profileId");
          final int _cursorIndexOfOpponentName = CursorUtil.getColumnIndexOrThrow(_cursor, "opponentName");
          final int _cursorIndexOfGameMode = CursorUtil.getColumnIndexOrThrow(_cursor, "gameMode");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfPlayerColor = CursorUtil.getColumnIndexOrThrow(_cursor, "playerColor");
          final int _cursorIndexOfTotalMoves = CursorUtil.getColumnIndexOrThrow(_cursor, "totalMoves");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfPlayedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "playedAt");
          final int _cursorIndexOfOpeningFEN = CursorUtil.getColumnIndexOrThrow(_cursor, "openingFEN");
          final List<GameRecord> _result = new ArrayList<GameRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameRecord _item;
            final long _tmpProfileId;
            _tmpProfileId = _cursor.getLong(_cursorIndexOfProfileId);
            final String _tmpOpponentName;
            if (_cursor.isNull(_cursorIndexOfOpponentName)) {
              _tmpOpponentName = null;
            } else {
              _tmpOpponentName = _cursor.getString(_cursorIndexOfOpponentName);
            }
            final String _tmpGameMode;
            if (_cursor.isNull(_cursorIndexOfGameMode)) {
              _tmpGameMode = null;
            } else {
              _tmpGameMode = _cursor.getString(_cursorIndexOfGameMode);
            }
            final String _tmpResult;
            if (_cursor.isNull(_cursorIndexOfResult)) {
              _tmpResult = null;
            } else {
              _tmpResult = _cursor.getString(_cursorIndexOfResult);
            }
            final String _tmpPlayerColor;
            if (_cursor.isNull(_cursorIndexOfPlayerColor)) {
              _tmpPlayerColor = null;
            } else {
              _tmpPlayerColor = _cursor.getString(_cursorIndexOfPlayerColor);
            }
            final int _tmpTotalMoves;
            _tmpTotalMoves = _cursor.getInt(_cursorIndexOfTotalMoves);
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpOpeningFEN;
            if (_cursor.isNull(_cursorIndexOfOpeningFEN)) {
              _tmpOpeningFEN = null;
            } else {
              _tmpOpeningFEN = _cursor.getString(_cursorIndexOfOpeningFEN);
            }
            _item = new GameRecord(_tmpProfileId,_tmpOpponentName,_tmpGameMode,_tmpResult,_tmpPlayerColor,_tmpTotalMoves,_tmpDurationMs,_tmpOpeningFEN);
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            _item.setId(_tmpId);
            final String _tmpDifficulty;
            if (_cursor.isNull(_cursorIndexOfDifficulty)) {
              _tmpDifficulty = null;
            } else {
              _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            }
            _item.setDifficulty(_tmpDifficulty);
            final long _tmpPlayedAt;
            _tmpPlayedAt = _cursor.getLong(_cursorIndexOfPlayedAt);
            _item.setPlayedAt(_tmpPlayedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public int getTotalGamesPlayed(final long profileId) {
    final String _sql = "SELECT COUNT(*) FROM game_history WHERE profileId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, profileId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
