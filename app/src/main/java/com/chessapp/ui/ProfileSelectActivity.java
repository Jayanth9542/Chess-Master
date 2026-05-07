package com.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chessapp.R;
import com.chessapp.adapter.ProfileAdapter;
import com.chessapp.databinding.ActivityProfileSelectBinding;
import com.chessapp.databinding.DialogCreateProfileBinding;
import com.chessapp.model.PlayerProfile;
import com.chessapp.repository.GameRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ProfileSelectActivity extends AppCompatActivity implements ProfileAdapter.OnProfileClickListener {

    private ActivityProfileSelectBinding binding;
    private GameRepository repository;
    private ProfileAdapter adapter;
    private List<PlayerProfile> allProfiles = new ArrayList<>();

    private final String[] avatars = {"♟", "♛", "♚", "🦁", "🐯", "🦊", "🐺", "🦅", "🐉", "🤖", "👑", "⚡"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new GameRepository(getApplication());
        setupRecyclerView();

        binding.btnNewProfile.setOnClickListener(v -> showCreateProfileDialog());
        binding.btnManage_profiles.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileManageActivity.class));
        });

        repository.getAllProfiles().observe(this, profiles -> {
            allProfiles = profiles;
            adapter.submitList(profiles);
            if (profiles.isEmpty()) {
                showCreateProfileDialog();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ProfileAdapter(this, false);
        binding.rvProfiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProfiles.setAdapter(adapter);
    }

    private void showCreateProfileDialog() {
        DialogCreateProfileBinding dialogBinding = DialogCreateProfileBinding.inflate(getLayoutInflater());
        
        // Setup avatars RadioGroup
        for (int i = 0; i < avatars.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(avatars[i]);
            rb.setTextSize(24);
            rb.setButtonDrawable(null);
            rb.setPadding(16, 16, 16, 16);
            rb.setBackgroundResource(R.drawable.bg_avatar_selector);
            rb.setId(i);
            dialogBinding.rgAvatars.addView(rb);
            if (i == 0) rb.setChecked(true);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("New Profile")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", (d, w) -> {
                    if (allProfiles.isEmpty()) finish();
                })
                .setCancelable(!allProfiles.isEmpty())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = dialogBinding.etName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    dialogBinding.tilName.setError("Name required");
                    return;
                }
                if (isDuplicateName(name)) {
                    dialogBinding.tilName.setError("Name already exists");
                    return;
                }

                int checkedId = dialogBinding.rgAvatars.getCheckedRadioButtonId();
                String avatar = avatars[checkedId];

                repository.insertProfile(new PlayerProfile(name, avatar), id -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profile created", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            });
        });

        dialog.show();
    }

    private boolean isDuplicateName(String name) {
        for (PlayerProfile p : allProfiles) {
            if (p.getDisplayName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    @Override
    public void onProfileClick(PlayerProfile profile) {
        repository.setActiveProfileId(profile.getId());
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.putExtra("ACTIVE_PROFILE_ID", profile.getId());
        startActivity(intent);
        finish();
    }

    @Override public void onEditClick(PlayerProfile profile) {}
    @Override public void onDeleteClick(PlayerProfile profile) {}
}
