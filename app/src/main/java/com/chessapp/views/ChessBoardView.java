package com.chessapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.chessapp.model.ChessBoard;
import com.chessapp.model.ChessPiece;
import com.chessapp.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Fully custom chess board View drawn on Canvas.
 *
 * Features:
 *  - 8×8 alternating light/dark squares
 *  - Piece Unicode symbols scaled to cell size
 *  - Touch selection with blue highlight ring
 *  - Green dot highlights for legal destination squares
 *  - Red tint on the king's square when in check
 *  - Rank/file labels (a-h, 1-8)
 *  - Callbacks via OnMoveSelectedListener
 */
public class ChessBoardView extends View {

    // ── Colours ───────────────────────────────────────────────────────
    private static final int CLR_LIGHT       = 0xFFF0D9B5;  // cream
    private static final int CLR_DARK        = 0xFFB58863;  // brown
    private static final int CLR_SELECTED    = 0xFF7FC97F;  // green tint on selected square
    private static final int CLR_LEGAL_DOT   = 0x8800AA00;  // translucent green dot
    private static final int CLR_CHECK       = 0xAAFF3333;  // red tint for king in check
    private static final int CLR_LAST_MOVE   = 0x55FFFF00;  // yellow tint for last move squares
    private static final int CLR_LABEL       = 0xFF8B6040;
    private static final int CLR_WHITE_PIECE = 0xFFFFFFFF;
    private static final int CLR_BLACK_PIECE = 0xFF1A1A1A;
    private static final int CLR_PIECE_SHADOW= 0x44000000;

    // ── Paints ────────────────────────────────────────────────────────
    private final Paint squarePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────
    private ChessBoard board;
    private int  selectedRow = -1;
    private int  selectedCol = -1;
    private List<Move>  legalMoves      = new ArrayList<>();
    private Move        lastMove        = null;
    private boolean     flipped         = false;   // flip for black's perspective
    private boolean     engineThinking  = false;

    private float cellSize;
    private float boardOffset; // small inset for labels

    public interface OnMoveSelectedListener {
        void onMoveSelected(Move move);
        void onPromotionRequired(Move move, ChessPiece.Color color);
    }
    private OnMoveSelectedListener moveListener;

    private boolean showLegalMoves = true;

    // ─────────────────────────────────────────────────────────────────
    public ChessBoardView(Context ctx)                 { super(ctx);      init(); }
    public ChessBoardView(Context ctx, AttributeSet a) { super(ctx, a);   init(); }
    public ChessBoardView(Context ctx, AttributeSet a, int def) {
        super(ctx, a, def); init();
    }

    private void init() {
        piecePaint.setTextAlign(Paint.Align.CENTER);
        piecePaint.setTypeface(Typeface.DEFAULT_BOLD);

        shadowPaint.setColor(CLR_PIECE_SHADOW);
        shadowPaint.setTextAlign(Paint.Align.CENTER);
        shadowPaint.setTypeface(Typeface.DEFAULT_BOLD);

        labelPaint.setColor(CLR_LABEL);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        dotPaint.setColor(CLR_LEGAL_DOT);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Measure: always square
    // ──────────────────────────────────────────────────────────────────
    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int size = Math.min(
            MeasureSpec.getSize(wSpec),
            MeasureSpec.getSize(hSpec)
        );
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        boardOffset = w * 0.04f;
        cellSize    = (w - 2 * boardOffset) / 8f;

        float labelSz = boardOffset * 0.65f;
        labelPaint.setTextSize(labelSz);

        float pieceSz = cellSize * 0.72f;
        piecePaint.setTextSize(pieceSz);
        shadowPaint.setTextSize(pieceSz);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Draw
    // ──────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        if (board == null) return;
        drawSquares(canvas);
        drawLabels(canvas);
        drawPieces(canvas);
    }

    private void drawSquares(Canvas canvas) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int drawR = flipped ? 7 - r : r;
                int drawC = flipped ? 7 - c : c;
                float x = boardOffset + drawC * cellSize;
                float y = boardOffset + drawR * cellSize;

                // Base square colour
                squarePaint.setColor(((r + c) % 2 == 0) ? CLR_LIGHT : CLR_DARK);
                canvas.drawRect(x, y, x + cellSize, y + cellSize, squarePaint);

                // Last move highlight
                if (lastMove != null &&
                        ((r == lastMove.fromRow && c == lastMove.fromCol) ||
                         (r == lastMove.toRow   && c == lastMove.toCol))) {
                    overlayPaint.setColor(CLR_LAST_MOVE);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, overlayPaint);
                }

                // Selected square
                if (r == selectedRow && c == selectedCol) {
                    overlayPaint.setColor(CLR_SELECTED);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, overlayPaint);
                }

                // Check highlight on king
                if (board.isInCheck()) {
                    ChessPiece p = board.getPieceAt(r, c);
                    if (p != null && p.getType() == ChessPiece.Type.KING
                            && p.getColor() == (board.isWhiteToMove()
                                ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK)) {
                        overlayPaint.setColor(CLR_CHECK);
                        canvas.drawRect(x, y, x + cellSize, y + cellSize, overlayPaint);
                    }
                }

                // Legal move dots
                if (showLegalMoves) {
                    boolean isLegal = false;
                    for (Move m : legalMoves) {
                        if (m.toRow == r && m.toCol == c) { isLegal = true; break; }
                    }
                    if (isLegal) {
                        float cx = x + cellSize / 2f;
                        float cy = y + cellSize / 2f;
                        ChessPiece target = board.getPieceAt(r, c);
                        if (target != null) {
                            // Draw ring for capture
                            dotPaint.setStyle(Paint.Style.STROKE);
                            dotPaint.setStrokeWidth(cellSize * 0.08f);
                            canvas.drawCircle(cx, cy, cellSize * 0.45f, dotPaint);
                            dotPaint.setStyle(Paint.Style.FILL);
                        } else {
                            canvas.drawCircle(cx, cy, cellSize * 0.16f, dotPaint);
                        }
                    }
                }
            }
        }
    }

    private void drawLabels(Canvas canvas) {
        float half = cellSize / 2f;
        for (int i = 0; i < 8; i++) {
            int di = flipped ? 7 - i : i;
            // Rank numbers (left edge)
            String rank = String.valueOf(8 - i);
            canvas.drawText(rank,
                boardOffset / 2f,
                boardOffset + di * cellSize + half + labelPaint.getTextSize() / 3f,
                labelPaint);
            // File letters (bottom edge)
            String file = String.valueOf((char)('a' + i));
            canvas.drawText(file,
                boardOffset + di * cellSize + half,
                boardOffset + 8 * cellSize + boardOffset * 0.75f,
                labelPaint);
        }
    }

    private void drawPieces(Canvas canvas) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board.getPieceAt(r, c);
                if (p == null) continue;

                int drawR = flipped ? 7 - r : r;
                int drawC = flipped ? 7 - c : c;
                float cx = boardOffset + drawC * cellSize + cellSize / 2f;
                float cy = boardOffset + drawR * cellSize + cellSize * 0.72f;

                String sym = p.getUnicodeSymbol();

                // Shadow (slightly offset)
                canvas.drawText(sym, cx + cellSize * 0.025f, cy + cellSize * 0.025f, shadowPaint);

                // Piece
                piecePaint.setColor(p.getColor() == ChessPiece.Color.WHITE
                    ? CLR_WHITE_PIECE : CLR_BLACK_PIECE);
                canvas.drawText(sym, cx, cy, piecePaint);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Touch
    // ──────────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (board == null || engineThinking) return true;

        float x = event.getX() - boardOffset;
        float y = event.getY() - boardOffset;
        if (x < 0 || y < 0) return true;

        int tapC = (int)(x / cellSize);
        int tapR = (int)(y / cellSize);
        if (tapR < 0 || tapR > 7 || tapC < 0 || tapC > 7) return true;

        int boardR = flipped ? 7 - tapR : tapR;
        int boardC = flipped ? 7 - tapC : tapC;

        handleTap(boardR, boardC);
        return true;
    }

    private void handleTap(int row, int col) {
        if (selectedRow < 0) {
            // First tap: select piece
            ChessPiece pc = board.getPieceAt(row, col);
            if (pc == null) return;
            ChessPiece.Color turn = board.isWhiteToMove()
                ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
            if (pc.getColor() != turn) return;

            selectedRow = row;
            selectedCol = col;
            legalMoves  = board.getLegalMoves(row, col);
            invalidate();
        } else {
            // Second tap: attempt move
            Move chosen = null;
            for (Move m : legalMoves) {
                if (m.toRow == row && m.toCol == col) { chosen = m; break; }
            }

            if (chosen != null) {
                // Check for pawn promotion
                ChessPiece pc = board.getPieceAt(chosen.fromRow, chosen.fromCol);
                if (pc != null && pc.getType() == ChessPiece.Type.PAWN &&
                        (chosen.toRow == 0 || chosen.toRow == 7)) {
                    if (moveListener != null) {
                        moveListener.onPromotionRequired(chosen, pc.getColor());
                    }
                } else {
                    lastMove = chosen;
                    if (moveListener != null) moveListener.onMoveSelected(chosen);
                }
                selectedRow = selectedCol = -1;
                legalMoves = new ArrayList<>();
                invalidate();
            } else {
                // Re-select if tapping own piece
                ChessPiece pc = board.getPieceAt(row, col);
                ChessPiece.Color turn = board.isWhiteToMove()
                    ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
                if (pc != null && pc.getColor() == turn) {
                    selectedRow = row;
                    selectedCol = col;
                    legalMoves  = board.getLegalMoves(row, col);
                } else {
                    selectedRow = selectedCol = -1;
                    legalMoves  = new ArrayList<>();
                }
                invalidate();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────
    public void setBoard(ChessBoard board) {
        this.board = board;
        selectedRow = selectedCol = -1;
        legalMoves  = new ArrayList<>();
        invalidate();
    }

    public void setEngineThinking(boolean thinking) {
        this.engineThinking = thinking;
        invalidate();
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    public boolean isFlipped() { return flipped; }

    public void setShowLegalMoves(boolean show) {
        this.showLegalMoves = show;
        invalidate();
    }

    public boolean isShowingLegalMoves() { return showLegalMoves; }

    public void setOnMoveSelectedListener(OnMoveSelectedListener l) {
        moveListener = l;
    }

    public void clearSelection() {
        selectedRow = selectedCol = -1;
        legalMoves  = new ArrayList<>();
        invalidate();
    }
}
