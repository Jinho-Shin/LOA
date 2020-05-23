/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author Jinho Shin
 */
class Board {

    /** Default number of moves for each side that results in a draw. */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /** Pattern describing a valid square designator (cr). */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /** A Board whose initial contents are taken from INITIALCONTENTS
     *  and in which the player playing TURN is to move. The resulting
     *  Board has
     *        get(col, row) == INITIALCONTENTS[row][col]
     *  Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     *
     *  CAUTION: The natural written notation for arrays initializers puts
     *  the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /** A new board in the standard initial position. */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /** A Board whose initial contents and state are copied from
     *  BOARD. */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /** Set my state to CONTENTS with SIDE to move. */
    void initialize(Piece[][] contents, Piece side) {
        for (int i = 0; i < contents.length; i += 1) {
            for (int j = 0; j < contents[i].length; j += 1) {
                Piece p = contents[i][j];
                Square s = sq(j, i);
                set(s, p);
            }
        }
        _moves.clear();
        _turn = side;
        _moveLimit = DEFAULT_MOVE_LIMIT;
        _subsetsInitialized = false;
    }

    /** Set me to the initial configuration. */
    void clear() {
        initialize(INITIAL_PIECES, BP);
    }

    /** Set my state to a copy of BOARD. */
    void copyFrom(Board board) {
        if (board == this) {
            return;
        } else {
            for (int i = 0; i < board._board.length; i += 1) {
                this._board[i] = board._board[i];
            }
            this._moves.addAll(board._moves);
            this._turn = board._turn;
            this._moveLimit = board._moveLimit;
            this._subsetsInitialized = false;
        }
    }

    /** Return the contents of the square at SQ. */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /** Set the square at SQ to V and set the side that is to move next
     *  to NEXT, if NEXT is not null. */
    void set(Square sq, Piece v, Piece next) {
        _board[sq.index()] = v;
        if (next != null) {
            _turn = next;
        }
    }

    /** Set the square at SQ to V, without modifying the side that
     *  moves next. */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /** Set limit on number of moves by each side that results in a tie to
     *  LIMIT, where 2 * LIMIT > movesMade(). */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }
        _moveLimit = 2 * limit;
    }

    /** Assuming isLegal(MOVE), make MOVE. This function assumes that
     *  MOVE.isCapture() will returan false.  If it saves the move for
     *  later retraction, makeMove itself uses MOVE.captureMove() to produce
     *  the capturing move. */
    void makeMove(Move move) {
        assert isLegal(move);
        Piece pFrom = get(move.getFrom());
        set(move.getFrom(), EMP);
        Piece pTo = get(move.getTo());
        set(move.getTo(), pFrom, _turn.opposite());
        if (pFrom.opposite() == pTo) {
            move = move.captureMove();
        }
        _moves.add(move);
        _subsetsInitialized = false;
    }

    /** Retract (unmake) one move, returning to the state immediately before
     *  that move.  Requires that movesMade () > 0. */
    void retract() {
        assert movesMade() > 0;
        Move move = _moves.get(_moves.size() - 1);
        Square to = move.getTo();
        Square from = move.getFrom();
        Piece p = get(to);
        if (move.isCapture()) {
            set(to, p.opposite(), _turn.opposite());
        } else {
            set(to, EMP, _turn.opposite());
        }
        set(from, p);
        _moves.remove(_moves.size() - 1);
    }

    /** Return the Piece representing who is next to move. */
    Piece turn() {
        return _turn;
    }

    /** Return true iff FROM - TO is a legal move for the player currently on
     *  move. */
    boolean isLegal(Square from, Square to) {
        if (get(from) != turn()) {
            return false;
        }
        int dir = from.direction(to);
        int numPieces = count(from, dir);
        boolean b = !blocked(from, to);
        int distance = from.distance(to);
        return from.isValidMove(to) &&  distance == numPieces && b;
    }

    /** Calculate piece from Square.
     * @param from towards.
     * @param dir1 and its opposite direction.
     * @return the count. */

    int count(Square from, int dir1) {
        int result = 1;
        int dir2 = (dir1 + 4) % 8;
        for (int i = 1; i < BOARD_SIZE; i += 1) {
            Square sq1 = from.moveDest(dir1, i);
            Square sq2 = from.moveDest(dir2, i);
            if (sq1 != null && get(sq1) != EMP) {
                result += 1;
            }
            if (sq2 != null && get(sq2) != EMP) {
                result += 1;
            }
        }
        return result;
    }



    /** Return true iff MOVE is legal for the player currently on move.
     *  The isCapture() property is ignored. */
    boolean isLegal(Move move) {
        return isLegal(move.getFrom(), move.getTo());
    }

    /** Return a sequence of all legal moves from this position. */
    List<Move> legalMoves() {
        List<Move> result = new ArrayList<Move>();
        for (int i = 0; i < BOARD_SIZE; i += 1) {
            for (int j = 0; j < BOARD_SIZE; j += 1) {
                Square from = sq(i, j);
                Piece p = get(from);
                if (p == _turn) {
                    for (int dir = 0; dir < 8; dir += 1) {
                        Square to = from.moveDest(dir, count(from, dir));
                        if (to != null && isLegal(from, to)) {
                            result.add(Move.mv(from, to));
                        }
                    }
                }
            }
        }
        return result;
    }

    /** Return true iff the game is over (either player has all his
     *  pieces continguous or there is a tie). */
    boolean gameOver() {
        return winner() != null;
    }

    /** Return true iff SIDE's pieces are continguous. */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /** Return the winning side, if any.  If the game is not over, result is
     *  null.  If the game has ended in a tie, returns EMP. */
    Piece winner() {
        boolean less = movesMade() < _moveLimit;
        if (!piecesContiguous(BP) && !piecesContiguous(WP) && less) {
            return null;
        } else if (piecesContiguous(_turn.opposite())) {
            _winner = _turn.opposite();
        } else if (piecesContiguous(_turn)) {
            _winner = _turn;
        } else {
            _winner = EMP;
        }
        return _winner;
    }

    /** Return the total number of moves that have been made (and not
     *  retracted).  Each valid call to makeMove with a normal move increases
     *  this number by 1. */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -=   1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /** Return true if a move from FROM to TO is blocked by an opposing
     *  piece or by a friendly piece on the target square. */
    private boolean blocked(Square from, Square to) {
        int dir = from.direction(to);
        int dis = from.distance(to);
        for (int i = 1; i < dis; i += 1) {
            if (get(from.moveDest(dir, i)) == get(from).opposite()) {
                return true;
            }
        }
        if (get(from) == get(to)) {
            return true;
        }
        return false;
    }

    /** Return the size of the as-yet unvisited cluster of squares
     *  containing P at and adjacent to SQ. VISITED indicates squares that
     *  have already been processed or are in different clusters.  Update
     *  VISITED to reflect squares counted. */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        if (sq == null) {
            return 0;
        }
        Piece p2 = get(sq);
        if (p == EMP) {
            return 0;
        } else if (p2 != p) {
            return 0;
        } else if (visited[sq.row()][sq.col()]) {
            return 0;
        } else {
            visited[sq.row()][sq.col()] = true;
            int result = 1;
            for (Square adj: sq.adjacent()) {
                result += numContig(adj, visited, p);
            }
            return result;
        }
    }

    /** Set the values of _whiteRegionSizes and _blackRegionSizes. */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        for (int i = 0; i < BOARD_SIZE; i += 1) {
            for (int j = 0; j < BOARD_SIZE; j += 1) {
                _visited[i][j] = false;
            }
        }
        for (int i = 0; i < BOARD_SIZE; i += 1) {
            for (int j = 0; j < BOARD_SIZE; j += 1) {
                Square sq = Square.sq(i, j);
                Piece p = get(sq);
                if (p == WP) {
                    if (!_visited[sq.row()][sq.col()]) {
                        int w = numContig(sq, _visited, p);
                        _whiteRegionSizes.add(w);
                    }
                } else if (p == BP) {
                    if (!_visited[sq.row()][sq.col()]) {
                        int b = numContig(sq, _visited, p);
                        _blackRegionSizes.add(b);
                    }
                }
            }
        }
        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /** Return the sizes of all the regions in the current union-find
     *  structure for side S. */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }

    /** Visited booleans. */
    private boolean[][] _visited = new boolean[BOARD_SIZE][BOARD_SIZE];

    /** Heuristic of the current board.
     * @param p for the side and
     * @param r for a random number.
     * @return the heuristic. */
    public int heuristic(Piece p, int r) {
        if (gameOver()) {
            if (winner() == p) {
                return 1000 << 10;
            } else if (winner() == p.opposite()) {
                return -1 * (1000 << 10);
            }
        }
        int sum = 0;
        for (int n: getRegionSizes(p)) {
            sum += n;
        }
        int ratio = 0;
        for (int n: getRegionSizes(p)) {
            int i = n * 100 / sum;
            ratio = Math.max(ratio, i);
        }
        int o = getRegionSizes(p.opposite()).size();
        return (ratio + o + r) << 10;
    }

    /** The standard initial configuration for Lines of Action (bottom row
     *  first). */
    static final Piece[][] INITIAL_PIECES = {
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }
    };

    /** Current contents of the board.  Square S is at _board[S.index()]. */
    private final Piece[] _board = new Piece[BOARD_SIZE  * BOARD_SIZE];
    /** List of all unretracted moves on this board, in order. */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /** Current side on move. */
    private Piece _turn;
    /** Limit on number of moves before tie is declared.  */
    private int _moveLimit;
    /** True iff the value of _winner is known to be valid. */
    private boolean _winnerKnown;
    /** Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     *  in progress).  Use only if _winnerKnown. */
    private Piece _winner;

    /** True iff subsets computation is up-to-date. */
    private boolean _subsetsInitialized;

    /** List of the sizes of continguous clusters of pieces, by color. */
    private final ArrayList<Integer>
        _whiteRegionSizes = new ArrayList<>(),
        _blackRegionSizes = new ArrayList<>();
}
