package com.chessapp.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.chessapp.model.PlayerProfile;
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
public final class ProfileDao_Impl implements ProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlayerProfile> __insertionAdapterOfPlayerProfile;

  private final EntityDeletionOrUpdateAdapter<PlayerProfile> __deletionAdapterOfPlayerProfile;

  private final EntityDeletionOrUpdateAdapter<PlayerProfile> __updateAdapterOfPlayerProfile;

  private final SharedSQLiteStatement __preparedStmtOfIncrementWins;

  private final SharedSQLiteStatement __preparedStmtOfIncrementLosses;

  private final SharedSQLiteStatement __preparedStmtOfIncrementDraws;

  public ProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlayerProfile = new EntityInsertionAdapter<PlayerProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `profiles` (`id`,`displayName`,`avatarEmoji`,`createdAt`,`totalWins`,`totalLosses`,`totalDraws`,`totalGames`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final PlayerProfile entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDisplayName());
        }
        if (entity.getAvatarEmoji() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAvatarEmoji());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getTotalWins());
        statement.bindLong(6, entity.getTotalLosses());
        statement.bindLong(7, entity.getTotalDraws());
        statement.bindLong(8, entity.getTotalGames());
      }
    };
    this.__deletionAdapterOfPlayerProfile = new EntityDeletionOrUpdateAdapter<PlayerProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `profiles` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final PlayerProfile entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfPlayerProfile = new EntityDeletionOrUpdateAdapter<PlayerProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `profiles` SET `id` = ?,`displayName` = ?,`avatarEmoji` = ?,`createdAt` = ?,`totalWins` = ?,`totalLosses` = ?,`totalDraws` = ?,`totalGames` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final PlayerProfile entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDisplayName());
        }
        if (entity.getAvatarEmoji() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAvatarEmoji());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getTotalWins());
        statement.bindLong(6, entity.getTotalLosses());
        statement.bindLong(7, entity.getTotalDraws());
        statement.bindLong(8, entity.getTotalGames());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfIncrementWins = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE profiles SET totalWins = totalWins + 1, totalGames = totalGames + 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementLosses = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE profiles SET totalLosses = totalLosses + 1, totalGames = totalGames + 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementDraws = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE profiles SET totalDraws = totalDraws + 1, totalGames = totalGames + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insertProfile(final PlayerProfile profile) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfPlayerProfile.insertAndReturnId(profile);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteProfile(final PlayerProfile profile) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfPlayerProfile.handle(profile);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateProfile(final PlayerProfile profile) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfPlayerProfile.handle(profile);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void incrementWins(final long profileId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementWins.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, profileId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfIncrementWins.release(_stmt);
    }
  }

  @Override
  public void incrementLosses(final long profileId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementLosses.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, profileId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfIncrementLosses.release(_stmt);
    }
  }

  @Override
  public void incrementDraws(final long profileId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementDraws.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, profileId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfIncrementDraws.release(_stmt);
    }
  }

  @Override
  public LiveData<List<PlayerProfile>> getAllProfiles() {
    final String _sql = "SELECT * FROM profiles ORDER BY displayName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"profiles"}, false, new Callable<List<PlayerProfile>>() {
      @Override
      @Nullable
      public List<PlayerProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfAvatarEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarEmoji");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTotalWins = CursorUtil.getColumnIndexOrThrow(_cursor, "totalWins");
          final int _cursorIndexOfTotalLosses = CursorUtil.getColumnIndexOrThrow(_cursor, "totalLosses");
          final int _cursorIndexOfTotalDraws = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDraws");
          final int _cursorIndexOfTotalGames = CursorUtil.getColumnIndexOrThrow(_cursor, "totalGames");
          final List<PlayerProfile> _result = new ArrayList<PlayerProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlayerProfile _item;
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpAvatarEmoji;
            if (_cursor.isNull(_cursorIndexOfAvatarEmoji)) {
              _tmpAvatarEmoji = null;
            } else {
              _tmpAvatarEmoji = _cursor.getString(_cursorIndexOfAvatarEmoji);
            }
            _item = new PlayerProfile(_tmpDisplayName,_tmpAvatarEmoji);
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            _item.setId(_tmpId);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item.setCreatedAt(_tmpCreatedAt);
            final int _tmpTotalWins;
            _tmpTotalWins = _cursor.getInt(_cursorIndexOfTotalWins);
            _item.setTotalWins(_tmpTotalWins);
            final int _tmpTotalLosses;
            _tmpTotalLosses = _cursor.getInt(_cursorIndexOfTotalLosses);
            _item.setTotalLosses(_tmpTotalLosses);
            final int _tmpTotalDraws;
            _tmpTotalDraws = _cursor.getInt(_cursorIndexOfTotalDraws);
            _item.setTotalDraws(_tmpTotalDraws);
            final int _tmpTotalGames;
            _tmpTotalGames = _cursor.getInt(_cursorIndexOfTotalGames);
            _item.setTotalGames(_tmpTotalGames);
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
  public PlayerProfile getProfileById(final long id) {
    final String _sql = "SELECT * FROM profiles WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
      final int _cursorIndexOfAvatarEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarEmoji");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final int _cursorIndexOfTotalWins = CursorUtil.getColumnIndexOrThrow(_cursor, "totalWins");
      final int _cursorIndexOfTotalLosses = CursorUtil.getColumnIndexOrThrow(_cursor, "totalLosses");
      final int _cursorIndexOfTotalDraws = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDraws");
      final int _cursorIndexOfTotalGames = CursorUtil.getColumnIndexOrThrow(_cursor, "totalGames");
      final PlayerProfile _result;
      if (_cursor.moveToFirst()) {
        final String _tmpDisplayName;
        if (_cursor.isNull(_cursorIndexOfDisplayName)) {
          _tmpDisplayName = null;
        } else {
          _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
        }
        final String _tmpAvatarEmoji;
        if (_cursor.isNull(_cursorIndexOfAvatarEmoji)) {
          _tmpAvatarEmoji = null;
        } else {
          _tmpAvatarEmoji = _cursor.getString(_cursorIndexOfAvatarEmoji);
        }
        _result = new PlayerProfile(_tmpDisplayName,_tmpAvatarEmoji);
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        _result.setId(_tmpId);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _result.setCreatedAt(_tmpCreatedAt);
        final int _tmpTotalWins;
        _tmpTotalWins = _cursor.getInt(_cursorIndexOfTotalWins);
        _result.setTotalWins(_tmpTotalWins);
        final int _tmpTotalLosses;
        _tmpTotalLosses = _cursor.getInt(_cursorIndexOfTotalLosses);
        _result.setTotalLosses(_tmpTotalLosses);
        final int _tmpTotalDraws;
        _tmpTotalDraws = _cursor.getInt(_cursorIndexOfTotalDraws);
        _result.setTotalDraws(_tmpTotalDraws);
        final int _tmpTotalGames;
        _tmpTotalGames = _cursor.getInt(_cursorIndexOfTotalGames);
        _result.setTotalGames(_tmpTotalGames);
      } else {
        _result = null;
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
