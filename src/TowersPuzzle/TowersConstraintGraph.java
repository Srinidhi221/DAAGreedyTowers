
import java.util.ArrayList;
import java.util.List;

public class TowersConstraintGraph {
    private final int size = 4; // Fixed 4x4
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

    public boolean canPlace(int[][] grid, int row, int col, int value,
            int leftClue, int rightClue,
            int topClue, int bottomClue) {
        if (hasConflict(grid, row, col, value)) {
            return false;
        }
        return true;
    }
}
