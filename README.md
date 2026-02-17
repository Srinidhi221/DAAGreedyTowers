# 4×4 Towers Puzzle Game

An interactive puzzle game with AI opponents demonstrating greedy algorithms, dynamic programming, and divide-and-conquer strategies. Built with Java Swing.

## About the Game

The Towers Puzzle (also known as Skyscraper) is a logic game where you fill a 4×4 grid with numbers 1–4. Each number appears exactly once per row and column. Numbers represent building heights, and border clues tell you how many buildings are visible from that direction (taller buildings block shorter ones behind them).

This is a constraint satisfaction problem combined with permutation validation.

## AI Strategies

The game features six different AI opponents, each using a different algorithmic approach.

### Greedy Algorithms

**Lives-Greedy (Survival)**  
Minimizes risk by choosing cells with the most legal values. Reduces probability of entering a deadlock state.  
Time: O(N³) | Space: O(N²) | Optimality: Local

**Completion-Greedy (Rusher)**  
Completes rows and columns as quickly as possible. Prioritizes nearly complete rows/columns.  
Time: O(N⁴) | Space: O(N²) | Optimality: Local

**Score-Greedy (Gambler)**  
Maximizes immediate score by simulating scoring impact and evaluating row/column completion bonuses.  
Time: O(N⁵) | Space: O(N²) | Optimality: Local

**Constraint-Greedy (MRV)**  
Uses Minimum Remaining Values heuristic from CSP. Selects cells with fewest legal values first.  
Time: O(N³) | Space: O(N²) | Optimality: Local

### Dynamic Programming

Breaks the puzzle into smaller subproblems and stores previously computed board states. Uses memoization to avoid recalculating valid configurations. Builds solution row-by-row with state reuse.  
Time: O(N² · 2^N) | Space: O(N · 2^N) | Optimality: Global

### Divide and Conquer

Solves the puzzle by recursively dividing into smaller subproblems. Solves one row at a time, recursively solves remaining rows, then validates column constraints and visibility clues.  
Time: O(N²) | Space: O(N²) | Optimality: Global

## Algorithm Comparison

| Strategy | Time Complexity | Space Complexity | Optimality |
|----------|-----------------|------------------|------------|
| Lives-Greedy | O(N³) | O(N²) | Local |
| Completion-Greedy | O(N⁴) | O(N²) | Local |
| Score-Greedy | O(N⁵) | O(N²) | Local |
| MRV-Greedy | O(N³) | O(N²) | Local |
| Dynamic Programming | O(N² · 2^N) | O(N · 2^N) | Global |
| Divide & Conquer | O(N²) | O(N²) | Global |

Greedy strategies offer fast performance but may miss optimal solutions. Dynamic programming balances speed and correctness through memoization. Divide and conquer guarantees correctness through exhaustive search.

## Getting Started

**Requirements**
- JDK 8 or higher
- Swing (included in JDK)

**Clone the Repository**
```bash
git clone https://github.com/yourusername/towers-puzzle-game.git
cd towers-puzzle-game
```

**Compile**
```bash
javac game/*.java
```

**Run**
```bash
java game.TowersssGameGUI
```

## Project Structure

```
src/game/
├── CellSorter.java
├── GameCore.java
├── PuzzleGenerator.java
├── StrategyCompletion.java
├── StrategyLives.java
├── StrategyMRV.java
├── StrategyScore.java
├── StrategyDP.java
├── StrategyDnC.java
└── TowersssGameGUI.java
```

## What This Project Demonstrates

- Greedy vs optimal strategies
- Heuristic vs exhaustive search
- Memoization in constraint problems
- Recursive problem decomposition
- Trade-offs between speed and correctness
- Visualization of algorithmic reasoning

The interactive GUI makes abstract algorithmic concepts tangible and observable in real-time.

## Course Information

**Course:** Design and Analysis of Algorithms (23CSE211)  
**Topics:** Greedy Algorithms, Dynamic Programming, Divide and Conquer
