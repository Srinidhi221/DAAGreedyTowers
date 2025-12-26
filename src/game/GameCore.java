package game;

import java.util.*;

//============================================================================
// VERTEX CLASS
//============================================================================
class Vertex {
    int row, col, position;
    List<Edge> incidentEdges = new ArrayList<>();

    Vertex(int row, int col, int position) {
        this.row = row;
        this.col = col;
        this.position = position;
    }

    void addEdge(Edge e) {
        incidentEdges.add(e);
    }
}

//============================================================================
// EDGE CLASS
//============================================================================
class Edge {
    Vertex origin, dest;
    Edge(Vertex origin, Vertex dest) {
        this.origin = origin;
        this.dest = dest;
    }
}

//============================================================================
// TOWERS CONSTRAINT GRAPH (WITH VISIBILITY)
//============================================================================
class TowersConstraintGraph {
    private final int size = 4;
    private List<Vertex> vertices = new ArrayList<>();

    TowersConstraintGraph() {
        buildGraph();
    }

    private void buildGraph() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                vertices.add(new Vertex(r, c, r * size + c));
            }
        }
        for (Vertex v : vertices) {
            for (Vertex u : vertices) {
                if (v != u && (v.row == u.row || v.col == u.col)) {
                    v.addEdge(new Edge(v, u));
                }
            }
        }
    }

    public Vertex getVertex(int row, int col) {
        return vertices.get(row * size + col);
    }

    public boolean hasConflict(int[][] grid, int row, int col, int value) {
        Vertex cell = getVertex(row, col);
        for (Edge e : cell.incidentEdges) {
            Vertex neighbor = e.dest;
            if (grid[neighbor.row][neighbor.col] == value) {
                return true;
            }
        }
        return false;
    }

    // === VISIBILITY CONSTRAINT METHODS ===

    private int visibleSoFar(int[] line) {
        int max = 0, count = 0;
        for (int h : line) {
            if (h == 0) break;
            if (h > max) {
                max = h;
                count++;
            }
        }
        return count;
    }

    private int maxPossibleVisibility(int[] line, int size) {
        int maxSeen = 0, filled = 0;
        for (int h : line) {
            if (h == 0) break;
            maxSeen = Math.max(maxSeen, h);
            filled++;
        }
        return visibleSoFar(line) + (size - filled);
    }

    private boolean violatesVisibility(int[] line, int clue, int size) {
        if (clue == 0) return false;
        int minV = visibleSoFar(line);
        int maxV = maxPossibleVisibility(line, size);
        return clue < minV || clue > maxV;
    }

    private int[] reverse(int[] arr) {
        int[] reversed = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            reversed[i] = arr[arr.length - 1 - i];
        }
        return reversed;
    }

    private int[] getColumn(int[][] grid, int col) {
        int[] column = new int[size];
        for (int r = 0; r < size; r++) {
            column[r] = grid[r][col];
        }
        return column;
    }

    public boolean canPlace(int[][] grid, int row, int col, int value,
                            int leftClue, int rightClue, int topClue, int bottomClue) {
        if (hasConflict(grid, row, col, value)) {
            return false;
        }

        int original = grid[row][col];
        grid[row][col] = value;
        boolean valid = true;

        if (violatesVisibility(grid[row], leftClue, size)) valid = false;
        if (valid && violatesVisibility(reverse(grid[row]), rightClue, size)) valid = false;
        if (valid && violatesVisibility(getColumn(grid, col), topClue, size)) valid = false;
        if (valid && violatesVisibility(reverse(getColumn(grid, col)), bottomClue, size)) valid = false;

        grid[row][col] = original;
        return valid;
    }

    public boolean validateCompleteLine(int[] line, int clue) {
        if (clue == 0) return true;
        int visible = 0, maxHeight = 0;
        for (int height : line) {
            if (height == 0) return false;
            if (height > maxHeight) {
                maxHeight = height;
                visible++;
            }
        }
        return visible == clue;
    }

    public boolean validatePuzzle(int[][] grid, int[] topClues, int[] rightClues,
                                  int[] bottomClues, int[] leftClues) {
        for (int r = 0; r < size; r++) {
            if (!validateCompleteLine(grid[r], leftClues[r])) return false;
            if (!validateCompleteLine(reverse(grid[r]), rightClues[r])) return false;
        }
        for (int c = 0; c < size; c++) {
            int[] col = getColumn(grid, c);
            if (!validateCompleteLine(col, topClues[c])) return false;
            if (!validateCompleteLine(reverse(col), bottomClues[c])) return false;
        }
        return true;
    }
}

//============================================================================
// PUZZLE GENERATOR (WITH VISIBILITY VALIDATION)
//============================================================================
class PuzzleGenerator {
    private static final int SIZE = 4;
    private Random random;
    private TowersConstraintGraph graph;

    public PuzzleGenerator() {
        this.random = new Random();
        this.graph = new TowersConstraintGraph();
    }

    private int calculateVisibility(int[] line) {
        int visible = 0, maxHeight = 0;
        for (int height : line) {
            if (height > maxHeight) {
                maxHeight = height;
                visible++;
            }
        }
        return visible;
    }

    private void generateCluesFromSolution(int[][] grid, int[] topClues, int[] rightClues,
                                           int[] bottomClues, int[] leftClues) {
        for (int r = 0; r < SIZE; r++) {
            leftClues[r] = calculateVisibility(grid[r]);
            int[] reversed = new int[SIZE];
            for (int c = 0; c < SIZE; c++) {
                reversed[c] = grid[r][SIZE - 1 - c];
            }
            rightClues[r] = calculateVisibility(reversed);
        }

        for (int c = 0; c < SIZE; c++) {
            int[] column = new int[SIZE];
            for (int r = 0; r < SIZE; r++) {
                column[r] = grid[r][c];
            }
            topClues[c] = calculateVisibility(column);

            int[] reversed = new int[SIZE];
            for (int r = 0; r < SIZE; r++) {
                reversed[r] = grid[SIZE - 1 - r][c];
            }
            bottomClues[c] = calculateVisibility(reversed);
        }
    }

    private boolean generateLatinSquare(int[][] grid, int row, int col) {
        if (row == SIZE) return true;

        int nextRow = (col == SIZE - 1) ? row + 1 : row;
        int nextCol = (col == SIZE - 1) ? 0 : col + 1;

        List<Integer> values = Arrays.asList(1, 2, 3, 4);
        Collections.shuffle(values, random);

        for (int value : values) {
            if (!graph.hasConflict(grid, row, col, value)) {
                grid[row][col] = value;
                if (generateLatinSquare(grid, nextRow, nextCol)) {
                    return true;
                }
                grid[row][col] = 0;
            }
        }
        return false;
    }

    public PuzzleData generatePuzzle() {
        int[][] grid = new int[SIZE][SIZE];
        int[] topClues = new int[SIZE];
        int[] rightClues = new int[SIZE];
        int[] bottomClues = new int[SIZE];
        int[] leftClues = new int[SIZE];

        // Generate a valid Latin square first
        if (generateLatinSquare(grid, 0, 0)) {
            // Derive valid clues from the solution
            generateCluesFromSolution(grid, topClues, rightClues, bottomClues, leftClues);
            return new PuzzleData(topClues, rightClues, bottomClues, leftClues);
        }

        // Fallback to known valid puzzle
        return new PuzzleData(
                new int[]{2, 3, 1, 2},
                new int[]{1, 3, 2, 2},
                new int[]{3, 1, 2, 2},
                new int[]{2, 2, 3, 1}
        );
    }

    public static class PuzzleData {
        public final int[] topClues, rightClues, bottomClues, leftClues;

        public PuzzleData(int[] top, int[] right, int[] bottom, int[] left) {
            this.topClues = top;
            this.rightClues = right;
            this.bottomClues = bottom;
            this.leftClues = left;
        }
    }
}

//============================================================================
// GAME STATE (FIXED WITH VISIBILITY)
//============================================================================
class GameState {
    private static final int SIZE = 4;
    private TowersConstraintGraph graph = new TowersConstraintGraph();
    private int[][] grid = new int[SIZE][SIZE];

    private int humanScore = 0, cpuScore = 0;
    private int humanLives = 100, cpuLives = 100;
    private boolean isHumanTurn = true;

    private int[] topClues, rightClues, bottomClues, leftClues;
    private String statusMessage = "";
    private String cpuReasoningExplanation = "";

    GameState(int[] top, int[] right, int[] bottom, int[] left) {
        this.topClues = Arrays.copyOf(top, SIZE);
        this.rightClues = Arrays.copyOf(right, SIZE);
        this.bottomClues = Arrays.copyOf(bottom, SIZE);
        this.leftClues = Arrays.copyOf(left, SIZE);
    }

    public boolean makeMove(int row, int col, int value, boolean isHuman) {
        if (grid[row][col] != 0) {
            statusMessage = "❌ Cell already filled!";
            return false;
        }

        // FIXED: Use canPlace with visibility constraints
        if (!graph.canPlace(grid, row, col, value,
                leftClues[row], rightClues[row],
                topClues[col], bottomClues[col])) {
            applyPenalty(isHuman, 10, "Invalid move (violates constraints)");
            return false;
        }

        grid[row][col] = value;
        int scoreGain = 1;

        boolean rowComplete = isRowComplete(row);
        boolean colComplete = isColumnComplete(col);

        if (rowComplete) {
            scoreGain += 10;
            if (validateRowVisibility(row)) scoreGain += 15;
            else applyPenalty(isHuman, 15, "Row visibility violation");
        }
        if (colComplete) {
            scoreGain += 10;
            if (validateColumnVisibility(col)) scoreGain += 15;
            else applyPenalty(isHuman, 15, "Column visibility violation");
        }

        if (isHuman) humanScore += scoreGain;
        else cpuScore += scoreGain;

        if (!hasAnyValidMoves()) {
            applyPenalty(isHuman, 5, "Deadlock - no legal moves");
        }

        statusMessage = isHuman ? "✓ Valid move! +" + scoreGain : "✓ CPU move! +" + scoreGain;
        return true;
    }

    private void applyPenalty(boolean isHuman, int amount, String reason) {
        if (isHuman) {
            humanLives = Math.max(0, humanLives - amount);
            statusMessage = "❌ " + reason + " (-" + amount + " lives)";
        } else {
            cpuLives = Math.max(0, cpuLives - amount);
            statusMessage = "❌ CPU " + reason.toLowerCase() + " (-" + amount + " lives)";
        }
    }

    public boolean validateRowVisibility(int row) {
        int leftCount = countVisible(grid[row], true);
        int rightCount = countVisible(grid[row], false);
        return leftCount == leftClues[row] && rightCount == rightClues[row];
    }

    public boolean validateColumnVisibility(int col) {
        int[] colVals = new int[SIZE];
        for (int r = 0; r < SIZE; r++) colVals[r] = grid[r][col];
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
        for (int c = 0; c < SIZE; c++) if (grid[row][c] == 0) return false;
        return true;
    }

    public boolean isColumnComplete(int col) {
        for (int r = 0; r < SIZE; r++) if (grid[r][col] == 0) return false;
        return true;
    }

    public boolean isBoardFull() {
        for (int[] row : grid) for (int v : row) if (v == 0) return false;
        return true;
    }

    public int countEmptyInRow(int row) {
        int cnt = 0;
        for (int c = 0; c < SIZE; c++) if (grid[row][c] == 0) cnt++;
        return cnt;
    }

    public int countEmptyInColumn(int col) {
        int cnt = 0;
        for (int r = 0; r < SIZE; r++) if (grid[r][col] == 0) cnt++;
        return cnt;
    }

    // FIXED: Check valid moves with visibility
    public boolean hasAnyValidMoves() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        if (graph.canPlace(grid, r, c, v,
                                leftClues[r], rightClues[r],
                                topClues[c], bottomClues[c])) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public String getWinner() {
        if (humanLives <= 0 && cpuLives <= 0) return "DRAW - Double KO";
        if (humanLives <= 0) return "CPU WINS";
        if (cpuLives <= 0) return "HUMAN WINS";
        if (isBoardFull() || !hasAnyValidMoves()) {
            int hTotal = humanScore + humanLives / 10;
            int cTotal = cpuScore + cpuLives / 10;
            if (hTotal > cTotal) return "HUMAN WINS";
            if (cTotal > hTotal) return "CPU WINS";
            return "DRAW";
        }
        return null;
    }

    public boolean isGameOver() {
        return humanLives <= 0 || cpuLives <= 0 || isBoardFull() || !hasAnyValidMoves();
    }

    // Getters
    public int[][] getGrid() { return grid; }
    public int getSize() { return SIZE; }
    public TowersConstraintGraph getGraph() { return graph; }
    public int getHumanScore() { return humanScore; }
    public int getCpuScore() { return cpuScore; }
    public int getHumanLives() { return humanLives; }
    public int getCpuLives() { return cpuLives; }
    public int[] getTopClues() { return topClues; }
    public int[] getRightClues() { return rightClues; }
    public int[] getBottomClues() { return bottomClues; }
    public int[] getLeftClues() { return leftClues; }
    public boolean isHumanTurn() { return isHumanTurn; }
    public void setHumanTurn(boolean t) { isHumanTurn = t; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String m) { statusMessage = m; }
    public String getCpuReasoningExplanation() { return cpuReasoningExplanation; }
    public void setCpuReasoningExplanation(String e) { cpuReasoningExplanation = e; }
}

//============================================================================
// CELL EVALUATION
//============================================================================
class CellEvaluation {
    int row, col;
    double score;
    String explanation;
    CellEvaluation(int row, int col, double score, String explanation) {
        this.row = row;
        this.col = col;
        this.score = score;
        this.explanation = explanation;
    }
}