package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.databinding.ActivityPvpSetupBinding;
import com.chessapp.repository.GameRepository;

public class PvPSetupActivity extends AppCompatActivity {

    private ActivityPvpSetupBinding binding;
    private GameRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPvpSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new GameRepository(getApplication());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.etPlayer1.setText(repository.getLastPlayer1Name());
        binding.etPlayer2.setText(repository.getLastPlayer2Name());

        binding.btnStartGame.setOnClickListener(v -> {
            String p1 = binding.etPlayer1.getText().toString().trim();
            String p2 = binding.etPlayer2.getText().toString().trim();
            
            if (p1.isEmpty()) p1 = "White";
            if (p2.isEmpty()) p2 = "Black";

            repository.savePlayer1Name(p1);
            repository.savePlayer2Name(p2);

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("mode", "PVP");
            intent.putExtra("player1", p1);
            intent.putExtra("player2", p2);
            startActivity(intent);
            finish();
        });
    }
}
