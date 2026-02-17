package FOR_EVal2;

import java.util.*;

/**
 * STRATEGY : Divide & Conquer (Simplified Version)
 * Full board scan without quadrant merging.
 */
public class StrategyDnC {

    // ── core fields ──────────────────────────────────────────
    private final GameState state;
    private final int SIZE;

    // ── reward constants ─────────────────────────────────────
    private static final double BASE_REWARD         = 1.0;
    private static final double ROW_COMPLETE_REWARD = 12.0;
    private static final double COL_COMPLETE_REWARD = 12.0;
    private static final double VIS_VALID_BONUS     = 18.0;
    private static final double DOUBLE_BONUS        = 28.0;
    private static final double QUAD_CONTROL_BONUS  = 5.0;
    private static final double LOW_OPT_PENALTY     = -5.0;

    // ── constructor ──────────────────────────────────────────
    public StrategyDnC(GameState state) {
        this.state = state;
        this.SIZE  = state.getSize();
    }

    // ════════════════════════════════════════════════════════
    //  PUBLIC API
    // ════════════════════════════════════════════════════════

    /**
     * Find best move by scanning entire board.
     */
    public int[] findBestMove() {

        double bestScore = Double.NEGATIVE_INFINITY;
        int bestRow = -1, bestCol = -1, bestValue = -1;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                if (state.getGrid()[r][c] != 0) continue;

                for (int v = 1; v <= SIZE; v++) {

                    if (state.getGraph().hasConflict(state.getGrid(), r, c, v))
                        continue;

                    int[][] after = deepCopy(state.getGrid());
                    after[r][c] = v;

                    double score = localScore(after, r, c, v);

                    if (score > bestScore) {
                        bestScore = score;
                        bestRow = r;
                        bestCol = c;
                        bestValue = v;
                    }
                }
            }
        }

        if (bestRow == -1) return null;
        return new int[]{ bestRow, bestCol, bestValue };
    }

    /**
     * Heat-map score for a single cell.
     */
    public double evaluateCell(int row, int col) {

        if (state.getGrid()[row][col] != 0) return 0.0;

        double max = 0;

        for (int v = 1; v <= SIZE; v++) {

            if (state.getGraph().hasConflict(state.getGrid(), row, col, v))
                continue;

            int[][] after = deepCopy(state.getGrid());
            after[row][col] = v;

            double score = localScore(after, row, col, v);
            max = Math.max(max, score);
        }

        return max;
    }

    // ════════════════════════════════════════════════════════
    //  LOCAL SCORE
    // ════════════════════════════════════════════════════════

    private double localScore(int[][] grid, int row, int col, int value) {

        double score = BASE_REWARD;

        boolean rowDone = isRowComplete(grid, row);
        boolean colDone = isColComplete(grid, col);

        if (rowDone) score += ROW_COMPLETE_REWARD;
        if (colDone) score += COL_COMPLETE_REWARD;
        if (rowDone && colDone) score += DOUBLE_BONUS;

        int opts = legalCount(row, col);

        if (opts <= 1) score += LOW_OPT_PENALTY * 2;
        else if (opts <= 2) score += LOW_OPT_PENALTY;

        return score;
    }

    // ════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════

    private boolean isRowComplete(int[][] g, int r) {
        for (int c = 0; c < SIZE; c++)
            if (g[r][c] == 0) return false;
        return true;
    }

    private boolean isColComplete(int[][] g, int c) {
        for (int r = 0; r < SIZE; r++)
            if (g[r][c] == 0) return false;
        return true;
    }

    private int legalCount(int row, int col) {
        int count = 0;
        for (int v = 1; v <= SIZE; v++)
            if (!state.getGraph().hasConflict(state.getGrid(), row, col, v))
                count++;
        return count;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++)
            copy[i] = src[i].clone();
        return copy;
    }
}
