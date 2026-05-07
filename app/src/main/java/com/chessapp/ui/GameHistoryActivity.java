package com.chessapp.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chessapp.R;
import com.chessapp.adapter.GameHistoryAdapter;
import com.chessapp.databinding.ActivityGameHistoryBinding;
import com.chessapp.model.PlayerProfile;
import com.chessapp.viewmodel.GameHistoryViewModel;

import java.util.Locale;

public class GameHistoryActivity extends AppCompatActivity {

    private ActivityGameHistoryBinding binding;
    private GameHistoryViewModel viewModel;
    private GameHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(GameHistoryViewModel.class);
        long activeProfileId = getIntent().getLongExtra("ACTIVE_PROFILE_ID", -1L);
        viewModel.init(activeProfileId);

        setupRecyclerView();
        setupFilters();
        observeViewModel();

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new GameHistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_filter_all) {
                viewModel.setFilter("ALL");
            } else if (checkedId == R.id.chip_filter_pvb) {
                viewModel.setFilter("PVB");
            } else if (checkedId == R.id.chip_filter_pvp) {
                viewModel.setFilter("PVP");
            }
        });
    }

    private void observeViewModel() {
        viewModel.getProfileLD().observe(this, profile -> {
            if (profile != null) {
                updateProfileHeader(profile);
            }
        });

        viewModel.getFilteredGamesLD().observe(this, games -> {
            adapter.submitList(games);
            binding.tvEmptyState.setVisibility(games.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void updateProfileHeader(PlayerProfile profile) {
        binding.tvAvatar.setText(profile.getAvatarEmoji());
        binding.tvName.setText(profile.getDisplayName());
        binding.chipWins.setText(getString(R.string.label_wins, profile.getTotalWins()));
        binding.chipLosses.setText(getString(R.string.label_losses, profile.getTotalLosses()));
        binding.chipDraws.setText(getString(R.string.label_draws, profile.getTotalDraws()));
        
        int total = profile.getTotalGames();
        double winRate = total == 0 ? 0 : (profile.getTotalWins() * 100.0 / total);
        binding.tvWinRate.setText(getString(R.string.label_win_rate, winRate));
        binding.tvTotalGames.setText(getString(R.string.label_total_games, total));
    }
}
