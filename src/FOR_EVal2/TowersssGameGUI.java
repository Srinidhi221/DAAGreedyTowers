package FOR_EVal2;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN GUI  –  Towers Puzzle  (4 × 4)   •   DP  &  D&C  AI Strategies
//  Splash screen → algorithm chooser → full game
// ─────────────────────────────────────────────────────────────────────────────
public class TowersssGameGUI extends JFrame {

    // ── puzzle constants ────────────────────────────────────────────────────
    private static final int N = 4;
    private static final int[] TOP    = {1, 3, 2, 2};
    private static final int[] RIGHT  = {3, 2, 1, 2};
    private static final int[] BOTTOM = {3, 1, 2, 2};
    private static final int[] LEFT   = {1, 3, 2, 2};

    // ── game objects ────────────────────────────────────────────────────────
    private GameState           gameState;
    private StrategyDP          strategyDP;
    private StrategyDnC         strategyDnC;

    // ── selection state ─────────────────────────────────────────────────────
    private int selectedRow = -1, selectedCol = -1;

    // ── UI widgets ──────────────────────────────────────────────────────────
    private JButton[][]  cellButtons     = new JButton[N][N];
    private JButton[]    valueButtons    = new JButton[N];
    private JLabel       statusLabel, humanScoreLabel, humanLivesLabel,
            cpuScoreLabel,  cpuLivesLabel;
    private JPanel       valueSelectionPanel;
    private JCheckBox    heatMapToggle;
    private JTextArea    reasoningArea;

    // ── strategy choice ─────────────────────────────────────────────────────
    public enum AlgoChoice { DP, DNC }
    private AlgoChoice currentAlgo = AlgoChoice.DP;

    // ── heat-map ─────────────────────────────────────────────────────────────
    private boolean  showHeatMap   = true;
    private double[][] heatMapValues = new double[N][N];

    // ── card-layout panels ──────────────────────────────────────────────────
    private CardLayout cardLayout;
    private JPanel     rootPanel;

    // ════════════════════════════════════════════════════════════════════════
    //  Constructor
    // ════════════════════════════════════════════════════════════════════════
    public TowersssGameGUI() {
        setTitle("Towers Puzzle — DP & D&C Strategies");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        rootPanel  = new JPanel(cardLayout);

        rootPanel.add(buildSplashPanel(), "SPLASH");
        rootPanel.add(buildGamePanel(),   "GAME");

        setContentPane(rootPanel);
        cardLayout.show(rootPanel, "SPLASH");

        pack();
        setMinimumSize(new Dimension(1100, 820));
        setResizable(true);
        setLocationRelativeTo(null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SPLASH / STRATEGY SELECTION PANEL
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildSplashPanel() {

        // ── animated gradient background ────────────────────────────────────
        AnimatedSplashBG bg = new AnimatedSplashBG();
        bg.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        // ── title block ─────────────────────────────────────────────────────
        JLabel title = new JLabel("TOWERS PUZZLE");
        title.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 52));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("4 × 4  •  Strategic Height Deduction");
        subtitle.setFont(new Font("Courier New", Font.PLAIN, 17));
        subtitle.setForeground(new Color(180, 200, 255, 200));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── divider ─────────────────────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(420, 2));
        sep.setForeground(new Color(100, 140, 255, 150));

        // ── section label ───────────────────────────────────────────────────
        JLabel chooseLabel = new JLabel("CHOOSE  CPU  ALGORITHM");
        chooseLabel.setFont(new Font("Courier New", Font.BOLD, 14));
        chooseLabel.setForeground(new Color(160, 190, 255));
        chooseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── DP card ──────────────────────────────────────────────────────────
        AlgoCard dpCard = new AlgoCard(
                "DP",
                "Dynamic Programming",
                "Memoises sub-problems to\nfind globally optimal moves.\nSmart, but heavy on memory.",
                new Color(56, 189, 248),    // sky-blue accent
                new Color(14, 165, 233)
        );

        // ── D&C card ─────────────────────────────────────────────────────────
        AlgoCard dncCard = new AlgoCard(
                "D&C",
                "Divide & Conquer",
                "Recursively splits the board\ninto sub-problems & merges.\nFast & aggressive.",
                new Color(167, 139, 250),   // violet accent
                new Color(139, 92, 246)
        );

        // single-selection behaviour
        dpCard .addActionListener(e -> { dpCard.setSelected(true);  dncCard.setSelected(false); currentAlgo = AlgoChoice.DP;  });
        dncCard.addActionListener(e -> { dpCard.setSelected(false); dncCard.setSelected(true);  currentAlgo = AlgoChoice.DNC; });
        dpCard.setSelected(true); // default

        JPanel algoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        algoRow.setOpaque(false);
        algoRow.add(dpCard);
        algoRow.add(dncCard);
        algoRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        algoRow.setMaximumSize(new Dimension(500, 220));

        // ── start button ─────────────────────────────────────────────────────
        JButton startBtn = new JButton("START GAME") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(56, 189, 248),
                        getWidth(), 0, new Color(139, 92, 246));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        startBtn.setFont(new Font("Courier New", Font.BOLD, 20));
        startBtn.setForeground(Color.WHITE);
        startBtn.setContentAreaFilled(false);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.setPreferredSize(new Dimension(260, 56));
        startBtn.setMaximumSize(new Dimension(260, 56));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        startBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { startBtn.setOpaque(false); }
            public void mouseExited (MouseEvent e) { startBtn.setOpaque(false); }
        });

        startBtn.addActionListener(e -> {
            initGame();
            updateHeatMap();
            updateDisplay();
            cardLayout.show(rootPanel, "GAME");
        });

        // ── assemble ─────────────────────────────────────────────────────────
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(22));
        card.add(sep);
        card.add(Box.createVerticalStrut(22));
        card.add(chooseLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(algoRow);
        card.add(Box.createVerticalStrut(30));
        card.add(startBtn);
        card.add(Box.createVerticalStrut(10));

        bg.add(card, gc);
        return bg;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ANIMATED GRADIENT BACKGROUND PANEL
    // ════════════════════════════════════════════════════════════════════════
    static class AnimatedSplashBG extends JPanel {
        private float offset = 0f;
        private final Timer anim;

        AnimatedSplashBG() {
            setBackground(new Color(5, 8, 24));
            anim = new Timer(30, e -> { offset += 0.008f; repaint(); });
            anim.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // base deep-navy fill
            g2.setColor(new Color(5, 8, 24));
            g2.fillRect(0, 0, w, h);

            // slow-moving radial glow blobs
            float[] cx = { (float)(w*0.3 + Math.sin(offset)*0.12*w),
                    (float)(w*0.7 + Math.cos(offset*0.8)*0.10*w),
                    (float)(w*0.5 + Math.sin(offset*1.3)*0.09*w) };
            float[] cy = { (float)(h*0.35 + Math.cos(offset)*0.10*h),
                    (float)(h*0.60 + Math.sin(offset*0.9)*0.12*h),
                    (float)(h*0.20 + Math.cos(offset*1.1)*0.08*h) };
            Color[] blobColors = {
                    new Color(20, 60, 160, 90),
                    new Color(80, 20, 180, 80),
                    new Color(10, 120, 200, 70)
            };
            int[] radii = { 280, 220, 200 };

            for (int i = 0; i < 3; i++) {
                RadialGradientPaint rg = new RadialGradientPaint(
                        cx[i], cy[i], radii[i],
                        new float[]{0f, 1f},
                        new Color[]{blobColors[i], new Color(0, 0, 0, 0)}
                );
                g2.setPaint(rg);
                g2.fillRect(0, 0, w, h);
            }

            // subtle grid lines
            g2.setColor(new Color(40, 60, 120, 25));
            g2.setStroke(new BasicStroke(1));
            for (int x = 0; x < w; x += 50) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 50) g2.drawLine(0, y, w, y);

            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ALGORITHM SELECTION CARD WIDGET
    // ════════════════════════════════════════════════════════════════════════
    static class AlgoCard extends JPanel {
        private boolean selected = false;
        private final Color accentLight, accentDark;
        private final java.util.List<ActionListener> listeners = new java.util.ArrayList<>();
        private float hoverAlpha = 0f;
        private final Timer hoverAnim;

        AlgoCard(String tag, String name, String desc, Color accentLight, Color accentDark) {
            this.accentLight = accentLight;
            this.accentDark  = accentDark;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setPreferredSize(new Dimension(200, 200));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            hoverAnim = new Timer(16, e -> repaint());

            JLabel tagLbl = new JLabel(tag);
            tagLbl.setFont(new Font("Courier New", Font.BOLD, 32));
            tagLbl.setForeground(accentLight);
            tagLbl.setAlignmentX(CENTER_ALIGNMENT);

            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 14));
            nameLbl.setForeground(Color.WHITE);
            nameLbl.setAlignmentX(CENTER_ALIGNMENT);

            JTextArea descArea = new JTextArea(desc);
            descArea.setEditable(false);
            descArea.setOpaque(false);
            descArea.setFont(new Font("Courier New", Font.PLAIN, 11));
            descArea.setForeground(new Color(180, 195, 230));
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setMaximumSize(new Dimension(170, 70));
            descArea.setAlignmentX(CENTER_ALIGNMENT);

            add(Box.createVerticalStrut(18));
            add(tagLbl);
            add(Box.createVerticalStrut(4));
            add(nameLbl);
            add(Box.createVerticalStrut(10));
            add(descArea);
            add(Box.createVerticalGlue());

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hoverAlpha = 1f; hoverAnim.start(); }
                public void mouseExited (MouseEvent e) { hoverAlpha = 0f; hoverAnim.start(); }
                public void mouseClicked(MouseEvent e) {
                    selected = true;
                    for (ActionListener l : listeners) l.actionPerformed(null);
                    repaint();
                }
            });
        }

        void addActionListener(ActionListener l) { listeners.add(l); }
        void setSelected(boolean s) { selected = s; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(2, 2, w-4, h-4, 18, 18);

            // background fill
            Color base = selected
                    ? new Color(accentDark.getRed(), accentDark.getGreen(), accentDark.getBlue(), 60)
                    : new Color(15, 25, 55, 200);
            g2.setColor(base);
            g2.fill(rr);

            // border
            float bw = selected ? 3f : (hoverAlpha > 0.1f ? 2f : 1.5f);
            Color borderCol = selected ? accentLight
                    : new Color(accentLight.getRed(), accentLight.getGreen(),
                    accentLight.getBlue(), (int)(80 + 120*hoverAlpha));
            g2.setStroke(new BasicStroke(bw));
            g2.setColor(borderCol);
            g2.draw(rr);

            // glow when selected
            if (selected) {
                g2.setColor(new Color(accentLight.getRed(), accentLight.getGreen(),
                        accentLight.getBlue(), 35));
                g2.setStroke(new BasicStroke(10));
                g2.draw(rr);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME PANEL
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(5, 8, 24));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setBackground(new Color(5, 8, 24));

        // ── top scoreboards ──────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 14, 8));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        humanScoreLabel = createScoreLabel("YOU  •  Score: 0",  new Color(56, 189, 248));
        cpuScoreLabel   = createScoreLabel("CPU  •  Score: 0",  new Color(167, 139, 250));
        humanLivesLabel = createScoreLabel("Lives: 100",    new Color(52, 211, 153));
        cpuLivesLabel   = createScoreLabel("Lives: 100",    new Color(251, 113, 133));

        topPanel.add(humanScoreLabel);
        topPanel.add(cpuScoreLabel);
        topPanel.add(humanLivesLabel);
        topPanel.add(cpuLivesLabel);
        panel.add(topPanel, BorderLayout.NORTH);

        // ── board ────────────────────────────────────────────────────────────
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setOpaque(false);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(8, 25, 8, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);

        // top clues
        for (int i = 0; i < N; i++) {
            gbc.gridx = i+1; gbc.gridy = 0;
            boardPanel.add(makeClueLabel(TOP[i], "↓"), gbc);
        }
        // grid + side clues
        for (int r = 0; r < N; r++) {
            gbc.gridx = 0; gbc.gridy = r+1;
            boardPanel.add(makeClueLabel(LEFT[r], "→"), gbc);

            for (int c = 0; c < N; c++) {
                final int row = r, col = c;
                JButton btn = new JButton("") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(getBackground());
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                    @Override protected void paintBorder(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(getForeground().equals(Color.WHITE)
                                ? new Color(56, 189, 248, 120)
                                : new Color(60, 75, 110, 180));
                        g2.setStroke(new BasicStroke(2));
                        g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                        g2.dispose();
                    }
                };
                btn.setPreferredSize(new Dimension(78, 78));
                btn.setFont(new Font("Georgia", Font.BOLD, 34));
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> handleCellClick(row, col));
                cellButtons[r][c] = btn;
                gbc.gridx = c+1; gbc.gridy = r+1;
                boardPanel.add(btn, gbc);
            }

            gbc.gridx = N+1; gbc.gridy = r+1;
            boardPanel.add(makeClueLabel(RIGHT[r], "←"), gbc);
        }
        // bottom clues
        for (int i = 0; i < N; i++) {
            gbc.gridx = i+1; gbc.gridy = N+1;
            boardPanel.add(makeClueLabel(BOTTOM[i], "↑"), gbc);
        }
        panel.add(boardPanel, BorderLayout.CENTER);

        // ── right control panel ──────────────────────────────────────────────
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(18, 10, 18, 22));
        rightPanel.setPreferredSize(new Dimension(330, 0));

        // algo badge
        JPanel algoBadge = new JPanel(new BorderLayout());
        algoBadge.setBackground(new Color(14, 20, 48));
        algoBadge.setBorder(new CompoundBorder(
                new LineBorder(new Color(56, 130, 220, 160), 2, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        algoBadge.setMaximumSize(new Dimension(290, 80));
        algoBadge.setAlignmentX(CENTER_ALIGNMENT);

        JLabel algoTagLbl = new JLabel("CPU ALGORITHM");
        algoTagLbl.setFont(new Font("Courier New", Font.BOLD, 11));
        algoTagLbl.setForeground(new Color(120, 150, 210));

        // this label shows which algo was picked on the splash
        JLabel algoNameLbl = new JLabel("–") {
            // refreshed when game starts via updateAlgoBadge()
        };
        algoNameLbl.setName("ALGO_NAME");
        algoNameLbl.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 22));
        algoNameLbl.setForeground(Color.WHITE);

        algoBadge.add(algoTagLbl, BorderLayout.NORTH);
        algoBadge.add(algoNameLbl, BorderLayout.CENTER);

        // heatmap toggle
        heatMapToggle = new JCheckBox("  Show Heat Map", true);
        heatMapToggle.setFont(new Font("Courier New", Font.BOLD, 13));
        heatMapToggle.setForeground(new Color(180, 200, 255));
        heatMapToggle.setOpaque(false);
        heatMapToggle.setAlignmentX(CENTER_ALIGNMENT);
        heatMapToggle.addActionListener(e -> { showHeatMap = heatMapToggle.isSelected(); updateDisplay(); });

        // back-to-menu button
        JButton backBtn = buildFancyBtn("Change Algorithm", new Color(80, 100, 200));
        backBtn.setAlignmentX(CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(290, 46));
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "SPLASH"));

        // new game button
        JButton resetBtn = buildFancyBtn("New Game", new Color(22, 160, 120));
        resetBtn.setAlignmentX(CENTER_ALIGNMENT);
        resetBtn.setMaximumSize(new Dimension(290, 46));
        resetBtn.addActionListener(e -> resetGame());

        // reasoning card
        JPanel reasonCard = buildDarkCard(290, 280);
        reasonCard.setLayout(new BoxLayout(reasonCard, BoxLayout.Y_AXIS));
        reasonCard.setAlignmentX(CENTER_ALIGNMENT);

        JLabel reasonTitle = new JLabel("CPU REASONING");
        reasonTitle.setFont(new Font("Courier New", Font.BOLD, 12));
        reasonTitle.setForeground(new Color(120, 155, 220));
        reasonTitle.setAlignmentX(CENTER_ALIGNMENT);

        reasoningArea = new JTextArea(9, 22);
        reasoningArea.setEditable(false);
        reasoningArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        reasoningArea.setLineWrap(true);
        reasoningArea.setWrapStyleWord(true);
        reasoningArea.setBackground(new Color(8, 12, 30));
        reasoningArea.setForeground(new Color(180, 210, 255));
        reasoningArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        reasoningArea.setText("Select a cell to begin.\nWatch the CPU think…");

        JScrollPane reasonScroll = new JScrollPane(reasoningArea);
        reasonScroll.setBorder(new LineBorder(new Color(40, 60, 110), 1));
        reasonScroll.setOpaque(false);
        reasonScroll.getViewport().setOpaque(false);

        reasonCard.add(Box.createVerticalStrut(6));
        reasonCard.add(reasonTitle);
        reasonCard.add(Box.createVerticalStrut(6));
        reasonCard.add(reasonScroll);
        reasonCard.add(Box.createVerticalStrut(6));

        // value selection panel
        valueSelectionPanel = buildValueSelectionPanel();
        valueSelectionPanel.setAlignmentX(CENTER_ALIGNMENT);
        valueSelectionPanel.setVisible(false);

        rightPanel.add(algoBadge);
        rightPanel.add(Box.createVerticalStrut(12));
        rightPanel.add(heatMapToggle);
        rightPanel.add(Box.createVerticalStrut(14));
        rightPanel.add(backBtn);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(resetBtn);
        rightPanel.add(Box.createVerticalStrut(18));
        rightPanel.add(reasonCard);
        rightPanel.add(Box.createVerticalStrut(18));
        rightPanel.add(valueSelectionPanel);

        panel.add(rightPanel, BorderLayout.EAST);

        // ── bottom status bar ─────────────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 25, 22, 25));

        statusLabel = new JLabel("Your turn! Click an empty cell.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Courier New", Font.BOLD, 15));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(14, 20, 45));
        statusLabel.setBorder(new CompoundBorder(
                new LineBorder(new Color(50, 80, 150, 180), 2),
                new EmptyBorder(12, 28, 12, 28)
        ));
        bottomPanel.add(statusLabel);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALUE SELECTION PANEL (inline picker)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildValueSelectionPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(14, 22, 52));
        p.setBorder(new CompoundBorder(
                new LineBorder(new Color(56, 189, 248), 2, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        p.setMaximumSize(new Dimension(290, 210));

        JLabel selLabel = new JLabel("SELECT  VALUE");
        selLabel.setFont(new Font("Courier New", Font.BOLD, 13));
        selLabel.setForeground(new Color(56, 189, 248));
        selLabel.setAlignmentX(CENTER_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        for (int i = 0; i < N; i++) {
            final int val = i + 1;
            JButton btn = buildFancyBtn(String.valueOf(val), new Color(30, 100, 200));
            btn.setFont(new Font("Georgia", Font.BOLD, 28));
            btn.setPreferredSize(new Dimension(60, 60));
            btn.addActionListener(e -> handleValueClick(val));
            valueButtons[i] = btn;
            grid.add(btn);
        }

        JButton cancelBtn = buildFancyBtn("Cancel", new Color(180, 50, 70));
        cancelBtn.setAlignmentX(CENTER_ALIGNMENT);
        cancelBtn.setMaximumSize(new Dimension(190, 36));
        cancelBtn.addActionListener(e -> {
            selectedRow = -1; selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            updateDisplay();
        });

        p.add(selLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(grid);
        p.add(Box.createVerticalStrut(8));
        p.add(cancelBtn);
        return p;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS  –  small reusable widgets
    // ════════════════════════════════════════════════════════════════════════
    private JLabel createScoreLabel(String text, Color accent) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Courier New", Font.BOLD, 15));
        l.setForeground(accent);
        l.setOpaque(true);
        l.setBackground(new Color(10, 18, 40));
        l.setBorder(new CompoundBorder(
                new LineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160), 2),
                new EmptyBorder(8, 18, 8, 18)
        ));
        return l;
    }

    private JLabel makeClueLabel(int v, String arrow) {
        JLabel l = new JLabel(arrow + " " + v, SwingConstants.CENTER);
        l.setFont(new Font("Courier New", Font.BOLD, 15));
        l.setForeground(new Color(130, 170, 255));
        l.setOpaque(true);
        l.setBackground(new Color(14, 22, 50));
        l.setBorder(new CompoundBorder(
                new LineBorder(new Color(50, 80, 150), 2),
                new EmptyBorder(4, 4, 4, 4)
        ));
        l.setPreferredSize(new Dimension(60, 60));
        return l;
    }

    private JButton buildFancyBtn(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? color.brighter() : color;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Courier New", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 42));
        return btn;
    }

    private JPanel buildDarkCard(int maxW, int maxH) {
        JPanel card = new JPanel();
        card.setBackground(new Color(10, 16, 38));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 60, 120), 2),
                new EmptyBorder(10, 10, 10, 10)
        ));
        card.setMaximumSize(new Dimension(maxW, maxH));
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME INITIALIZATION
    // ════════════════════════════════════════════════════════════════════════
    private void initGame() {
        PuzzleGenerator generator = new PuzzleGenerator();
        PuzzleGenerator.PuzzleData puzzle = generator.generatePuzzle();

        gameState = new GameState(
                puzzle.topClues, puzzle.rightClues,
                puzzle.bottomClues, puzzle.leftClues
        );

        strategyDP  = new StrategyDP(gameState);
        strategyDnC = new StrategyDnC(gameState);

        // refresh the algo badge label on the game panel
        updateAlgoBadge();
    }

    /** Finds the ALGO_NAME label in the game panel and updates it. */
    private void updateAlgoBadge() {
        // walk the component tree
        JPanel gamePanel = (JPanel) rootPanel.getComponent(1);
        updateAlgoBadgeIn(gamePanel);
    }

    private void updateAlgoBadgeIn(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel lbl = (JLabel) comp;
                if ("ALGO_NAME".equals(lbl.getName())) {
                    if (currentAlgo == AlgoChoice.DP) {
                        lbl.setText("Dynamic Programming");
                        lbl.setForeground(new Color(56, 189, 248));
                    } else {
                        lbl.setText("Divide && Conquer");
                        lbl.setForeground(new Color(167, 139, 250));
                    }
                    return;
                }
            }
            if (comp instanceof Container) updateAlgoBadgeIn((Container) comp);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  USER INTERACTION
    // ════════════════════════════════════════════════════════════════════════
    private void handleCellClick(int r, int c) {
        if (!gameState.isHumanTurn() || gameState.isGameOver() || gameState.getGrid()[r][c] != 0) return;
        selectedRow = r;
        selectedCol = c;
        showValueSelection();
        updateDisplay();
    }

    private void showValueSelection() {
        for (int i = 0; i < N; i++) {
            boolean legal = gameState.checkLegalMove(selectedRow, selectedCol, i+1);
            valueButtons[i].setEnabled(legal);
        }
        valueSelectionPanel.setVisible(true);
        statusLabel.setText("Choose a value for cell (" + (selectedRow+1) + "," + (selectedCol+1) + ")");
    }

    private void handleValueClick(int val) {
        if (selectedRow == -1) return;

        if (gameState.checkForDeadlock(true)) {
            statusLabel.setText("No legal moves! −5 lives, skipping turn.");
            selectedRow = -1; selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            gameState.setHumanTurn(false);
            updateDisplay();
            new Timer(1500, e -> {
                if (!checkGameEnd()) { updateHeatMap(); animateHeatMap(0); }
            }) {{ setRepeats(false); }}.start();
            return;
        }

        boolean accepted = gameState.makeMove(selectedRow, selectedCol, val, true);
        selectedRow = -1; selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        if (accepted) gameState.setHumanTurn(false);
        clearHeatMap();
        updateDisplay();
        if (checkGameEnd()) return;
        if (accepted)
            new Timer(600, e -> { updateHeatMap(); animateHeatMap(0); }) {{ setRepeats(false); }}.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CPU MOVE
    // ════════════════════════════════════════════════════════════════════════
    private void doCPUMove() {
        if (gameState.checkForDeadlock(false)) {
            statusLabel.setText("CPU has no legal moves! −5 lives, skipping turn.");
            gameState.setHumanTurn(true);
            updateDisplay();
            return;
        }

        int[] move = (currentAlgo == AlgoChoice.DP)
                ? strategyDP.findBestMove()
                : strategyDnC.findBestMove();

        if (move == null) {
            gameState.setStatusMessage("CPU has no valid moves!");
            clearHeatMap();
            updateDisplay();
            return;
        }

        reasoningArea.setText(gameState.getCpuReasoningExplanation());
        gameState.makeMove(move[0], move[1], move[2], false);
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                heatMapValues[r][c] = 0;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HEAT MAP
    // ════════════════════════════════════════════════════════════════════════
    private void updateHeatMap() {
        double max = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (gameState.getGrid()[r][c] == 0) {
                    double score = (currentAlgo == AlgoChoice.DP)
                            ? strategyDP.evaluateCell(r, c)
                            : strategyDnC.evaluateCell(r, c);
                    heatMapValues[r][c] = score;
                    max = Math.max(max, score);
                } else {
                    heatMapValues[r][c] = 0;
                }
            }
        }
        if (max > 0)
            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++)
                    heatMapValues[r][c] /= max;
    }

    private void animateHeatMap(int idx) {
        if (idx >= N * N) {
            new Timer(1200, e -> {
                if (!gameState.isGameOver()) {
                    doCPUMove();
                    clearHeatMap();
                    gameState.setHumanTurn(true);
                    updateDisplay();
                    checkGameEnd();
                }
            }) {{ setRepeats(false); }}.start();
            return;
        }
        int r = idx / N, c = idx % N;
        if (gameState.getGrid()[r][c] == 0 && showHeatMap)
            cellButtons[r][c].setBackground(heatColor(heatMapValues[r][c]));

        new Timer(70, e -> animateHeatMap(idx + 1)) {{ setRepeats(false); }}.start();
    }

    private Color heatColor(double h) {
        if (h < 0.01) return new Color(12, 20, 45);
        double t = Math.min(h, 1.0);
        return (currentAlgo == AlgoChoice.DP)
                // sky-blue → teal gradient for DP
                ? blend(new Color(14, 40, 100), new Color(56, 189, 248), t)
                // purple → violet gradient for D&C
                : blend(new Color(50, 14, 100), new Color(200, 140, 255), t);
    }

    private Color blend(Color a, Color b, double t) {
        return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    private void clearHeatMap() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (gameState.getGrid()[r][c] == 0)
                    cellButtons[r][c].setBackground(
                            (r == selectedRow && c == selectedCol)
                                    ? new Color(30, 90, 200)
                                    : new Color(12, 20, 45)
                    );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DISPLAY UPDATE
    // ════════════════════════════════════════════════════════════════════════
    private void updateDisplay() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton b = cellButtons[r][c];
                int val = gameState.getGrid()[r][c];
                if (val != 0) {
                    b.setText(String.valueOf(val));
                    b.setBackground(new Color(22, 80, 170));
                    b.setForeground(Color.WHITE);
                    b.setEnabled(false);
                } else {
                    b.setText("");
                    b.setEnabled(gameState.isHumanTurn() && !gameState.isGameOver());
                    if (r == selectedRow && c == selectedCol) {
                        b.setBackground(new Color(30, 90, 200));
                        b.setForeground(new Color(56, 189, 248));
                    } else {
                        b.setBackground(showHeatMap && heatMapValues[r][c] > 0.01
                                ? heatColor(heatMapValues[r][c])
                                : new Color(12, 20, 45));
                        b.setForeground(new Color(60, 80, 130));
                    }
                }
            }
        }

        humanScoreLabel.setText("YOU  •  Score: " + gameState.getHumanScore());
        humanLivesLabel.setText("Lives: " + gameState.getHumanLives());
        cpuScoreLabel  .setText("CPU  •  Score: " + gameState.getCpuScore());
        cpuLivesLabel  .setText("Lives: " + gameState.getCpuLives());

        String msg = gameState.getStatusMessage();
        if (!msg.isEmpty()) {
            statusLabel.setText(msg);
        } else if (gameState.isHumanTurn()) {
            statusLabel.setText(selectedRow == -1 ? "✦ Your turn — click a cell" : "✦ Choose a value");
        } else {
            String algoName = currentAlgo == AlgoChoice.DP ? "DP" : "D&C";
            statusLabel.setText("⟳  CPU thinking  [ " + algoName + " ] …");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME END  &  RESET
    // ════════════════════════════════════════════════════════════════════════
    private boolean checkGameEnd() {
        if (!gameState.isGameOver()) return false;
        String winner = gameState.getWinner();
        if (winner == null) winner = "Game Over";
        statusLabel.setText(winner);

        String msg = "═══  GAME OVER  ═══\n\n"
                + winner + "\n\n"
                + "YOU  →  Score: " + gameState.getHumanScore()
                +        "  |  Lives: " + gameState.getHumanLives() + "\n"
                + "CPU  →  Score: " + gameState.getCpuScore()
                +        "  |  Lives: " + gameState.getCpuLives();

        JOptionPane.showMessageDialog(this, msg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    private void resetGame() {
        initGame();
        selectedRow = -1; selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        reasoningArea.setText("New game — watch the CPU think…");
        clearHeatMap();
        updateHeatMap();
        updateDisplay();
        statusLabel.setText("✦ New game started — your turn.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowersssGameGUI().setVisible(true));
    }
}
