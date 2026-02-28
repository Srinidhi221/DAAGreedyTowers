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
