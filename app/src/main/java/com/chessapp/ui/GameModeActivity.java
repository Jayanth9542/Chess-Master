package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.databinding.ActivityGameModeBinding;

public class GameModeActivity extends AppCompatActivity {

    private ActivityGameModeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBackMode.setOnClickListener(v -> finish());

        binding.cardPvb.setOnClickListener(v -> {
            startActivity(new Intent(this, PvBotSetupActivity.class));
        });

        binding.cardPvp.setOnClickListener(v -> {
            startActivity(new Intent(this, PvPSetupActivity.class));
        });

        binding.cardP2p.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Coming Soon")
                    .setMessage("Online multiplayer is currently under development.")
                    .setPositiveButton("OK", null)
                    .show();
        });
    }
}