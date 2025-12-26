package game;

import java.util.*;

//============================================================================
//COMPLETION GREEDY STRATEGY - Person 2
//============================================================================
public class StrategyCompletion {
 private GameState state;

 public StrategyCompletion(GameState state) {
     this.state = state;
 }

 public int[] findBestMove() {
     int size = state.getSize();
     double bestScore = -Double.MAX_VALUE;
     int bestRow = -1, bestCol = -1;
     String bestExplanation = "";

     for (int r = 0; r < size; r++) {
         for (int c = 0; c < size; c++) {
             if (state.getGrid()[r][c] == 0) {
                 CellEvaluation eval = evaluateCompletion(r, c);
                 if (eval.score > bestScore) {
                     bestScore = eval.score;
                     bestRow = r;
                     bestCol = c;
                     bestExplanation = eval.explanation;
                 }
             }
         }
     }

     if (bestRow == -1) return null;

     int bestValue = findLegalValue(bestRow, bestCol);
     if (bestValue == -1) return null;

     state.setCpuReasoningExplanation(bestExplanation);
     return new int[]{bestRow, bestCol, bestValue};
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
         "📍 Cell: (%d,%d)\n" +
         "📏 Row empty: %d → priority %.1f\n" +
         "📏 Col empty: %d → priority %.1f\n" +
         "🎯 Completion bonus: %.1f\n" +
         "📈 TOTAL SCORE: %.1f\n" +
         "════════════════════════════\n" +
         "STRATEGY: Rush to finish rows & columns!",
         row, col, emptyRow, rowPriority, emptyCol, colPriority, bonus, finalScore
     );

     return new CellEvaluation(row, col, finalScore, explanation);
 }

 private int findLegalValue(int row, int col) {
     for (int v = 1; v <= state.getSize(); v++) {
         if (!state.getGraph().hasConflict(state.getGrid(), row, col, v)) {
             return v;
         }
     }
     return -1;
 }

 public double evaluateCell(int row, int col) {
     if (state.getGrid()[row][col] != 0) return 0.0;
     return evaluateCompletion(row, col).score;
 }
}