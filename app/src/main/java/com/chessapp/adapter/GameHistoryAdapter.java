package com.chessapp.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.chessapp.databinding.ItemGameHistoryBinding;
import com.chessapp.model.GameRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameHistoryAdapter extends ListAdapter<GameRecord, GameHistoryAdapter.ViewHolder> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM 2025", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aa", Locale.getDefault());

    public GameHistoryAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemGameHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemGameHistoryBinding binding;

        ViewHolder(ItemGameHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GameRecord record) {
            String badgeText = "D";
            int badgeColor = Color.parseColor("#757575");
            if ("WIN".equals(record.getResult())) {
                badgeText = "W";
                badgeColor = Color.parseColor("#388E3C");
            } else if ("LOSS".equals(record.getResult())) {
                badgeText = "L";
                badgeColor = Color.parseColor("#D32F2F");
            }
            binding.tvResultBadge.setText(badgeText);
            binding.tvResultBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

            binding.tvOpponent.setText("vs " + record.getOpponentName());
            binding.tvMode.setText("PVP".equals(record.getGameMode()) ? "Player vs Player" : "Player vs Bot");
            
            String colorEmoji = "WHITE".equals(record.getPlayerColor()) ? "⬜" : "⬛";
            binding.tvDetails.setText(String.format("Played as %s %s · %d moves", 
                    colorEmoji, record.getPlayerColor().toLowerCase(), record.getTotalMoves()));

            Date date = new Date(record.getPlayedAt());
            binding.tvDate.setText(dateFormat.format(date));
            binding.tvTime.setText(timeFormat.format(date));

            long seconds = record.getDurationMs() / 1000;
            binding.tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60));
        }
    }

    static class DiffCallback extends DiffUtil.ItemCallback<GameRecord> {
        @Override
        public boolean areItemsTheSame(@NonNull GameRecord oldItem, @NonNull GameRecord newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull GameRecord oldItem, @NonNull GameRecord newItem) {
            return oldItem.getPlayedAt() == newItem.getPlayedAt() &&
                   oldItem.getResult().equals(newItem.getResult());
        }
    }
}
