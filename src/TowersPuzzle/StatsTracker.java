/**
 * StatsTracker – live complexity metrics for the backtracking search.
 */
public class StatsTracker {
    private long nodesExplored;
    private long branchesPruned;
    private int  currentDepth;
    private int  maxDepth;
    private long totalAssignments;
    private long backtrackCount;
    private long solutionsFound;
    private long startTime;

    public StatsTracker() { reset(); }

    public void reset() {
        nodesExplored = branchesPruned = totalAssignments = backtrackCount = solutionsFound = 0;
        currentDepth = maxDepth = 0;
        startTime = System.currentTimeMillis();
    }

    public void nodeExplored()  { nodesExplored++; }
    public void branchPruned()  { branchesPruned++; }
    public void assignment()    { totalAssignments++; }
    public void backtrack()     { backtrackCount++; }
    public void solutionFound() { solutionsFound++; }

    public void enterDepth(int d) {
        currentDepth = d;
        if (d > maxDepth) maxDepth = d;
    }

    public long getNodesExplored()  { return nodesExplored; }
    public long getBranchesPruned() { return branchesPruned; }
    public int  getCurrentDepth()   { return currentDepth; }
    public int  getMaxDepth()       { return maxDepth; }
    public long getBacktrackCount() { return backtrackCount; }
    public long getSolutionsFound() { return solutionsFound; }
    public long getTotalAssignments(){ return totalAssignments; }

    public double estimatedBranchingFactor() {
        long d = backtrackCount + solutionsFound + 1;
        return (double) totalAssignments / d;
    }

    public long elapsedMs() { return System.currentTimeMillis() - startTime; }
}