package game;

import java.util.*;

public class StrategyLives {
 private GameState state;

 public StrategyLives(GameState state) {
     this.state = state;
 }

 
 public int[] findBestMove() {
	    int size = state.getSize();
	    int cpuLives = state.getCpuLives();
	    double emergencyMultiplier = calculateEmergencyMultiplier(cpuLives);
	    String status = getEmergencyStatus(cpuLives);

	    // Collect all empty cells with their survival evaluations
	    List<CellEvaluation> candidates = new ArrayList<>();

	    for (int r = 0; r < size; r++) {
	        for (int c = 0; c < size; c++) {
	            if (state.getGrid()[r][c] == 0) {
	                CellEvaluation eval = evaluateSurvival(r, c, emergencyMultiplier, status);
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

	    // Shared professional sorting with center preference
	    candidates.sort(CellSorter.getComparator(size));

	    // Pick the safest cell
	    CellEvaluation best = candidates.get(0);
	    state.setCpuReasoningExplanation(best.explanation);

	    // Choose any legal value (conservative — avoids risk)
	    int bestValue = findLegalValue(best.row, best.col);
	    if (bestValue == -1) {
	        return null;
	    }

	    return new int[]{best.row, best.col, bestValue};
	}
 private CellEvaluation evaluateSurvival(int row, int col, double emergencyMultiplier, String status) {
     int legalCount = countLegalValues(row, col);

     if (legalCount == 0) {
         return new CellEvaluation(row, col, -1000.0,
             " DEATH TRAP: No legal values → instant -10 lives penalty!");
     }

     double baseSafety = legalCount * 25.0;
     double livesFactor = (100.0 - state.getCpuLives()) / 20.0;
     double finalScore = (baseSafety * emergencyMultiplier) + livesFactor;

     String explanation = String.format(
         "【SURVIVAL GREEDY - %s】\n" +
         "════════════════════════════\n" +
         " Cell: (%d,%d)\n" +
         "  CPU Lives: %d\n" +
         " Legal options: %d\n" +
         "  Base safety: %.1f\n" +
         " Emergency multiplier: ×%.1f\n" +
         " Lives preservation: +%.1f\n" +
         " FINAL SCORE: %.1f\n" +
         "════════════════════════════\n" +
         "STRATEGY: Maximize survival – avoid penalties at all costs!",
         status, row, col, state.getCpuLives(), legalCount,
         baseSafety, emergencyMultiplier, livesFactor, finalScore
     );

     return new CellEvaluation(row, col, finalScore, explanation);
 }

 private double calculateEmergencyMultiplier(int lives) {
     if (lives <= 15) return 4.0;
     if (lives <= 30) return 3.0;
     if (lives <= 50) return 2.0;
     if (lives <= 75) return 1.5;
     return 1.0;
 }

 private String getEmergencyStatus(int lives) {
     if (lives <= 15) return "CRITICAL ";
     if (lives <= 30) return "EMERGENCY ";
     if (lives <= 50) return "WARNING ";
     if (lives <= 75) return "ALERT ";
     return "SAFE ✅";
 }

 private int countLegalValues(int row, int col) {
     int count = 0;
     for (int v = 1; v <= state.getSize(); v++) {
         if (!state.getGraph().hasConflict(state.getGrid(), row, col, v)) {
             count++;
         }
     }
     return count;
 }

 private int findLegalValue(int row, int col) {
     for (int v = 1; v <= state.getSize(); v++) {
         if (!state.getGraph().hasConflict(state.getGrid(), row, col, v)) {
             return v;
         }
     }
     return -1;
 }

 // Heat map support
 public double evaluateCell(int row, int col) {
     if (state.getGrid()[row][col] != 0) return 0.0;
     return evaluateSurvival(row, col, calculateEmergencyMultiplier(state.getCpuLives()), "").score;
 }
}