package FOR_EVal2;
import java.util.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────────┐
 *  STRATEGY  :  Dynamic Programming                                         │
 *  APPROACH  :  Memoised sub-problem decomposition.                         │
 *               The board is flattened into a bitmask (filled cells).       │
 *               For every empty cell we compute the "DP value" = the best   │
 *               cumulative score reachable from that state, stored in a     │
 *               HashMap<Long, Double> memo table.                            │
 *               Heat-map uses the top-1 DP value per cell.                  │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
public class StrategyDP {

    // ── core fields ──────────────────────────────────────────────────────────
    private final GameState state;
    private final int       SIZE;

    /** memo table  :  board-state key  →  best achievable score */
    private final Map<Long, Double> memo = new HashMap<>();

    // ── reward constants (same philosophy as StrategyScore) ──────────────────
    private static final double BASE_REWARD          =  1.0;
    private static final double ROW_COMPLETE_REWARD  = 12.0;
    private static final double COL_COMPLETE_REWARD  = 12.0;
    private static final double VIS_VALID_BONUS      = 18.0;
    private static final double DOUBLE_BONUS         = 30.0;
    private static final double LOW_OPTIONS_PENALTY  = -6.0;
    private static final double FUTURE_DEPTH_WEIGHT  =  0.6;   // discount per look-ahead ply

    // ── constructor ──────────────────────────────────────────────────────────
    public StrategyDP(GameState state) {
        this.state = state;
        this.SIZE  = state.getSize();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Find the globally best (row, col, value) triple using DP look-ahead.
     * Returns int[]{row, col, value} or null if no legal move exists.
     */
    public int[] findBestMove() {
        memo.clear();   // fresh memo each CPU turn

        List<MoveEval> candidates = new ArrayList<>();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (state.getGrid()[r][c] != 0) continue;
                for (int v = 1; v <= SIZE; v++) {
                    if (state.getGraph().hasConflict(state.getGrid(), r, c, v)) continue;

                    int[][] after = deepCopy(state.getGrid());
                    after[r][c] = v;

                    double immediateScore = immediateReward(after, r, c, v);
                    double futureScore    = dpValue(after, gridKey(after), 1);
                    double total          = immediateScore + futureScore;

                    candidates.add(new MoveEval(r, c, v, immediateScore, futureScore, total));
                }
            }
        }

        if (candidates.isEmpty()) return null;

        // sort: highest total first; tie-break by fewer legal options (forcing)
        candidates.sort(Comparator
                .comparingDouble(MoveEval::total).reversed()
                .thenComparingInt(m -> legalCount(m.row, m.col)));

        MoveEval best = candidates.get(0);
        state.setCpuReasoningExplanation(buildExplanation(best, candidates));
        return new int[]{ best.row, best.col, best.value };
    }

    public double evaluateCell(int row, int col) {
        if (state.getGrid()[row][col] != 0) return 0.0;
        double max = 0;
        for (int v = 1; v <= SIZE; v++) {
            if (state.getGraph().hasConflict(state.getGrid(), row, col, v)) continue;
            int[][] after = deepCopy(state.getGrid());
            after[row][col] = v;
            double score = immediateReward(after, row, col, v)
                    + dpValue(after, gridKey(after), 1) * 0.5; // cheaper for heat-map
            max = Math.max(max, score);
        }
        return max;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DP CORE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Recursive memoised DP.
     * Returns the best future reward from the given board state.
     *
     * @param grid  current board (after a hypothetical move)
     * @param key   compact long key of the board
     * @param depth recursion depth (used for discounting)
     */
    private double dpValue(int[][] grid, long key, int depth) {
        if (depth >= 3) return 0;   // horizon limit – keeps it snappy

        if (memo.containsKey(key)) return memo.get(key);

        double best = 0;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] != 0) continue;
                for (int v = 1; v <= SIZE; v++) {
                    if (state.getGraph().hasConflict(grid, r, c, v)) continue;

                    int[][] next = deepCopy(grid);
                    next[r][c] = v;

                    double reward = immediateReward(next, r, c, v);
                    double future = dpValue(next, gridKey(next), depth + 1);
                    double total  = reward + FUTURE_DEPTH_WEIGHT * future;

                    if (total > best) best = total;
                }
            }
        }

        memo.put(key, best);
        return best;
    }

    //  REWARD FUNCTION  (shared by immediate + future scoring)

    private double immediateReward(int[][] grid, int row, int col, int value) {
        double score = BASE_REWARD;

        boolean rowDone = isRowComplete(grid, row);
        boolean colDone = isColComplete(grid, col);

        if (rowDone) {
            score += ROW_COMPLETE_REWARD;
            if (rowVisibilityValid(grid, row)) score += VIS_VALID_BONUS;
        }
        if (colDone) {
            score += COL_COMPLETE_REWARD;
            if (colVisibilityValid(grid, col)) score += VIS_VALID_BONUS;
        }
        if (rowDone && colDone) score += DOUBLE_BONUS;

        // penalise moves that leave very few future options for this cell's peers
        int opts = legalCount(row, col);
        if (opts <= 1) score += LOW_OPTIONS_PENALTY * 2;
        else if (opts <= 2) score += LOW_OPTIONS_PENALTY;

        return score;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VISIBILITY  (Towers clue validation)
    // ════════════════════════════════════════════════════════════════════════

    /** Check the left/right clues for a completed row. */
    private boolean rowVisibilityValid(int[][] grid, int row) {
        int[] leftClues  = state.getLeftClues();
        int[] rightClues = state.getRightClues();

        int fromLeft  = countVisible(grid[row], false);
        int fromRight = countVisible(grid[row], true);

        if (leftClues[row]  != 0 && fromLeft  != leftClues[row])  return false;
        if (rightClues[row] != 0 && fromRight != rightClues[row]) return false;
        return true;
    }

    /** Check the top/bottom clues for a completed column. */
    private boolean colVisibilityValid(int[][] grid, int col) {
        int[] topClues    = state.getTopClues();
        int[] bottomClues = state.getBottomClues();

        int[] colArr = new int[SIZE];
        for (int r = 0; r < SIZE; r++) colArr[r] = grid[r][col];

        int fromTop    = countVisible(colArr, false);
        int fromBottom = countVisible(colArr, true);

        if (topClues[col]    != 0 && fromTop    != topClues[col])    return false;
        if (bottomClues[col] != 0 && fromBottom != bottomClues[col]) return false;
        return true;
    }

    /** Count visible towers in a line (forward or reverse). */
    private int countVisible(int[] line, boolean reverse) {
        int visible = 0, maxH = 0;
        int len = line.length;
        for (int i = (reverse ? len-1 : 0);
             reverse ? i >= 0 : i < len;
             i += (reverse ? -1 : 1)) {
            if (line[i] > maxH) { maxH = line[i]; visible++; }
        }
        return visible;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITY
    // ════════════════════════════════════════════════════════════════════════

    private boolean isRowComplete(int[][] g, int r) {
        for (int c = 0; c < SIZE; c++) if (g[r][c] == 0) return false;
        return true;
    }

    private boolean isColComplete(int[][] g, int c) {
        for (int r = 0; r < SIZE; r++) if (g[r][c] == 0) return false;
        return true;
    }

    private int legalCount(int row, int col) {
        int count = 0;
        for (int v = 1; v <= SIZE; v++)
            if (!state.getGraph().hasConflict(state.getGrid(), row, col, v)) count++;
        return count;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) copy[i] = src[i].clone();
        return copy;
    }

    /**
     * Encode the board into a unique long key for the memo table.
     * Works for SIZE ≤ 6 with values 0-6 (3 bits each → 48 bits max for 4×4).
     */
    private long gridKey(int[][] grid) {
        long key = 0;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                key = key * (SIZE + 1) + grid[r][c];
        return key;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  EXPLANATION TEXT  (shown in reasoning panel)
    // ════════════════════════════════════════════════════════════════════════

    private String buildExplanation(MoveEval best, List<MoveEval> all) {
        int explored = all.size();
        int memoHits = memo.size();

        return String.format(
                "【DYNAMIC PROGRAMMING】\n" +
                        "════════════════════════════\n" +
                        " Move : %d  at  (%d , %d)\n" +
                        " Immediate reward : %+.1f\n" +
                        " Future (DP)      : %+.1f\n" +
                        " Total score      :  %.1f\n" +
                        "────────────────────────────\n" +
                        " States memoised  : %d\n" +
                        " Moves explored   : %d\n" +
                        " Look-ahead depth : 3 plies\n" +
                        "════════════════════════════\n" +
                        "STRATEGY: Memoised sub-problem\n" +
                        "optimisation — picks globally\n" +
                        "best reachable outcome.",
                best.value, best.row + 1, best.col + 1,
                best.immediate, best.future, best.total,
                memoHits, explored
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INNER DATA CLASS
    // ════════════════════════════════════════════════════════════════════════

    private static class MoveEval {
        final int    row, col, value;
        final double immediate, future, total;

        MoveEval(int row, int col, int value,
                 double immediate, double future, double total) {
            this.row = row; this.col = col; this.value = value;
            this.immediate = immediate; this.future = future; this.total = total;
        }

        double total() { return total; }
    }
}

