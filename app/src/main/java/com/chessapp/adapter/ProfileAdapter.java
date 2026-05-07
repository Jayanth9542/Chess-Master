package com.chessapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.chessapp.databinding.ItemProfileBinding;
import com.chessapp.model.PlayerProfile;

public class ProfileAdapter extends ListAdapter<PlayerProfile, ProfileAdapter.ViewHolder> {

    private final OnProfileClickListener listener;
    private final boolean manageMode;

    public interface OnProfileClickListener {
        void onProfileClick(PlayerProfile profile);
        void onEditClick(PlayerProfile profile);
        void onDeleteClick(PlayerProfile profile);
    }

    public ProfileAdapter(OnProfileClickListener listener, boolean manageMode) {
        super(new DiffCallback());
        this.listener = listener;
        this.manageMode = manageMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemProfileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener, manageMode);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileBinding binding;

        ViewHolder(ItemProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PlayerProfile profile, OnProfileClickListener listener, boolean manageMode) {
            binding.tvAvatar.setText(profile.getAvatarEmoji());
            binding.tvName.setText(profile.getDisplayName());
            binding.tvStats.setText(String.format("%dW  %dL  %dD",
                    profile.getTotalWins(), profile.getTotalLosses(), profile.getTotalDraws()));

            if (manageMode) {
                binding.ivEdit.setVisibility(View.VISIBLE);
                binding.ivDelete.setVisibility(View.VISIBLE);
                binding.ivEdit.setOnClickListener(v -> listener.onEditClick(profile));
                binding.ivDelete.setOnClickListener(v -> listener.onDeleteClick(profile));
                binding.getRoot().setClickable(false);
            } else {
                binding.ivEdit.setVisibility(View.GONE);
                binding.ivDelete.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(v -> listener.onProfileClick(profile));
            }
        }
    }

    static class DiffCallback extends DiffUtil.ItemCallback<PlayerProfile> {
        @Override
        public boolean areItemsTheSame(@NonNull PlayerProfile oldItem, @NonNull PlayerProfile newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlayerProfile oldItem, @NonNull PlayerProfile newItem) {
            return oldItem.getDisplayName().equals(newItem.getDisplayName()) &&
                   oldItem.getAvatarEmoji().equals(newItem.getAvatarEmoji()) &&
                   oldItem.getTotalWins() == newItem.getTotalWins() &&
                   oldItem.getTotalLosses() == newItem.getTotalLosses() &&
                   oldItem.getTotalDraws() == newItem.getTotalDraws();
        }
    }
}
