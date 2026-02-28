
import java.util.*;

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
            best[2], best[0] + 1, best[1] + 1, score, nodesExplored, pruned
        );
    }

}
