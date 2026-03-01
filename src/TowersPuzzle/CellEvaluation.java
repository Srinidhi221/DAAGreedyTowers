
public class CellEvaluation {
    public int row, col;
    public double score;
    public String explanation;
    public final int value;

    // Explicit Heuristic Fields
    public int mrvCount; 
    public int lcvScore;
public CellEvaluation(int row, int col, int value, int mrvCount, int lcvScore, String explanation) {
        this.row = row;
        this.col = col;
        this.value = value;
        this.mrvCount = mrvCount;
        this.lcvScore = lcvScore;
        this.explanation = explanation;
        this.score = 0.0; // Defaulting score since it's not used by MRV/LCV
    }

    // ── LEGACY CONSTRUCTORS (For Greedy / DP Strategies) ──
    public CellEvaluation(int row, int col, double score, String explanation, int value) {
        this.row = row;
        this.col = col;
        this.score = score;
        this.explanation = explanation;
        this.value = value;
        this.mrvCount = 0;
        this.lcvScore = 0;
    }

    public CellEvaluation(int row, int col, double score, String explanation) {
        this.row = row;
        this.col = col;
        this.score = score;
        this.explanation = explanation;
        this.value = -1;
        this.mrvCount = 0;
        this.lcvScore = 0;
    }
}
