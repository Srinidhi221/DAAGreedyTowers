
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
            double distB = Math.abs(b.row - center) + Math.abs(b.col - center);

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
      public static Comparator<CellEvaluation> getMrvComparator(int size) {
        return (a, b) -> {
            // Primary criterion: MRV (Lowest remaining values first) 
            int mrvCmp = Integer.compare(a.mrvCount, b.mrvCount);
            if (mrvCmp != 0) {
                return mrvCmp;
            }

            // Secondary criterion: Favor cells nearer to center (Manhattan distance)
            double center = (size - 1) / 2.0;
            double distA = Math.abs(a.row - center) + Math.abs(a.col - center);
            
            // Bug fix: The original code used 'a.col' here instead of 'b.col'
            double distB = Math.abs(b.row - center) + Math.abs(b.col - center);

            int distCmp = Double.compare(distA, distB);
            if (distCmp != 0) {
                return distCmp;
            }

            // Tertiary criterion: Deterministic ordering to prevent random behavior
            if (a.row != b.row) {
                return Integer.compare(a.row, b.row);
            }
            return Integer.compare(a.col, b.col);
        };
    }

    /*
     * 2. LCV COMPARATOR (Value Ordering)
     * Ranks the numbers 1 through N for a single chosen cell.
     * Orders descending by lcvScore (higher future options is better).
     */
    public static Comparator<CellEvaluation> getLcvComparator() {
        return (a, b) -> {
            // Primary criterion: LCV (Highest future options first) 
            int lcvCmp = Integer.compare(b.lcvScore, a.lcvScore);
            if (lcvCmp != 0) {
                return lcvCmp;
            }

            // Secondary criterion: Deterministic tie-breaker
            return Integer.compare(b.value, a.value);
        };
    }

    /*
     * 3. LEGACY COMPARATOR (General Score)
     * Retained for backward compatibility with Greedy and Divide & Conquer.
     */
    public static Comparator<CellEvaluation> getLegacyComparator(int size) {
        return (a, b) -> {
            // Primary criterion: higher heuristic score first
            int scoreCmp = Double.compare(b.score, a.score);
            if (scoreCmp != 0) {
                return scoreCmp;
            }

            // Secondary criterion: favor cells nearer to center
            double center = (size - 1) / 2.0;
            double distA = Math.abs(a.row - center) + Math.abs(a.col - center);
            double distB = Math.abs(b.row - center) + Math.abs(b.col - center);

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
