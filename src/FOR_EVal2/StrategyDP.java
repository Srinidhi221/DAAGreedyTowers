package FOR_EVal2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*  STRATEGY  :  Dynamic Programming                                         
 *  APPROACH  :  Memoised sub-problem decomposition.                         
 *               The board is flattened into a bitmask (filled cells).       
 *               For every empty cell we compute the "DP value" = the best   
 *               cumulative score reachable from that state, stored in a     
 *               HashMap<Long, Double> memo table.                            
 *               Heat-map uses the top-1 DP value per cell.   */
public class StrategyDP {

    // core fields
    private final GameState state;
    private final int       SIZE;


    //memo table  :  board-state key  →  best achievable score 
    private final Map<Long, Double> memo = new HashMap<>();
    

    // reward constants 
    private static final double BASE_REWARD          =  1.0;
    private static final double ROW_COMPLETE_REWARD  = 12.0;
    private static final double COL_COMPLETE_REWARD  = 12.0;
    private static final double VIS_VALID_BONUS      = 18.0;
    private static final double DOUBLE_BONUS         = 30.0;
    private static final double LOW_OPTIONS_PENALTY  = -6.0;
    private static final double FUTURE_DEPTH_WEIGHT  =  0.6;   // discount per look-ahead ply


     // ── constructor 
    public StrategyDP(GameState state) {
        this.state = state;
        this.SIZE  = state.getSize();
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


        
    //  VISIBILITY  (Towers clue validation)
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


    //  UTILITY
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


}
