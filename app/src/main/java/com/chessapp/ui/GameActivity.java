package com.chessapp.ui;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.chessapp.R;
import com.chessapp.databinding.ActivityGameBinding;
import com.chessapp.model.GameState;
import com.chessapp.model.Move;
import com.chessapp.viewmodel.GameViewModel;
import com.chessapp.views.ChessBoardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.chessapp.repository.GameRepository;

public class GameActivity extends AppCompatActivity {

    private ActivityGameBinding binding;
    private GameViewModel viewModel;
    private GameRepository repository;
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        repository = new GameRepository(getApplication());
        
        setupBoard();
        setupControls();
        observeViewModel();
        
        if (savedInstanceState == null) {
            startNewGame();
        }
    }

    private void setupBoard() {
        binding.chessBoardView.setAnimationsEnabled(repository.isAnimationsEnabled());
        binding.chessBoardView.setOnMoveSelectedListener(new ChessBoardView.OnMoveSelectedListener() {
            @Override
            public void onMoveSelected(Move move) {
                if (!isGameOver) viewModel.onHumanMove(move);
            }

            @Override
            public void onPromotionRequired(Move move, com.chessapp.model.ChessPiece.Color color) {
                if (isGameOver) return;
                showPromotionDialog(move, color);
            }
        });
    }

    private void showPromotionDialog(Move move, com.chessapp.model.ChessPiece.Color color) {
        View view = getLayoutInflater().inflate(R.layout.dialog_promotion, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Promote Pawn")
                .setView(view)
                .setCancelable(false)
                .create();

        setupPromotionButton(view, R.id.btn_promo_queen, move, com.chessapp.model.ChessPiece.Type.QUEEN, color, dialog);
        setupPromotionButton(view, R.id.btn_promo_rook, move, com.chessapp.model.ChessPiece.Type.ROOK, color, dialog);
        setupPromotionButton(view, R.id.btn_promo_bishop, move, com.chessapp.model.ChessPiece.Type.BISHOP, color, dialog);
        setupPromotionButton(view, R.id.btn_promo_knight, move, com.chessapp.model.ChessPiece.Type.KNIGHT, color, dialog);

        dialog.show();
    }

    private void setupPromotionButton(View root, int id, Move move, com.chessapp.model.ChessPiece.Type type,
                                      com.chessapp.model.ChessPiece.Color color, androidx.appcompat.app.AlertDialog dialog) {
        android.widget.TextView tv = root.findViewById(id);
        com.chessapp.model.ChessPiece temp = new com.chessapp.model.ChessPiece(type, color);
        tv.setText(temp.getUnicodeSymbol());
        tv.setOnClickListener(v -> {
            move.promotion = type;
            viewModel.onHumanMove(move);
            dialog.dismiss();
        });
    }

    private void setupControls() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnUndo.setOnClickListener(v -> {
            if (!isGameOver) viewModel.undoMove();
        });

        binding.btnResign.setOnClickListener(v -> {
            if (!isGameOver) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.action_resign)
                    .setMessage("Are you sure you want to resign?")
                    .setPositiveButton("Resign", (d, w) -> viewModel.resign())
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        binding.btnHint.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), "The Bot suggests analyzing the center.", Snackbar.LENGTH_SHORT).show();
        });

        binding.btnOptions.setOnClickListener(v -> showGameOptionsDialog());
    }

    private void showGameOptionsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_game_options, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("In-Game Options")
                .setView(view)
                .show();

        view.findViewById(R.id.btn_opt_flip).setOnClickListener(v -> {
            binding.chessBoardView.setFlipped(!binding.chessBoardView.isFlipped());
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_opt_explorer).setOnClickListener(v -> {
            boolean current = binding.chessBoardView.isShowingLegalMoves();
            binding.chessBoardView.setShowLegalMoves(!current);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_opt_pause).setOnClickListener(v -> {
            viewModel.togglePause();
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_opt_exit).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
    }

    private void startNewGame() {
        isGameOver = false;
        String mode = getIntent().getStringExtra("mode");
        String p1 = getIntent().getStringExtra("player1");
        String p2 = getIntent().getStringExtra("player2");
        String diff = getIntent().getStringExtra("difficulty");

        GameState.GameMode gMode = "PVB".equals(mode) ? GameState.GameMode.PVB : GameState.GameMode.PVP;
        GameState.Difficulty gDiff = GameState.Difficulty.valueOf(diff != null ? diff : "INTERMEDIATE");

        binding.tvPlayer1Name.setText(p1 != null ? p1 : "White");
        binding.tvPlayer2Name.setText(p2 != null ? p2 : "Black");

        // Hide bot timer in PvB mode as requested
        binding.tvBlackTime.setVisibility(gMode == GameState.GameMode.PVB ? View.GONE : View.VISIBLE);

        viewModel.startGame(new GameState(gMode, p1, p2, gDiff));
    }

    private void observeViewModel() {
        viewModel.getBoardLD().observe(this, board -> {
            binding.chessBoardView.setBoard(board);
        });

        viewModel.getStatusLD().observe(this, status -> {
            binding.tvGameStatus.setText(status);
            if (status.contains("CHECK")) {
                binding.tvGameStatus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink));
            }
        });

        viewModel.getEngineThinkingLD().observe(this, thinking -> {
            binding.progressThinking.setVisibility(thinking ? View.VISIBLE : View.GONE);
            binding.chessBoardView.setEngineThinking(thinking);
        });

        viewModel.getGameOverLD().observe(this, over -> {
            if (over) {
                isGameOver = true;
                showGameOverDialog();
            }
        });

        viewModel.getWhiteTimeLD().observe(this, t -> binding.tvWhiteTime.setText(t));
        viewModel.getBlackTimeLD().observe(this, t -> binding.tvBlackTime.setText(t));

        viewModel.getLastMoveLD().observe(this, move -> {
            binding.tvLastMove.setText(getString(R.string.label_last_move, move));
        });

        viewModel.getLastMoveObjLD().observe(this, move -> {
            if (move != null) {
                binding.chessBoardView.animateMove(move);
            }
        });

        viewModel.getGameLogLD().observe(this, log -> {
            binding.tvGameLog.setText(log);
            binding.scrollLog.post(() -> binding.scrollLog.fullScroll(View.FOCUS_DOWN));
        });

        viewModel.getToastLD().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showGameOverDialog() {
        String result = viewModel.getGameState().getResult();
        new MaterialAlertDialogBuilder(this)
            .setTitle("Game Over")
            .setMessage(result)
            .setPositiveButton("New Game", (d, w) -> startNewGame())
            .setNegativeButton("Menu", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }
}
