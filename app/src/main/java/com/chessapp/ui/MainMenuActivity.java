package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.databinding.ActivityMainMenuBinding;

public class MainMenuActivity extends AppCompatActivity {

    private ActivityMainMenuBinding binding;
    private long activeProfileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        activeProfileId = getIntent().getLongExtra("ACTIVE_PROFILE_ID", -1L);

        binding.btnPlayBot.setOnClickListener(v -> {
            Intent intent = new Intent(this, PvBotSetupActivity.class);
            intent.putExtra("ACTIVE_PROFILE_ID", activeProfileId);
            startActivity(intent);
        });

        binding.btnPlayPvP.setOnClickListener(v -> {
            Intent intent = new Intent(this, PvPSetupActivity.class);
            intent.putExtra("ACTIVE_PROFILE_ID", activeProfileId);
            startActivity(intent);
        });

        binding.btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        binding.btnAbout.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
        });

        binding.btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameHistoryActivity.class);
            intent.putExtra("ACTIVE_PROFILE_ID", activeProfileId);
            startActivity(intent);
        });
    }
}
