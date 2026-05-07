package com.chessapp.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.chessapp.R;
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
            Context context = itemView.getContext();
            String badgeText = "D";
            int badgeColor = ContextCompat.getColor(context, R.color.color_draw);
            
            if ("WIN".equals(record.getResult())) {
                badgeText = "W";
                badgeColor = ContextCompat.getColor(context, R.color.color_win);
            } else if ("LOSS".equals(record.getResult())) {
                badgeText = "L";
                badgeColor = ContextCompat.getColor(context, R.color.color_loss);
            }
            binding.tvResultBadge.setText(badgeText);
            binding.tvResultBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

            binding.tvOpponent.setText(context.getString(R.string.label_vs_opponent, record.getOpponentName()));
            binding.tvMode.setText(context.getString("PVP".equals(record.getGameMode()) ? R.string.mode_pvp : R.string.mode_pvb));
            
            String colorEmoji = "WHITE".equals(record.getPlayerColor()) ? "⬜" : "⬛";
            binding.tvDetails.setText(context.getString(R.string.label_game_details, 
                    colorEmoji, record.getPlayerColor().toLowerCase(Locale.getDefault()), record.getTotalMoves()));

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
