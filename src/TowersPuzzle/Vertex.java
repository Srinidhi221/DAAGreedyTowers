
import java.util.ArrayList;
import java.util.List;

public class Vertex {
    public int row, col, id;
    public List<Edge> incidentEdges = new ArrayList<>();

    public Vertex(int row, int col, int id) {
        this.row = row;
        this.col = col;
        this.id  = id;
    }

    public void addEdge(Edge e) {
        incidentEdges.add(e);
    }
}
