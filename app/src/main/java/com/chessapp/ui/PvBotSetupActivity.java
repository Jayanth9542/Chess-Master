package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.R;
import com.chessapp.databinding.ActivityPvbotSetupBinding;
import com.chessapp.repository.GameRepository;

public class PvBotSetupActivity extends AppCompatActivity {

    private ActivityPvbotSetupBinding binding;
    private GameRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPvbotSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new GameRepository(getApplication());

        binding.btnBack.setOnClickListener(v -> finish());

        // 6-Tier Difficulty List
        String[] levels = {
                getString(R.string.diff_beginner),
                getString(R.string.diff_intermediate),
                getString(R.string.diff_expert),
                getString(R.string.diff_grandmaster),
                getString(R.string.diff_magnus)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, levels);
        binding.spinnerDifficulty.setAdapter(adapter);

        // Load Persistence
        binding.etPlayerName.setText(repository.getLastPlayer1Name());
        String lastDiff = repository.getLastDifficulty();
        binding.spinnerDifficulty.setText(getLocalizedDiff(lastDiff), false);

        binding.btnStartGame.setOnClickListener(v -> {
            String name = binding.etPlayerName.getText().toString().trim();
            if (name.isEmpty()) name = "Challenger";
            
            String selectedText = binding.spinnerDifficulty.getText().toString();
            String difficultyKey = mapToDifficultyKey(selectedText);

            repository.savePlayer1Name(name);
            repository.saveLastDifficulty(difficultyKey);

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("mode", "PVB");
            intent.putExtra("player1", name);
            intent.putExtra("player2", "Stockfish AI");
            intent.putExtra("difficulty", difficultyKey);
            startActivity(intent);
            finish();
        });
    }

    private String getLocalizedDiff(String key) {
        switch (key) {
            case "BEGINNER":     return getString(R.string.diff_beginner);
            case "EXPERT":       return getString(R.string.diff_expert);
            case "GRANDMASTER":  return getString(R.string.diff_grandmaster);
            case "MAGNUS":       return getString(R.string.diff_magnus);
            default:             return getString(R.string.diff_intermediate);
        }
    }

    private String mapToDifficultyKey(String localized) {
        if (localized.equals(getString(R.string.diff_beginner))) return "BEGINNER";
        if (localized.equals(getString(R.string.diff_expert)))   return "EXPERT";
        if (localized.equals(getString(R.string.diff_grandmaster))) return "GRANDMASTER";
        if (localized.equals(getString(R.string.diff_magnus)))   return "MAGNUS";
        return "INTERMEDIATE";
    }
}
