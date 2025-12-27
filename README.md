

## Project Overview
This project implements an interactive **4×4 Towers Puzzle Game** featuring a competitive AI opponent that employs four distinct **greedy algorithm strategies**. Built in Java with Swing GUI, the application demonstrates practical applications of greedy algorithms in game AI and constraint satisfaction problems.

## What is the Towers Puzzle?
The Towers Puzzle is a logic game where:
- Players fill a 4×4 grid with numbers 1-4
- No number repeats in any row or column
- Numbers represent building heights
- Border clues indicate how many "buildings" are visible from each direction (taller buildings obscure shorter ones)

## Greedy Algorithm Implementation
This project showcases **four greedy strategies**, each optimizing different objectives:

### 1. Lives-Greedy Strategy (Survival)
**Objective:** Minimize risk and maximize survival  
**Approach:** Prioritizes cells with fewer legal options to reduce guessing  
**Greedy Choice:** Select moves with highest safety probability

### 2. Completion-Greedy Strategy (Rusher)
**Objective:** Complete the puzzle as quickly as possible  
**Approach:** Focuses on nearly-complete rows and columns  
**Greedy Choice:** Fill cells that maximize completion percentage

### 3. Score-Greedy Strategy (Gambler)
**Objective:** Maximize point accumulation  
**Approach:** Evaluates immediate score gains vs. risk  
**Greedy Choice:** Select highest potential point value moves

### 4. Constraint-Greedy Strategy (MRV)
**Objective:** Apply Minimum Remaining Values heuristic  
**Approach:** Uses CSP principles to reduce branching factor  
**Greedy Choice:** Fill cells with fewest remaining legal values first

## Technical Details

### Core Algorithm Structure
Each strategy implements:
```java
double evaluateCell(int row, int col)
// Calculates heuristic score for each empty cell
// Time Complexity: O(N²) for full board evaluation

int[] findBestMove()
// Selects optimal move based on strategy's greedy criterion
// Time Complexity: O(N³) worst case
```

### Key Features
- **Real-time visualization:** Heat maps show AI decision-making process
- **Move validation:** O(N) constraint checking per move
- **Clue verification:** O(N) visibility calculation per direction
- **Dynamic scoring:** Rewards valid moves, penalizes violations
- **Deadlock detection:** Identifies no-legal-moves scenarios

## How to Run

### Requirements
- Java Development Kit (JDK) 8 or higher
- Standard Swing library (included in JDK)

### Execution
```bash
# Compile
javac game/*.java

# Run
java game.TowersssGameGUI
```

## Project Structure
```
game/
├── TowersssGameGUI.java      # Main GUI and game controller
├── GameState.java             # Game logic and validation
├── PuzzleGenerator.java       # Generates valid puzzles
├── StrategyLives.java         # Survival-focused greedy strategy
├── StrategyCompletion.java    # Completion-focused greedy strategy
├── StrategyScore.java         # Score-maximizing greedy strategy
└── StrategyMRV.java          # Constraint-based greedy strategy
```

## Game Mechanics
- **Scoring:** +10 per correct move, +20 for completing rows/columns
- **Lives System:** Start with 100 lives, lose 5 per invalid move
- **Victory:** Complete board first, eliminate opponent, or achieve higher score
- **Turn-based:** Human vs. AI with selectable strategies

## Educational Significance
This project demonstrates:
1. **Greedy algorithm design** with multiple optimization criteria
2. **Heuristic evaluation** in constrained environments
3. **Trade-offs** between different greedy approaches (optimal vs. practical)
4. **Algorithm visualization** for understanding decision-making processes
5. **Real-world application** of theoretical DSA concepts

## Algorithm Analysis
| Strategy | Time Complexity | Space Complexity | Optimality |
|----------|----------------|------------------|------------|
| Lives    | O(N³)          | O(N²)            | Local      |
| Completion | O(N³)        | O(N²)            | Local      |
| Score    | O(N³)          | O(N²)            | Local      |
| MRV      | O(N³)          | O(N²)            | Local      |

*Note: Greedy algorithms provide locally optimal choices but don't guarantee global optimality.*

## Conclusion
This implementation effectively illustrates how greedy algorithms can be applied to game AI, demonstrating that different greedy criteria lead to distinct behaviors and outcomes. The visual interface makes the abstract algorithmic concepts tangible and observable in real-time.

---
**Course:** Data Structures and Algorithms (23CSE203)  
**Focus:** Greedy Algorithms & Heuristic Problem Solving
