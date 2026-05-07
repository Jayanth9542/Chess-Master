package com.chessapp.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.chessapp.R;
import com.chessapp.databinding.ActivityPvbotSetupBinding;
import com.chessapp.repository.GameRepository;
import com.google.android.material.card.MaterialCardView;

public class PvBotSetupActivity extends AppCompatActivity {

    private ActivityPvbotSetupBinding binding;
    private GameRepository repository;
    private long activeProfileId;

    private String humanColor = "WHITE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPvbotSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new GameRepository(getApplication());

        activeProfileId = getIntent().getLongExtra(GameActivity.EXTRA_PROFILE_ID, -1L);

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

        setupColorCards();

        binding.btnStartGame.setOnClickListener(v -> {
            String name = binding.etPlayerName.getText().toString().trim();
            if (name.isEmpty()) name = "Player 1";
            
            String selectedText = binding.spinnerDifficulty.getText().toString();
            String difficultyKey = mapToDifficultyKey(selectedText);

            repository.savePlayer1Name(name);
            repository.saveLastDifficulty(difficultyKey);

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_MODE, "PVB");
            intent.putExtra(GameActivity.EXTRA_PLAYER1, name);
            intent.putExtra(GameActivity.EXTRA_PLAYER2, "Bot AI");
            intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficultyKey);
            intent.putExtra(GameActivity.EXTRA_HUMAN_COLOR, humanColor);
            intent.putExtra(GameActivity.EXTRA_BOARD_FLIPPED, "BLACK".equals(humanColor));
            intent.putExtra(GameActivity.EXTRA_PROFILE_ID, activeProfileId);
            startActivity(intent);
            finish();
        });
    }

    private void setupColorCards() {
        View.OnClickListener listener = v -> {
            if (v.getId() == R.id.card_play_white) humanColor = "WHITE";
            else if (v.getId() == R.id.card_play_black) humanColor = "BLACK";
            updateCardHighlights();
        };

        binding.cardPlayWhite.setOnClickListener(listener);
        binding.cardPlayBlack.setOnClickListener(listener);

        updateCardHighlights();
    }

    private void updateCardHighlights() {
        highlightCard(binding.cardPlayWhite, "WHITE".equals(humanColor));
        highlightCard(binding.cardPlayBlack, "BLACK".equals(humanColor));
    }

    private void highlightCard(MaterialCardView card, boolean selected) {
        if (selected) {
            int color = ContextCompat.getColor(this, R.color.brand_primary);
            card.setStrokeColor(color);
            card.setStrokeWidth(4);
            card.setCardBackgroundColor(ColorStateList.valueOf(color).withAlpha(51)); // 20% alpha
        } else {
            card.setStrokeWidth(0);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.brand_surface));
        }
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
