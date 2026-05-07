package com.chessapp.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.chessapp.R;
import com.chessapp.databinding.ActivityPvpSetupBinding;
import com.chessapp.repository.GameRepository;
import com.google.android.material.card.MaterialCardView;

import java.util.Random;

public class PvPSetupActivity extends AppCompatActivity {

    private ActivityPvpSetupBinding binding;
    private GameRepository repository;
    private long activeProfileId;

    private String colorChoice = "RANDOM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPvpSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new GameRepository(getApplication());

        activeProfileId = getIntent().getLongExtra(GameActivity.EXTRA_PROFILE_ID, -1L);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.etPlayer1.setText(repository.getLastPlayer1Name());
        binding.etPlayer2.setText(repository.getLastPlayer2Name());

        setupColorCards();

        binding.btnStartGame.setOnClickListener(v -> {
            String p1 = binding.etPlayer1.getText().toString().trim();
            String p2 = binding.etPlayer2.getText().toString().trim();
            
            if (p1.isEmpty()) p1 = "Player 1";
            if (p2.isEmpty()) p2 = "Player 2";

            repository.savePlayer1Name(p1);
            repository.savePlayer2Name(p2);

            String p1Color = "WHITE";
            String p2Color = "BLACK";

            if ("RANDOM".equals(colorChoice)) {
                if (new Random().nextBoolean()) {
                    p1Color = "WHITE"; p2Color = "BLACK";
                } else {
                    p1Color = "BLACK"; p2Color = "WHITE";
                }
            } else if ("WHITE_FIRST".equals(colorChoice)) {
                p1Color = "WHITE"; p2Color = "BLACK";
            } else if ("BLACK_FIRST".equals(colorChoice)) {
                p1Color = "BLACK"; p2Color = "WHITE";
            }

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_MODE, "PVP");
            intent.putExtra(GameActivity.EXTRA_PLAYER1, p1);
            intent.putExtra(GameActivity.EXTRA_PLAYER2, p2);
            intent.putExtra(GameActivity.EXTRA_PLAYER1_COLOR, p1Color);
            intent.putExtra(GameActivity.EXTRA_PLAYER2_COLOR, p2Color);
            intent.putExtra(GameActivity.EXTRA_BOARD_FLIPPED, "BLACK".equals(p1Color));
            intent.putExtra(GameActivity.EXTRA_PROFILE_ID, activeProfileId);
            startActivity(intent);
            finish();
        });
    }

    private void setupColorCards() {
        View.OnClickListener listener = v -> {
            if (v.getId() == R.id.card_white_first) colorChoice = "WHITE_FIRST";
            else if (v.getId() == R.id.card_random) colorChoice = "RANDOM";
            else if (v.getId() == R.id.card_black_first) colorChoice = "BLACK_FIRST";
            updateCardHighlights();
        };

        binding.cardWhiteFirst.setOnClickListener(listener);
        binding.cardRandom.setOnClickListener(listener);
        binding.cardBlackFirst.setOnClickListener(listener);

        updateCardHighlights();
    }

    private void updateCardHighlights() {
        highlightCard(binding.cardWhiteFirst, "WHITE_FIRST".equals(colorChoice));
        highlightCard(binding.cardRandom, "RANDOM".equals(colorChoice));
        highlightCard(binding.cardBlackFirst, "BLACK_FIRST".equals(colorChoice));
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
}
