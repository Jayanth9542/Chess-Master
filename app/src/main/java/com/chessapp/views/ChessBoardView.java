package com.chessapp.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.chessapp.model.ChessBoard;
import com.chessapp.model.ChessPiece;
import com.chessapp.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Fully custom chess board View drawn on Canvas.
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
    private boolean     flipped         = false;
    private boolean     engineThinking  = false;
    private boolean     animationsEnabled = true;

    private float cellSize;
    private float boardOffset;

    // ── Animation ─────────────────────────────────────────────────────
    private Move animatingMove = null;
    private float animProgress = 0f; // 0 to 1
    private ValueAnimator moveAnimator;

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

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int size = Math.min(MeasureSpec.getSize(wSpec), MeasureSpec.getSize(hSpec));
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

    @Override
    protected void onDraw(Canvas canvas) {
        if (board == null) return;
        drawSquares(canvas);
        drawLabels(canvas);
        drawPieces(canvas);
        drawAnimatingPiece(canvas);
    }

    private void drawSquares(Canvas canvas) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int drawR = flipped ? 7 - r : r;
                int drawC = flipped ? 7 - c : c;
                float x = boardOffset + drawC * cellSize;
                float y = boardOffset + drawR * cellSize;

                squarePaint.setColor(((r + c) % 2 == 0) ? CLR_LIGHT : CLR_DARK);
                canvas.drawRect(x, y, x + cellSize, y + cellSize, squarePaint);

                if (lastMove != null &&
                        ((r == lastMove.fromRow && c == lastMove.fromCol) ||
                         (r == lastMove.toRow   && c == lastMove.toCol))) {
                    overlayPaint.setColor(CLR_LAST_MOVE);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, overlayPaint);
                }

                if (r == selectedRow && c == selectedCol) {
                    overlayPaint.setColor(CLR_SELECTED);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, overlayPaint);
                }

                if (board.isInCheck()) {
                    ChessPiece p = board.getPieceAt(r, c);
                    if (p != null && p.getType() == ChessPiece.Type.KING
                            && p.getColor() == (board.isWhiteToMove()
                                ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK)) {
                        overlayPaint.setColor(CLR_CHECK);
                        canvas.drawRect(x, y, x + cellSize, y + cellSize, overlayPaint);
                    }
                }

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
            String rank = String.valueOf(8 - i);
            canvas.drawText(rank, boardOffset / 2f,
                boardOffset + di * cellSize + half + labelPaint.getTextSize() / 3f, labelPaint);
            String file = String.valueOf((char)('a' + i));
            canvas.drawText(file, boardOffset + di * cellSize + half,
                boardOffset + 8 * cellSize + boardOffset * 0.75f, labelPaint);
        }
    }

    private void drawPieces(Canvas canvas) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // If animating, don't draw the piece at its SOURCE OR DESTINATION yet.
                // The animation logic handles drawing the piece in transition.
                if (animatingMove != null && 
                    ((r == animatingMove.fromRow && c == animatingMove.fromCol) ||
                     (r == animatingMove.toRow && c == animatingMove.toCol))) {
                    continue;
                }

                ChessPiece p = board.getPieceAt(r, c);
                if (p == null) continue;

                int drawR = flipped ? 7 - r : r;
                int drawC = flipped ? 7 - c : c;
                float cx = boardOffset + drawC * cellSize + cellSize / 2f;
                float cy = boardOffset + drawR * cellSize + cellSize * 0.72f;

                String sym = p.getUnicodeSymbol();
                canvas.drawText(sym, cx + cellSize * 0.025f, cy + cellSize * 0.025f, shadowPaint);
                piecePaint.setColor(p.getColor() == ChessPiece.Color.WHITE
                    ? CLR_WHITE_PIECE : CLR_BLACK_PIECE);
                canvas.drawText(sym, cx, cy, piecePaint);
            }
        }
    }

    private void drawAnimatingPiece(Canvas canvas) {
        if (animatingMove == null || board == null) return;

        // The piece is already at toRow/toCol in the board model.
        ChessPiece p = board.getPieceAt(animatingMove.toRow, animatingMove.toCol);
        if (p == null) return;

        // Visual start/end coordinates (considering flip)
        int fr = flipped ? 7 - animatingMove.fromRow : animatingMove.fromRow;
        int fc = flipped ? 7 - animatingMove.fromCol : animatingMove.fromCol;
        int tr = flipped ? 7 - animatingMove.toRow   : animatingMove.toRow;
        int tc = flipped ? 7 - animatingMove.toCol   : animatingMove.toCol;

        float startX = boardOffset + fc * cellSize + cellSize / 2f;
        float startY = boardOffset + fr * cellSize + cellSize * 0.72f;
        float endX   = boardOffset + tc * cellSize + cellSize / 2f;
        float endY   = boardOffset + tr * cellSize + cellSize * 0.72f;

        float currX = startX + (endX - startX) * animProgress;
        float currY = startY + (endY - startY) * animProgress;

        String sym = p.getUnicodeSymbol();
        canvas.drawText(sym, currX + cellSize * 0.025f, currY + cellSize * 0.025f, shadowPaint);
        piecePaint.setColor(p.getColor() == ChessPiece.Color.WHITE
                ? CLR_WHITE_PIECE : CLR_BLACK_PIECE);
        canvas.drawText(sym, currX, currY, piecePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (board == null || engineThinking || animatingMove != null) return true;

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
            Move chosen = null;
            for (Move m : legalMoves) {
                if (m.toRow == row && m.toCol == col) { chosen = m; break; }
            }

            if (chosen != null) {
                ChessPiece pc = board.getPieceAt(chosen.fromRow, chosen.fromCol);
                if (pc != null && pc.getType() == ChessPiece.Type.PAWN &&
                        (chosen.toRow == 0 || chosen.toRow == 7)) {
                    if (moveListener != null) moveListener.onPromotionRequired(chosen, pc.getColor());
                } else {
                    animateMove(chosen);
                    if (moveListener != null) moveListener.onMoveSelected(chosen);
                }
                selectedRow = selectedCol = -1;
                legalMoves = new ArrayList<>();
                invalidate();
            } else {
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

    private int lastMoveCount = 0;

    public void setBoard(ChessBoard board) {
        this.board = board;
        if (board != null) {
            lastMoveCount = board.getMoveCount();
            // Clear last move if it's a new board state from undo/reset
            if (lastMoveCount == 0) lastMove = null;
        }
        selectedRow = selectedCol = -1;
        legalMoves  = new ArrayList<>();
        invalidate();
    }

    public void animateMove(final Move move) {
        if (!animationsEnabled) {
            lastMove = move;
            invalidate();
            return;
        }

        if (moveAnimator != null) moveAnimator.cancel();

        animatingMove = move;
        lastMove = move;
        animProgress = 0f;

        moveAnimator = ValueAnimator.ofFloat(0f, 1f);
        moveAnimator.setDuration(300);
        moveAnimator.setInterpolator(new DecelerateInterpolator());
        moveAnimator.addUpdateListener(animation -> {
            animProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatingMove = null;
                invalidate();
            }
        });
        moveAnimator.start();
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
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
