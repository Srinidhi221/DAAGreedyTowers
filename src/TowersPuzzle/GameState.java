
public class GameState {
    private static final int SIZE = 4; // Fixed 4x4
    private TowersConstraintGraph graph = new TowersConstraintGraph();
    private int[][] grid = new int[SIZE][SIZE];

    private int humanScore = 0, cpuScore = 0;
    private int humanLives = 100, cpuLives = 100;
    private boolean isHumanTurn = true;

    private int[] topClues = new int[SIZE];
    private int[] rightClues = new int[SIZE];
    private int[] bottomClues = new int[SIZE];
    private int[] leftClues = new int[SIZE];

    private String statusMessage = "";
    private String cpuReasoningExplanation = "";

    public GameState(int[] top, int[] right, int[] bottom, int[] left) {
        System.arraycopy(top, 0, this.topClues, 0, SIZE);
        System.arraycopy(right, 0, this.rightClues, 0, SIZE);
        System.arraycopy(bottom, 0, this.bottomClues, 0, SIZE);
        System.arraycopy(left, 0, this.leftClues, 0, SIZE);
    }

    public boolean makeMove(int row, int col, int value, boolean isHuman) {
        if (grid[row][col] != 0) {
            statusMessage = "❌ Cell already filled!";
            return false;
        }

        if (graph.hasConflict(grid, row, col, value)) {
            applyPenalty(isHuman, 10, "Constraint violation");
            return false;
        }

        grid[row][col] = value;
        int scoreGain = 0;
        boolean hadViolation = false;

        boolean rowComplete = isRowComplete(row);
        if (rowComplete) {
            if (validateRowVisibility(row)) {
                scoreGain += 15;
            } else {
                applyPenalty(isHuman, 15, "Row visibility violation");
                hadViolation = true;
            }
        }

        boolean colComplete = isColumnComplete(col);
        if (colComplete) {
            if (validateColumnVisibility(col)) {
                scoreGain += 15;
            } else {
                applyPenalty(isHuman, 15, "Column visibility violation");
                hadViolation = true;
            }
        }

        if (!rowComplete && !colComplete) {
            scoreGain = 1;
        }

        if (isHuman)
            humanScore += scoreGain;
        else
            cpuScore += scoreGain;

        if (hadViolation) {
            statusMessage = isHuman ? "⚠️ Move placed but violated clues! -15 lives"
                    : "⚠️ CPU violated clues! -15 lives";
        } else {
            statusMessage = isHuman ? "✓ Valid move! +" + scoreGain + " points"
                    : "✓ CPU scored +" + scoreGain + " points";
        }

        return true;
    }

    public boolean checkForDeadlock(boolean isHuman) {
        if (!hasAnyValidMoves()) {
            applyPenalty(isHuman, 5, "Deadlock - no legal moves");
            return true;
        }
        return false;
    }

    private void applyPenalty(boolean isHuman, int amount, String reason) {
        if (isHuman) {
            humanLives = Math.max(0, humanLives - amount);
            statusMessage = "❌ " + reason + " (-" + amount + " lives) → Lives: " + humanLives;
        } else {
            cpuLives = Math.max(0, cpuLives - amount);
            statusMessage = "❌ CPU " + reason.toLowerCase() + " (-" + amount + " lives) → Lives: " + cpuLives;
        }
    }

    public boolean validateRowVisibility(int row) {
        int leftCount = countVisible(grid[row], true);
        int rightCount = countVisible(grid[row], false);
        return leftCount == leftClues[row] && rightCount == rightClues[row];
    }

    public boolean validateColumnVisibility(int col) {
        int[] colVals = new int[SIZE];
        for (int r = 0; r < SIZE; r++)
            colVals[r] = grid[r][col];
        int topCount = countVisible(colVals, true);
        int bottomCount = countVisible(colVals, false);
        return topCount == topClues[col] && bottomCount == bottomClues[col];
    }

    private int countVisible(int[] buildings, boolean forward) {
        int visible = 0, maxH = 0;
        int start = forward ? 0 : buildings.length - 1;
        int end = forward ? buildings.length : -1;
        int step = forward ? 1 : -1;
        for (int i = start; i != end; i += step) {
            if (buildings[i] > maxH) {
                visible++;
                maxH = buildings[i];
            }
        }
        return visible;
    }

    public boolean isRowComplete(int row) {
        for (int c = 0; c < SIZE; c++)
            if (grid[row][c] == 0)
                return false;
        return true;
    }

    public boolean isColumnComplete(int col) {
        for (int r = 0; r < SIZE; r++)
            if (grid[r][col] == 0)
                return false;
        return true;
    }

    public boolean isBoardFull() {
        for (int[] row : grid)
            for (int v : row)
                if (v == 0)
                    return false;
        return true;
    }

    public int countEmptyInRow(int row) {
        int cnt = 0;
        for (int c = 0; c < SIZE; c++)
            if (grid[row][c] == 0)
                cnt++;
        return cnt;
    }

    public int countEmptyInColumn(int col) {
        int cnt = 0;
        for (int r = 0; r < SIZE; r++)
            if (grid[r][col] == 0)
                cnt++;
        return cnt;
    }

    public boolean hasAnyValidMoves() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        if (!graph.hasConflict(grid, r, c, v))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkLegalMove(int row, int col, int value) {
        if (grid[row][col] != 0) {
            return false;
        }
        if (graph.hasConflict(grid, row, col, value)) {
            return false;
        }
        return true;
    }

    public boolean hasValidMovesForPlayer(boolean isHuman) {
        if (isHuman && !isHumanTurn)
            return true;
        if (!isHuman && isHumanTurn)
            return true;
        return hasAnyValidMoves();
    }

    public String getWinner() {
        if (humanLives <= 0 && cpuLives <= 0) {
            return "DRAW - Double KO!";
        }
        if (humanLives <= 0) {
            return "CPU WINS - Human out of lives!";
        }
        if (cpuLives <= 0) {
            return "HUMAN WINS - CPU out of lives!";
        }

        if (isBoardFull() || !hasAnyValidMoves()) {
            int humanTotal = humanScore + (humanLives / 10);
            int cpuTotal = cpuScore + (cpuLives / 10);

            if (humanTotal > cpuTotal) {
                return String.format("HUMAN WINS - Total: %d vs %d", humanTotal, cpuTotal);
            } else if (cpuTotal > humanTotal) {
                return String.format("CPU WINS - Total: %d vs %d", cpuTotal, humanTotal);
            } else {
                return String.format("DRAW - Equal Totals: %d", humanTotal);
            }
        }
        return null;
    }

    public boolean isGameOver() {
        if (humanLives <= 0 || cpuLives <= 0) {
            return true;
        }
        if (isBoardFull()) {
            return true;
        }
        if (!hasAnyValidMoves()) {
            return true;
        }
        return false;
    }

    public int[][] getGrid() {
        return grid;
    }

    public int getSize() {
        return SIZE;
    }

    public TowersConstraintGraph getGraph() {
        return graph;
    }

    public int getHumanScore() {
        return humanScore;
    }

    public int getCpuScore() {
        return cpuScore;
    }

    public int getHumanLives() {
        return humanLives;
    }

    public int getCpuLives() {
        return cpuLives;
    }

    public int[] getTopClues() {
        return topClues;
    }

    public int[] getRightClues() {
        return rightClues;
    }

    public int[] getBottomClues() {
        return bottomClues;
    }

    public int[] getLeftClues() {
        return leftClues;
    }

    public boolean isHumanTurn() {
        return isHumanTurn;
    }

    public void setHumanTurn(boolean t) {
        isHumanTurn = t;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String m) {
        statusMessage = m;
    }

    public String getCpuReasoningExplanation() {
        return cpuReasoningExplanation;
    }

    public void setCpuReasoningExplanation(String e) {
        cpuReasoningExplanation = e;
    }
}
