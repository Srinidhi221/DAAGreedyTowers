
public class StrategyBacktracking {

    private final GameState state;
    private final int SIZE;

    // Metrics for explanation
    private int nodesExplored = 0;
    private int pruned = 0;

    public StrategyBacktracking(GameState state) {
        this.state = state;
        this.SIZE = state.getSize();
    }
    
    // ===============================
    // HEURISTIC / SCORING FUNCTIONS
    // ===============================

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
        return countVis(g[row], false) == state.getLeftClues()[row]
                && countVis(g[row], true) == state.getRightClues()[row];
    }

    private boolean colVisOk(int[][] g, int col) {
        int[] arr = new int[SIZE];
        for (int r = 0; r < SIZE; r++)
            arr[r] = g[r][col];

        return countVis(arr, false) == state.getTopClues()[col]
                && countVis(arr, true) == state.getBottomClues()[col];
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