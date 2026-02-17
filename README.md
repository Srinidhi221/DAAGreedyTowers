# 🏗️ 4×4 Towers Puzzle Game  
### Greedy, Dynamic Programming & Divide-and-Conquer AI

---

## 📌 Project Overview
This project implements an interactive **4×4 Towers (Skyscraper) Puzzle Game** featuring a CPU opponent that employs multiple algorithmic strategies.

Built in **Java using Swing GUI**, the application demonstrates practical applications of:

- Greedy Algorithms  
- Dynamic Programming  
- Divide and Conquer  
- Constraint Satisfaction Problems (CSP)  
- Heuristic Game AI  

---

## 🧩 What is the Towers Puzzle?

The Towers Puzzle is a logic game where:

- Players fill a 4×4 grid with numbers **1–4**
- No number repeats in any row or column
- Numbers represent building heights
- Border clues indicate how many "buildings" are visible from each direction  
  (Taller buildings obscure shorter ones)

It is essentially a **constraint satisfaction + permutation validation problem**.

---

# 🟢 Greedy Algorithm Implementation

This project initially implemented **four greedy strategies**, each optimizing different objectives.

---

## 1️⃣ Lives-Greedy Strategy (Survival)

**Objective:** Minimize risk and maximize survival  

**Approach:**  
- Evaluate all empty cells  
- Prefer cells with maximum legal values  
- Reduce deadlock probability  

**Greedy Choice:**  
Select the safest move with highest flexibility.

**Time Complexity:** `O(N³)`  
**Space Complexity:** `O(N²)`  
**Optimality:** Local

---

## 2️⃣ Completion-Greedy Strategy (Rusher)

**Objective:** Complete rows and columns quickly  

**Approach:**  
- Prioritize nearly complete rows/columns  
- Fill cells that maximize completion percentage  

**Greedy Choice:**  
Choose move that increases completion ratio the most.

**Time Complexity:** `O(N⁴)`  
**Space Complexity:** `O(N²)`  
**Optimality:** Local

---

## 3️⃣ Score-Greedy Strategy (Gambler)

**Objective:** Maximize immediate score  

**Approach:**  
- Simulate scoring impact  
- Evaluate row/column completion bonus  
- Compare risk vs reward  

**Greedy Choice:**  
Select highest immediate point gain.

**Time Complexity:** `O(N⁵)`  
**Space Complexity:** `O(N²)`  
**Optimality:** Local

---

## 4️⃣ Constraint-Greedy Strategy (MRV)

(Minimum Remaining Values Heuristic)

**Objective:** Reduce branching factor  

**Approach:**  
- Select cell with fewest legal values  
- Apply CSP principles  

**Greedy Choice:**  
Fill cell with minimum remaining legal values first.

**Time Complexity:** `O(N³)`  
**Space Complexity:** `O(N²)`  
**Optimality:** Local (Heuristic-based)

---

# 🔵 Phase 2: Advanced Algorithmic Strategies

To extend the project beyond greedy heuristics, two advanced approaches were implemented:

---

## 5️⃣ Dynamic Programming Strategy

**Objective:** Avoid recomputation and optimize subproblem reuse.

### Core Idea
- Break puzzle into smaller subproblems
- Store previously computed board states
- Use memoization to avoid recalculating valid configurations

### DP State May Include:
- Current row index  
- Column usage mask  
- Used numbers mask  
- Partial visibility validation  

### Working Principle:
1. Generate valid row permutations  
2. Use memoization for partial board states  
3. Build solution row-by-row  
4. Reuse previously computed configurations  

### Time Complexity:
- Row permutations: `O(N!)`  
- DP transitions: `O(N² · 2^N)`  
- Optimized practical complexity: `≈ O(N² · 2^N)`

### Space Complexity:
`O(N · 2^N)`

### Optimality:
Global (finds correct solution if fully explored)

---

## 6️⃣ Divide and Conquer Strategy

**Objective:** Solve puzzle by recursively dividing into smaller subproblems.

### Core Idea
1. Divide:
   - Solve one row at a time  

2. Conquer:
   - Recursively solve remaining rows  

3. Combine:
   - Validate column constraints  
   - Validate visibility clues  

### Recurrence:
T(N) = N × T(N-1)

### Time Complexity:
`O(N!)`

### Space Complexity:
`O(N²)`

### Optimality:
Global (exhaustive search)

---

# ⚙️ Core Algorithm Structure

Each greedy strategy implements:

```java
double evaluateCell(int row, int col);
// Calculates heuristic score

int[] findBestMove();
// Selects best move based on strategy
```

DP & D&C strategies implement:

```java
boolean solveBoard(int row);
// Recursive or memoized solver
```

---

## 🎮 Key Features

- Real-time visualization  
- Heat map showing AI decision-making  
- O(N) constraint checking per move  
- O(N) visibility calculation per direction  
- Dynamic scoring system  
- Deadlock detection  
- Strategy selector for CPU  

---

## 📁 Project Structure

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
├── StrategyDivideConquer.java
├── TowersssGameGUI.java
└── README.md
```

---

## ▶️ How to Run

### Requirements
- JDK 8 or higher  
- Swing (included in JDK)

### Compile
```bash
javac game/*.java
```

### Run
```bash
java game.TowersssGameGUI
```

---

## 📊 Algorithm Comparison

| Strategy              | Time Complexity     | Space Complexity | Optimality |
|----------------------|--------------------|------------------|------------|
| Lives-Greedy         | O(N³)              | O(N²)            | Local      |
| Completion-Greedy    | O(N⁴)              | O(N²)            | Local      |
| Score-Greedy         | O(N⁵)              | O(N²)            | Local      |
| MRV-Greedy           | O(N³)              | O(N²)            | Local      |
| Divide & Conquer     | O(N!)              | O(N²)            | Global     |
| Dynamic Programming  | O(N² · 2^N)        | O(N · 2^N)       | Global     |

---

## 🎓 Educational Significance

This project demonstrates:

- Greedy vs Optimal strategies  
- Heuristic vs Exhaustive search  
- Memoization in constraint problems  
- Recursive problem decomposition  
- Trade-offs between speed and correctness  
- Visualization of algorithmic reasoning  

---

## 🏁 Conclusion

This project evolved from a Greedy-based AI into a complete comparative study of:

- Greedy Algorithms  
- Dynamic Programming  
- Divide and Conquer  

It highlights the trade-offs between:

- Speed vs Completeness  
- Local vs Global Optimization  
- Heuristics vs Exact Methods  

The interactive GUI makes abstract algorithmic concepts tangible and observable in real-time.

---

**Course:** Design and Analysis of Algorithms (23CSE211)  
**Focus:** Greedy Algorithms, Dynamic Programming & Divide and Conquer  
