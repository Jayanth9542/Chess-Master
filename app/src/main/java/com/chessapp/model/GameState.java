package com.chessapp.model;

/**
 * Holds all runtime state for a single game session.
 */
public class GameState {

    public enum GameMode   { PVP, PVB }
    
    // 6-Tier Difficulty System
    public enum Difficulty { 
        BEGINNER, 
        INTERMEDIATE, 
        ADVANCED, 
        EXPERT, 
        GRANDMASTER, 
        MAGNUS 
    }

    private final GameMode   mode;
    private final String     player1Name;
    private final String     player2Name;
    private final Difficulty difficulty;
    private final ChessBoard board;

    private boolean gameOver = false;
    private String  result   = "";

    private long whiteElapsedMs = 0;
    private long blackElapsedMs = 0;
    private long timerStartMs   = 0;

    public GameState(GameMode mode, String player1Name, String player2Name,
                     Difficulty difficulty) {
        this.mode       = mode;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.difficulty = difficulty;
        this.board      = new ChessBoard();
    }

    public GameMode   getMode()        { return mode; }
    public String     getPlayer1Name() { return player1Name; }
    public String     getPlayer2Name() { return player2Name; }
    public Difficulty getDifficulty()  { return difficulty; }
    public ChessBoard getBoard()       { return board; }
    public boolean    isGameOver()     { return gameOver; }
    public String     getResult()      { return result; }

    public String getCurrentPlayerName() {
        return board.isWhiteToMove() ? player1Name : player2Name;
    }

    public void setGameOver(boolean over) {
        this.gameOver = over;
        if (over) this.result = board.getGameStatus();
    }

    public void setGameOver(boolean over, String customResult) {
        this.gameOver = over;
        this.result = customResult;
    }

    public void startTurnTimer() { timerStartMs = System.currentTimeMillis(); }

    public long getWhiteElapsedMs() { return whiteElapsedMs; }
    public long getBlackElapsedMs() { return blackElapsedMs; }

    public long getCurrentTurnElapsedMs() {
        if (timerStartMs == 0 || gameOver) return 0;
        return System.currentTimeMillis() - timerStartMs;
    }

    public String getWhiteTimeDisplay() {
        long total = whiteElapsedMs + (board.isWhiteToMove() ? getCurrentTurnElapsedMs() : 0);
        return formatMs(total);
    }

    public String getBlackTimeDisplay() {
        long total = blackElapsedMs + (!board.isWhiteToMove() ? getCurrentTurnElapsedMs() : 0);
        return formatMs(total);
    }

    public void pauseTurnTimer() {
        if (timerStartMs == 0) return;
        long elapsed = System.currentTimeMillis() - timerStartMs;
        if (board.isWhiteToMove()) whiteElapsedMs += elapsed;
        else                       blackElapsedMs += elapsed;
        timerStartMs = 0;
    }

    private static String formatMs(long ms) {
        long seconds = ms / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
