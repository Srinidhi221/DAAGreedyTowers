package game;
import java.util.*;

public class StrategyMRV {

    // Reference to current game state
    private GameState state;

    public StrategyMRV(GameState state) {
        this.state = state;
    }

    /*
     * Uses Minimum Remaining Values (MRV) heuristic
     * to decide the most constrained empty cell
     */
    public int[] findBestMove() {
        int size = state.getSize();

        // Candidate cells that are empty and not dead-ends
        List<CellEvaluation> candidates = new ArrayList<>();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {

                // Only evaluate empty cells
                if (state.getGrid()[r][c] == 0) {

                    CellEvaluation eval = evaluateMRVGreedy(r, c);

                    // Filter out cells with no legal moves
                    if (eval.score > -999) {
                        candidates.add(eval);
                    }
                }
            }
        }

        // No valid move possible
        if (candidates.isEmpty()) {
            return null;
        }

        // Sort by MRV score and positional preference
        candidates.sort(CellSorter.getComparator(size));

        // Best evaluated cell
        CellEvaluation best = candidates.get(0);
        state.setCpuReasoningExplanation(best.explanation);

        // Assign a legal value to the chosen cell
        int value = findLegalValueForCell(best.row, best.col);
        if (value == -1) {
            return null;
        }

        return new int[]{best.row, best.col, value};
    }

    /*
     * Computes MRV score for a cell using
     * number of remaining legal assignments
     */
    private CellEvaluation evaluateMRVGreedy(int row, int col) {

        int legalValuesCount = countLegalValues(row, col);

        // Inverse relation: fewer options => higher priority
        double score = 1000.0 / (legalValuesCount + 1);

        String warning = "";
        String status = "";

        // Constraint classification
        if (legalValuesCount == 0) {
            warning = " ⚠ DEATH TRAP - No legal values!";
            status = " AVOID THIS CELL!";
            score = -1000;
        } 
        else if (legalValuesCount == 1) {
            warning = " ⚠ CRITICAL - Only 1 option left!";
            status = " FORCED MOVE";
        } 
        else if (legalValuesCount == 2) {
            warning = "  HIGH PRIORITY";
            status = " Very constrained";
        } 
        else if (legalValuesCount <= 3) {
            status = " Moderately constrained";
        } 
        else {
            status = "✅ Less constrained";
        }

        // Human-readable explanation for visualization/debugging
        String explanation = String.format(
            "【MRV GREEDY - Constraint Solver】\n" +
            "════════════════════════════\n" +
            " Cell: (%d,%d)\n" +
            " Legal options: %d%s\n" +
            " MRV Score: 1000 / (%d + 1) = %.1f\n" +
            " Status: %s\n" +
            "════════════════════════════\n" +
            " FINAL SCORE: %.1f\n" +
            "────────────────────────────\n" +
            "Heuristic: prioritize most constrained variables",
            row, col, legalValuesCount, warning,
            legalValuesCount, score, status, score
        );

        return new CellEvaluation(row, col, score, explanation);
    }

    /*
     * Determines number of valid values that can be
     * assigned to a specific cell
     */
    private int countLegalValues(int row, int col) {

        int size = state.getSize();
        int[][] grid = state.getGrid();
        Set<Integer> usedValues = new HashSet<>();

        // Scan row for occupied values
        for (int c = 0; c < size; c++) {
            if (grid[row][c] != 0) {
                usedValues.add(grid[row][c]);
            }
        }

        // Scan column for occupied values
        for (int r = 0; r < size; r++) {
            if (grid[r][col] != 0) {
                usedValues.add(grid[r][col]);
            }
        }

        // Count values not already used
        int count = 0;
        for (int v = 1; v <= size; v++) {
            if (!usedValues.contains(v)) {
                count++;
            }
        }

        return count;
    }

    /*
     * Picks the first available legal value
     * (visibility constraints ignored intentionally)
     */
    private int findLegalValueForCell(int row, int col) {

        int size = state.getSize();
        int[][] grid = state.getGrid();
        Set<Integer> usedValues = new HashSet<>();

        // Row check
        for (int c = 0; c < size; c++) {
            if (grid[row][c] != 0) {
                usedValues.add(grid[row][c]);
            }
        }

        // Column check
        for (int r = 0; r < size; r++) {
            if (grid[r][col] != 0) {
                usedValues.add(grid[r][col]);
            }
        }

        // Return smallest valid value
        for (int v = 1; v <= size; v++) {
            if (!usedValues.contains(v)) {
                return v;
            }
        }

        return -1;
    }

    /*
     * Used by UI heat-map to show constraint intensity
     */
    public double evaluateCell(int row, int col) {

        if (state.getGrid()[row][col] != 0) {
            return 0.0;
        }

        CellEvaluation eval = evaluateMRVGreedy(row, col);
        return Math.max(0, eval.score);
    }
}
