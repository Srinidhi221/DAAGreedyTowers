package game;

import java.util.Comparator;

/*
 * Utility class responsible for ordering candidate cells
 * based on heuristic priority and positional preference
 */
public final class CellSorter {

    // Prevent object creation (static utility class)
    private CellSorter() {
    }

    /*
     * Returns a comparator customized for the current board size.
     * Ordering criteria:
     * 1) MRV score (descending)
     * 2) Distance from board center
     * 3) Stable row–column ordering
     */
    public static Comparator<CellEvaluation> getComparator(int size) {

        return (a, b) -> {

            // Primary criterion: higher heuristic score first
            int scoreCmp = Double.compare(b.score, a.score);
            if (scoreCmp != 0) {
                return scoreCmp;
            }

            // Secondary criterion: favor cells nearer to center
            double center = (size - 1) / 2.0;

            double distA = Math.abs(a.row - center) + Math.abs(a.col - center);
            double distB = Math.abs(b.row - center) + Math.abs(a.col - center);

            int distCmp = Double.compare(distA, distB);
            if (distCmp != 0) {
                return distCmp;
            }

            // Tertiary criterion: deterministic ordering
            if (a.row != b.row) {
                return Integer.compare(a.row, b.row);
            }

            return Integer.compare(a.col, b.col);
        };
    }
}
