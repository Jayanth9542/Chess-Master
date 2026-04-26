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
import com.chessapp.model.GameState;
import com.chessapp.model.Move;
import com.chessapp.repository.GameRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MVVM ViewModel for an active chess game.
 *
 * Responsibilities:
 *  - Drive the ChessBoard model
 *  - Dispatch bot-move requests to Stockfish on a background thread
 *  - Expose LiveData for the UI to observe
 */
public class GameViewModel extends AndroidViewModel {

    private static final String TAG = "GameViewModel";

    // ── LiveData exposed to UI ─────────────────────────────────────────
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

    public GameViewModel(@NonNull Application application) {
        super(application);
        repository = new GameRepository(application);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Startup
    // ──────────────────────────────────────────────────────────────────
    public void startGame(GameState state) {
        gameState = state;
        boardLD.setValue(state.getBoard());
        updateStatus();

        if (state.getMode() == GameState.GameMode.PVB) {
            initEngine();
            // Reset engine state for a new game session
            executor.execute(() -> {
                EngineBridge.getInstance().sendCommand("ucinewgame");
                EngineBridge.getInstance().isEngineReady(); // This blocks until readyok
            });
        }
        state.startTurnTimer();
        timerHandler.post(timerRunnable);
    }

    private void initEngine() {
        executor.execute(() -> {
            boolean ok = EngineBridge.getInstance().initEngine();
            if (!ok) {
                toastLD.postValue("⚠ Engine init failed – check Stockfish files");
                Log.e(TAG, "Stockfish initEngine() returned false");
            } else {
                Log.i(TAG, "Stockfish ready");
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────
    //  Human move
    // ──────────────────────────────────────────────────────────────────
    public void onHumanMove(Move move) {
        if (gameState == null || gameState.isGameOver()) return;

        ChessBoard board = gameState.getBoard();
        
        // Prevent human moving during bot's turn in PvB
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

        // In PvB the bot always plays Black (index 1 / !whiteToMove after human)
        if (gameState.getMode() == GameState.GameMode.PVB && !board.isWhiteToMove()) {
            requestBotMove();
        } else {
            gameState.startTurnTimer();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Bot move
    // ──────────────────────────────────────────────────────────────────
    private void requestBotMove() {
        if (gameState == null || gameState.isGameOver()) return;

        engineThinkingLD.setValue(true);
        statusLD.setValue("Bot is thinking…");

        String fen = gameState.getBoard().toFEN();
        int[] params = EngineBridge.getDifficultyParams(
                gameState.getDifficulty().name());
        int depth = params[0];
        int skill = params[1];

        executor.execute(() -> {
            try {
                Log.d(TAG, "Requesting bot move for FEN: " + fen);
                String uci = EngineBridge.getInstance().getBestMove(fen, depth, skill);
                
                if (uci == null || uci.isEmpty()) {
                    Log.w(TAG, "Engine returned no move (null or empty)");
                    toastLD.postValue("Engine returned no move");
                    statusLD.postValue("Engine Error");
                    engineThinkingLD.postValue(false);
                    return;
                }

                Log.d(TAG, "Engine returned UCI: " + uci);
                Move move = parseBotMove(uci);
                
                String timeStr = gameState.getBlackTimeDisplay();
                ChessBoard board = gameState.getBoard();

                if (move == null) {
                    Log.w(TAG, "Engine suggested illegal/unparseable move: " + uci + ". Falling back to random legal move.");
                    synchronized (board) {
                        java.util.List<Move> allLegal = board.getAllLegalMoves();
                        if (!allLegal.isEmpty()) {
                            move = allLegal.get((int) (Math.random() * allLegal.size()));
                            Log.i(TAG, "Fallback move selected: " + move.toUCI());
                        } else {
                            Log.e(TAG, "No legal moves available for fallback!");
                            engineThinkingLD.postValue(false);
                            return;
                        }
                    }
                }

                // Add artificial delay for the user to process the previous state
                Thread.sleep(1500);

                synchronized (board) {
                    if (gameState.isGameOver() || board.isWhiteToMove()) {
                        Log.w(TAG, "Bot move aborted: Board state changed during delay");
                        return;
                    }

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
                        gameOverLD.postValue(true);
                    } else {
                        gameState.startTurnTimer();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Bot move error", e);
                toastLD.postValue("Engine error: " + e.getMessage());
            } finally {
                engineThinkingLD.postValue(false);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────
    //  Parse UCI move string  (e.g. "e2e4", "e7e8q")
    // ──────────────────────────────────────────────────────────────────
    private Move parseBotMove(String uci) {
        if (uci == null || uci.length() < 4) return null;
        try {
            int fromCol = uci.charAt(0) - 'a';
            int fromRow = 8 - (uci.charAt(1) - '0');
            int toCol   = uci.charAt(2) - 'a';
            int toRow   = 8 - (uci.charAt(3) - '0');

            Move move = new Move(fromRow, fromCol, toRow, toCol);

            // Promotion
            if (uci.length() == 5) {
                switch (uci.charAt(4)) {
                    case 'q': move.promotion = ChessPiece.Type.QUEEN;  break;
                    case 'r': move.promotion = ChessPiece.Type.ROOK;   break;
                    case 'b': move.promotion = ChessPiece.Type.BISHOP; break;
                    case 'n': move.promotion = ChessPiece.Type.KNIGHT; break;
                }
            }

            // Detect castling (king moves 2 squares)
            ChessBoard board = gameState.getBoard();
            ChessPiece pc = board.getPieceAt(fromRow, fromCol);
            if (pc == null) {
                Log.e(TAG, "Illegal bot move: No piece at " + uci.substring(0, 2));
                return null;
            }

            if (pc.getType() == ChessPiece.Type.KING && Math.abs(fromCol - toCol) == 2) {
                move.wasCastling = true;
            }

            // Detect en passant (pawn moves diagonally to empty square)
            if (pc.getType() == ChessPiece.Type.PAWN && fromCol != toCol && board.getPieceAt(toRow, toCol) == null) {
                move.wasEnPassant = true;
            }
            
            // Validate move legality
            if (!board.isLegal(move)) {
                Log.e(TAG, "Illegal bot move suggested: " + uci);
                return null;
            }
            return move;
        } catch (Exception e) {
            Log.e(TAG, "parseBotMove failed for: " + uci, e);
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Undo
    // ──────────────────────────────────────────────────────────────────
    public void undoMove() {
        if (gameState == null || !gameState.getBoard().canUndo()) return;

        ChessBoard board = gameState.getBoard();

        synchronized (board) {
            // In PvB undo both bot and human moves to keep human as white
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

    private boolean isPaused = false;

    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
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

    // ── LiveData getters ──────────────────────────────────────────────
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
        if (current == null || current.isEmpty()) {
            gameLogLD.setValue(entry);
        } else {
            gameLogLD.setValue(current + "\n" + entry);
        }
    }

    private void addToLogPost(String entry) {
        String current = gameLogLD.getValue();
        if (current == null || current.isEmpty()) {
            gameLogLD.postValue(entry);
        } else {
            gameLogLD.postValue(current + "\n" + entry);
        }
    }

    public void resign() {
        if (gameState == null || gameState.isGameOver()) return;
        
        String winner = gameState.getBoard().isWhiteToMove() ? 
            gameState.getPlayer2Name() : gameState.getPlayer1Name();
        if (winner == null || winner.isEmpty()) {
            winner = gameState.getBoard().isWhiteToMove() ? "Black" : "White";
        }
        
        String msg = "Resignation. " + winner + " wins!";
        gameState.setGameOver(true, msg);
        
        gameOverLD.setValue(true);
        statusLD.setValue(msg);
        addToLog("Game ended by resignation.");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        timerHandler.removeCallbacks(timerRunnable);
        executor.shutdown();
        if (gameState != null && gameState.getMode() == GameState.GameMode.PVB) {
            EngineBridge.getInstance().stopEngine();
        }
    }
}
