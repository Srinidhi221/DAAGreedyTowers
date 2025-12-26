package game;
import java.util.*;

public class StrategyMRV {
    private GameState state;

    public StrategyMRV(GameState state) {
        this.state = state;
    }
 
    public int[] findBestMove() {
        int size = state.getSize();

        // Collect all empty cells with their MRV evaluations
        List<CellEvaluation> candidates = new ArrayList<>();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (state.getGrid()[r][c] == 0) {
                    CellEvaluation eval = evaluateMRVGreedy(r, c);
                    // Skip death traps (score = -1000)
                    if (eval.score > -999) {
                        candidates.add(eval);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        // Use shared professional sorting with center preference
        candidates.sort(CellSorter.getComparator(size));

        // Pick the best cell
        CellEvaluation best = candidates.get(0);
        state.setCpuReasoningExplanation(best.explanation);

        // Choose a legal value (your existing visibility-ignorant logic)
        int bestValue = findLegalValueForCell(best.row, best.col);
        if (bestValue == -1) {
            return null;
        }
        return new int[]{best.row, best.col, bestValue};
    }

    // CORE EVALUATION LOGIC
    private CellEvaluation evaluateMRVGreedy(int row, int col) {
        int size = state.getSize();
        
        // 1. Count legal values for this cell
        int legalValuesCount = countLegalValues(row, col);
        
        // 2. MRV scoring: FEWER options = HIGHER priority
        // Formula: 1000 / (options + 1)
        double score = 1000.0 / (legalValuesCount + 1);
        
        // 3. WARNING for cells with 0 or 1 options
        String warning = "";
        String status = "";
        if (legalValuesCount == 0) {
            warning = " ⚠ DEATH TRAP - No legal values!";
            status = " AVOID THIS CELL!";
            score = -1000; // Lowest possible score
        } else if (legalValuesCount == 1) {
            warning = " ⚠ CRITICAL - Only 1 option left!";
            status = " FORCED MOVE";
        } else if (legalValuesCount == 2) {
            warning = "  HIGH PRIORITY";
            status = " Very constrained";
        } else if (legalValuesCount <= 3) {
            status = " Moderately constrained";
        } else {
            status = "✅ Less constrained";
        }
        
        // 4. Generate explanation
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
            "STRATEGY: Solve most constrained cells first!\n" +
            "Fewer options = Higher priority = Better constraint solving",
            row, col, legalValuesCount, warning, legalValuesCount, score, status, score
        );
        
        return new CellEvaluation(row, col, score, explanation);
    }

    // HELPER METHODS
    private int countLegalValues(int row, int col) {
        int size = state.getSize();
        int[][] grid = state.getGrid();
        Set<Integer> usedValues = new HashSet<>();
        
        // Check row
        for (int c = 0; c < size; c++) {
            if (grid[row][c] != 0) {
                usedValues.add(grid[row][c]);
            }
        }       
        // Check column
        for (int r = 0; r < size; r++) {
            if (grid[r][col] != 0) {
                usedValues.add(grid[r][col]);
            }
        }        
        // Count values NOT used (legal options)
        int legalCount = 0;
        for (int v = 1; v <= size; v++) {
            if (!usedValues.contains(v)) {
                legalCount++;
            }
        }     
        return legalCount;
    }
    
    private int findLegalValueForCell(int row, int col) {
        int size = state.getSize();
        Set<Integer> usedValues = new HashSet<>();
        int[][] grid = state.getGrid();
        
        // Check row
        for (int c = 0; c < size; c++) {
            if (grid[row][c] != 0) {
                usedValues.add(grid[row][c]);
            }
        }
        
        // Check column
        for (int r = 0; r < size; r++) {
            if (grid[r][col] != 0) {
                usedValues.add(grid[r][col]);
            }
        }      
        // Return first available legal value (ignoring visibility)
        for (int v = 1; v <= size; v++) {
            if (!usedValues.contains(v)) {
                return v;
            }
        }
        return -1;
    }

    // HEAT MAP SUPPORT
    public double evaluateCell(int row, int col) {
        if (state.getGrid()[row][col] != 0) return 0.0;
        
        CellEvaluation eval = evaluateMRVGreedy(row, col);
        return Math.max(0, eval.score); // Return positive score for heat map
    }
}