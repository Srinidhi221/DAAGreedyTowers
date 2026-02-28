
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
    private static final double BASE_REWARD = 1.0;
    private static final double ROW_COMPLETE_REWARD = 12.0;
    private static final double COL_COMPLETE_REWARD = 12.0;
    private static final double VIS_VALID_BONUS = 18.0;
    private static final double DOUBLE_BONUS = 28.0;
    private static final double QUAD_CONTROL_BONUS = 5.0;
    private static final double LOW_OPT_PENALTY = -5.0;

    // ── constructor ──────────────────────────────────────────
    public StrategyDnC(GameState state) {
        this.state = state;
        this.SIZE = state.getSize();
    }

    // PUBLIC API
    public int[] findBestMove() {
        // Divide board into quadrants; each returns its local champion.
        // Conquer: pick the global best among all champions.
        int half = SIZE / 2;

        // Four quadrant regions [rowStart, rowEnd, colStart, colEnd]
        int[][] quads = {
                { 0, half, 0, half }, // top-left
                { 0, half, half, SIZE }, // top-right
                { half, SIZE, 0, half }, // bottom-left
                { half, SIZE, half, SIZE } // bottom-right
        };

        List<MoveEval> allCandidates = new ArrayList<>();
        List<MoveEval> quadChampions = new ArrayList<>();
        List<String> quadSummaries = new ArrayList<>();
        int[] quadEmptyCounts = new int[4];

        for (int q = 0; q < 4; q++) {
            int rS = quads[q][0], rE = quads[q][1];
            int cS = quads[q][2], cE = quads[q][3];

            List<MoveEval> quadMoves = solveQuadrant(rS, rE, cS, cE);
            quadEmptyCounts[q] = emptyInQuadrant(rS, rE, cS, cE);
            allCandidates.addAll(quadMoves);

            if (!quadMoves.isEmpty()) {
                quadMoves.sort(Comparator.comparingDouble(MoveEval::score).reversed());
                MoveEval champ = quadMoves.get(0);
                // bonus for controlling the richest quadrant
                if (quadEmptyCounts[q] == maxOf(quadEmptyCounts)) {
                    champ = new MoveEval(champ.row, champ.col, champ.value,
                            champ.score + QUAD_CONTROL_BONUS,
                            champ.legalOpts, champ.rowDone, champ.colDone);
                }
                quadChampions.add(champ);
                quadSummaries.add(String.format(
                        "  Q%d [r%d-%d c%d-%d]: best=%d@(%d,%d) score=%.1f empty=%d",
                        q + 1, rS + 1, rE, cS + 1, cE,
                        champ.value, champ.row + 1, champ.col + 1,
                        champ.score, quadEmptyCounts[q]));
            } else {
                quadSummaries.add(String.format(
                        "  Q%d [r%d–%d c%d–%d]: NO MOVES (empty=%d)",
                        q + 1, rS + 1, rE, cS + 1, cE, quadEmptyCounts[q]));
            }
        }

        if (quadChampions.isEmpty())
            return null;

        // Merge step: pick the global champion
        quadChampions.sort(Comparator
                .comparingDouble(MoveEval::score).reversed()
                .thenComparingInt(m -> -m.legalOpts)); // more options = more control

        MoveEval best = quadChampions.get(0);
        state.setCpuReasoningExplanation(
                buildExplanation(best, quadSummaries, allCandidates.size()));
        return new int[] { best.row, best.col, best.value };
    }

    /**
     * Heat-map score for a single cell.
     */
    public double evaluateCell(int row, int col) {

        if (state.getGrid()[row][col] != 0)
            return 0.0;

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

    // DIVIDE – score every legal move inside one quadrant

    private List<MoveEval> solveQuadrant(int rStart, int rEnd, int cStart, int cEnd) {
        List<MoveEval> moves = new ArrayList<>();

        for (int r = rStart; r < rEnd; r++) {
            for (int c = cStart; c < cEnd; c++) {
                if (state.getGrid()[r][c] != 0)
                    continue;
                for (int v = 1; v <= SIZE; v++) {
                    if (state.getGraph().hasConflict(state.getGrid(), r, c, v))
                        continue;
                    int[][] after = deepCopy(state.getGrid());
                    after[r][c] = v;
                    double score = localScore(after, r, c, v);
                    int opts = legalCount(r, c);
                    boolean rowDone = isRowComplete(after, r);
                    boolean colDone = isColComplete(after, c);
                    moves.add(new MoveEval(r, c, v, score, opts, rowDone, colDone));
                }
            }
        }

        // Recursively refine: if quadrant has sub-regions, score them too
        // (For 4×4 the base case is reached immediately; structure is in place
        // for larger N where half > 2 means we recurse further.)
        if ((rEnd - rStart) > 2 && (cEnd - cStart) > 2) {
            int rMid = (rStart + rEnd) / 2;
            int cMid = (cStart + cEnd) / 2;
            moves.addAll(solveQuadrant(rStart, rMid, cStart, cMid));
            moves.addAll(solveQuadrant(rStart, rMid, cMid, cEnd));
            moves.addAll(solveQuadrant(rMid, rEnd, cStart, cMid));
            moves.addAll(solveQuadrant(rMid, rEnd, cMid, cEnd));
        }

        return moves;
    }

    // LOCAL SCORE (immediate heuristic for a single move)
    private double localScore(int[][] grid, int row, int col, int value) {
        double score = BASE_REWARD;

        boolean rowDone = isRowComplete(grid, row);
        boolean colDone = isColComplete(grid, col);

        if (rowDone) {
            score += ROW_COMPLETE_REWARD;
            if (rowVisibilityValid(grid, row))
                score += VIS_VALID_BONUS;
        }
        if (colDone) {
            score += COL_COMPLETE_REWARD;
            if (colVisibilityValid(grid, col))
                score += VIS_VALID_BONUS;
        }
        if (rowDone && colDone)
            score += DOUBLE_BONUS;

        int opts = legalCount(row, col);
        if (opts <= 1)
            score += LOW_OPT_PENALTY * 2;
        else if (opts <= 2)
            score += LOW_OPT_PENALTY;

        return score;
    }

    // VISIBILITY (Towers clue validation)

    private boolean rowVisibilityValid(int[][] grid, int row) {
        int[] left = state.getLeftClues();
        int[] right = state.getRightClues();
        if (left[row] != 0 && countVisible(grid[row], false) != left[row])
            return false;
        if (right[row] != 0 && countVisible(grid[row], true) != right[row])
            return false;
        return true;
    }

    private boolean colVisibilityValid(int[][] grid, int col) {
        int[] top = state.getTopClues();
        int[] bottom = state.getBottomClues();
        int[] colArr = new int[SIZE];
        for (int r = 0; r < SIZE; r++)
            colArr[r] = grid[r][col];
        if (top[col] != 0 && countVisible(colArr, false) != top[col])
            return false;
        if (bottom[col] != 0 && countVisible(colArr, true) != bottom[col])
            return false;
        return true;
    }

    private int countVisible(int[] line, boolean reverse) {
        int visible = 0, maxH = 0, len = line.length;
        for (int i = reverse ? len - 1 : 0; reverse ? i >= 0 : i < len; i += reverse ? -1 : 1) {
            if (line[i] > maxH) {
                maxH = line[i];
                visible++;
            }
        }
        return visible;
    }

    // UTILITY
    private boolean isRowComplete(int[][] g, int r) {
        for (int c = 0; c < SIZE; c++)
            if (g[r][c] == 0)
                return false;
        return true;
    }

    private boolean isColComplete(int[][] g, int c) {
        for (int r = 0; r < SIZE; r++)
            if (g[r][c] == 0)
                return false;
        return true;
    }

    private int legalCount(int row, int col) {
        int count = 0;
        for (int v = 1; v <= SIZE; v++)
            if (!state.getGraph().hasConflict(state.getGrid(), row, col, v))
                count++;
        return count;
    }

    private int emptyInQuadrant(int rS, int rE, int cS, int cE) {
        int count = 0;
        for (int r = rS; r < rE; r++)
            for (int c = cS; c < cE; c++)
                if (state.getGrid()[r][c] == 0)
                    count++;
        return count;
    }

    private int maxOf(int[] arr) {
        int m = 0;
        for (int v : arr)
            if (v > m)
                m = v;
        return m;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++)
            copy[i] = src[i].clone();
        return copy;
    }

    // EXPLANATION TEXT

    private String buildExplanation(MoveEval best, List<String> quadSummaries, int totalMoves) {
        StringBuilder sb = new StringBuilder();
        sb.append("【DIVIDE & CONQUER】\n");
        sb.append("════════════════════════════\n");
        sb.append(String.format(" Move : %d  at  (%d , %d)\n",
                best.value, best.row + 1, best.col + 1));
        sb.append(String.format(" Score        : %.1f\n", best.score));
        sb.append(String.format(" Legal opts   : %d\n", best.legalOpts));
        sb.append(String.format(" Row done?    : %s\n", best.rowDone ? "YES ✓" : "no"));
        sb.append(String.format(" Col done?    : %s\n", best.colDone ? "YES ✓" : "no"));
        sb.append("────────────────────────────\n");
        sb.append(" Quadrant analysis:\n");
        for (String s : quadSummaries)
            sb.append(s).append("\n");
        sb.append("────────────────────────────\n");
        sb.append(String.format(" Total moves scanned: %d\n", totalMoves));
        sb.append("════════════════════════════\n");
        sb.append("STRATEGY: Board split into\n");
        sb.append("quadrants → local champions\n");
        sb.append("merged for global winner.");
        return sb.toString();
    }

    // INNER DATA CLASS
    private static class MoveEval {
        final int row, col, value, legalOpts;
        final double score;
        final boolean rowDone, colDone;

        MoveEval(int row, int col, int value, double score,
                int legalOpts, boolean rowDone, boolean colDone) {
            this.row = row;
            this.col = col;
            this.value = value;
            this.score = score;
            this.legalOpts = legalOpts;
            this.rowDone = rowDone;
            this.colDone = colDone;
        }

        double score() {
            return score;
        }
    }
}
