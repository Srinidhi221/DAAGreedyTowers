
public class CellEvaluation {
    public int row, col;
    public double score;
    public String explanation;
    public final int value;

    public CellEvaluation(int row, int col, double score, String explanation, int value) {
        this.row = row;
        this.col = col;
        this.score = score;
        this.explanation = explanation;
        this.value = value;
    }

    public CellEvaluation(int row, int col, double score, String explanation) {
        this.row = row;
        this.col = col;
        this.score = score;
        this.explanation = explanation;
        this.value = -1;
    }
}
