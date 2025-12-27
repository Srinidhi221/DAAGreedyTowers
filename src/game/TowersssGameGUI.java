package game;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// ============================================================================
// MAIN GUI - Towers Puzzle Game (4x4) with 4 Greedy Strategies + Adjacency List
// ============================================================================

public class TowersssGameGUI extends JFrame {
    private static final int N = 4;

    private GameState gameState;
    private StrategyLives strategyLives;
    private StrategyCompletion strategyCompletion;
    private StrategyScore strategyScore;
    private StrategyMRV strategyMRV;

    private int selectedRow = -1, selectedCol = -1;

    private JButton[][] cellButtons = new JButton[N][N];
    private JButton[] valueButtons = new JButton[N];
    private JLabel statusLabel, humanScoreLabel, humanLivesLabel, cpuScoreLabel, cpuLivesLabel;
    private JPanel valueSelectionPanel;
    private JComboBox<String> strategyCombo;
    private JCheckBox heatMapToggle;
    private JTextArea reasoningArea;

    // Adjacency List tracking
    private Map<String, List<String>> adjacencyList = new LinkedHashMap<>();
    private List<MoveNode> moveHistory = new ArrayList<>();
    private int moveCounter = 0;

    // Move node class to track move details
    private static class MoveNode {
        int id;
        int row, col, value;
        boolean isHuman;

        MoveNode(int id, int row, int col, int value, boolean isHuman) {
            this.id = id;
            this.row = row;
            this.col = col;
            this.value = value;
            this.isHuman = isHuman;
        }

        String getKey() {
            return "M" + id + "[" + row + "," + col + "=" + value + "]" + (isHuman ? "(H)" : "(C)");
        }

        boolean isAdjacent(MoveNode other) {
            // Adjacent if same row OR same column
            return this.row == other.row || this.col == other.col;
        }
    }

    private enum Strategy {
        LIVES("Lives-Greedy (Survival)"),
        COMPLETION("Completion-Greedy (Rusher)"),
        SCORE("Score-Greedy (Gambler)"),
        MRV("Constraint-Greedy (MRV)");

        private final String name;
        Strategy(String n) { name = n; }
        public String toString() { return name; }
    }

    private Strategy currentStrategy = Strategy.LIVES;
    private boolean showHeatMap = true;
    private double[][] heatMapValues = new double[N][N];

    public TowersssGameGUI() {
        setTitle("Towers Puzzle - 4×4 with 4 AI Strategies");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(20, 20));
        getContentPane().setBackground(new Color(15, 23, 42));

        initGame();
        initComponents();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        updateDisplay();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("TOWERS GAME - MOVE ADJACENCY LIST TRACKER");
        System.out.println("=".repeat(70));
        System.out.println("Tracking move connections (same row/column relationships)");
        System.out.println("=".repeat(70) + "\n");
    }

    // ============================================================================
    // ADJACENCY LIST METHODS
    // ============================================================================

    private void addMoveToAdjacencyList(int row, int col, int value, boolean isHuman) {
        moveCounter++;
        MoveNode newMove = new MoveNode(moveCounter, row, col, value, isHuman);
        String newKey = newMove.getKey();

        // Initialize adjacency list for this move
        adjacencyList.put(newKey, new ArrayList<>());

        // Check connections with all previous moves
        for (MoveNode prevMove : moveHistory) {
            if (newMove.isAdjacent(prevMove)) {
                String prevKey = prevMove.getKey();

                // Add bidirectional edge
                adjacencyList.get(newKey).add(prevKey);
                adjacencyList.get(prevKey).add(newKey);
            }
        }

        moveHistory.add(newMove);
        printAdjacencyList(newMove);
    }

    private void printAdjacencyList(MoveNode latestMove) {
        System.out.println("\n" + "-".repeat(70));
        System.out.println("Move #" + moveCounter + " → Cell[" + latestMove.row + "," + latestMove.col +
                "] = " + latestMove.value + " by " + (latestMove.isHuman ? "HUMAN" : "CPU"));
        System.out.println("-".repeat(70));

        // Print the adjacency list
        System.out.println("\nCURRENT ADJACENCY LIST:");
        System.out.println("─".repeat(70));

        for (Map.Entry<String, List<String>> entry : adjacencyList.entrySet()) {
            String node = entry.getKey();
            List<String> neighbors = entry.getValue();

            System.out.print(node + " → ");
            if (neighbors.isEmpty()) {
                System.out.println("[ No adjacent moves ]");
            } else {
                System.out.println(neighbors);
            }
        }

        System.out.println("─".repeat(70));
        System.out.println("Total Moves: " + moveCounter + " | Nodes: " + adjacencyList.size() +
                " | Edges: " + countEdges());
        System.out.println();
    }

    private int countEdges() {
        int count = 0;
        for (List<String> neighbors : adjacencyList.values()) {
            count += neighbors.size();
        }
        return count / 2; // Each edge is counted twice (bidirectional)
    }

    private void printFinalAdjacencyStats() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("FINAL ADJACENCY LIST STATISTICS");
        System.out.println("=".repeat(70));
        System.out.println("Total Moves Made: " + moveCounter);
        System.out.println("Total Nodes: " + adjacencyList.size());
        System.out.println("Total Edges: " + countEdges());

        // Find most connected move
        String mostConnected = null;
        int maxConnections = 0;
        for (Map.Entry<String, List<String>> entry : adjacencyList.entrySet()) {
            if (entry.getValue().size() > maxConnections) {
                maxConnections = entry.getValue().size();
                mostConnected = entry.getKey();
            }
        }

        if (mostConnected != null) {
            System.out.println("Most Connected Move: " + mostConnected +
                    " with " + maxConnections + " connections");
        }

        System.out.println("=".repeat(70) + "\n");
    }

    private void resetAdjacencyList() {
        adjacencyList.clear();
        moveHistory.clear();
        moveCounter = 0;

        System.out.println("\n" + "=".repeat(70));
        System.out.println("GAME RESET - Adjacency List Cleared");
        System.out.println("=".repeat(70) + "\n");
    }

    // ============================================================================
    // GAME INITIALIZATION
    // ============================================================================

    private void initGame() {
        PuzzleGenerator generator = new PuzzleGenerator();
        PuzzleGenerator.PuzzleData puzzle = generator.generatePuzzle();

        gameState = new GameState(
                puzzle.topClues,
                puzzle.rightClues,
                puzzle.bottomClues,
                puzzle.leftClues
        );

        strategyLives = new StrategyLives(gameState);
        strategyCompletion = new StrategyCompletion(gameState);
        strategyScore = new StrategyScore(gameState);
        strategyMRV = new StrategyMRV(gameState);
    }

    // ============================================================================
    // GUI COMPONENTS
    // ============================================================================

    private void initComponents() {
        // Top stats panel with glassmorphism
        JPanel topPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        humanScoreLabel = createModernStatCard(" YOUR SCORE", "0", new Color(59, 130, 246), new Color(30, 58, 138));
        humanLivesLabel = createModernStatCard(" YOUR LIVES", "100", new Color(16, 185, 129), new Color(6, 78, 59));
        cpuScoreLabel = createModernStatCard(" CPU SCORE", "0", new Color(168, 85, 247), new Color(88, 28, 135));
        cpuLivesLabel = createModernStatCard(" CPU LIVES", "100", new Color(239, 68, 68), new Color(127, 29, 29));

        topPanel.add(humanScoreLabel);
        topPanel.add(humanLivesLabel);
        topPanel.add(cpuScoreLabel);
        topPanel.add(cpuLivesLabel);
        add(topPanel, BorderLayout.NORTH);

        // Game board with modern styling
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setOpaque(false);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);

        // Top clues with modern styling
        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = 0;
            boardPanel.add(createModernClue(gameState.getTopClues()[i], "↓"), gbc);
        }

        // Board with left/right clues
        for (int r = 0; r < N; r++) {
            gbc.gridx = 0; gbc.gridy = r + 1;
            boardPanel.add(createModernClue(gameState.getLeftClues()[r], "→"), gbc);

            for (int c = 0; c < N; c++) {
                final int row = r, col = c;
                JButton btn = new ModernCellButton("");
                btn.setPreferredSize(new Dimension(95, 95));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 42));
                btn.setFocusPainted(false);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> handleCellClick(row, col));

                // Hover effect
                btn.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        if (btn.isEnabled() && gameState.getGrid()[row][col] == 0) {
                            btn.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 3));
                        }
                    }
                    public void mouseExited(MouseEvent e) {
                        if (row != selectedRow || col != selectedCol) {
                            btn.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2));
                        }
                    }
                });

                cellButtons[r][c] = btn;
                gbc.gridx = c + 1; gbc.gridy = r + 1;
                boardPanel.add(btn, gbc);
            }

            gbc.gridx = N + 1; gbc.gridy = r + 1;
            boardPanel.add(createModernClue(gameState.getRightClues()[r], "←"), gbc);
        }

        // Bottom clues
        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = N + 1;
            boardPanel.add(createModernClue(gameState.getBottomClues()[i], "↑"), gbc);
        }
        add(boardPanel, BorderLayout.CENTER);

        // Right control panel with modern design
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 25));
        rightPanel.setPreferredSize(new Dimension(340, 0));

        // Strategy selector with modern card
        JPanel strategyCard = createModernCard();
        strategyCard.setLayout(new BoxLayout(strategyCard, BoxLayout.Y_AXIS));

        JLabel stratLabel = new JLabel(" CPU STRATEGY");
        stratLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stratLabel.setForeground(new Color(148, 163, 184));
        stratLabel.setAlignmentX(LEFT_ALIGNMENT);

        strategyCombo = new JComboBox<>();
        for (Strategy s : Strategy.values()) strategyCombo.addItem(s.toString());
        strategyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        strategyCombo.setMaximumSize(new Dimension(300, 40));
        strategyCombo.setAlignmentX(LEFT_ALIGNMENT);
        strategyCombo.setBackground(new Color(30, 41, 59));
        strategyCombo.setForeground(Color.WHITE);
        strategyCombo.addActionListener(e -> {
            currentStrategy = Strategy.values()[strategyCombo.getSelectedIndex()];
            updateHeatMap();
            updateDisplay();
        });

        strategyCard.add(stratLabel);
        strategyCard.add(Box.createVerticalStrut(10));
        strategyCard.add(strategyCombo);

        heatMapToggle = new JCheckBox(" Show Heat Map", true);
        heatMapToggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        heatMapToggle.setForeground(Color.WHITE);
        heatMapToggle.setOpaque(false);
        heatMapToggle.setAlignmentX(LEFT_ALIGNMENT);
        heatMapToggle.addActionListener(e -> {
            showHeatMap = heatMapToggle.isSelected();
            updateDisplay();
        });

        strategyCard.add(Box.createVerticalStrut(15));
        strategyCard.add(heatMapToggle);

        JButton resetBtn = createModernButton(" New Game", new Color(59, 130, 246));
        resetBtn.setAlignmentX(LEFT_ALIGNMENT);
        resetBtn.addActionListener(e -> resetGame());

        rightPanel.add(strategyCard);
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(resetBtn);
        rightPanel.add(Box.createVerticalStrut(20));

        // CPU Reasoning Card
        JPanel reasoningCard = createModernCard();
        reasoningCard.setLayout(new BoxLayout(reasoningCard, BoxLayout.Y_AXIS));
        reasoningCard.setMaximumSize(new Dimension(300, 280));

        JLabel reasonLabel = new JLabel(" CPU REASONING");
        reasonLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        reasonLabel.setForeground(new Color(148, 163, 184));
        reasonLabel.setAlignmentX(LEFT_ALIGNMENT);

        reasoningArea = new JTextArea(12, 25);
        reasoningArea.setEditable(false);
        reasoningArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reasoningArea.setLineWrap(true);
        reasoningArea.setWrapStyleWord(true);
        reasoningArea.setBackground(new Color(15, 23, 42));
        reasoningArea.setForeground(new Color(203, 213, 225));
        reasoningArea.setCaretColor(Color.WHITE);
        reasoningArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reasoningArea.setText("Select a strategy and watch the CPU think...");

        JScrollPane reasonScroll = new JScrollPane(reasoningArea);
        reasonScroll.setBorder(null);
        reasonScroll.setOpaque(false);
        reasonScroll.getViewport().setOpaque(false);

        reasoningCard.add(reasonLabel);
        reasoningCard.add(Box.createVerticalStrut(10));
        reasoningCard.add(reasonScroll);

        rightPanel.add(reasoningCard);
        rightPanel.add(Box.createVerticalStrut(20));

        // Value selection panel with modern design
        valueSelectionPanel = new JPanel();
        valueSelectionPanel.setLayout(new BoxLayout(valueSelectionPanel, BoxLayout.Y_AXIS));
        valueSelectionPanel.setBackground(new Color(30, 41, 59));
        valueSelectionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 3),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        valueSelectionPanel.setMaximumSize(new Dimension(300, 280));
        valueSelectionPanel.setVisible(false);

        JLabel selectLabel = new JLabel("SELECT VALUE");
        selectLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        selectLabel.setForeground(new Color(59, 130, 246));
        selectLabel.setAlignmentX(CENTER_ALIGNMENT);

        JPanel valGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        valGrid.setOpaque(false);
        for (int i = 0; i < N; i++) {
            final int val = i + 1;
            JButton btn = createModernValueButton(String.valueOf(val));
            btn.addActionListener(e -> handleValueClick(val));
            valueButtons[i] = btn;
            valGrid.add(btn);
        }

        JButton cancelBtn = createModernButton("✖ Cancel", new Color(239, 68, 68));
        cancelBtn.setAlignmentX(CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> {
            selectedRow = -1; selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            updateDisplay();
        });

        valueSelectionPanel.add(selectLabel);
        valueSelectionPanel.add(Box.createVerticalStrut(15));
        valueSelectionPanel.add(valGrid);
        valueSelectionPanel.add(Box.createVerticalStrut(15));
        valueSelectionPanel.add(cancelBtn);

        rightPanel.add(valueSelectionPanel);
        add(rightPanel, BorderLayout.EAST);

        // Bottom status with modern styling
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 25, 25));

        statusLabel = new JLabel("Your turn! Click an empty cell.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(30, 41, 59));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                BorderFactory.createEmptyBorder(15, 30, 15, 30)));

        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Modern UI component creators
    private JPanel createModernCard() {
        JPanel card = new JPanel();
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(300, 200));
        return card;
    }

    private JButton createModernButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setMaximumSize(new Dimension(300, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });

        return btn;
    }

    private JButton createModernValueButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(80, 80));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 36));
        btn.setBackground(new Color(59, 130, 246));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(37, 99, 235));
            }
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(59, 130, 246));
            }
        });

        return btn;
    }

    private JLabel createModernStatCard(String title, String value, Color accent, Color dark) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(true);
        container.setBackground(new Color(30, 41, 59));
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(new Color(148, 163, 184));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);

        container.add(titleLabel);
        container.add(Box.createVerticalStrut(5));
        container.add(valueLabel);

        JLabel wrapper = new JLabel();
        wrapper.setLayout(new BorderLayout());
        wrapper.add(container);
        return wrapper;
    }

    private JLabel createModernClue(int v, String arrow) {
        JLabel l = new JLabel("<html><center>" + arrow + "<br><b>" + v + "</b></center></html>", SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 18));
        l.setForeground(new Color(148, 163, 184));
        l.setOpaque(true);
        l.setBackground(new Color(30, 41, 59));
        l.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2));
        l.setPreferredSize(new Dimension(55, 55));
        return l;
    }

    // Custom cell button with rounded corners
    class ModernCellButton extends JButton {
        public ModernCellButton(String text) {
            super(text);
            setContentAreaFilled(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(getBackground().darker());
            } else {
                g2.setColor(getBackground());
            }

            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            super.paintComponent(g2);
            g2.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getForeground());
            g2.setStroke(new BasicStroke(2));
            g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 12, 12));
            g2.dispose();
        }
    }

    // ============================================================================
    // USER INTERACTION
    // ============================================================================

    private void handleCellClick(int r, int c) {
        if (!gameState.isHumanTurn() || gameState.isGameOver() || gameState.getGrid()[r][c] != 0) {
            return;
        }
        selectedRow = r;
        selectedCol = c;
        showValueSelection();
        updateDisplay();
    }

    private void showValueSelection() {
        int[] topClues = gameState.getTopClues();
        int[] rightClues = gameState.getRightClues();
        int[] bottomClues = gameState.getBottomClues();
        int[] leftClues = gameState.getLeftClues();

        for (int i = 0; i < N; i++) {
            int val = i + 1;
            boolean legal = gameState.getGraph().canPlace(
                    gameState.getGrid(), selectedRow, selectedCol, val,
                    leftClues[selectedRow], rightClues[selectedRow],
                    topClues[selectedCol], bottomClues[selectedCol]
            );
            valueButtons[i].setEnabled(legal);
            valueButtons[i].setBackground(legal ? new Color(59, 130, 246) : new Color(71, 85, 105));
        }
        valueSelectionPanel.setVisible(true);
        statusLabel.setText("Choose a value for cell (" + (selectedRow+1) + "," + (selectedCol+1) + ")");
    }

    private void handleValueClick(int val) {
        if (selectedRow == -1) return;

        gameState.makeMove(selectedRow, selectedCol, val, true);

        // Add to adjacency list
        addMoveToAdjacencyList(selectedRow, selectedCol, val, true);

        selectedRow = -1;
        selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        gameState.setHumanTurn(false);

        clearHeatMap();
        updateDisplay();

        if (checkGameEnd()) return;

        Timer delay = new Timer(600, e -> {
            updateHeatMap();
            animateHeatMap(0);
        });
        delay.setRepeats(false);
        delay.start();
    }

    // ============================================================================
    // HEAT MAP ANIMATION & COLORS
    // ============================================================================

    private void animateHeatMap(int idx) {
        if (idx >= N * N) {
            Timer delay = new Timer(1200, e -> {
                if (!gameState.isGameOver()) {
                    doCPUMove();
                    clearHeatMap();
                    gameState.setHumanTurn(true);
                    updateDisplay();
                    checkGameEnd();
                }
            });
            delay.setRepeats(false);
            delay.start();
            return;
        }

        int r = idx / N, c = idx % N;
        if (gameState.getGrid()[r][c] == 0 && showHeatMap) {
            cellButtons[r][c].setBackground(getHeatColor(heatMapValues[r][c]));
        }

        Timer t = new Timer(70, e -> animateHeatMap(idx + 1));
        t.setRepeats(false);
        t.start();
    }

  /*  private Color getHeatColor(double h) {
        if (h < 0.01) return new Color(30, 41, 59);

        double ratio = Math.min(h, 1.0);

        return switch (currentStrategy) {
            case LIVES ->       new Color(34 + (int)(151 * ratio), 185 + (int)(31 * ratio), 95 + (int)(125 * ratio));
            case COMPLETION ->  new Color(239 + (int)(15 * ratio), 68 + (int)(82 * ratio), 68 + (int)(82 * ratio));
            case SCORE ->       new Color(255, 165 + (int)(90 * ratio), 0);
            case MRV ->         new Color(130 + (int)(56 * ratio), 39, 144 + (int)(64 * ratio));
        };
    }*/


    private Color getHeatColor(double h) {
        if (h < 0.01) return new Color(30, 41, 59);

        double ratio = Math.min(h, 1.0);

        return switch (currentStrategy) {
            case LIVES -> {
                // Bright emerald green gradient (light to vibrant)
                int r = 110 + (int)(106 * ratio);  // 110 -> 216
                int g = 231 + (int)(0 * ratio);     // 231 -> 231 (keep bright)
                int b = 183 + (int)(-71 * ratio);   // 183 -> 112
                yield new Color(r, g, b);
            }
            case COMPLETION -> {
                // Bright coral/salmon gradient (light to intense)
                int r = 252 + (int)(0 * ratio);     // 252 -> 252 (keep bright)
                int g = 165 + (int)(-80 * ratio);   // 165 -> 85
                int b = 165 + (int)(-80 * ratio);   // 165 -> 85
                yield new Color(r, g, b);
            }
            case SCORE -> {
                // Bright gold/amber gradient (light to vibrant orange)
                int r = 253 + (int)(0 * ratio);     // 253 -> 253 (keep bright)
                int g = 224 + (int)(-74 * ratio);   // 224 -> 150
                int b = 71 + (int)(-71 * ratio);    // 71 -> 0
                yield new Color(r, g, b);
            }
            case MRV -> {
                // Bright purple/magenta gradient (light to vivid)
                int r = 216 + (int)(22 * ratio);    // 216 -> 238
                int g = 180 + (int)(-86 * ratio);   // 180 -> 94
                int b = 254 + (int)(-48 * ratio);   // 254 -> 206
                yield new Color(r, g, b);
            }
        };
    }

    private void clearHeatMap() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (gameState.getGrid()[r][c] == 0) {
                    cellButtons[r][c].setBackground(
                            (r == selectedRow && c == selectedCol) ? new Color(59, 130, 246) : new Color(30, 41, 59)
                    );
                }
            }
        }
    }

    // ============================================================================
    // CPU MOVE
    // ============================================================================

    private void doCPUMove() {
        int[] move = switch (currentStrategy) {
            case LIVES -> strategyLives.findBestMove();
            case COMPLETION -> strategyCompletion.findBestMove();
            case SCORE -> strategyScore.findBestMove();
            case MRV -> strategyMRV.findBestMove();
        };

        if (move == null) {
            gameState.setStatusMessage("CPU has no valid moves!");
            clearHeatMap();
            updateDisplay();
            return;
        }

        reasoningArea.setText(gameState.getCpuReasoningExplanation());
        gameState.makeMove(move[0], move[1], move[2], false);

        // Add to adjacency list
        addMoveToAdjacencyList(move[0], move[1], move[2], false);

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                heatMapValues[r][c] = 0;
            }
        }
    }

    // ============================================================================
    // HEAT MAP CALCULATION
    // ============================================================================

    private void updateHeatMap() {
        double max = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (gameState.getGrid()[r][c] == 0) {
                    double score = switch (currentStrategy) {
                        case LIVES -> strategyLives.evaluateCell(r, c);
                        case COMPLETION -> strategyCompletion.evaluateCell(r, c);
                        case SCORE -> strategyScore.evaluateCell(r, c);
                        case MRV -> strategyMRV.evaluateCell(r, c);
                    };
                    heatMapValues[r][c] = score;
                    max = Math.max(max, score);
                } else {
                    heatMapValues[r][c] = 0;
                }
            }
        }

        if (max > 0) {
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    heatMapValues[r][c] /= max;
                }
            }
        }
    }

    // ============================================================================
    // DISPLAY UPDATE
    // ============================================================================

    private void updateDisplay() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton b = cellButtons[r][c];
                int val = gameState.getGrid()[r][c];
                if (val != 0) {
                    b.setText(String.valueOf(val));
                    b.setBackground(new Color(59, 130, 246));
                    b.setForeground(Color.WHITE);
                    b.setEnabled(false);
                    b.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 2));
                } else {
                    b.setText("");
                    if (r == selectedRow && c == selectedCol) {
                        b.setBackground(new Color(59, 130, 246));
                        b.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 4));
                    } else {
                        b.setBackground(showHeatMap && heatMapValues[r][c] > 0.01 ?
                                getHeatColor(heatMapValues[r][c]) : new Color(30, 41, 59));
                        b.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2));
                    }
                    b.setEnabled(gameState.isHumanTurn() && !gameState.isGameOver());
                }
            }
        }

        updateStatCard(humanScoreLabel, String.valueOf(gameState.getHumanScore()));
        updateStatCard(humanLivesLabel, String.valueOf(gameState.getHumanLives()));
        updateStatCard(cpuScoreLabel, String.valueOf(gameState.getCpuScore()));
        updateStatCard(cpuLivesLabel, String.valueOf(gameState.getCpuLives()));

        String msg = gameState.getStatusMessage();
        if (!msg.isEmpty()) {
            statusLabel.setText(msg);
        } else if (gameState.isHumanTurn()) {
            statusLabel.setText(selectedRow == -1 ? " Your turn! Click a cell." : " Select a value");
        } else {
            statusLabel.setText(" CPU thinking (" + currentStrategy + ")...");
        }
    }

    private void updateStatCard(JLabel wrapper, String value) {
        Component comp = wrapper.getComponent(0);
        if (comp instanceof JPanel panel) {
            Component[] components = panel.getComponents();
            if (components.length >= 2 && components[2] instanceof JLabel valueLabel) {
                valueLabel.setText(value);
            }
        }
    }

    // ============================================================================
    // GAME END & RESET
    // ============================================================================

    private boolean checkGameEnd() {
        if (gameState.isGameOver()) {
            printFinalAdjacencyStats();

            String winner = gameState.getWinner();
            if (winner == null) winner = "Game Over";

            statusLabel.setText(winner);

            String msg = "═══ GAME OVER ═══\n\n" +
                    winner + "\n\n" +
                    "Final Stats:\n" +
                    "YOU → Score: " + gameState.getHumanScore() + " | Lives: " + gameState.getHumanLives() + "\n" +
                    "CPU → Score: " + gameState.getCpuScore() + " | Lives: " + gameState.getCpuLives();

            JOptionPane.showMessageDialog(this, msg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    private void resetGame() {
        initGame();
        resetAdjacencyList();
        selectedRow = -1;
        selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        reasoningArea.setText("Select a strategy and watch the CPU think...");
        updateHeatMap();
        updateDisplay();
        statusLabel.setText(" New game started! Your turn.");
    }

    // ============================================================================
    // MAIN
    // ============================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowersGameGUI().setVisible(true));
    }
}
