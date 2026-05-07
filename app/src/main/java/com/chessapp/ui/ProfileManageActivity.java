package com.chessapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chessapp.R;
import com.chessapp.adapter.ProfileAdapter;
import com.chessapp.databinding.ActivityProfileManageBinding;
import com.chessapp.databinding.DialogCreateProfileBinding;
import com.chessapp.model.PlayerProfile;
import com.chessapp.repository.GameRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ProfileManageActivity extends AppCompatActivity implements ProfileAdapter.OnProfileClickListener {

    private ActivityProfileManageBinding binding;
    private GameRepository repository;
    private ProfileAdapter adapter;
    private List<PlayerProfile> allProfiles = new ArrayList<>();

    private final String[] avatars = {"♟", "♛", "♚", "🦁", "🐯", "🦊", "🐺", "🦅", "🐉", "🤖", "👑", "⚡"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new GameRepository(getApplication());
        setupRecyclerView();

        binding.btnBack.setOnClickListener(v -> finish());

        repository.getAllProfiles().observe(this, profiles -> {
            allProfiles = profiles;
            adapter.submitList(profiles);
        });
    }

    private void setupRecyclerView() {
        adapter = new ProfileAdapter(this, true);
        binding.rvProfiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProfiles.setAdapter(adapter);
    }

    @Override
    public void onProfileClick(PlayerProfile profile) {}

    @Override
    public void onEditClick(PlayerProfile profile) {
        showEditProfileDialog(profile);
    }

    @Override
    public void onDeleteClick(PlayerProfile profile) {
        if (allProfiles.size() <= 1) {
            Snackbar.make(binding.getRoot(), "You must have at least one profile.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete " + profile.getDisplayName() + "? All game history will be lost.")
                .setPositiveButton("Delete", (d, w) -> {
                    repository.deleteProfile(profile);
                    if (repository.getActiveProfileId() == profile.getId()) {
                        repository.setActiveProfileId(-1L);
                    }
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditProfileDialog(PlayerProfile profile) {
        DialogCreateProfileBinding dialogBinding = DialogCreateProfileBinding.inflate(getLayoutInflater());
        dialogBinding.etName.setText(profile.getDisplayName());

        int initialAvatarIndex = 0;
        for (int i = 0; i < avatars.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(avatars[i]);
            rb.setTextSize(24);
            rb.setButtonDrawable(null);
            rb.setPadding(16, 16, 16, 16);
            rb.setBackgroundResource(R.drawable.bg_avatar_selector);
            rb.setId(i);
            dialogBinding.rgAvatars.addView(rb);
            if (avatars[i].equals(profile.getAvatarEmoji())) {
                initialAvatarIndex = i;
                rb.setChecked(true);
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Rename Profile")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = dialogBinding.etName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    dialogBinding.tilName.setError("Name required");
                    return;
                }
                if (!name.equalsIgnoreCase(profile.getDisplayName()) && isDuplicateName(name)) {
                    dialogBinding.tilName.setError("Name already exists");
                    return;
                }

                int checkedId = dialogBinding.rgAvatars.getCheckedRadioButtonId();
                profile.setDisplayName(name);
                profile.setAvatarEmoji(avatars[checkedId]);

                repository.updateProfile(profile);
                dialog.dismiss();
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
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
}
