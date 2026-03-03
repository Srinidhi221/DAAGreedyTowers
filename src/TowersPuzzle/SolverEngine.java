import java.util.*;

/**
 * SolverEngine – 4×4 Skyscraper solver: Backtracking + MRV + LCV + Forward Checking.
 * Fires events to a VisualizerCallback so the GUI can animate each step.
 */
public class SolverEngine implements Runnable {

    public static final int N = 4;

    // ── Clues ─────────────────────────────────────────────────────────────────
    public int[] topClues, bottomClues, leftClues, rightClues;

    // ── State ─────────────────────────────────────────────────────────────────
    private int[][] grid = new int[N][N];
    @SuppressWarnings("unchecked")
    private Set<Integer>[][] domains = new Set[N][N];

    // ── Control ───────────────────────────────────────────────────────────────
    public volatile boolean running   = true;
    public volatile boolean paused    = false;
    public volatile long    stepDelay = 300;   // ms between steps
    public int              maxSolutions = 5;

    private final StatsTracker  stats;
    private final SolverCallback callback;

    // ── Callback interface ────────────────────────────────────────────────────
    public interface SolverCallback {
        enum EventType { SELECT, TRY, PRUNE, BACKTRACK, SOLUTION, COMPLETE, RESET }
        void onEvent(EventType type, int row, int col, int value,
                     int[][] gridSnapshot, StatsTracker stats, String message);
    }

    public SolverEngine(StatsTracker stats, SolverCallback cb,
                        int[] top, int[] bottom, int[] left, int[] right) {
        this.stats    = stats;
        this.callback = cb;
        this.topClues    = top;
        this.bottomClues = bottom;
        this.leftClues   = left;
        this.rightClues  = right;
    }

    @Override
    public void run() {
        for (int[] row : grid) Arrays.fill(row, 0);
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                domains[r][c] = new HashSet<>();
                for (int v = 1; v <= N; v++) domains[r][c].add(v);
            }
        stats.reset();
        fire(SolverCallback.EventType.RESET, -1, -1, 0, "Initialised");
        backtrack(0);
        fire(SolverCallback.EventType.COMPLETE, -1, -1, 0,
             "Search complete. Solutions: " + stats.getSolutionsFound());
    }

    // ── Core backtracking ─────────────────────────────────────────────────────

    private void backtrack(int filled) {
        if (!running) return;
        if (stats.getSolutionsFound() >= maxSolutions && maxSolutions > 0) return;

        if (filled == N * N) {
            if (checkAllClues()) {
                stats.solutionFound();
                fire(SolverCallback.EventType.SOLUTION, -1, -1, 0,
                     "✦ SOLUTION #" + stats.getSolutionsFound() + " FOUND!");
                sleep(stepDelay * 4);
            }
            return;
        }

        stats.enterDepth(filled);

        // MRV
        int[] cell = selectMRV();
        int r = cell[0], c = cell[1];
        List<Integer> values = orderLCV(r, c);

        fire(SolverCallback.EventType.SELECT, r, c, 0,
             "MRV → (" + r + "," + c + ")  domain=" + values);

        for (int v : values) {
            if (!running) return;
            if (stats.getSolutionsFound() >= maxSolutions && maxSolutions > 0) return;

            waitIfPaused();
            stats.nodeExplored();
            stats.assignment();
            grid[r][c] = v;

            Map<int[], Integer> removed = forwardCheck(r, c, v);
            boolean wipeout = (removed == null);

            if (!wipeout && isConsistentPartial(r, c)) {
                fire(SolverCallback.EventType.TRY, r, c, v,
                     "Try " + v + " at (" + r + "," + c + ")  depth=" + filled);
                sleep(stepDelay);
                backtrack(filled + 1);
            } else {
                stats.branchPruned();
                fire(SolverCallback.EventType.PRUNE, r, c, v,
                     "PRUNE " + v + " at (" + r + "," + c + ") – constraint violated");
                sleep(stepDelay);
            }

            grid[r][c] = 0;
            if (removed != null) restoreDomains(removed);
        }

        stats.backtrack();
        fire(SolverCallback.EventType.BACKTRACK, r, c, 0,
             "BACKTRACK from (" + r + "," + c + ")");
        sleep(stepDelay / 2);
    }

    // ── MRV ──────────────────────────────────────────────────────────────────

    private int[] selectMRV() {
        int min = N + 1, br = -1, bc = -1;
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] == 0 && domains[r][c].size() < min) {
                    min = domains[r][c].size(); br = r; bc = c;
                }
        return new int[]{br, bc};
    }

    // ── LCV ──────────────────────────────────────────────────────────────────

    private List<Integer> orderLCV(int r, int c) {
        List<Integer> vals = new ArrayList<>(domains[r][c]);
        vals.sort(Comparator.comparingInt(v -> countConstrainedPeers(r, c, v)));
        return vals;
    }

    private int countConstrainedPeers(int r, int c, int val) {
        int cnt = 0;
        for (int col = 0; col < N; col++)
            if (col != c && grid[r][col] == 0 && domains[r][col].contains(val)) cnt++;
        for (int row = 0; row < N; row++)
            if (row != r && grid[row][c] == 0 && domains[row][c].contains(val)) cnt++;
        return cnt;
    }

    // ── Forward checking ──────────────────────────────────────────────────────

    private Map<int[], Integer> forwardCheck(int r, int c, int val) {
        Map<int[], Integer> removed = new HashMap<>();
        for (int col = 0; col < N; col++) {
            if (col != c && grid[r][col] == 0) {
                if (domains[r][col].remove(val)) {
                    removed.put(new int[]{r, col}, val);
                    if (domains[r][col].isEmpty()) { restoreDomains(removed); return null; }
                }
            }
        }
        for (int row = 0; row < N; row++) {
            if (row != r && grid[row][c] == 0) {
                if (domains[row][c].remove(val)) {
                    removed.put(new int[]{row, c}, val);
                    if (domains[row][c].isEmpty()) { restoreDomains(removed); return null; }
                }
            }
        }
        return removed;
    }

    private void restoreDomains(Map<int[], Integer> removed) {
        for (Map.Entry<int[], Integer> e : removed.entrySet())
            domains[e.getKey()[0]][e.getKey()[1]].add(e.getValue());
    }

    // ── Constraint checking ───────────────────────────────────────────────────

    private boolean isConsistentPartial(int r, int c) {
        if (hasDuplicateInRow(r) || hasDuplicateInCol(c)) return false;
        if (isRowFull(r)) {
            if (!checkVisible(grid[r], leftClues[r], true))  return false;
            if (!checkVisible(grid[r], rightClues[r], false)) return false;
        }
        if (isColFull(c)) {
            int[] col = getCol(c);
            if (!checkVisible(col, topClues[c], true))    return false;
            if (!checkVisible(col, bottomClues[c], false)) return false;
        }
        return true;
    }

    private boolean checkAllClues() {
        for (int r = 0; r < N; r++) {
            if (!checkVisible(grid[r], leftClues[r], true))  return false;
            if (!checkVisible(grid[r], rightClues[r], false)) return false;
        }
        for (int c = 0; c < N; c++) {
            int[] col = getCol(c);
            if (!checkVisible(col, topClues[c], true))    return false;
            if (!checkVisible(col, bottomClues[c], false)) return false;
        }
        return true;
    }

    private boolean checkVisible(int[] line, int clue, boolean forward) {
        if (clue == 0) return true;
        int visible = 0, maxH = 0;
        for (int i = 0; i < N; i++) {
            int idx = forward ? i : N - 1 - i;
            if (line[idx] > maxH) { maxH = line[idx]; visible++; }
        }
        return visible == clue;
    }

    private boolean isRowFull(int r) {
        for (int c = 0; c < N; c++) if (grid[r][c] == 0) return false;
        return true;
    }
    private boolean isColFull(int c) {
        for (int r = 0; r < N; r++) if (grid[r][c] == 0) return false;
        return true;
    }
    private int[] getCol(int c) {
        int[] col = new int[N]; for (int r = 0; r < N; r++) col[r] = grid[r][c]; return col;
    }
    private boolean hasDuplicateInRow(int r) {
        boolean[] s = new boolean[N+1];
        for (int c = 0; c < N; c++) {
            int v = grid[r][c]; if (v != 0) { if (s[v]) return true; s[v]=true; }
        }
        return false;
    }
    private boolean hasDuplicateInCol(int c) {
        boolean[] s = new boolean[N+1];
        for (int r = 0; r < N; r++) {
            int v = grid[r][c]; if (v != 0) { if (s[v]) return true; s[v]=true; }
        }
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fire(SolverCallback.EventType type, int r, int c, int v, String msg) {
        int[][] snap = new int[N][N];
        for (int i = 0; i < N; i++) snap[i] = grid[i].clone();
        callback.onEvent(type, r, c, v, snap, stats, msg);
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void waitIfPaused() {
        while (paused && running) sleep(50);
    }

    public int[][] getGrid() { return grid; }
}