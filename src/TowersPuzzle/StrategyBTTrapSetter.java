import java.util.*;

/**
 * STRATEGY : Backtracking (The "Trap Setter")
 * Utilizes Exhaustive DFS optimized by MRV/LCV Heuristics.
 */
public class StrategyBTTrapSetter {

    private final GameState state;
    private final int SIZE;

    private int nodesExplored = 0;
    private int pruned = 0;

    // Safety valve to prevent the UI from freezing on early turns
    private static final int SOLUTION_LIMIT = 50;

    public StrategyBTTrapSetter(GameState state) {
        this.state = state;
        this.SIZE = state.getSize();
    }

    
    //THE ADVERSARIAL WRAPPER
    
    public int[] findBestMove() {
        nodesExplored = 0;
        pruned = 0;

        int[][] grid = deepCopy(state.getGrid());
        int[] bestMove = { -1, -1, -1 };
        int[] fallbackMove = { -1, -1, -1 };
        int minValidFutures = Integer.MAX_VALUE;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] != 0)
                    continue;

                for (int v = 1; v <= SIZE; v++) {
                    if (state.getGraph().hasConflict(grid, r, c, v)) {
                        pruned++;
                        continue;
                    }

                    // Save first legal move as fallback
                    if (fallbackMove[0] == -1) {
                        fallbackMove = new int[] { r, c, v };
                    }

                    grid[r][c] = v;
                    nodesExplored++;

                    int validFutures = countSolutions(grid, 0);

                    if (validFutures > 0 && validFutures < minValidFutures) {
                        minValidFutures = validFutures;
                        bestMove[0] = r;
                        bestMove[1] = c;
                        bestMove[2] = v;
                    }

                    grid[r][c] = 0;
                }
            }
        }

        // If no trapping move found, use fallback legal move
        if (bestMove[0] == -1) {
            if (fallbackMove[0] == -1)
                return null;
            bestMove = fallbackMove;
            minValidFutures = 0;
        }

        state.setCpuReasoningExplanation(buildExplanation(bestMove, minValidFutures));
        return bestMove;
    }

    //THE CORE DFS COUNTER
    private int countSolutions(int[][] grid, int currentCount) {
        if (currentCount >= SOLUTION_LIMIT)
            return currentCount;

        //Use MRV to find the most constrained cell
        CellEvaluation bestCell = getBestCellMRV(grid);

        // BASE CASE: No empty cells found! Board is full.
        if (bestCell == null) {
            //
            if (isFullBoardVisibilityValid(grid)) {
                return currentCount + 1;
            }
            return currentCount;
        }

        // If MRV found a cell with 0 options, this branch is a dead end
        if (bestCell.mrvCount == 0)
            return currentCount;

        // Use LCV to sort the numbers we try in this cell
        List<CellEvaluation> sortedValues = getSortedValuesLCV(grid, bestCell.row, bestCell.col);

        // RECURSIVE CASE: Try the sorted values
        for (CellEvaluation eval : sortedValues) {
            int v = eval.value;

            grid[bestCell.row][bestCell.col] = v;
            nodesExplored++;

            currentCount = countSolutions(grid, currentCount); // Recurse

            grid[bestCell.row][bestCell.col] = 0; // Backtrack (Undo)
        }

        return currentCount;
    }


    //THE HEURISTIC OPTIMIZERS (MRV & LCV)
    
    
// Minimum Remaining Values (MRV) - Finds the most constrained cell
    private CellEvaluation getBestCellMRV(int[][] grid) {
        // 
        List<CellEvaluation> emptyCells = new ArrayList<>();
        
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    int validOptions = 0;
                    for (int v = 1; v <= SIZE; v++) {
                        if (!state.getGraph().hasConflict(grid, r, c, v)) {
                            validOptions++;
                        }
                    }
                    
                    // If a cell has 0 options, the board is dead. Return it immediately to force a prune.
                    if (validOptions == 0) return new CellEvaluation(r, c, -1, 0, 0, "");
                    
                    emptyCells.add(new CellEvaluation(r, c, -1, validOptions, 0, ""));
                }
            }
        }
        
        if (emptyCells.isEmpty()) return null; // Board is full
        
        // Sort using the custom Comparator built in CellSorter
        emptyCells.sort(CellSorter.getMrvComparator(SIZE));
        return emptyCells.get(0); 
    }
    
    // Least Constraining Value (LCV) - Ranks the numbers 1 through SIZE
    private List<CellEvaluation> getSortedValuesLCV(int[][] grid, int r, int c) {
        List<CellEvaluation> valueCandidates = new ArrayList<>();
        
        for (int v = 1; v <= SIZE; v++) {
            if (state.getGraph().hasConflict(grid, r, c, v)) {
                pruned++;
                continue;
            }
            
            // Simulate the move to calculate its LCV score
            grid[r][c] = v;
            int futureOptions = countImmediateFutureOptions(grid);
            grid[r][c] = 0; // Undo
            
            valueCandidates.add(new CellEvaluation(r, c, v, 0, futureOptions, ""));
        }
        
        valueCandidates.sort(CellSorter.getLcvComparator());
        return valueCandidates;
    }
    // Helper for LCV: Counts how many total legal placements remain on the board immediately after a move
    private int countImmediateFutureOptions(int[][] grid) {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        if (!state.getGraph().hasConflict(grid, r, c, v)) count++;
                    }
                }
            }
        }
        return count;
    }

    // VISIBILITY VALIDATORS & UTILS (Shared Architecture)

    private boolean isFullBoardVisibilityValid(int[][] grid) {
        for (int i = 0; i < SIZE; i++) {
            if (!rowVisOk(grid, i) || !colVisOk(grid, i))
                return false;
        }
        return true;
    }

    private boolean rowVisOk(int[][] g, int row) {
        int left = state.getLeftClues()[row];
        int right = state.getRightClues()[row];

        // Safely ignore missing (0) clues
        if (left != 0 && countVis(g[row], false) != left)
            return false;
        if (right != 0 && countVis(g[row], true) != right)
            return false;
        return true;
    }

    private boolean colVisOk(int[][] g, int col) {
        int top = state.getTopClues()[col];
        int bottom = state.getBottomClues()[col];

        int[] arr = new int[SIZE];
        for (int r = 0; r < SIZE; r++)
            arr[r] = g[r][col];

        // Safely ignore missing (0) clues
        if (top != 0 && countVis(arr, false) != top)
            return false;
        if (bottom != 0 && countVis(arr, true) != bottom)
            return false;
        return true;
    }

    private int countVis(int[] line, boolean rev) {
        int vis = 0, maxH = 0;
        for (int i = rev ? SIZE - 1 : 0; rev ? i >= 0 : i < SIZE; i += rev ? -1 : 1) {
            if (line[i] > maxH) {
                maxH = line[i];
                vis++;
            }
        }
        return vis;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] c = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++)
            c[i] = src[i].clone();
        return c;
    }

    private String buildExplanation(int[] best, int minValidFutures) {
        String futures = minValidFutures >= SOLUTION_LIMIT ? SOLUTION_LIMIT + "+" : String.valueOf(minValidFutures);
        return String.format(
                "【HEURISTIC TRAP-SETTER】\n" +
                        " Move : %d  at  (%d , %d)\n" +
                        " Futures remaining: %s\n" +
                        "────────────────────────────\n" +
                        " Nodes Explored : %d\n" +
                        " Branches Pruned: %d\n" +
                        "════════════════════════════\n" +
                        "STRATEGY: DFS optimized by\n" +
                        "MRV & LCV heuristics to trap\n" +
                        "the opponent efficiently.",
                best[2], best[0] + 1, best[1] + 1, futures, nodesExplored, pruned);
    }

}

