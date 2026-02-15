package FOR_EVal2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

// MAIN GUI - Towers Puzzle Game (4x4) with 4 Greedy Strategies
public class TowersssGameGUI extends JFrame {
    private static final int N = 4;
private static final int[] TOP = {1, 3, 2, 2};
private static final int[] RIGHT = {3, 2, 1, 2};
private static final int[] BOTTOM = {3, 1, 2, 2};
private static final int[] LEFT = {1, 3, 2, 2};

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
        getContentPane().setBackground(new Color(15, 23, 42)); // Dark slate background

        initGame();
        initComponents();

        pack();
        setMinimumSize(new Dimension(1000, 1000));   // forces a nice big window
        setResizable(true);
        setLocationRelativeTo(null);
        //setResizable(false);
        updateDisplay();
    }


    // GAME INITIALIZATION
    private void initGame() {
        // Generate a valid puzzle with consistent clues
        PuzzleGenerator generator = new PuzzleGenerator();
        PuzzleGenerator.PuzzleData puzzle = generator.generatePuzzle();

        // Initialize game state with generated clues
        gameState = new GameState(
                puzzle.topClues,
                puzzle.rightClues,
                puzzle.bottomClues,
                puzzle.leftClues
        );

        // Initialize strategies
        strategyLives = new StrategyLives(gameState);
        strategyCompletion = new StrategyCompletion(gameState);
        strategyScore = new StrategyScore(gameState);
        strategyMRV = new StrategyMRV(gameState);
    }

    // GUI COMPONENTS
    private void initComponents() {

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 15, 8));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        humanScoreLabel = createLabel("YOU - Score: 0", new Color(59, 130, 246), new Color(30, 58, 138));
        cpuScoreLabel = createLabel("CPU - Score: 0", new Color(168, 85, 247), new Color(88, 28, 135));
        humanLivesLabel = createLabel("Lives: 100", new Color(16, 185, 129), new Color(6, 78, 59));
        cpuLivesLabel = createLabel("Lives: 100", new Color(239, 68, 68), new Color(127, 29, 29));

        topPanel.add(humanScoreLabel);
        topPanel.add(cpuScoreLabel);
        topPanel.add(humanLivesLabel);
        topPanel.add(cpuLivesLabel);
        add(topPanel, BorderLayout.NORTH);

        // Game board with modern styling
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setOpaque(false);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);

        // Top clues with modern styling
        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = 0;
            boardPanel.add(createClue(TOP[i]), gbc);
        }

        // Board with left/right clues
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

        // Bottom clues
        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = N + 1;
            boardPanel.add(createClue(BOTTOM[i]), gbc);
        }
        add(boardPanel, BorderLayout.CENTER);

        // Right control panel with modern design - CLEAN & ALIGNED
JPanel rightPanel = new JPanel();
rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
rightPanel.setOpaque(false);
rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));  // padding around whole panel
rightPanel.setPreferredSize(new Dimension(360, 0));  // slightly wider for breathing room

// === STRATEGY CARD ===
JPanel strategyCard = createModernCard();
strategyCard.setLayout(new BoxLayout(strategyCard, BoxLayout.Y_AXIS));
strategyCard.setAlignmentX(CENTER_ALIGNMENT);
strategyCard.setMaximumSize(new Dimension(320, 140));

JLabel stratLabel = new JLabel(" CPU STRATEGY");
stratLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
stratLabel.setForeground(new Color(148, 163, 184));
stratLabel.setAlignmentX(CENTER_ALIGNMENT);

strategyCombo = new JComboBox<>();
for (Strategy s : Strategy.values()) strategyCombo.addItem(s.toString());
strategyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
strategyCombo.setMaximumSize(new Dimension(300, 40));
strategyCombo.setAlignmentX(CENTER_ALIGNMENT);
strategyCombo.setBackground(new Color(30, 41, 59));
strategyCombo.setForeground(Color.WHITE);
strategyCombo.addActionListener(e -> {
    currentStrategy = Strategy.values()[strategyCombo.getSelectedIndex()];
    updateHeatMap();
    updateDisplay();
});

heatMapToggle = new JCheckBox(" Show Heat Map", true);
heatMapToggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
heatMapToggle.setForeground(Color.WHITE);
heatMapToggle.setOpaque(false);
heatMapToggle.setAlignmentX(CENTER_ALIGNMENT);
heatMapToggle.addActionListener(e -> {
    showHeatMap = heatMapToggle.isSelected();
    updateDisplay();
});

strategyCard.add(Box.createVerticalStrut(10));
strategyCard.add(stratLabel);
strategyCard.add(Box.createVerticalStrut(10));
strategyCard.add(strategyCombo);
strategyCard.add(Box.createVerticalStrut(15));
strategyCard.add(heatMapToggle);
strategyCard.add(Box.createVerticalStrut(10));

rightPanel.add(strategyCard);
rightPanel.add(Box.createVerticalStrut(25)); 

// === NEW GAME BUTTON ===
JButton resetBtn = createModernButton("🔄 New Game", new Color(59, 130, 246));
resetBtn.setAlignmentX(CENTER_ALIGNMENT);
resetBtn.setMaximumSize(new Dimension(300, 55));
resetBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
resetBtn.addActionListener(e -> resetGame());

rightPanel.add(resetBtn);
rightPanel.add(Box.createVerticalStrut(25));

// === CPU REASONING CARD ===
JPanel reasoningCard = createModernCard();
reasoningCard.setLayout(new BoxLayout(reasoningCard, BoxLayout.Y_AXIS));
reasoningCard.setAlignmentX(CENTER_ALIGNMENT);
reasoningCard.setMaximumSize(new Dimension(320, 300)); 

JLabel reasonLabel = new JLabel("💭 CPU REASONING");
reasonLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
reasonLabel.setForeground(new Color(148, 163, 184));
reasonLabel.setAlignmentX(CENTER_ALIGNMENT);

reasoningArea = new JTextArea(10, 25);
reasoningArea.setEditable(false);
reasoningArea.setFont(new Font("Consolas", Font.PLAIN, 13));
reasoningArea.setLineWrap(true);
reasoningArea.setWrapStyleWord(true);
reasoningArea.setBackground(new Color(15, 23, 42));
reasoningArea.setForeground(new Color(203, 213, 225));
reasoningArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
reasoningArea.setText("Select a strategy and watch the CPU think...");

JScrollPane reasonScroll = new JScrollPane(reasoningArea);
reasonScroll.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));
reasonScroll.setOpaque(false);
reasonScroll.getViewport().setOpaque(false);

reasoningCard.add(Box.createVerticalStrut(5));
reasoningCard.add(reasonLabel);
reasoningCard.add(Box.createVerticalStrut(5));
reasoningCard.add(reasonScroll);
reasoningCard.add(Box.createVerticalStrut(5));

rightPanel.add(reasoningCard);
rightPanel.add(Box.createVerticalStrut(25));  

// === VALUE SELECTION PANEL ===
valueSelectionPanel = new JPanel();
valueSelectionPanel.setLayout(new BoxLayout(valueSelectionPanel, BoxLayout.Y_AXIS));
valueSelectionPanel.setBackground(new Color(30, 41, 59));
valueSelectionPanel.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(new Color(59, 130, 246), 3),
    BorderFactory.createEmptyBorder(6, 6, 6, 6)
));
valueSelectionPanel.setMaximumSize(new Dimension(280, 200));
valueSelectionPanel.setAlignmentX(CENTER_ALIGNMENT);
valueSelectionPanel.setVisible(false);

JLabel selectLabel = new JLabel("SELECT VALUE");
selectLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
selectLabel.setForeground(new Color(59, 130, 246));
selectLabel.setAlignmentX(CENTER_ALIGNMENT);

JPanel valGrid = new JPanel(new GridLayout(2, 2, 8, 8));
valGrid.setOpaque(false);
for (int i = 0; i < N; i++) {
    final int val = i + 1;
    JButton btn = createModernValueButton(String.valueOf(val));
    btn.addActionListener(e -> handleValueClick(val));
    valueButtons[i] = btn;
    valGrid.add(btn);
}

JButton cancelBtn = createModernButton("Cancel", new Color(239, 68, 68));
cancelBtn.setAlignmentX(CENTER_ALIGNMENT);
cancelBtn.setMaximumSize(new Dimension(180, 25));
cancelBtn.addActionListener(e -> {
    selectedRow = -1; selectedCol = -1;
    valueSelectionPanel.setVisible(false);
    updateDisplay();
});

valueSelectionPanel.add(selectLabel);
valueSelectionPanel.add(Box.createVerticalStrut(6));
valueSelectionPanel.add(valGrid);
valueSelectionPanel.add(Box.createVerticalStrut(10));
valueSelectionPanel.add(cancelBtn);

rightPanel.add(valueSelectionPanel);

add(rightPanel, BorderLayout.EAST);

        rightPanel.add(valueSelectionPanel);
        add(rightPanel, BorderLayout.EAST);

        // Bottom status with modern styling
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 25, 25));

        statusLabel = new JLabel("Your turn! Click an empty cell.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(30, 41, 59));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                BorderFactory.createEmptyBorder(15, 30, 15, 30)));

        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }


    private JLabel createLabel(String txt, Color fg, Color bg) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        l.setForeground(fg);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg.brighter(), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        return l;
    }

    private JPanel createModernCard() {
        JPanel card = new JPanel();
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(280, 200));
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
        btn.setPreferredSize(new Dimension(55, 55));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 28));
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

    
    private JLabel createClue(int v) {
        JLabel l = new JLabel(String.valueOf(v), SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 20));
        l.setForeground(new Color(79, 70, 229));
        l.setPreferredSize(new Dimension(40, 40));
        return l;}

    private JLabel createModernClue(int v, String arrow) {
    String html;
    if (arrow.equals("→") || arrow.equals("←")) {
        html = "<html><center>" + (arrow.equals("→") ? arrow + " <b>" + v + "</b>" : "<b>" + v + "</b> " + arrow) + "</center></html>";
    } else {
        html = "<html><center>" + arrow + "<br><b>" + v + "</b></center></html>";
    }
    JLabel l = new JLabel(html, SwingConstants.CENTER);
    l.setFont(new Font("Segoe UI", Font.BOLD, 16));  // Reduced from 18 to fit better in square
    l.setForeground(new Color(148, 163, 184));
    l.setOpaque(true);
    l.setBackground(new Color(30, 41, 59));
    //l.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2));
    l.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(new Color(71, 85, 105), 3),
    BorderFactory.createEmptyBorder(5, 5, 5, 5)
));
    l.setPreferredSize(new Dimension(95, 95));  // Match cell size for uniformity
    return l;
}

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

    // USER INTERACTION
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
            boolean legal = gameState.checkLegalMove(selectedRow, selectedCol, val);
            valueButtons[i].setEnabled(legal);
            valueButtons[i].setBackground(legal ? new Color(59, 130, 246) : new Color(71, 85, 105));
        }
        valueSelectionPanel.setVisible(true);
        statusLabel.setText("Choose a value for cell (" + (selectedRow+1) + "," + (selectedCol+1) + ")");
    }

    private void handleValueClick(int val) {
        if (selectedRow == -1) return;

        // Check for deadlock BEFORE allowing move
        if (gameState.checkForDeadlock(true)) {
            statusLabel.setText("You have no legal moves! -5 lives, skipping turn");
            selectedRow = -1;
            selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            gameState.setHumanTurn(false);
            updateDisplay();
            
            Timer delay = new Timer(1500, e -> {
                if (!checkGameEnd()) {
                    updateHeatMap();
                    animateHeatMap(0);
                }
            });
            delay.setRepeats(false);
            delay.start();
            return;
        }

        boolean moveAccepted = gameState.makeMove(selectedRow, selectedCol, val, true);
        

        selectedRow = -1;
        selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        
        // ONLY switch turns if move was valid (not rejected)
        if (moveAccepted) {
            gameState.setHumanTurn(false);
        }
        
        // Clear heat map immediately after human move
        clearHeatMap();
        updateDisplay();

        if (checkGameEnd()) return;

        // Only proceed to CPU turn if move was accepted
        if (moveAccepted) {
            Timer delay = new Timer(600, e -> {
                updateHeatMap();
                animateHeatMap(0);
            });
            delay.setRepeats(false);
            delay.start();
        }
    }

    // HEAT MAP ANIMATION & COLORS
    private void animateHeatMap(int idx) {
        if (idx >= N * N) {
            Timer delay = new Timer(1200, e -> {
                if (!gameState.isGameOver()) {
                    doCPUMove();
                    clearHeatMap(); // Clear heat map after CPU move
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

    private Color getHeatColor(double h) {
        if (h < 0.01) return new Color(30, 41, 59);

        double ratio = Math.min(h, 1.0);

        return switch (currentStrategy) {
            case LIVES ->       new Color(34 + (int)(151 * ratio), 185 + (int)(31 * ratio), 95 + (int)(125 * ratio));
            case COMPLETION ->  new Color(239 + (int)(15 * ratio), 68 + (int)(82 * ratio), 68 + (int)(82 * ratio));
            case SCORE ->       new Color(255, 165 + (int)(90 * ratio), 0);
            case MRV ->         new Color(130 + (int)(56 * ratio), 39, 144 + (int)(64 * ratio));
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

    // CPU MOVE
    private void doCPUMove() {
        // Check for deadlock for CPU
        if (gameState.checkForDeadlock(false)) {
            statusLabel.setText("CPU has no legal moves! -5 lives, skipping turn");
            gameState.setHumanTurn(true);
            updateDisplay();
            return;
        }
        
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
        boolean moveAccepted = gameState.makeMove(move[0], move[1], move[2], false);

        // Clear heat map values after CPU move
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                heatMapValues[r][c] = 0;
            }
        }
    }

    // HEAT MAP CALCULATION
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

    // DISPLAY UPDATE
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

        humanScoreLabel.setText("YOU - Score: " + gameState.getHumanScore());
        humanLivesLabel.setText("Lives: " + gameState.getHumanLives());
        cpuScoreLabel.setText("CPU - Score: " + gameState.getCpuScore());
        cpuLivesLabel.setText("Lives: " + gameState.getCpuLives());

        String msg = gameState.getStatusMessage();
        if (!msg.isEmpty()) {
            statusLabel.setText(msg);
        } else if (gameState.isHumanTurn()) {
            statusLabel.setText(selectedRow == -1 ? "✨ Your turn! Click a cell." : "🎯 Select a value");
        } else {
            statusLabel.setText("🤔 CPU thinking (" + currentStrategy + ")...");
        }
    }


    // GAME END & RESET
    private boolean checkGameEnd() {
        if (gameState.isGameOver()) {
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
        selectedRow = -1;
        selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        reasoningArea.setText("Select a strategy and watch the CPU think...");
        updateHeatMap();
        updateDisplay();
        statusLabel.setText("✨ New game started! Your turn.");
    }

    // MAIN
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowersssGameGUI().setVisible(true));
    }
}