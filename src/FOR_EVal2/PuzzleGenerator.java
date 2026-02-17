package FOR_EVal2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleGenerator {
    private static final int N = 4;
    private Random random = new Random();

    public static class PuzzleData {
        public int[] topClues;
        public int[] rightClues;
        public int[] bottomClues;
        public int[] leftClues;
        public int[][] solution;

        public PuzzleData(int[] top, int[] right, int[] bottom, int[] left, int[][] sol) {
            this.topClues = top;
            this.rightClues = right;
            this.bottomClues = bottom;
            this.leftClues = left;
            this.solution = sol;
        }
    }

    public PuzzleData generatePuzzle() {
        int[][] grid = generateValidGrid();

        int[] topClues = new int[N];
        int[] rightClues = new int[N];
        int[] bottomClues = new int[N];
        int[] leftClues = new int[N];

        for (int i = 0; i < N; i++) {
            leftClues[i] = countVisible(getRow(grid, i), true);
            rightClues[i] = countVisible(getRow(grid, i), false);
            topClues[i] = countVisible(getColumn(grid, i), true);
            bottomClues[i] = countVisible(getColumn(grid, i), false);
        }

        return new PuzzleData(topClues, rightClues, bottomClues, leftClues, grid);
    }

    private int[][] generateValidGrid() {
        int[][] grid = new int[N][N];
        if (fillGrid(grid, 0, 0)) {
            return grid;
        }
        return createSimpleValidGrid();
    }

    private boolean fillGrid(int[][] grid, int row, int col) {
        if (row == N) return true;

        int nextRow = (col == N - 1) ? row + 1 : row;
        int nextCol = (col == N - 1) ? 0 : col + 1;

        List<Integer> values = new ArrayList<>();
        for (int i = 1; i <= N; i++) values.add(i);
        Collections.shuffle(values, random);

        for (int val : values) {
            if (isValidPlacement(grid, row, col, val)) {
                grid[row][col] = val;
                if (fillGrid(grid, nextRow, nextCol)) {
                    return true;
                }
                grid[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isValidPlacement(int[][] grid, int row, int col, int val) {
        for (int c = 0; c < N; c++) {
            if (grid[row][c] == val) return false;
        }
        for (int r = 0; r < N; r++) {
            if (grid[r][col] == val) return false;
        }
        return true;
    }

    private int[] getRow(int[][] grid, int row) {
        return grid[row].clone();
    }

    private int[] getColumn(int[][] grid, int col) {
        int[] column = new int[N];
        for (int r = 0; r < N; r++) {
            column[r] = grid[r][col];
        }
        return column;
    }

    private int countVisible(int[] line, boolean fromStart) {
        int count = 0;
        int maxSeen = 0;

        if (fromStart) {
            for (int i = 0; i < N; i++) {
                if (line[i] > maxSeen) {
                    count++;
                    maxSeen = line[i];
                }
            }
        } else {
            for (int i = N - 1; i >= 0; i--) {
                if (line[i] > maxSeen) {
                    count++;
                    maxSeen = line[i];
                }
            }
        }
        return count;
    }

    private int[][] createSimpleValidGrid() {
        return new int[][] {
                {1, 2, 3, 4},
                {2, 3, 4, 1},
                {3, 4, 1, 2},
                {4, 1, 2, 3}
        };
    }
}
