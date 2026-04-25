package com.chessapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Complete chess board with full rule enforcement:
 *  - All piece moves including sliding pieces
 *  - Castling (king-side and queen-side)
 *  - En passant
 *  - Pawn promotion
 *  - Check / checkmate / stalemate detection
 *  - Undo via move history stack
 *  - FEN string generation for Stockfish
 */
public class ChessBoard {

    // board[row][col]: row 0 = rank 8 (black back rank), row 7 = rank 1 (white back rank)
    private final ChessPiece[][] board = new ChessPiece[8][8];

    private boolean whiteToMove = true;

    // Castling rights: [0]=white-K, [1]=white-Q, [2]=black-k, [3]=black-q
    private boolean[] castlingRights = new boolean[4];

    // En passant target square (-1 if none)
    private int epRow = -1;
    private int epCol = -1;

    private int halfMoveClock = 0;
    private int fullMoveNumber = 1;

    private final Stack<Move> history = new Stack<>();

    // ──────────────────────────────────────────────────────────────────
    //  Construction / reset
    // ──────────────────────────────────────────────────────────────────
    public ChessBoard() { reset(); }

    /** Restore to standard start position. */
    public void reset() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = null;

        history.clear();
        whiteToMove    = true;
        halfMoveClock  = 0;
        fullMoveNumber = 1;
        epRow = epCol  = -1;
        for (int i = 0; i < 4; i++) castlingRights[i] = true;

        // Pawns
        for (int c = 0; c < 8; c++) {
            board[1][c] = new ChessPiece(ChessPiece.Type.PAWN, ChessPiece.Color.BLACK);
            board[6][c] = new ChessPiece(ChessPiece.Type.PAWN, ChessPiece.Color.WHITE);
        }

        // Back ranks
        ChessPiece.Type[] back = {
            ChessPiece.Type.ROOK,   ChessPiece.Type.KNIGHT, ChessPiece.Type.BISHOP,
            ChessPiece.Type.QUEEN,  ChessPiece.Type.KING,
            ChessPiece.Type.BISHOP, ChessPiece.Type.KNIGHT, ChessPiece.Type.ROOK
        };
        for (int c = 0; c < 8; c++) {
            board[0][c] = new ChessPiece(back[c], ChessPiece.Color.BLACK);
            board[7][c] = new ChessPiece(back[c], ChessPiece.Color.WHITE);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Public accessors
    // ──────────────────────────────────────────────────────────────────
    public ChessPiece getPieceAt(int row, int col) {
        return valid(row, col) ? board[row][col] : null;
    }
    public boolean isWhiteToMove() { return whiteToMove; }
    public int     getMoveCount()  { return history.size(); }
    public boolean canUndo()       { return !history.isEmpty(); }

    // ──────────────────────────────────────────────────────────────────
    //  Legal move generation
    // ──────────────────────────────────────────────────────────────────
    /** Returns all fully-legal moves for the piece at (row,col). */
    public List<Move> getLegalMoves(int row, int col) {
        ChessPiece pc = board[row][col];
        if (pc == null) return new ArrayList<>();
        ChessPiece.Color side = whiteToMove ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        if (pc.getColor() != side) return new ArrayList<>();

        List<Move> pseudo = generatePseudo(row, col);
        List<Move> legal  = new ArrayList<>(pseudo.size());
        for (Move m : pseudo) {
            if (isLegal(m)) legal.add(m);
        }
        return legal;
    }

    /** All legal moves for the current player (used for checkmate / stalemate). */
    public List<Move> getAllLegalMoves() {
        List<Move> all = new ArrayList<>();
        ChessPiece.Color side = whiteToMove ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p != null && p.getColor() == side)
                    all.addAll(getLegalMoves(r, c));
            }
        return all;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Pseudo-legal generation (ignores whether own king is in check)
    // ──────────────────────────────────────────────────────────────────
    private List<Move> generatePseudo(int row, int col) {
        List<Move> moves = new ArrayList<>();
        ChessPiece pc = board[row][col];
        if (pc == null) return moves;
        switch (pc.getType()) {
            case PAWN:   pawnMoves  (row, col, moves); break;
            case KNIGHT: knightMoves(row, col, moves); break;
            case BISHOP: slideMoves (row, col, moves, true,  false); break;
            case ROOK:   slideMoves (row, col, moves, false, true);  break;
            case QUEEN:  slideMoves (row, col, moves, true,  true);  break;
            case KING:   kingMoves  (row, col, moves); break;
        }
        return moves;
    }

    private void pawnMoves(int row, int col, List<Move> moves) {
        ChessPiece.Color color = board[row][col].getColor();
        int dir      = (color == ChessPiece.Color.WHITE) ? -1 : 1;
        int startRow = (color == ChessPiece.Color.WHITE) ?  6 : 1;
        int promRow  = (color == ChessPiece.Color.WHITE) ?  0 : 7;

        // Single push
        int nr = row + dir;
        if (valid(nr, col) && board[nr][col] == null) {
            addPawnMove(row, col, nr, col, promRow, moves);
            // Double push from start
            int nr2 = row + 2 * dir;
            if (row == startRow && valid(nr2, col) && board[nr2][col] == null) {
                moves.add(new Move(row, col, nr2, col));
            }
        }

        // Captures (including en-passant)
        for (int dc : new int[]{-1, 1}) {
            int nc = col + dc;
            if (!valid(nr, nc)) continue;
            ChessPiece target = board[nr][nc];
            if (target != null && target.getColor() != color) {
                addPawnMove(row, col, nr, nc, promRow, moves);
            } else if (nr == epRow && nc == epCol) {
                Move ep = new Move(row, col, nr, nc);
                ep.wasEnPassant = true;
                moves.add(ep);
            }
        }
    }

    private void addPawnMove(int fr, int fc, int tr, int tc,
                              int promRow, List<Move> moves) {
        if (tr == promRow) {
            for (ChessPiece.Type pt : new ChessPiece.Type[]{
                    ChessPiece.Type.QUEEN, ChessPiece.Type.ROOK,
                    ChessPiece.Type.BISHOP, ChessPiece.Type.KNIGHT}) {
                Move m = new Move(fr, fc, tr, tc);
                m.promotion = pt;
                moves.add(m);
            }
        } else {
            moves.add(new Move(fr, fc, tr, tc));
        }
    }

    private void knightMoves(int row, int col, List<Move> moves) {
        int[][] offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        ChessPiece.Color color = board[row][col].getColor();
        for (int[] d : offsets) {
            int r = row + d[0], c = col + d[1];
            if (valid(r, c)) {
                ChessPiece t = board[r][c];
                if (t == null || t.getColor() != color)
                    moves.add(new Move(row, col, r, c));
            }
        }
    }

    private void slideMoves(int row, int col, List<Move> moves,
                             boolean diag, boolean straight) {
        ChessPiece.Color color = board[row][col].getColor();
        int[][] dirs = diag && straight
                ? new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}
                : diag
                    ? new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}
                    : new int[][]{{-1,0},{0,-1},{0,1},{1,0}};
        for (int[] d : dirs) {
            int r = row + d[0], c = col + d[1];
            while (valid(r, c)) {
                ChessPiece t = board[r][c];
                if (t == null) {
                    moves.add(new Move(row, col, r, c));
                } else {
                    if (t.getColor() != color) moves.add(new Move(row, col, r, c));
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
    }

    private void kingMoves(int row, int col, List<Move> moves) {
        ChessPiece.Color color = board[row][col].getColor();
        int[][] offs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] d : offs) {
            int r = row + d[0], c = col + d[1];
            if (valid(r, c)) {
                ChessPiece t = board[r][c];
                if (t == null || t.getColor() != color)
                    moves.add(new Move(row, col, r, c));
            }
        }
        // Castling
        boolean isWhite = (color == ChessPiece.Color.WHITE);
        int castleRow   = isWhite ? 7 : 0;
        boolean attackByOpp = !isWhite; // we check if opponent attacks squares

        if (row == castleRow && col == 4 && !isAttacked(castleRow, 4, attackByOpp)) {
            // Kingside
            int ksIdx = isWhite ? 0 : 2;
            if (castlingRights[ksIdx]
                    && board[castleRow][5] == null && board[castleRow][6] == null
                    && !isAttacked(castleRow, 5, attackByOpp)
                    && !isAttacked(castleRow, 6, attackByOpp)) {
                Move m = new Move(row, col, row, 6);
                m.wasCastling = true;
                moves.add(m);
            }
            // Queenside
            int qsIdx = isWhite ? 1 : 3;
            if (castlingRights[qsIdx]
                    && board[castleRow][3] == null
                    && board[castleRow][2] == null
                    && board[castleRow][1] == null
                    && !isAttacked(castleRow, 3, attackByOpp)
                    && !isAttacked(castleRow, 2, attackByOpp)) {
                Move m = new Move(row, col, row, 2);
                m.wasCastling = true;
                moves.add(m);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Attack detection
    // ──────────────────────────────────────────────────────────────────
    /** Is square (row,col) attacked by the given side? */
    public boolean isAttacked(int row, int col, boolean byWhite) {
        ChessPiece.Color atk = byWhite ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;

        // Pawn attacks
        int pd = byWhite ? 1 : -1;
        for (int dc : new int[]{-1, 1}) {
            int pr = row + pd, pc2 = col + dc;
            if (valid(pr, pc2)) {
                ChessPiece p = board[pr][pc2];
                if (p != null && p.getType() == ChessPiece.Type.PAWN && p.getColor() == atk)
                    return true;
            }
        }
        // Knight
        for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int r = row+d[0], c = col+d[1];
            if (valid(r,c)) {
                ChessPiece p = board[r][c];
                if (p != null && p.getColor() == atk && p.getType() == ChessPiece.Type.KNIGHT)
                    return true;
            }
        }
        // Bishop / Queen diagonal
        for (int[] d : new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}) {
            int r = row+d[0], c = col+d[1];
            while (valid(r,c)) {
                ChessPiece p = board[r][c];
                if (p != null) {
                    if (p.getColor() == atk &&
                        (p.getType() == ChessPiece.Type.BISHOP || p.getType() == ChessPiece.Type.QUEEN))
                        return true;
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        // Rook / Queen straight
        for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            int r = row+d[0], c = col+d[1];
            while (valid(r,c)) {
                ChessPiece p = board[r][c];
                if (p != null) {
                    if (p.getColor() == atk &&
                        (p.getType() == ChessPiece.Type.ROOK || p.getType() == ChessPiece.Type.QUEEN))
                        return true;
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        // King
        for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int r = row+d[0], c = col+d[1];
            if (valid(r,c)) {
                ChessPiece p = board[r][c];
                if (p != null && p.getColor() == atk && p.getType() == ChessPiece.Type.KING)
                    return true;
            }
        }
        return false;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Legality check (make/unmake)
    // ──────────────────────────────────────────────────────────────────
    private boolean isLegal(Move move) {
        ChessPiece pc = board[move.fromRow][move.fromCol];
        if (pc == null) return false;

        // Snapshot board for undo
        ChessPiece captured = board[move.toRow][move.toCol];

        // Execute on board
        board[move.toRow][move.toCol] = pc;
        board[move.fromRow][move.fromCol] = null;

        int epCapRow = -1, epCapCol = -1;
        if (move.wasEnPassant) {
            int d = (pc.getColor() == ChessPiece.Color.WHITE) ? 1 : -1;
            epCapRow = move.toRow + d;
            epCapCol = move.toCol;
            captured = board[epCapRow][epCapCol];
            board[epCapRow][epCapCol] = null;
        }

        // Castling: also move rook temporarily
        if (move.wasCastling) {
            int cr = move.fromRow;
            if (move.toCol == 6) { board[cr][5] = board[cr][7]; board[cr][7] = null; }
            else                 { board[cr][3] = board[cr][0]; board[cr][0] = null; }
        }

        // Find own king
        boolean isWhite = (pc.getColor() == ChessPiece.Color.WHITE);
        int kr = -1, kc = -1;
        outer:
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p != null && p.getType() == ChessPiece.Type.KING
                               && p.getColor() == pc.getColor()) {
                    kr = r; kc = c; break outer;
                }
            }

        boolean legal = (kr >= 0) && !isAttacked(kr, kc, !isWhite);

        // Undo
        board[move.fromRow][move.fromCol] = pc;
        board[move.toRow][move.toCol] = move.wasEnPassant ? null : captured;
        if (move.wasEnPassant && epCapRow >= 0) board[epCapRow][epCapCol] = captured;
        if (move.wasCastling) {
            int cr = move.fromRow;
            if (move.toCol == 6) { board[cr][7] = board[cr][5]; board[cr][5] = null; }
            else                 { board[cr][0] = board[cr][3]; board[cr][3] = null; }
        }
        return legal;
    }

    // ──────────────────────────────────────────────────────────────────
    //  makeMove / undoMove
    // ──────────────────────────────────────────────────────────────────
    public void makeMove(Move move) {
        ChessPiece pc = board[move.fromRow][move.fromCol];

        // Save undo state into the move itself
        move.capturedPiece      = board[move.toRow][move.toCol];
        move.prevCastlingRights = castlingRights.clone();
        move.prevEpRow          = epRow;
        move.prevEpCol          = epCol;
        move.prevHalfMoveClock  = halfMoveClock;

        // Basic move
        board[move.toRow][move.toCol]     = pc;
        board[move.fromRow][move.fromCol] = null;

        // En passant capture
        if (move.wasEnPassant) {
            int d = (pc.getColor() == ChessPiece.Color.WHITE) ? 1 : -1;
            int capR = move.toRow + d;
            move.capturedPiece = board[capR][move.toCol];
            board[capR][move.toCol] = null;
        }

        // Castling: move the rook
        if (move.wasCastling) {
            int cr = move.fromRow;
            if (move.toCol == 6) { board[cr][5] = board[cr][7]; board[cr][7] = null; }
            else                 { board[cr][3] = board[cr][0]; board[cr][0] = null; }
        }

        // Promotion
        if (move.promotion != null) {
            board[move.toRow][move.toCol] = new ChessPiece(move.promotion, pc.getColor());
        }

        // Update en passant square
        epRow = epCol = -1;
        if (pc.getType() == ChessPiece.Type.PAWN
                && Math.abs(move.toRow - move.fromRow) == 2) {
            epRow = (move.fromRow + move.toRow) / 2;
            epCol = move.fromCol;
        }

        // Update castling rights
        updateCastlingRights(move, pc);

        // Half-move clock
        halfMoveClock = (pc.getType() == ChessPiece.Type.PAWN
                        || move.capturedPiece != null) ? 0 : halfMoveClock + 1;

        if (!whiteToMove) fullMoveNumber++;
        whiteToMove = !whiteToMove;
        history.push(move);
    }

    private void updateCastlingRights(Move move, ChessPiece pc) {
        if (pc.getType() == ChessPiece.Type.KING) {
            if (pc.getColor() == ChessPiece.Color.WHITE) { castlingRights[0] = castlingRights[1] = false; }
            else                                          { castlingRights[2] = castlingRights[3] = false; }
        }
        if (pc.getType() == ChessPiece.Type.ROOK) {
            if (move.fromRow == 7 && move.fromCol == 7) castlingRights[0] = false;
            if (move.fromRow == 7 && move.fromCol == 0) castlingRights[1] = false;
            if (move.fromRow == 0 && move.fromCol == 7) castlingRights[2] = false;
            if (move.fromRow == 0 && move.fromCol == 0) castlingRights[3] = false;
        }
        // Also revoke if a rook's home square is captured
        if (move.toRow == 7 && move.toCol == 7) castlingRights[0] = false;
        if (move.toRow == 7 && move.toCol == 0) castlingRights[1] = false;
        if (move.toRow == 0 && move.toCol == 7) castlingRights[2] = false;
        if (move.toRow == 0 && move.toCol == 0) castlingRights[3] = false;
    }

    public Move undoMove() {
        if (history.isEmpty()) return null;
        Move move = history.pop();
        ChessPiece pc = board[move.toRow][move.toCol];

        // Restore castling rights / ep / clock
        castlingRights = move.prevCastlingRights;
        epRow = move.prevEpRow;
        epCol = move.prevEpCol;
        halfMoveClock = move.prevHalfMoveClock;

        // Undo promotion
        if (move.promotion != null) {
            pc = new ChessPiece(ChessPiece.Type.PAWN, pc.getColor());
        }

        board[move.fromRow][move.fromCol] = pc;
        board[move.toRow][move.toCol] = move.wasEnPassant ? null : move.capturedPiece;

        if (move.wasEnPassant) {
            int d = (pc.getColor() == ChessPiece.Color.WHITE) ? 1 : -1;
            board[move.toRow + d][move.toCol] = move.capturedPiece;
        }

        if (move.wasCastling) {
            int cr = move.fromRow;
            if (move.toCol == 6) { board[cr][7] = board[cr][5]; board[cr][5] = null; }
            else                 { board[cr][0] = board[cr][3]; board[cr][3] = null; }
        }

        whiteToMove = !whiteToMove;
        if (whiteToMove) fullMoveNumber--;
        return move;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Game-state queries
    // ──────────────────────────────────────────────────────────────────
    public boolean isInCheck() {
        ChessPiece.Color side = whiteToMove ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p != null && p.getType() == ChessPiece.Type.KING
                               && p.getColor() == side)
                    return isAttacked(r, c, !whiteToMove);
            }
        return false;
    }

    public boolean isCheckmate()  { return isInCheck() && getAllLegalMoves().isEmpty(); }
    public boolean isStalemate()  { return !isInCheck() && getAllLegalMoves().isEmpty(); }
    public boolean isGameOver()   { return isCheckmate() || isStalemate(); }

    public String getGameStatus() {
        if (isCheckmate()) {
            return (whiteToMove ? "Black" : "White") + " wins by checkmate!";
        }
        if (isStalemate()) return "Draw by stalemate!";
        if (halfMoveClock >= 100) return "Draw by 50-move rule!";
        if (isInCheck())   return (whiteToMove ? "White" : "Black") + " is in check!";
        return (whiteToMove ? "White" : "Black") + "'s turn";
    }

    // ──────────────────────────────────────────────────────────────────
    //  FEN generation
    // ──────────────────────────────────────────────────────────────────
    public String toFEN() {
        StringBuilder fen = new StringBuilder(80);
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p == null) { empty++; }
                else {
                    if (empty > 0) { fen.append(empty); empty = 0; }
                    fen.append(p.toFENChar());
                }
            }
            if (empty > 0) fen.append(empty);
            if (r < 7) fen.append('/');
        }
        fen.append(whiteToMove ? " w " : " b ");

        StringBuilder cr = new StringBuilder();
        if (castlingRights[0]) cr.append('K');
        if (castlingRights[1]) cr.append('Q');
        if (castlingRights[2]) cr.append('k');
        if (castlingRights[3]) cr.append('q');
        fen.append(cr.length() == 0 ? "-" : cr.toString());

        if (epRow >= 0) {
            fen.append(' ').append((char)('a' + epCol)).append(8 - epRow);
        } else {
            fen.append(" -");
        }
        fen.append(' ').append(halfMoveClock).append(' ').append(fullMoveNumber);
        return fen.toString();
    }

    // ──────────────────────────────────────────────────────────────────
    //  Utilities
    // ──────────────────────────────────────────────────────────────────
    private static boolean valid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}
