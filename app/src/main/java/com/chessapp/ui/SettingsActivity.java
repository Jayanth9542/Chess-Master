package com.chessapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.chessapp.databinding.ActivitySettingsBinding;
import com.chessapp.repository.GameRepository;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private GameRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new GameRepository(getApplication());

        binding.btnBackSettings.setOnClickListener(v -> finish());

        // Load settings
        binding.switchSound.setChecked(repository.isSoundEnabled());
        binding.switchAnimations.setChecked(repository.isAnimationsEnabled());
        
        String currentTheme = repository.getTheme();
        if ("dark".equals(currentTheme)) {
            binding.rbDark.setChecked(true);
        } else {
            binding.rbLight.setChecked(true);
        }

        binding.btnSaveSettings.setOnClickListener(v -> {
            repository.setSoundEnabled(binding.switchSound.isChecked());
            repository.setAnimationsEnabled(binding.switchAnimations.isChecked());
            
            String theme = binding.rbDark.isChecked() ? "dark" : "light";
            repository.setTheme(theme);
            
            if ("dark".equals(theme)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            
            finish();
        });
    }
}