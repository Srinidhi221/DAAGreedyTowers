package game;

import java.util.Comparator;

public final class CellSorter {

    private CellSorter() {
    } // Prevent instantiation

    // Returns a comparator that knows the current board size
    public static Comparator<CellEvaluation> getComparator(int size) {
        return (a, b) -> {
            // 1. Primary: Highest score first (most constrained)
            int scoreCmp = Double.compare(b.score, a.score);
            if (scoreCmp != 0)
                return scoreCmp;

            // 2. Secondary: Prefer cells closer to center
            double center = (size - 1) / 2.0;
            double distA = Math.abs(a.row - center) + Math.abs(a.col - center);
            double distB = Math.abs(b.row - center) + Math.abs(a.col - center);
            int distCmp = Double.compare(distA, distB);
            if (distCmp != 0)
                return distCmp;

            // 3. Tertiary: Stable order - top row first, then left column
            if (a.row != b.row)
                return Integer.compare(a.row, b.row);
            return Integer.compare(a.col, b.col);
        };
    }
}