
public class StrategyBTForwardCheck {

    private final GameState state;
    private final int SIZE;

    // Metrics for explanation
    private int nodesExplored = 0;
    private int pruned = 0;

    public StrategyBTForwardCheck(GameState state) {
        this.state = state;
        this.SIZE  = state.getSize();
    }


       public int[] findBestMove() {
        nodesExplored = 0; pruned = 0;
        int[][] grid = deepCopy(state.getGrid());
        int[] best = {-1,-1,-1};
        double[] bestScore = {Double.NEGATIVE_INFINITY};

        
        return best;
    }

    public double evaluateCell(int row, int col) {
        if (state.getGrid()[row][col] != 0) return 0.0;
        double max = 0;
        int[][] grid = deepCopy(state.getGrid());
        for (int v = 1; v <= SIZE; v++) {
            if (state.getGraph().hasConflict(grid, row, col, v)) continue;
            grid[row][col] = v;
            max = Math.max(max, immediateReward(grid, row, col));
            grid[row][col] = 0;
        }
        return max;
    }

    private boolean[][][] initDomains(int[][] grid) {
        boolean[][][] domains = new boolean[SIZE][SIZE][SIZE + 1];
        
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        // Populate initial domain based on current board conflicts
                        domains[r][c][v] = !state.getGraph().hasConflict(grid, r, c, v);
                    }
                }
            }
        }
        return domains;
    }

    private boolean[][][] copyDomains(boolean[][][] src) {
        boolean[][][] dest = new boolean[SIZE][SIZE][SIZE + 1];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.arraycopy(src[i][j], 0, dest[i][j], 0, SIZE + 1);
            }
        }
        return dest;
    }

    private boolean isDomainEmpty(boolean[][][] domains, int r, int c) {
        for (int v = 1; v <= SIZE; v++) {
            if (domains[r][c][v]) return false;
        }
        return true; 
    }
    // HEURISTIC / SCORING FUNCTIONS
    private double immediateReward(int[][] grid, int row, int col) {
        double score = 1.0;
        boolean rowDone = true, colDone = true;
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == 0) rowDone = false;
            if (grid[i][col] == 0) colDone = false;
        }
        if (rowDone) score += 10.0;
        if (colDone) score += 10.0;
        if (rowDone && colDone) score += 5.0;
        return score;
    }

    private boolean isFullBoardVisibilityValid(int[][] grid) {
        for (int i = 0; i < SIZE; i++) {
            if (!rowVisOk(grid, i) || !colVisOk(grid, i)) return false;
        }
        return true;
    }

    private boolean rowVisOk(int[][] g, int row) {
        int left = state.getLeftClues()[row];
        int right = state.getRightClues()[row];
        if (left != 0 && countVis(g[row], false) != left) return false;
        if (right != 0 && countVis(g[row], true) != right) return false;
        return true;
    }
    
    private boolean colVisOk(int[][] g, int col) {
        int top = state.getTopClues()[col];
        int bottom = state.getBottomClues()[col];
        int[] arr = new int[SIZE]; 
        for(int r = 0; r < SIZE; r++) arr[r] = g[r][col];
        if (top != 0 && countVis(arr, false) != top) return false;
        if (bottom != 0 && countVis(arr, true) != bottom) return false;
        return true;
    }
    
    private int countVis(int[] line, boolean rev) {
        int vis = 0, maxH = 0;

        for (int i = rev ? SIZE - 1 : 0;
             rev ? i >= 0 : i < SIZE;
             i += rev ? -1 : 1) {

            if (line[i] > maxH) {
                maxH = line[i];
                vis++;
            }
        }

        return vis;
    }

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

    private int legalCount(int[][] grid, int row, int col) {
        int cnt = 0;

        for (int v = 1; v <= SIZE; v++)
            if (!state.getGraph().hasConflict(grid, row, col, v))
                cnt++;

        return cnt;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] c = new int[SIZE][SIZE];

        for (int i = 0; i < SIZE; i++)
            c[i] = src[i].clone();

        return c;
    }

    // ===============================
    // EXPLANATION BUILDER
    // ===============================

    private String buildExplanation(int[] best, double score) {
        return String.format(
            "【BACKTRACKING】\n" +
            "════════════════════════════\n" +
            " Move : %d  at  (%d , %d)\n" +
            " Branch score     : %.1f\n" +
            "────────────────────────────\n" +
            " Nodes explored   : %d\n" +
            " Branches pruned  : %d\n" +
            " Search depth     : 3 plies\n" +
            "════════════════════════════\n" +
            "STRATEGY: Depth-first search\n" +
            "with constraint pruning —\n" +
            "undoes bad moves instantly.",
            best[2], best[0] + 1, best[1] + 1,
            score, nodesExplored, pruned
        );
    }
}
