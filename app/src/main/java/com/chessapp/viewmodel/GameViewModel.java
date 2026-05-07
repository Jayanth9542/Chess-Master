package com.chessapp.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.chessapp.engine.EngineBridge;
import com.chessapp.model.ChessBoard;
import com.chessapp.model.ChessPiece;
import com.chessapp.model.GameRecord;
import com.chessapp.model.GameState;
import com.chessapp.model.Move;
import com.chessapp.repository.GameRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MVVM ViewModel for an active chess game.
 */
public class GameViewModel extends AndroidViewModel {

    private static final String TAG = "GameViewModel";

    private final MutableLiveData<ChessBoard> boardLD          = new MutableLiveData<>();
    private final MutableLiveData<String>     statusLD         = new MutableLiveData<>();
    private final MutableLiveData<Boolean>    gameOverLD       = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>    engineThinkingLD = new MutableLiveData<>(false);
    private final MutableLiveData<String>     currentPlayerLD  = new MutableLiveData<>();
    private final MutableLiveData<String>     whiteTimeLD      = new MutableLiveData<>("00:00");
    private final MutableLiveData<String>     blackTimeLD      = new MutableLiveData<>("00:00");
    private final MutableLiveData<String>     lastMoveLD       = new MutableLiveData<>("--");
    private final MutableLiveData<Move>       lastMoveObjLD    = new MutableLiveData<>();
    private final MutableLiveData<String>     toastLD          = new MutableLiveData<>();
    private final MutableLiveData<String>     gameLogLD        = new MutableLiveData<>("");

    private final GameRepository   repository;
    private final ExecutorService  executor = Executors.newSingleThreadExecutor();
    private final Handler          timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable         timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimers();
            timerHandler.postDelayed(this, 1000);
        }
    };

    private GameState gameState;
    private long activeProfileId = -1L;
    private long gameStartTimeMs = 0;
    private String humanColor = "WHITE";

    public GameViewModel(@NonNull Application application) {
        super(application);
        repository = new GameRepository(application);
    }

    public void setActiveProfileId(long id) {
        this.activeProfileId = id;
    }

    public void setGameStartTimeMs(long time) {
        this.gameStartTimeMs = time;
    }

    public void setHumanColor(String color) {
        this.humanColor = color;
    }

    public boolean isBotWhite() {
        return gameState != null
                && gameState.getMode() == GameState.GameMode.PVB
                && "BLACK".equals(humanColor);
    }

    public void startGame(GameState state) {
        gameState = state;
        boardLD.setValue(state.getBoard());
        updateStatus();

        if (state.getMode() == GameState.GameMode.PVB) {
            initEngine();
            executor.execute(() -> {
                EngineBridge.getInstance().sendCommand("ucinewgame");
                EngineBridge.getInstance().isEngineReady();
            });

            // If bot is White and the game hasn't started yet, trigger first move
            if (isBotWhite() && state.getBoard().getMoveCount() == 0) {
                executor.execute(() -> {
                    try {
                        int attempts = 0;
                        while (!EngineBridge.getInstance().isEngineReady() && attempts < 60) {
                            Thread.sleep(50);
                            attempts++;
                        }
                        if (attempts >= 60) {
                            toastLD.postValue("Engine timed out – Bot cannot move");
                            return;
                        }

                        new Handler(Looper.getMainLooper()).post(() -> engineThinkingLD.setValue(true));
                        requestBotMove();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Bot opening move task interrupted", e);
                    }
                });
            }
        }
        state.startTurnTimer();
        timerHandler.post(timerRunnable);
    }

    private void initEngine() {
        executor.execute(() -> {
            boolean ok = EngineBridge.getInstance().initEngine();
            if (!ok) {
                toastLD.postValue("⚠ Engine init failed – check Stockfish files");
            }
        });
    }

    public void onHumanMove(Move move) {
        if (gameState == null || gameState.isGameOver()) return;

        ChessBoard board = gameState.getBoard();
        if (gameState.getMode() == GameState.GameMode.PVB && !board.isWhiteToMove()) {
            return;
        }

        synchronized (board) {
            String timeStr = gameState.getWhiteTimeDisplay();
            gameState.pauseTurnTimer();
            board.makeMove(move);
            
            String uci = move.toUCI();
            lastMoveLD.setValue(uci);
            lastMoveObjLD.setValue(move);
            addToLog("White: " + uci + " (" + timeStr + ")");

            boardLD.setValue(board);
            updateTimers();
            updateStatus();
        }

        if (checkGameOver()) return;

        if (gameState.getMode() == GameState.GameMode.PVB && !board.isWhiteToMove()) {
            requestBotMove();
        } else {
            gameState.startTurnTimer();
        }
    }

    void requestBotMove() {
        if (gameState == null || gameState.isGameOver()) return;

        engineThinkingLD.postValue(true);
        statusLD.postValue("Bot is thinking…");

        String fen = gameState.getBoard().toFEN();
        int[] params = EngineBridge.getDifficultyParams(gameState.getDifficulty().name());
        int depth = params[0];
        int skill = params[1];

        executor.execute(() -> {
            try {
                String uci = EngineBridge.getInstance().getBestMove(fen, depth, skill);
                if (uci == null || uci.isEmpty()) {
                    engineThinkingLD.postValue(false);
                    return;
                }

                Move move = parseBotMove(uci);
                String timeStr = gameState.getBlackTimeDisplay();
                ChessBoard board = gameState.getBoard();

                if (move == null) {
                    synchronized (board) {
                        java.util.List<Move> allLegal = board.getAllLegalMoves();
                        if (!allLegal.isEmpty()) {
                            move = allLegal.get((int) (Math.random() * allLegal.size()));
                        } else {
                            engineThinkingLD.postValue(false);
                            return;
                        }
                    }
                }

                Thread.sleep(1500);

                synchronized (board) {
                    if (gameState.isGameOver() || board.isWhiteToMove()) return;

                    board.makeMove(move);
                    boardLD.postValue(board);
                    lastMoveLD.postValue(move.toUCI());
                    lastMoveObjLD.postValue(move);
                    addToLogPost("Black: " + move.toUCI() + " (" + timeStr + ")");
                    updateTimersPost();
                    statusLD.postValue(board.getGameStatus());
                    currentPlayerLD.postValue(gameState.getCurrentPlayerName());

                    if (board.isGameOver()) {
                        gameState.setGameOver(true);
                        saveGameRecord();
                        gameOverLD.postValue(true);
                    } else {
                        gameState.startTurnTimer();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Bot move error", e);
            } finally {
                engineThinkingLD.postValue(false);
            }
        });
    }

    private Move parseBotMove(String uci) {
        if (uci == null || uci.length() < 4) return null;
        try {
            int fromCol = uci.charAt(0) - 'a';
            int fromRow = 8 - (uci.charAt(1) - '0');
            int toCol   = uci.charAt(2) - 'a';
            int toRow   = 8 - (uci.charAt(3) - '0');
            Move move = new Move(fromRow, fromCol, toRow, toCol);
            if (uci.length() == 5) {
                switch (uci.charAt(4)) {
                    case 'q': move.promotion = ChessPiece.Type.QUEEN;  break;
                    case 'r': move.promotion = ChessPiece.Type.ROOK;   break;
                    case 'b': move.promotion = ChessPiece.Type.BISHOP; break;
                    case 'n': move.promotion = ChessPiece.Type.KNIGHT; break;
                }
            }
            ChessBoard board = gameState.getBoard();
            ChessPiece pc = board.getPieceAt(fromRow, fromCol);
            if (pc == null) return null;
            if (pc.getType() == ChessPiece.Type.KING && Math.abs(fromCol - toCol) == 2) move.wasCastling = true;
            if (pc.getType() == ChessPiece.Type.PAWN && fromCol != toCol && board.getPieceAt(toRow, toCol) == null) move.wasEnPassant = true;
            if (!board.isLegal(move)) return null;
            return move;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveGameRecord() {
        if (activeProfileId == -1L || gameState == null) return;

        String res = "DRAW";
        String status = gameState.getBoard().getGameStatus();
        if (status.contains("White wins")) res = "WIN";
        else if (status.contains("Black wins")) res = "LOSS";
        else if (status.contains("Resignation")) {
             if (status.contains(gameState.getPlayer1Name())) res = "WIN";
             else res = "LOSS";
        }

        long duration = System.currentTimeMillis() - gameStartTimeMs;
        GameRecord record = new GameRecord(
                activeProfileId,
                gameState.getPlayer2Name(),
                gameState.getMode().name(),
                res,
                "WHITE",
                gameState.getBoard().getMoveCount(),
                duration,
                null // FEN after move 10 not implemented here for simplicity
        );
        if (gameState.getMode() == GameState.GameMode.PVB) {
            record.setDifficulty(gameState.getDifficulty().name());
        }

        repository.insertGameRecord(record);
        repository.incrementProfileStats(activeProfileId, res);
    }

    public void undoMove() {
        if (gameState == null || !gameState.getBoard().canUndo()) return;
        ChessBoard board = gameState.getBoard();
        synchronized (board) {
            if (gameState.getMode() == GameState.GameMode.PVB) {
                if (board.getMoveCount() >= 2) { board.undoMove(); board.undoMove(); }
                else if (board.getMoveCount() == 1) { board.undoMove(); }
            } else {
                board.undoMove();
            }
            gameState.setGameOver(false);
            boardLD.setValue(board);
            gameOverLD.setValue(false);
            updateStatus();
        }
    }

    public void togglePause() {
        if (gameState == null) return;
        boolean isPaused = statusLD.getValue() != null && statusLD.getValue().equals("Game Paused");
        if (!isPaused) {
            timerHandler.removeCallbacks(timerRunnable);
            statusLD.setValue("Game Paused");
        } else {
            timerHandler.post(timerRunnable);
            updateStatus();
        }
    }

    private boolean checkGameOver() {
        if (gameState.getBoard().isGameOver()) {
            gameState.setGameOver(true);
            saveGameRecord();
            gameOverLD.setValue(true);
            statusLD.setValue(gameState.getBoard().getGameStatus());
            return true;
        }
        return false;
    }

    private void updateStatus() {
        if (gameState == null) return;
        statusLD.setValue(gameState.getBoard().getGameStatus());
        currentPlayerLD.setValue(gameState.getCurrentPlayerName());
    }

    private void updateTimers() {
        if (gameState == null) return;
        whiteTimeLD.setValue(gameState.getWhiteTimeDisplay());
        blackTimeLD.setValue(gameState.getBlackTimeDisplay());
    }

    private void updateTimersPost() {
        if (gameState == null) return;
        whiteTimeLD.postValue(gameState.getWhiteTimeDisplay());
        blackTimeLD.postValue(gameState.getBlackTimeDisplay());
    }

    public LiveData<ChessBoard> getBoardLD()          { return boardLD; }
    public LiveData<String>     getStatusLD()         { return statusLD; }
    public LiveData<Boolean>    getGameOverLD()       { return gameOverLD; }
    public LiveData<Boolean>    getEngineThinkingLD() { return engineThinkingLD; }
    public LiveData<String>     getCurrentPlayerLD()  { return currentPlayerLD; }
    public LiveData<String>     getWhiteTimeLD()      { return whiteTimeLD; }
    public LiveData<String>     getBlackTimeLD()      { return blackTimeLD; }
    public LiveData<String>     getLastMoveLD()       { return lastMoveLD; }
    public LiveData<Move>       getLastMoveObjLD()    { return lastMoveObjLD; }
    public LiveData<String>     getToastLD()          { return toastLD; }
    public LiveData<String>     getGameLogLD()        { return gameLogLD; }
    public GameState            getGameState()        { return gameState; }

    private void addToLog(String entry) {
        String current = gameLogLD.getValue();
        gameLogLD.setValue((current == null || current.isEmpty()) ? entry : current + "\n" + entry);
    }

    private void addToLogPost(String entry) {
        String current = gameLogLD.getValue();
        gameLogLD.postValue((current == null || current.isEmpty()) ? entry : current + "\n" + entry);
    }

    public void resign() {
        if (gameState == null || gameState.isGameOver()) return;
        String winner = gameState.getBoard().isWhiteToMove() ? gameState.getPlayer2Name() : gameState.getPlayer1Name();
        if (winner == null || winner.isEmpty()) winner = gameState.getBoard().isWhiteToMove() ? "Black" : "White";
        String msg = "Resignation. " + winner + " wins!";
        gameState.setGameOver(true, msg);
        saveGameRecord();
        gameOverLD.setValue(true);
        statusLD.setValue(msg);
        addToLog("Game ended by resignation.");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        timerHandler.removeCallbacks(timerRunnable);
        executor.shutdown();
        if (gameState != null && gameState.getMode() == GameState.GameMode.PVB) EngineBridge.getInstance().stopEngine();
    }
}
