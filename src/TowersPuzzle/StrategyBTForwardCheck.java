
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

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] != 0) continue;
                for (int v = 1; v <= SIZE; v++) {
                    if (state.getGraph().hasConflict(grid, r, c, v)) { pruned++; continue; }
                    grid[r][c] = v; nodesExplored++;
                    double score = immediateReward(grid, r, c) + backtrack(grid, 1);
                    if (score > bestScore[0]) { bestScore[0]=score; best[0]=r; best[1]=c; best[2]=v; }
                    grid[r][c] = 0;
                }
            }
        }
        if (best[0] == -1) return null;
        state.setCpuReasoningExplanation(buildExplanation(best, bestScore[0]));
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

    private double backtrack(int[][] grid, int depth) {
        if (depth >= 3) return 0;
        double best = 0;
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) {
            if (grid[r][c] != 0) continue;
            for (int v = 1; v <= SIZE; v++) {
                if (state.getGraph().hasConflict(grid, r, c, v)) { pruned++; continue; }
                grid[r][c] = v; nodesExplored++;
                best = Math.max(best, immediateReward(grid,r,c) + 0.5*backtrack(grid, depth+1));
                grid[r][c] = 0;
            }
        }
        return best;
    }
    
    // HEURISTIC / SCORING FUNCTIONS
    private double immediateReward(int[][] grid, int row, int col) {
        double score = 1.0;

        boolean rd = isRowComplete(grid, row);
        boolean cd = isColComplete(grid, col);

        if (rd) {
            score += 12.0;
            if (rowVisOk(grid, row))
                score += 18.0;
        }

        if (cd) {
            score += 12.0;
            if (colVisOk(grid, col))
                score += 18.0;
        }

        if (rd && cd)
            score += 28.0;

        int opts = legalCount(grid, row, col);

        if (opts <= 1)
            score -= 12.0;
        else if (opts <= 2)
            score -= 5.0;

        return score;
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
