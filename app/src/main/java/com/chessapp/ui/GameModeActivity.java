package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.databinding.ActivityGameModeBinding;

public class GameModeActivity extends AppCompatActivity {

    private ActivityGameModeBinding binding;
    private long activeProfileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        activeProfileId = getIntent().getLongExtra("ACTIVE_PROFILE_ID", -1L);

        binding.btnBackMode.setOnClickListener(v -> finish());

        binding.cardPvb.setOnClickListener(v -> {
            Intent intent = new Intent(this, PvBotSetupActivity.class);
            intent.putExtra("ACTIVE_PROFILE_ID", activeProfileId);
            startActivity(intent);
        });

        binding.cardPvp.setOnClickListener(v -> {
            Intent intent = new Intent(this, PvPSetupActivity.class);
            intent.putExtra("ACTIVE_PROFILE_ID", activeProfileId);
            startActivity(intent);
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
