package com.chessapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chessapp.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBackAbout.setOnClickListener(v -> finish());
    }
}
