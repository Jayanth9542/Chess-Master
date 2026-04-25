package com.chessapp.model;

/**
 * Represents a chess move from one square to another.
 * Also carries undo-state so ChessBoard can reverse the move exactly.
 */
public class Move {

    // ── Core move data ────────────────────────────────────────────────
    public final int fromRow;
    public final int fromCol;
    public final int toRow;
    public final int toCol;

    /** Non-null only for pawn promotions. */
    public ChessPiece.Type promotion = null;

    // ── Flags (set during move generation) ───────────────────────────
    public boolean wasCastling   = false;
    public boolean wasEnPassant  = false;

    // ── Undo state (filled in by ChessBoard.makeMove) ─────────────────
    public ChessPiece capturedPiece      = null;
    public boolean[]  prevCastlingRights = null;
    public int        prevEpRow          = -1;
    public int        prevEpCol          = -1;
    public int        prevHalfMoveClock  = 0;

    // ─────────────────────────────────────────────────────────────────
    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow   = toRow;
        this.toCol   = toCol;
    }

    /**
     * Convert to UCI notation (e.g. "e2e4", "e7e8q").
     * Row 0 = rank 8, row 7 = rank 1.
     */
    public String toUCI() {
        char fromFile = (char)('a' + fromCol);
        char toFile   = (char)('a' + toCol);
        int  fromRank = 8 - fromRow;
        int  toRank   = 8 - toRow;

        StringBuilder sb = new StringBuilder(5);
        sb.append(fromFile).append(fromRank)
          .append(toFile).append(toRank);

        if (promotion != null) {
            switch (promotion) {
                case QUEEN:  sb.append('q'); break;
                case ROOK:   sb.append('r'); break;
                case BISHOP: sb.append('b'); break;
                case KNIGHT: sb.append('n'); break;
                default: break;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return fromRow == m.fromRow && fromCol == m.fromCol
            && toRow   == m.toRow   && toCol   == m.toCol;
    }

    @Override
    public int hashCode() {
        return fromRow * 1000 + fromCol * 100 + toRow * 10 + toCol;
    }

    @Override
    public String toString() {
        return toUCI();
    }
}
