package com.chessapp.model;

/**
 * Represents a single chess piece: its type and colour.
 * Also provides FEN character and Unicode symbol helpers.
 */
public class ChessPiece {

    public enum Type   { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
    public enum Color  { WHITE, BLACK }

    private final Type  type;
    private final Color color;

    public ChessPiece(Type type, Color color) {
        this.type  = type;
        this.color = color;
    }

    public Type  getType()  { return type;  }
    public Color getColor() { return color; }

    /** Returns the FEN character (upper-case = white, lower-case = black). */
    public char toFENChar() {
        char c;
        switch (type) {
            case PAWN:   c = 'p'; break;
            case KNIGHT: c = 'n'; break;
            case BISHOP: c = 'b'; break;
            case ROOK:   c = 'r'; break;
            case QUEEN:  c = 'q'; break;
            case KING:   c = 'k'; break;
            default:     c = '?';
        }
        return color == Color.WHITE ? Character.toUpperCase(c) : c;
    }

    /** Unicode chess symbol for drawing on the board view. */
    public String getUnicodeSymbol() {
        if (color == Color.WHITE) {
            switch (type) {
                case KING:   return "\u2654";
                case QUEEN:  return "\u2655";
                case ROOK:   return "\u2656";
                case BISHOP: return "\u2657";
                case KNIGHT: return "\u2658";
                case PAWN:   return "\u2659";
            }
        } else {
            switch (type) {
                case KING:   return "\u265A";
                case QUEEN:  return "\u265B";
                case ROOK:   return "\u265C";
                case BISHOP: return "\u265D";
                case KNIGHT: return "\u265E";
                case PAWN:   return "\u265F";
            }
        }
        return "?";
    }

    @Override
    public String toString() {
        return color.name() + "_" + type.name();
    }
}
