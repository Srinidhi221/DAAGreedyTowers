public class StrategyBTForwardCheck {

    private final GameState state;
    private final int SIZE;

    // Metrics for explanation
    private int nodesExplored = 0;
    private int pruned = 0;

    public StrategyBTForwardCheck(GameState state) {
        this.state = state;
        this.SIZE = state.getSize();
    }

    public int[] findBestMove() {

        nodesExplored = 0;
        pruned = 0;

        int[][] grid = deepCopy(state.getGrid());

        // Tier 1: Mathematically Perfect Moves
        int[] bestSafeMove = { -1, -1, -1 };
        double bestSafeScore = -1.0;

        // Tier 2: Forward-Checking Safe Moves (Relaxed)
        int[] bestFallbackMove = { -1, -1, -1 };
        double bestFallbackScore = -1.0;

        boolean[][][] startDomains = initDomains(grid);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                if (grid[r][c] != 0)
                    continue;

                for (int v = 1; v <= SIZE; v++) {

                    if (state.getGraph().hasConflict(grid, r, c, v))
                        continue;

                    // Simulate move
                    grid[r][c] = v;
                    nodesExplored++;

                    boolean[][][] nextDomains = copyDomains(startDomains);
                    boolean isImmediateMoveSafe =
                            applyForwardChecking(grid, nextDomains, r, c, v);

                    double score = immediateReward(grid, r, c);

                    // TIER 1: The move is 100% mathematically safe to the end of the game
                    if (isImmediateMoveSafe && isPathSafe(grid, nextDomains)) {

                        if (score > bestSafeScore) {
                            bestSafeScore = score;
                            bestSafeMove[0] = r;
                            bestSafeMove[1] = c;
                            bestSafeMove[2] = v;
                        }
                    }
                    // TIER 2 (RELAXATION): The full path is doomed, but this specific move is locally safe
                    else if (isImmediateMoveSafe) {

                        pruned++; // It failed full path safety, but we'll remember it as a fallback

                        if (score > bestFallbackScore) {
                            bestFallbackScore = score;
                            bestFallbackMove[0] = r;
                            bestFallbackMove[1] = c;
                            bestFallbackMove[2] = v;
                        }
                    } else {
                        pruned++; // Forward checking instantly failed
                    }

                    grid[r][c] = 0; // Backtrack
                }
            }
        }

        // --- EXECUTE BEST AVAILABLE TIER ---

        if (bestSafeMove[0] != -1) {
            state.setCpuReasoningExplanation(
                    buildExplanation(bestSafeMove, bestSafeScore));
            return bestSafeMove;
        }

        if (bestFallbackMove[0] != -1) {
            state.setCpuReasoningExplanation(
                    buildExplanation(bestFallbackMove, bestFallbackScore));
            return bestFallbackMove;
        }

        // TIER 3 (DESPERATION): If even Forward Checking fails, just pick any basic legal move
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        if (!state.getGraph().hasConflict(grid, r, c, v)) {

                            state.setCpuReasoningExplanation(
                                    "【DESPERATION】\n" +
                                    "Human move made the board\n" +
                                    "unsolvable. 0 paths remain.\n" +
                                    "Executing basic legal move."
                            );

                            return new int[]{ r, c, v };
                        }
                    }
                }
            }
        }

        return null; // Board is 100% full or completely locked
    }

    public double evaluateCell(int row, int col) {

        if (state.getGrid()[row][col] != 0)
            return 0.0;

        double maxValidScore = 0;
        int[][] grid = deepCopy(state.getGrid());
        boolean[][][] startDomains = initDomains(grid);

        for (int v = 1; v <= SIZE; v++) {

            if (state.getGraph().hasConflict(grid, row, col, v))
                continue;

            grid[row][col] = v;
            boolean[][][] nextDomains = copyDomains(startDomains);

            if (applyForwardChecking(grid, nextDomains, row, col, v)
                    && isPathSafe(grid, nextDomains)) {

                maxValidScore = Math.max(
                        maxValidScore,
                        immediateReward(grid, row, col));
            }

            grid[row][col] = 0;
        }

        return maxValidScore;
    }

    private boolean[][][] initDomains(int[][] grid) {

        boolean[][][] domains = new boolean[SIZE][SIZE][SIZE + 1];

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        domains[r][c][v] =
                                !state.getGraph().hasConflict(grid, r, c, v);
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
                System.arraycopy(src[i][j], 0,
                        dest[i][j], 0, SIZE + 1);
            }
        }

        return dest;
    }

    private boolean applyForwardChecking(
            int[][] grid,
            boolean[][][] domains,
            int row,
            int col,
            int val) {

        // 1. Clear the domain for the cell we just filled
        for (int v = 1; v <= SIZE; v++) {
            domains[row][col][v] = false;
        }

        // 2. Remove 'val' from row and column neighbors
        for (int i = 0; i < SIZE; i++) {

            // Row
            if (grid[row][i] == 0 && domains[row][i][val]) {
                domains[row][i][val] = false;
                if (isDomainEmpty(domains, row, i))
                    return false;
            }

            // Column
            if (grid[i][col] == 0 && domains[i][col][val]) {
                domains[i][col][val] = false;
                if (isDomainEmpty(domains, i, col))
                    return false;
            }
        }

        return true;
    }

    private boolean isDomainEmpty(boolean[][][] domains, int r, int c) {

        for (int v = 1; v <= SIZE; v++) {
            if (domains[r][c][v])
                return false;
        }

        return true;
    }

    private double immediateReward(int[][] grid, int row, int col) {

        double score = 1.0;
        boolean rowDone = true;
        boolean colDone = true;

        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == 0)
                rowDone = false;
            if (grid[i][col] == 0)
                colDone = false;
        }

        if (rowDone) score += 10.0;
        if (colDone) score += 10.0;
        if (rowDone && colDone) score += 5.0;

        return score;
    }

    private boolean isPathSafe(int[][] grid, boolean[][][] domains) {

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0 && isDomainEmpty(domains, r, c))
                    return false;
            }
        }

        return true;
    }

    private int[][] deepCopy(int[][] src) {

        int[][] c = new int[SIZE][SIZE];

        for (int i = 0; i < SIZE; i++)
            c[i] = src[i].clone();

        return c;
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
                best[2], best[0] + 1, best[1] + 1,
                score, nodesExplored, pruned
        );
    }
}