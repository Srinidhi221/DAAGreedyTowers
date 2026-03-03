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
            state.setCpuReasoningExplanation(buildExplanation(bestSafeMove, "Mathematically Perfect"));
            return bestSafeMove;
        } 
        
        if (bestFallbackMove[0] != -1) {
            state.setCpuReasoningExplanation(buildExplanation(bestFallbackMove, "Fallback: Local FC Safe"));
            return bestFallbackMove;
        }

        // TIER 3 (DESPERATION): If even Forward Checking fails, just pick any basic legal move
        // for (int r = 0; r < SIZE; r++) {
        //     for (int c = 0; c < SIZE; c++) {
        //         if (grid[r][c] == 0) {
        //             for (int v = 1; v <= SIZE; v++) {
        //                 if (!state.getGraph().hasConflict(grid, r, c, v)) {

        //                     state.setCpuReasoningExplanation(
        //                             "【DESPERATION】\n" +
        //                             "Human move made the board\n" +
        //                             "unsolvable. 0 paths remain.\n" +
        //                             "Executing basic legal move."
        //                     );

        //                     return new int[]{ r, c, v };
        //                 }
        //             }
        //         }
        //     }
        // }

            // TIER 3 (DESPERATION): Pick the move that minimizes human's options
int[] bestDesperationMove = {-1, -1, -1};
double bestDesperationScore = -1.0;

for (int r = 0; r < SIZE; r++) {
    for (int c = 0; c < SIZE; c++) {
        if (grid[r][c] == 0) {
            for (int v = 1; v <= SIZE; v++) {
                if (!state.getGraph().hasConflict(grid, r, c, v)) {
                    
                    // Simulate the move
                    grid[r][c] = v;
                    
                    // Count how many legal moves the human has left
                    int humanOptions = 0;
                    for (int hr = 0; hr < SIZE; hr++) {
                        for (int hc = 0; hc < SIZE; hc++) {
                            if (grid[hr][hc] == 0) {
                                for (int hv = 1; hv <= SIZE; hv++) {
                                    if (!state.getGraph().hasConflict(grid, hr, hc, hv)) {
                                        humanOptions++;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Score: fewer human options = better for CPU
                    // Also factor in immediate reward
                    double score = immediateReward(grid, r, c) 
                                 - (humanOptions * 0.1);
                    
                    if (score > bestDesperationScore) {
                        bestDesperationScore = score;
                        bestDesperationMove[0] = r;
                        bestDesperationMove[1] = c;
                        bestDesperationMove[2] = v;
                    }
                    
                    grid[r][c] = 0; // undo simulation
                }
            }
        }
    }
}

if (bestDesperationMove[0] != -1) {
    state.setCpuReasoningExplanation(
        "【DESPERATION — STRATEGIC】\n" +
        "Board is unsolvable but CPU\n" +
        "minimizes your options.\n" +
        "────────────────────────────\n" +
        "Human options remaining: " + (int)(bestDesperationScore) + "\n" +
        "Executing least-bad move."
    );
    return bestDesperationMove;
}



        return null; // Board is 100% full or completely locked
    }

    public double evaluateCell(int row, int col) {
        if (state.getGrid()[row][col] != 0) return 0.0;
        double maxValidScore = 0.0;
        int[][] grid = deepCopy(state.getGrid());
        boolean[][][] startDomains = initDomains(grid);
        for (int v = 1; v <= SIZE; v++) {
            if (state.getGraph().hasConflict(grid, row, col, v)) continue;
            grid[row][col] = v;
            boolean[][][] nextDomains = copyDomains(startDomains);
            if (applyForwardChecking(grid, nextDomains, row, col, v)) {
                maxValidScore = Math.max(maxValidScore, immediateReward(grid, row, col));
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
    int emptyR = -1, emptyC = -1;
    int minOptions = SIZE + 1;

    for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
            if (grid[r][c] != 0) continue;
            int count = 0;
            for (int v = 1; v <= SIZE; v++) if (domains[r][c][v]) count++;
            if (count < minOptions) {
                minOptions = count; emptyR = r; emptyC = c;
            }
        }
    }

    if (emptyR == -1) return isFullBoardVisibilityValid(grid);

    for (int v = 1; v <= SIZE; v++) {
        if (!domains[emptyR][emptyC][v]) continue;

    

        grid[emptyR][emptyC] = v;
        nodesExplored++;
        
        boolean[][][] nextDomains = copyDomains(domains);
        if (applyForwardChecking(grid, nextDomains, emptyR, emptyC, v)) {
            if (isPathSafe(grid, nextDomains)) {
                
                grid[emptyR][emptyC] = 0;
                return true;
            }
        } else {
            pruned++;
        }

        
        grid[emptyR][emptyC] = 0;
    }
    return false;
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
        for(int i = rev ? SIZE - 1 : 0; rev ? i >= 0 : i < SIZE; i += rev ? -1 : 1) {
            if(line[i] > maxH) { maxH = line[i]; vis++; }
        }
        return vis;
    }

    private int[][] deepCopy(int[][] src){ 
        int[][] c = new int[SIZE][SIZE]; 
        for(int i = 0; i < SIZE; i++) c[i] = src[i].clone(); 
        return c; 
    }

    private String buildExplanation(int[] best, String status) {
        return String.format(
            "【CONSTRAINT ENFORCER】\n" +
            " Move : %d  at  (%d , %d)\n" +
            " Status: %s\n" +
            "────────────────────────────\n" +
            " Nodes Explored : %d\n" +
            " Branches Pruned: %d\n" +
            "════════════════════════════\n" +
            "STRATEGY: DFS accelerated by\n" +
            "Forward Checking. Relaxes to\n" +
            "local safety if board is doomed.",
            best[2], best[0] + 1, best[1] + 1, status, nodesExplored, pruned);
    }
}
