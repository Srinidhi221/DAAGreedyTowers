package FOR_EVal2;

import java.util.ArrayList;
import java.util.List;

public class StrategyCompletion {
 private GameState state;

 public StrategyCompletion(GameState state) {
     this.state = state;
 }
 
 
 public int[] findBestMove() {
	    int size = state.getSize();

	    // Collect ALL possible moves: (row, col, value, score, explanation)
	    List<CellEvaluation> candidates = new ArrayList<>();

	    for (int r = 0; r < size; r++) {
	        for (int c = 0; c < size; c++) {
	            if (state.getGrid()[r][c] == 0) {
	                CellEvaluation baseEval = evaluateCompletion(r, c);

	                // Try every legal value in this cell
	                for (int v = 1; v <= size; v++) {
	                    if (!state.getGraph().hasConflict(state.getGrid(), r, c, v)) {
	                        double visibilityRisk = calculateVisibilityRisk(r, c, v);

	                        // Greedy adjusted score: completion priority minus small risk penalty
	                        double adjustedScore = baseEval.score - (visibilityRisk * 0.3);

	                        // Create explanation (same as before, but per value)
	                        String explanation = String.format(
	                            "【COMPLETION GREEDY】\n" +
	                            "════════════════════════════\n" +
	                            " Cell: (%d,%d) = %d\n" +
	                            " Row empty: %d → priority %.1f\n" +
	                            " Col empty: %d → priority %.1f\n" +
	                            " Completion bonus: %.1f\n" +
	                            " Visibility risk: %.1f\n" +
	                            " ADJUSTED SCORE: %.1f\n" +
	                            "════════════════════════════\n" +
	                            "STRATEGY: Rush to complete!\n" +
	                            "%s",
	                            r, c, v,
	                            state.countEmptyInRow(r), 100.0 / (state.countEmptyInRow(r) + 1),
	                            state.countEmptyInColumn(c), 100.0 / (state.countEmptyInColumn(c) + 1),
	                            baseEval.score,
	                            visibilityRisk,
	                            adjustedScore,
	                            visibilityRisk > 0 ? " HIGH PENALTY RISK!" : "✓ Safe move"
	                        );

	                        // Add this full move (cell + value) as a candidate
	                        candidates.add(new CellEvaluation(r, c, adjustedScore, explanation, v));
	                    }
	                }
	            }
	        }
	    }

	    if (candidates.isEmpty()) {
	        return null;
	    }

	    // Uses shared intelligent sorting
	    candidates.sort(CellSorter.getComparator(size));

	    // Pick the absolute best move
	    CellEvaluation best = candidates.get(0);

	    state.setCpuReasoningExplanation(best.explanation);

	    return new int[]{best.row, best.col, best.value};  // value is now stored!
	}

 private CellEvaluation evaluateCompletion(int row, int col) {
     int emptyRow = state.countEmptyInRow(row);
     int emptyCol = state.countEmptyInColumn(col);
     

     double rowPriority = 100.0 / (emptyRow + 1);
     double colPriority = 100.0 / (emptyCol + 1);
     double bonus = 0.0;
     if (emptyRow == 1) bonus += 50.0;
     if (emptyCol == 1) bonus += 50.0;
     if (emptyRow == 1 && emptyCol == 1) bonus += 100.0;

     double finalScore = rowPriority + colPriority + bonus;

     String explanation = String.format(
         "【COMPLETION GREEDY】\n" +
         "════════════════════════════\n" +
         " Cell: (%d,%d)\n" +
         " Row empty: %d → priority %.1f\n" +
         " Col empty: %d → priority %.1f\n" +
         " Completion bonus: %.1f\n" +
         " TOTAL SCORE: %.1f\n" +
         "════════════════════════════\n" +
         "STRATEGY: Rush to finish rows & columns!",
         row, col, emptyRow, rowPriority, emptyCol, colPriority, bonus, finalScore
     );

     return new CellEvaluation(row, col, finalScore, explanation);
 }
 
 private double calculateVisibilityRisk(int row, int col, int value) {
     double risk = 0.0;
     
     // Simulate placing the value
     int[][] grid = state.getGrid();
     int originalValue = grid[row][col];
     grid[row][col] = value;
     
     // Check if row would be complete
     boolean rowComplete = state.isRowComplete(row);
     if (rowComplete) {
         // Will this violate visibility clues?
         if (!state.validateRowVisibility(row)) {
             risk += 15.0;  // -15 lives penalty risk!
         }
     }
     
     // Check if column would be complete
     boolean colComplete = state.isColumnComplete(col);
     if (colComplete) {
         // Will this violate visibility clues?
         if (!state.validateColumnVisibility(col)) {
             risk += 15.0;  // -15 lives penalty risk!
         }
     }
     
     // Restore original value
     grid[row][col] = originalValue;
     
     return risk;
 }


 public double evaluateCell(int row, int col) {
     if (state.getGrid()[row][col] != 0) return 0.0;
     return evaluateCompletion(row, col).score;
 }
}