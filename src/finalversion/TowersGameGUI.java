package game;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TowersGameGUI extends JFrame {
    private static final int N = 4;
    private static final int[] TOP = {1, 3, 2, 2};
    private static final int[] RIGHT = {3, 2, 1, 2};
    private static final int[] BOTTOM = {3, 1, 2, 2};
    private static final int[] LEFT = {1, 3, 2, 2};

    private int[][] grid = new int[N][N];
    private boolean[][] rowUsed = new boolean[N][N+1];
    private boolean[][] colUsed = new boolean[N][N+1];
    
    private int humanScore = 0, cpuScore = 0;
    private int humanLives = 100, cpuLives = 100;
    private boolean humanTurn = true, gameOver = false;
    private int selectedRow = -1, selectedCol = -1;
    
    private JButton[][] cellButtons = new JButton[N][N];
    private JButton[] valueButtons = new JButton[N];
    private JLabel statusLabel, humanScoreLabel, humanLivesLabel, cpuScoreLabel, cpuLivesLabel;
    private JPanel valueSelectionPanel;
    private JComboBox<String> strategyCombo;
    private JCheckBox heatMapToggle;
    
    private enum Strategy {
        SCORE_GREEDY("Score-Greedy"), LIVES_GREEDY("Lives-Greedy"),
        CONSTRAINT_GREEDY("Constraint-Greedy"), COMPLETION_GREEDY("Completion-Greedy");
        private final String name;
        Strategy(String n) { name = n; }
        public String toString() { return name; }
    }
    
    private Strategy currentStrategy = Strategy.SCORE_GREEDY;
    private boolean showHeatMap = true;
    private double[][] heatMapValues = new double[N][N];

    public TowersGameGUI() {
        setTitle("Towers Puzzle - 4 Greedy Strategies");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(245, 245, 250));
        initComponents();
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        updateDisplay();
    }

    private void initComponents() {
        // Top stats panel
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 15, 8));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        humanScoreLabel = createLabel("YOU - Score: 0", new Color(30, 64, 175), new Color(219, 234, 254));
        cpuScoreLabel = createLabel("CPU - Score: 0", new Color(127, 29, 29), new Color(254, 226, 226));
        humanLivesLabel = createLabel("Lives: 100", new Color(16, 185, 129), new Color(209, 250, 229));
        cpuLivesLabel = createLabel("Lives: 100", new Color(239, 68, 68), new Color(254, 226, 226));
        
        topPanel.add(humanScoreLabel);
        topPanel.add(cpuScoreLabel);
        topPanel.add(humanLivesLabel);
        topPanel.add(cpuLivesLabel);
        add(topPanel, BorderLayout.NORTH);

        // Game board
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setOpaque(false);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);

        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = 0;
            boardPanel.add(createClue(TOP[i]), gbc);
        }

        for (int r = 0; r < N; r++) {
            gbc.gridx = 0; gbc.gridy = r + 1;
            boardPanel.add(createClue(LEFT[r]), gbc);

            for (int c = 0; c < N; c++) {
                final int row = r, col = c;
                JButton btn = new JButton("");
                btn.setPreferredSize(new Dimension(75, 75));
                btn.setFont(new Font("Arial", Font.BOLD, 32));
                btn.setBackground(Color.WHITE);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createLineBorder(new Color(200,200,200), 2));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> handleCellClick(row, col));
                cellButtons[r][c] = btn;
                gbc.gridx = c + 1; gbc.gridy = r + 1;
                boardPanel.add(btn, gbc);
            }

            gbc.gridx = N + 1; gbc.gridy = r + 1;
            boardPanel.add(createClue(RIGHT[r]), gbc);
        }

        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = N + 1;
            boardPanel.add(createClue(BOTTOM[i]), gbc);
        }
        add(boardPanel, BorderLayout.CENTER);

        // Right control panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        rightPanel.setPreferredSize(new Dimension(220, 0));
        
        JLabel stratLabel = new JLabel("CPU Strategy:");
        stratLabel.setFont(new Font("Arial", Font.BOLD, 13));
        stratLabel.setAlignmentX(LEFT_ALIGNMENT);
        
        strategyCombo = new JComboBox<>();
        for (Strategy s : Strategy.values()) strategyCombo.addItem(s.toString());
        strategyCombo.setMaximumSize(new Dimension(210, 30));
        strategyCombo.setAlignmentX(LEFT_ALIGNMENT);
        strategyCombo.addActionListener(e -> {
            currentStrategy = Strategy.values()[strategyCombo.getSelectedIndex()];
            updateHeatMap();
            updateDisplay();
        });
        
        heatMapToggle = new JCheckBox("Show Heat Map", true);
        heatMapToggle.setFont(new Font("Arial", Font.BOLD, 12));
        heatMapToggle.setOpaque(false);
        heatMapToggle.setAlignmentX(LEFT_ALIGNMENT);
        heatMapToggle.addActionListener(e -> {
            showHeatMap = heatMapToggle.isSelected();
            updateDisplay();
        });
        
        JButton resetBtn = new JButton("New Game");
        resetBtn.setFont(new Font("Arial", Font.BOLD, 14));
        resetBtn.setBackground(new Color(79, 70, 229));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFocusPainted(false);
        resetBtn.setMaximumSize(new Dimension(210, 40));
        resetBtn.setAlignmentX(LEFT_ALIGNMENT);
        resetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetBtn.addActionListener(e -> resetGame());
        
        rightPanel.add(stratLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(strategyCombo);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(heatMapToggle);
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(resetBtn);
        rightPanel.add(Box.createVerticalStrut(20));
        
        // Value selection
        valueSelectionPanel = new JPanel();
        valueSelectionPanel.setLayout(new BoxLayout(valueSelectionPanel, BoxLayout.Y_AXIS));
        valueSelectionPanel.setOpaque(false);
        valueSelectionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(147,197,253), 2),
            "Select Value", 0, 0, new Font("Arial", Font.BOLD, 13), new Color(59,130,246)));
        valueSelectionPanel.setVisible(false);
        
        JPanel valGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        valGrid.setOpaque(false);
        for (int i = 0; i < N; i++) {
            final int val = i + 1;
            JButton btn = new JButton(String.valueOf(val));
            btn.setPreferredSize(new Dimension(50, 50));
            btn.setFont(new Font("Arial", Font.BOLD, 24));
            btn.setBackground(new Color(79, 70, 229));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> handleValueClick(val));
            valueButtons[i] = btn;
            valGrid.add(btn);
        }
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 12));
        cancelBtn.setBackground(new Color(239, 68, 68));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setMaximumSize(new Dimension(210, 35));
        cancelBtn.setAlignmentX(CENTER_ALIGNMENT);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> {
            selectedRow = -1; selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            updateDisplay();
        });
        
        valueSelectionPanel.add(valGrid);
        valueSelectionPanel.add(Box.createVerticalStrut(10));
        valueSelectionPanel.add(cancelBtn);
        rightPanel.add(valueSelectionPanel);
        add(rightPanel, BorderLayout.EAST);

        // Status
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        statusLabel = new JLabel("Your turn! Click a cell.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String txt, Color fg, Color bg) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 16));
        l.setForeground(fg);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg.brighter(), 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        return l;
    }

    private JLabel createClue(int v) {
        JLabel l = new JLabel(String.valueOf(v), SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 20));
        l.setForeground(new Color(79, 70, 229));
        l.setPreferredSize(new Dimension(40, 40));
        return l;
    }

    private void handleCellClick(int r, int c) {
        if (!humanTurn || gameOver || grid[r][c] != 0) return;
        selectedRow = r; selectedCol = c;
        showValueSelection();
        updateDisplay();
    }

    private void showValueSelection() {
        List<Integer> avail = new ArrayList<>();
        for (int v = 1; v <= N; v++) if (canPlace(selectedRow, selectedCol, v)) avail.add(v);
        for (int i = 0; i < N; i++) valueButtons[i].setEnabled(avail.contains(i+1));
        valueSelectionPanel.setVisible(true);
        statusLabel.setText("Cell (" + (selectedRow+1) + "," + (selectedCol+1) + ") selected →");
    }

    private void handleValueClick(int val) {
        if (selectedRow == -1) return;
        placeValue(selectedRow, selectedCol, val, true);
        selectedRow = -1; selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        humanTurn = false;
        updateDisplay();
        if (checkGameEnd()) return;
        
        Timer t = new Timer(500, e -> { updateHeatMap(); animateHeatMap(0); });
        t.setRepeats(false);
        t.start();
    }

    private void animateHeatMap(int idx) {
        if (idx >= N*N) {
            Timer t = new Timer(1500, e -> {
                clearHeatMap();
                if (!gameOver && countEmpty() > 0) {
                    doCPUMove();
                    humanTurn = true;
                    updateDisplay();
                    checkGameEnd();
                }
            });
            t.setRepeats(false);
            t.start();
            return;
        }
        int r = idx / N, c = idx % N;
        if (grid[r][c] == 0 && showHeatMap) {
            cellButtons[r][c].setBackground(getHeatColor(heatMapValues[r][c]));
        }
        Timer t = new Timer(120, e -> animateHeatMap(idx + 1));
        t.setRepeats(false);
        t.start();
    }

    private Color getHeatColor(double h) {
        if (h < 0.01) return Color.WHITE;
        int r, g, b;
        double t;
        
        switch (currentStrategy) {
            case SCORE_GREEDY: // Blue
                if (h < 0.33) {
                    t = h * 3;
                    return new Color((int)(255-84*t), (int)(255-86*t), 255);
                } else if (h < 0.67) {
                    t = (h-0.33)*3;
                    return new Color((int)(171-104*t), (int)(169-73*t), 255);
                } else {
                    t = (h-0.67)*3;
                    return new Color((int)(67-54*t), (int)(96-25*t), (int)(255-94*t));
                }
                
            case LIVES_GREEDY: // Green
                if (h < 0.33) {
                    t = h * 3;
                    return new Color((int)(220-130*t), (int)(252-36*t), (int)(220-91*t));
                } else if (h < 0.67) {
                    t = (h-0.33)*3;
                    return new Color((int)(90-56*t), (int)(216-31*t), (int)(129-34*t));
                } else {
                    t = (h-0.67)*3;
                    return new Color((int)(34-18*t), (int)(185-50*t), (int)(95-31*t));
                }
                
            case CONSTRAINT_GREEDY: // Purple
                if (h < 0.33) {
                    t = h * 3;
                    return new Color((int)(237-51*t), (int)(233-137*t), (int)(254-46*t));
                } else if (h < 0.67) {
                    t = (h-0.33)*3;
                    return new Color((int)(186-56*t), (int)(96-57*t), (int)(208-64*t));
                } else {
                    t = (h-0.67)*3;
                    return new Color((int)(130-56*t), 39, (int)(144-4*t));
                }
                
            case COMPLETION_GREEDY: // Red
                if (h < 0.33) {
                    t = h * 3;
                    return new Color(254, (int)(226-76*t), (int)(226-124*t));
                } else if (h < 0.67) {
                    t = (h-0.33)*3;
                    return new Color((int)(254-15*t), (int)(150-82*t), (int)(102-34*t));
                } else {
                    t = (h-0.67)*3;
                    return new Color((int)(239-28*t), (int)(68-23*t), (int)(68-23*t));
                }
        }
        return Color.WHITE;
    }

    private void clearHeatMap() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] == 0) cellButtons[r][c].setBackground(Color.WHITE);
    }

    private void placeValue(int r, int c, int v, boolean isH) {
        boolean ok = canPlace(r, c, v);
        int eRow = countEmptyRow(r), eCol = countEmptyCol(c);
        
        grid[r][c] = v;
        rowUsed[r][v] = colUsed[c][v] = true;

        if (ok) {
            int pts = 1;
            if (eRow == 1) pts += 2;
            if (eCol == 1) pts += 2;
            if (isH) {
                humanScore += pts;
                humanLives += 5;
                statusLabel.setText("✓ You: "+v+" [+"+pts+" pts, +5 lives]");
            } else {
                cpuScore += pts;
                cpuLives += 5;
                statusLabel.setText("✓ CPU: "+v+" [+"+pts+" pts, +5 lives]");
            }
        } else {
            if (isH) {
                humanScore = Math.max(0, humanScore - 5);
                humanLives = Math.max(0, humanLives - 10);
                statusLabel.setText("✗ INVALID! [-5 pts, -10 lives]");
            } else {
                cpuScore = Math.max(0, cpuScore - 5);
                cpuLives = Math.max(0, cpuLives - 10);
                statusLabel.setText("✗ CPU INVALID! [-5 pts, -10 lives]");
            }
        }
        
        if (countEmpty() == 0 || humanLives <= 0 || cpuLives <= 0) {
            gameOver = true;
            declareWinner();
        }
    }

    private boolean canPlace(int r, int c, int v) {
        if (r < 0 || r >= N || c < 0 || c >= N || grid[r][c] != 0 || rowUsed[r][v] || colUsed[c][v]) return false;
        grid[r][c] = v;
        rowUsed[r][v] = colUsed[c][v] = true;
        boolean ok = checkRow(r, LEFT[r], true) && checkRow(r, RIGHT[r], false) &&
                     checkCol(c, TOP[c], true) && checkCol(c, BOTTOM[c], false);
        grid[r][c] = 0;
        rowUsed[r][v] = colUsed[c][v] = false;
        return ok;
    }

    private boolean checkRow(int r, int clue, boolean left) {
        int vis = 0, max = 0;
        int[] dir = left ? new int[]{0,1,2,3} : new int[]{3,2,1,0};
        for (int i = 0; i < N; i++) {
            int h = grid[r][dir[i]];
            if (h > max) { vis++; max = h; }
        }
        boolean full = true;
        for (int c = 0; c < N; c++) if (grid[r][c] == 0) full = false;
        return !(vis > clue || (full && vis != clue));
    }

    private boolean checkCol(int c, int clue, boolean top) {
        int vis = 0, max = 0;
        int[] dir = top ? new int[]{0,1,2,3} : new int[]{3,2,1,0};
        for (int i = 0; i < N; i++) {
            int h = grid[dir[i]][c];
            if (h > max) { vis++; max = h; }
        }
        boolean full = true;
        for (int r = 0; r < N; r++) if (grid[r][c] == 0) full = false;
        return !(vis > clue || (full && vis != clue));
    }

    private int countEmptyRow(int r) {
        int c = 0;
        for (int i = 0; i < N; i++) if (grid[r][i] == 0) c++;
        return c;
    }

    private int countEmptyCol(int c) {
        int cnt = 0;
        for (int r = 0; r < N; r++) if (grid[r][c] == 0) cnt++;
        return cnt;
    }

    private int countEmpty() {
        int c = 0;
        for (int r = 0; r < N; r++) for (int i = 0; i < N; i++) if (grid[r][i] == 0) c++;
        return c;
    }

    private void doCPUMove() {
        Move m = getBest();
        if (m == null) {
            gameOver = true;
            statusLabel.setText("CPU no moves! YOU WIN!");
            declareWinner();
            return;
        }
        placeValue(m.r, m.c, m.v, false);
    }

    private boolean hasMove() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] == 0)
                    for (int v = 1; v <= N; v++)
                        if (canPlace(r, c, v)) return true;
        return false;
    }

    private boolean checkGameEnd() {
        if (humanLives <= 0) {
            gameOver = true;
            statusLabel.setText("💀 Your lives = 0! CPU WINS!");
            declareWinner();
            return true;
        }
        if (cpuLives <= 0) {
            gameOver = true;
            statusLabel.setText("🎉 CPU lives = 0! YOU WIN!");
            declareWinner();
            return true;
        }
        if (countEmpty() == 0) {
            gameOver = true;
            declareWinner();
            return true;
        }
        if (!hasMove()) {
            gameOver = true;
            statusLabel.setText(humanTurn ? "No moves! CPU WINS!" : "CPU no moves! YOU WIN!");
            declareWinner();
            return true;
        }
        return false;
    }

    private void declareWinner() {
        String w;
        if (humanLives <= 0) w = "CPU";
        else if (cpuLives <= 0) w = "YOU";
        else if (humanScore > cpuScore) w = "YOU";
        else if (cpuScore > humanScore) w = "CPU";
        else if (humanLives > cpuLives) w = "YOU";
        else if (cpuLives > humanLives) w = "CPU";
        else w = "TIE";
        
        String msg = "═══ GAME OVER ═══\n\n" +
                    (w.equals("TIE") ? "It's a TIE!\n\n" : "Winner: "+w+"!\n\n") +
                    "Final Stats:\n" +
                    "YOU: "+humanScore+" pts, "+humanLives+" lives\n" +
                    "CPU: "+cpuScore+" pts, "+cpuLives+" lives";
        JOptionPane.showMessageDialog(this, msg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    private Move getBest() {
        List<Move> cand = new ArrayList<>();
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] == 0)
                    for (int v = 1; v <= N; v++)
                        if (canPlace(r, c, v))
                            cand.add(new Move(r, c, v, eval(r, c, v)));
        if (cand.isEmpty()) return null;
        Collections.sort(cand, (a,b) -> Double.compare(b.eval, a.eval));
        return cand.get(0);
    }

    private double eval(int r, int c, int v) {
        grid[r][c] = v;
        rowUsed[r][v] = colUsed[c][v] = true;
        double e = 0;
        switch (currentStrategy) {
            case SCORE_GREEDY: e = evalScore(r, c); break;
            case LIVES_GREEDY: e = evalLives(r, c); break;
            case CONSTRAINT_GREEDY: e = evalConstraint(r, c); break;
            case COMPLETION_GREEDY: e = evalCompletion(r, c); break;
        }
        grid[r][c] = 0;
        rowUsed[r][v] = colUsed[c][v] = false;
        return e;
    }

    private double evalScore(int r, int c) {
        int pts = 1;
        if (countEmptyRow(r) == 0) pts += 2;
        if (countEmptyCol(c) == 0) pts += 2;
        return 2.0 * pts + 0.5 * 5;
    }

    private double evalLives(int r, int c) {
        int pts = 1;
        if (countEmptyRow(r) == 0) pts += 2;
        if (countEmptyCol(c) == 0) pts += 2;
        return 0.5 * pts + 2.0 * 5;
    }

    private double evalConstraint(int r, int c) {
        int opts = 0;
        for (int v = 1; v <= N; v++) if (!rowUsed[r][v] && !colUsed[c][v]) opts++;
        double sc = opts > 0 ? 10.0 / opts * 15.0 : 0;
        int pts = 1;
        if (countEmptyRow(r) == 0) pts += 2;
        if (countEmptyCol(c) == 0) pts += 2;
        return sc + pts * 5.0;
    }

    private double evalCompletion(int r, int c) {
        int er = countEmptyRow(r), ec = countEmptyCol(c);
        double sc = 0;
        if (er > 0) sc += 10.0 / er;
        if (ec > 0) sc += 10.0 / ec;
        int pts = 1;
        if (er == 1) pts += 2;
        if (ec == 1) pts += 2;
        return sc * 12.0 + pts * 8.0;
    }

    private void updateHeatMap() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                heatMapValues[r][c] = 0;
        if (gameOver) return;
        
        double max = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) continue;
                double best = 0;
                for (int v = 1; v <= N; v++)
                    if (canPlace(r, c, v))
                        best = Math.max(best, eval(r, c, v));
                heatMapValues[r][c] = best;
                max = Math.max(max, best);
            }
        }
        if (max > 0)
            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++)
                    heatMapValues[r][c] /= max;
    }

    private void updateDisplay() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton b = cellButtons[r][c];
                if (grid[r][c] != 0) {
                    b.setText(String.valueOf(grid[r][c]));
                    b.setBackground(new Color(79, 70, 229));
                    b.setForeground(Color.WHITE);
                    b.setEnabled(false);
                } else {
                    b.setText("");
                    if (r == selectedRow && c == selectedCol) {
                        b.setBackground(new Color(191, 219, 254));
                        b.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 3));
                    } else {
                        b.setBackground(Color.WHITE);
                        b.setBorder(BorderFactory.createLineBorder(new Color(200,200,200), 2));
                    }
                    b.setEnabled(humanTurn && !gameOver);
                }
            }
        }
        humanScoreLabel.setText("YOU - Score: " + humanScore);
        humanLivesLabel.setText("Lives: " + humanLives);
        cpuScoreLabel.setText("CPU - Score: " + cpuScore);
        cpuLivesLabel.setText("Lives: " + cpuLives);
        if (!gameOver && humanTurn && selectedRow == -1) {
            statusLabel.setText("Your turn! Click a cell.");
        } else if (!gameOver && !humanTurn) {
            statusLabel.setText("CPU analyzing...");
        }
    }
    
    
    private void resetGame() {
        grid = new int[N][N];
        rowUsed = new boolean[N][N+1];
        colUsed = new boolean[N][N+1];
        humanScore = cpuScore = 0;
        humanLives = cpuLives = 100;
        humanTurn = true;
        gameOver = false;
        selectedRow = selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        clearHeatMap();
        updateHeatMap();
        updateDisplay();
    }

    private static class Move {
        int r, c, v;
        double eval;
        Move(int r, int c, int v, double eval) {
            this.r = r;
            this.c = c;
            this.v = v;
            this.eval = eval;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowersssGameGUI().setVisible(true));
    }
}
