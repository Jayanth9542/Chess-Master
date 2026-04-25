package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.databinding.ActivityMainMenuBinding;

public class MainMenuActivity extends AppCompatActivity {

    private ActivityMainMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnPlayBot.setOnClickListener(v -> {
            startActivity(new Intent(this, PvBotSetupActivity.class));
        });

        binding.btnPlayPvP.setOnClickListener(v -> {
            startActivity(new Intent(this, PvPSetupActivity.class));
        });

        binding.btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        binding.btnAbout.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
        });
    }
}
