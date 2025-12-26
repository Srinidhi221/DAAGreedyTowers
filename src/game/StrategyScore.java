package game;

import java.util.*;

//============================================================================
//SCORE GREEDY STRATEGY - Person 3
//============================================================================
public class StrategyScore {
 private GameState state;

 public StrategyScore(GameState state) {
     this.state = state;
 }

 public int[] findBestMove() {
     int size = state.getSize();
     double bestScore = -Double.MAX_VALUE;
     int bestRow = -1, bestCol = -1, bestValue = -1;
     String bestExplanation = "";

     for (int r = 0; r < size; r++) {
         for (int c = 0; c < size; c++) {
             if (state.getGrid()[r][c] == 0) {
                 for (int v = 1; v <= size; v++) {
                     if (!state.getGraph().hasConflict(state.getGrid(), r, c, v)) {
                         CellEvaluation eval = evaluateScore(r, c, v);
                         if (eval.score > bestScore) {
                             bestScore = eval.score;
                             bestRow = r;
                             bestCol = c;
                             bestValue = v;
                             bestExplanation = eval.explanation;
                         }
                     }
                 }
             }
         }
     }

     if (bestRow == -1) return null;

     state.setCpuReasoningExplanation(bestExplanation);
     return new int[]{bestRow, bestCol, bestValue};
 }

 private CellEvaluation evaluateScore(int row, int col, int value) {
     int[][] temp = deepCopyGrid(state.getGrid());
     temp[row][col] = value;

     double score = 1.0; // base

     boolean rowComp = isRowComplete(temp, row);
     boolean colComp = isColumnComplete(temp, col);

     if (rowComp) {
         score += 10.0;
         if (!visibilityObviouslyWrong(temp, row, true)) score += 15.0;
     }
     if (colComp) {
         score += 10.0;
         if (!visibilityObviouslyWrong(temp, col, false)) score += 15.0;
     }
     if (rowComp && colComp) score += 25.0;

     int legalCount = countLegalValues(row, col);
     if (legalCount <= 2) score -= 5.0;

     String explanation = String.format(
         "【SCORE GREEDY】\n" +
         "════════════════════════════\n" +
         "📍 Move: %d at (%d,%d)\n" +
         "🎯 Legal options: %d%s\n" +
         "%s%s%s" +
         "📈 PROJECTED SCORE: %.1f\n" +
         "════════════════════════════\n" +
         "STRATEGY: Maximize immediate points!",
         value, row, col, legalCount, legalCount <= 2 ? " → -5 risk" : "",
         rowComp ? "✓ Completes ROW (+10 +15 gamble)\n" : "",
         colComp ? "✓ Completes COL (+10 +15 gamble)\n" : "",
         rowComp && colComp ? "🎉 DOUBLE COMPLETION (+25)\n" : "",
         score
     );

     return new CellEvaluation(row, col, score, explanation);
 }

 private boolean visibilityObviouslyWrong(int[][] grid, int index, boolean isRow) {
     // Simplified check – you can expand if needed
     return false; // we gamble unless clearly impossible (conservative)
 }

 private boolean isRowComplete(int[][] g, int r) {
     for (int c = 0; c < state.getSize(); c++) if (g[r][c] == 0) return false;
     return true;
 }

 private boolean isColumnComplete(int[][] g, int c) {
     for (int r = 0; r < state.getSize(); r++) if (g[r][c] == 0) return false;
     return true;
 }

 private int countLegalValues(int row, int col) {
     int count = 0;
     for (int v = 1; v <= state.getSize(); v++) {
         if (!state.getGraph().hasConflict(state.getGrid(), row, col, v)) count++;
     }
     return count;
 }

 private int[][] deepCopyGrid(int[][] original) {
     int[][] copy = new int[state.getSize()][state.getSize()];
     for (int i = 0; i < state.getSize(); i++) copy[i] = original[i].clone();
     return copy;
 }

 public double evaluateCell(int row, int col) {
     if (state.getGrid()[row][col] != 0) return 0.0;
     double max = 0;
     for (int v = 1; v <= state.getSize(); v++) {
         if (!state.getGraph().hasConflict(state.getGrid(), row, col, v)) {
             max = Math.max(max, evaluateScore(row, col, v).score);
         }
     }
     return max;
 }
}