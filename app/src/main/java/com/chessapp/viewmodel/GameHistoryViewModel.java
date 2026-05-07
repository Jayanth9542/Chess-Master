package com.chessapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.chessapp.model.GameRecord;
import com.chessapp.model.PlayerProfile;
import com.chessapp.repository.GameRepository;

import java.util.ArrayList;
import java.util.List;

public class GameHistoryViewModel extends AndroidViewModel {

    private final GameRepository repository;
    private final MediatorLiveData<List<GameRecord>> filteredGamesLD = new MediatorLiveData<>();
    private final MutableLiveData<String> filterLD = new MutableLiveData<>("ALL");
    private LiveData<List<GameRecord>> allGamesLD;
    private LiveData<PlayerProfile> profileLD;
    private final MutableLiveData<PlayerProfile> profileSyncLD = new MutableLiveData<>();

    public GameHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new GameRepository(application);
    }

    public void init(long profileId) {
        allGamesLD = repository.getGameHistoryForProfile(profileId);
        
        // Profile LiveData
        new Thread(() -> {
            PlayerProfile profile = repository.getProfileByIdSync(profileId);
            profileSyncLD.postValue(profile);
        }).start();

        filteredGamesLD.addSource(allGamesLD, games -> applyFilter());
        filteredGamesLD.addSource(filterLD, filter -> applyFilter());
    }

    private void applyFilter() {
        List<GameRecord> allGames = allGamesLD.getValue();
        String filter = filterLD.getValue();

        if (allGames == null) {
            filteredGamesLD.setValue(new ArrayList<>());
            return;
        }

        if (filter == null || filter.equals("ALL")) {
            filteredGamesLD.setValue(allGames);
        } else {
            List<GameRecord> filtered = new ArrayList<>();
            for (GameRecord record : allGames) {
                if (record.getGameMode().equals(filter)) {
                    filtered.add(record);
                }
            }
            filteredGamesLD.setValue(filtered);
        }
    }

    public LiveData<List<GameRecord>> getFilteredGamesLD() {
        return filteredGamesLD;
    }

    public LiveData<PlayerProfile> getProfileLD() {
        return profileSyncLD;
    }

    public void setFilter(String filter) {
        filterLD.setValue(filter);
    }
}
