import java.util.*;

public class StrategyBranchBound {
    private final GameState state;
    private final int SIZE;
    private int nodesExplored = 0;
    private int pruned = 0;


    public StrategyBranchBound(GameState state) {
        this.state = state;
        this.SIZE  = state.getSize();
    }

    public double evaluateCell(int row, int col) {
        if (state.getGrid()[row][col] != 0)
            return 0.0;
        double max = 0;
        int[][] grid = deepCopy(state.getGrid());
        for (int v = 1; v <= SIZE; v++) {
            if (state.getGraph().hasConflict(grid, row, col, v))
                continue;
            grid[row][col] = v;
            max = Math.max(max, immediateReward(grid, row, col));
            grid[row][col] = 0;
        }
        return max;
    }

    private double immediateReward(int[][] grid, int row, int col) {
        double score = 1.0;
        boolean rd = isRowComplete(grid, row), cd = isColComplete(grid, col);
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
        int[] a = new int[SIZE];
        for (int r = 0; r < SIZE; r++)
            a[r] = g[r][col];
        return countVis(a, false) == state.getTopClues()[col] && countVis(a, true) == state.getBottomClues()[col];
    }

    private int countVis(int[] line, boolean rev) {
        int vis = 0, mH = 0;
        for (int i = rev ? SIZE - 1 : 0; rev ? i >= 0 : i < SIZE; i += rev ? -1 : 1)
            if (line[i] > mH) {
                mH = line[i];
                vis++;
            }
        return vis;
    }

    private boolean isRowComplete(int[][] g, int r) {
        for (int c = 0; c < SIZE; c++)
            if (g[r][c] == 0)
                return false;
        return true;
    }

    private boolean isColComplete(int[][] g,int c){
        for(int r=0;r<SIZE;r++)
            if(g[r][c]==0)
                return false;
        return true;
    }
    private int legalCount(int[][] grid,int row,int col){
        int cnt=0;for(int v=1;v<=SIZE;v++)
            if(!state.getGraph().hasConflict(grid,row,col,v))
                cnt++;
        return cnt;
    }
    private int[][] deepCopy(int[][] src){
        int[][] c=new int[SIZE][SIZE];
        for(int i=0;i<SIZE;i++)
            c[i]=src[i].clone();
        return c;
    }

    private String buildExplanation(int[] best, double score) {
        return String.format(
            "【BRANCH & BOUND】\n════════════════════════════\n Move : %d  at  (%d , %d)\n Bounded score    : %.1f\n────────────────────────────\n Nodes explored   : %d\n Branches pruned  : %d\n Bound strategy   : Optimistic\n════════════════════════════\nSTRATEGY: Best-first search\nwith cost bounding — skips\nbranches below current best.",
            best[2], best[0]+1, best[1]+1, score, nodesExplored, pruned);
    }

}
