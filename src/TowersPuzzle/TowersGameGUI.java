import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Comparator;

// ═══════════════════════════════════════════════════════════════════════════
//  TOWERS PUZZLE  ·  Complete UI  ·  4 Strategies  ·  Live Heat-Map
// ═══════════════════════════════════════════════════════════════════════════
public class TowersGameGUI extends JFrame {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int N = 4;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG           = new Color(7,  9,  15);
    private static final Color SURFACE      = new Color(13, 18, 32);
    private static final Color SURFACE2     = new Color(19, 27, 48);
    private static final Color BORDER_DIM   = new Color(255,255,255, 18);
    private static final Color TEXT         = new Color(232, 234, 242);
    private static final Color MUTED        = new Color(140, 158, 190);
    private static final Color GOLD         = new Color(201, 168, 76);
    private static final Color GOLD_LIGHT   = new Color(240, 204, 122);

    // Per-algo accent colours
    private static final Color[] ACCENT = {
        new Color( 56, 189, 248),   // DP   – sky blue
        new Color(167, 139, 250),   // D&C  – violet
        new Color( 52, 211, 153),   // BT   – emerald
        new Color(251, 146,  60),   // B&B  – amber
    };

    // ── Algo enum ────────────────────────────────────────────────────────────
    public enum Algo { DP, DNC, BT, BB }
    private Algo currentAlgo = Algo.DP;

    // ── Game objects ─────────────────────────────────────────────────────────
    private GameState        gameState;
    private StrategyDP       stratDP;
    private StrategyDnC      stratDnC;
    private StrategyBTForwardCheck stratBT;
    private StrategyBranchBound  stratBB;

    // ── Selection ────────────────────────────────────────────────────────────
    private int selRow = -1, selCol = -1;

    // ── Heat-map ─────────────────────────────────────────────────────────────
    private double[][] heat = new double[N][N];
    private boolean showHeat = true;

    // ── UI widgets ───────────────────────────────────────────────────────────
    private JButton[][]  cell   = new JButton[N][N];
    private JButton[]    valBtn = new JButton[N];
    private JLabel       statusLbl, humanScoreLbl, humanLivesLbl, cpuScoreLbl, cpuLivesLbl;
    private JLabel       algoBadgeLbl;
    private JPanel       valuePanel;
    private JCheckBox    heatToggle;
    private JTextArea    reasonArea;

    // ── Card layout ──────────────────────────────────────────────────────────
    private CardLayout cardLayout;
    private JPanel     rootPanel;

    // ════════════════════════════════════════════════════════════════════════
    //  Constructor
    // ════════════════════════════════════════════════════════════════════════
    public TowersGameGUI() {
        setTitle("Towers Puzzle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        rootPanel  = new JPanel(cardLayout);
        rootPanel.add(buildMenuPanel(), "MENU");
        rootPanel.add(buildGamePanel(), "GAME");

        setContentPane(rootPanel);
        cardLayout.show(rootPanel, "MENU");

        pack();
        setMinimumSize(new Dimension(1180, 850));
        setResizable(true);
        setLocationRelativeTo(null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MENU  (same aesthetic as the approved HTML design, now in Swing)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildMenuPanel() {
        AnimatedBG bg = new AnimatedBG();
        bg.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        // ── eyebrow ──────────────────────────────────────────────────────────
        JLabel eye = styledLabel("4  ×  4   DEDUCTION  PUZZLE", 11, GOLD, Font.PLAIN);
        eye.setAlignmentX(CENTER_ALIGNMENT);

        // ── title ────────────────────────────────────────────────────────────
        JLabel title = new JLabel("TOWERS") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                FontMetrics fm = g2.getFontMetrics();
                GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, getWidth(), 0, GOLD_LIGHT);
                g2.setPaint(gp);
                g2.setFont(getFont());
                g2.drawString(getText(), 0, fm.getAscent());
                g2.dispose();
            }
        };
        title.setFont(new Font("Serif", Font.BOLD, 72));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        // ── sub ───────────────────────────────────────────────────────────────
        JLabel sub = styledLabel("CHOOSE YOUR STRATEGY  ·  CHALLENGE THE CPU", 12, MUTED, Font.PLAIN);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        // ── divider ──────────────────────────────────────────────────────────
        JPanel div = buildDivider("SELECT  CPU  ALGORITHM");
        div.setAlignmentX(CENTER_ALIGNMENT);
        div.setMaximumSize(new Dimension(660, 24));

        // ── 2×2 algo card grid ────────────────────────────────────────────────
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(660, 360));
        grid.setAlignmentX(CENTER_ALIGNMENT);

        String[] tags  = {"DP",  "D&C", "BT",  "B&B"};
        String[] names = {"Dynamic Programming","Divide & Conquer","Backtracking","Branch & Bound"};
        String[] codes = {"01","02","03","04"};
        String[] descs = {
            "Memoises sub-problems to find globally optimal moves.",
            "Recursively splits the board into quadrants, merges best.",
            "Depth-first search — prunes branches on constraint fail.",
            "Best-first with cost bounding — skips provably bad branches."
        };
        String[] pill1 = {"Memory-heavy","Fast","Exhaustive","Pruned search"};
        String[] pill2 = {"Optimal","Recursive","Constraint-driven","Optimal"};
        Algo[]   algos = {Algo.DP, Algo.DNC, Algo.BT, Algo.BB};

        AlgoCard[] cards = new AlgoCard[4];
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            cards[i] = new AlgoCard(tags[i], codes[i], names[i], descs[i], pill1[i], pill2[i], ACCENT[i]);
            cards[i].addActionListener(e -> {
                for (AlgoCard c : cards) c.setSelected(false);
                cards[idx].setSelected(true);
                currentAlgo = algos[idx];
            });
            grid.add(cards[i]);
        }
        cards[0].setSelected(true);

        // ── start button ──────────────────────────────────────────────────────
        JButton startBtn = new JButton("START  GAME  →") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GOLD_LIGHT, getWidth(), 0, GOLD);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        startBtn.setFont(new Font("Serif", Font.BOLD, 18));
        startBtn.setForeground(new Color(10, 12, 20));
        startBtn.setContentAreaFilled(false);
        startBtn.setFocusPainted(false);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.setPreferredSize(new Dimension(280, 54));
        startBtn.setMaximumSize(new Dimension(280, 54));
        startBtn.setAlignmentX(CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> { initGame(); updateHeat(); updateDisplay(); cardLayout.show(rootPanel, "GAME"); });

        card.add(Box.createVerticalStrut(8));
        card.add(eye);
        card.add(Box.createVerticalStrut(6));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(sub);
        card.add(Box.createVerticalStrut(24));
        card.add(div);
        card.add(Box.createVerticalStrut(20));
        card.add(grid);
        card.add(Box.createVerticalStrut(28));
        card.add(startBtn);
        card.add(Box.createVerticalStrut(12));

        bg.add(card, gc);
        return bg;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME BOARD PANEL
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildGamePanel() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);

        // ── top bar ───────────────────────────────────────────────────────────
        root.add(buildTopBar(), BorderLayout.NORTH);

        // ── centre: board + right panel ───────────────────────────────────────
        JPanel centre = new JPanel(new BorderLayout(20, 0));
        centre.setBackground(BG);
        centre.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        centre.add(buildBoardPanel(), BorderLayout.CENTER);
        centre.add(buildRightPanel(), BorderLayout.EAST);
        root.add(centre, BorderLayout.CENTER);

        // ── status bar ────────────────────────────────────────────────────────
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    // ── Top scoreboard ────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setBackground(new Color(10, 14, 28));
        bar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255,255,255,20)),
            BorderFactory.createEmptyBorder(22, 28, 22, 28)
        ));
        GridBagConstraints g = new GridBagConstraints();

        // Human side
        g.gridx=0; g.gridy=0; g.anchor=GridBagConstraints.WEST; g.weightx=1;
        humanScoreLbl = scoreLabel("YOU  ·  Score: 0",  ACCENT[0]);
        bar.add(humanScoreLbl, g);

        g.gridx=0; g.gridy=1;
        humanLivesLbl = scoreLabel("Lives: 100", new Color(52,211,153));
        bar.add(humanLivesLbl, g);

        // Title centre
        g.gridx=1; g.gridy=0; g.gridheight=2; g.anchor=GridBagConstraints.CENTER; g.weightx=0;
        JLabel t = new JLabel("TOWERS", SwingConstants.CENTER);
        t.setFont(new Font("Serif", Font.BOLD, 28));
        t.setForeground(GOLD_LIGHT);
        bar.add(t, g);

        // CPU side
        g.gridx=2; g.gridy=0; g.gridheight=1; g.anchor=GridBagConstraints.EAST; g.weightx=1;
        cpuScoreLbl = scoreLabel("CPU  ·  Score: 0", ACCENT[1]);
        bar.add(cpuScoreLbl, g);

        g.gridx=2; g.gridy=1;
        cpuLivesLbl = scoreLabel("Lives: 100", new Color(251,113,133));
        bar.add(cpuLivesLbl, g);

        return bar;
    }

    private JLabel scoreLabel(String t, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Monospaced", Font.BOLD, 22));
        l.setForeground(c);
        return l;
    }

    // ── Board (clues + cells) ─────────────────────────────────────────────────
    private JPanel buildBoardPanel() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);

        int[] top    = {1,3,2,2};
        int[] right  = {3,2,1,2};
        int[] bottom = {3,1,2,2};
        int[] left   = {1,3,2,2};

        // top clues
        for (int i = 0; i < N; i++) {
            gc.gridx = i+1; gc.gridy = 0;
            wrap.add(clueLabel(top[i], "↓"), gc);
        }
        // rows
        for (int r = 0; r < N; r++) {
            gc.gridx=0; gc.gridy=r+1;
            wrap.add(clueLabel(left[r], "→"), gc);

            for (int c = 0; c < N; c++) {
                final int fr=r, fc=c;
                cell[r][c] = buildCell(r, c);
                cell[r][c].addActionListener(e -> handleCellClick(fr, fc));
                gc.gridx=c+1; gc.gridy=r+1;
                wrap.add(cell[r][c], gc);
            }
            gc.gridx=N+1; gc.gridy=r+1;
            wrap.add(clueLabel(right[r], "←"), gc);
        }
        // bottom clues
        for (int i = 0; i < N; i++) {
            gc.gridx=i+1; gc.gridy=N+1;
            wrap.add(clueLabel(bottom[i], "↑"), gc);
        }
        return wrap;
    }

    private JButton buildCell(int r, int c) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // tower icon if value set
                int val = gameState != null ? gameState.getGrid()[r][c] : 0;
                if (val != 0) {
                    drawTower(g2, val, getWidth(), getHeight(), getBackground());
                }
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean selected = (r==selRow && c==selCol);
                g2.setColor(selected ? accentColor().brighter() : new Color(255,255,255,22));
                g2.setStroke(new BasicStroke(selected ? 2.5f : 1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(90, 90));
        btn.setBackground(SURFACE);
        btn.setFont(new Font("Serif", Font.BOLD, 38));
        btn.setForeground(TEXT);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Draw a stylised building silhouette for value v inside a cell. */
    private void drawTower(Graphics2D g2, int val, int W, int H, Color bg) {
        Color accent = accentColor();
        float ratio  = val / (float) N;
        int   floors = val;

        // int bW  = W / 3;
        // int bH  = (int)(H * 0.55 * ratio);
        // int bX  = (W - bW) / 2;
        // int bY  = H - bH - H/12;

        // GradientPaint gp = new GradientPaint(bX, bY, accent.brighter(),
        //                                      bX, bY+bH, accent.darker());
        // g2.setPaint(gp);
        // g2.fillRoundRect(bX, bY, bW, bH, 4, 4);

        // g2.setColor(new Color(255,255,255,50));
        // g2.setStroke(new BasicStroke(1));
        // for (int f = 1; f < floors; f++) {
        //     int fy = bY + (bH * f / floors);
        //     g2.drawLine(bX+2, fy, bX+bW-2, fy);
        // }

        // g2.setColor(new Color(255,255,200,180));
        // int winW = Math.max(3, bW/4), winH = Math.max(3, bH/(floors*2));
        // for (int f = 0; f < floors; f++) {
        //     int wy = bY + (bH * f / floors) + winH/2;
        //     g2.fillRect(bX + bW/2 - winW/2, wy, winW, winH);
        // }

        g2.setFont(new Font("Serif", Font.BOLD, W/4));
        g2.setColor(TEXT);
        FontMetrics fm = g2.getFontMetrics();
        String s = String.valueOf(val);
        g2.drawString(s, (W - fm.stringWidth(s))/2, H/2 + fm.getAscent()/4);
    }

    private JLabel clueLabel(int v, String arrow) {
        JLabel l = new JLabel(arrow + " " + v, SwingConstants.CENTER);
        l.setFont(new Font("Monospaced", Font.BOLD, 14));
        l.setForeground(GOLD_LIGHT);
        l.setOpaque(true);
        l.setBackground(new Color(20, 30, 60));
        l.setBorder(new CompoundBorder(
            new LineBorder(new Color(201,168,76,80), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));
        l.setPreferredSize(new Dimension(58, 58));
        return l;
    }

    // ── Right panel ───────────────────────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setPreferredSize(new Dimension(300, 0));

        // algo badge
        JPanel badge = darkCard(280, 72);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        JLabel tag = styledLabel("CPU  ALGORITHM", 9, MUTED, Font.PLAIN);
        tag.setAlignmentX(CENTER_ALIGNMENT);
        algoBadgeLbl = styledLabel("Dynamic Programming", 18, ACCENT[0], Font.BOLD);
        algoBadgeLbl.setAlignmentX(CENTER_ALIGNMENT);
        badge.add(Box.createVerticalStrut(10));
        badge.add(tag);
        badge.add(Box.createVerticalStrut(4));
        badge.add(algoBadgeLbl);
        badge.add(Box.createVerticalStrut(10));
        badge.setAlignmentX(CENTER_ALIGNMENT);

        // heatmap toggle
        heatToggle = new JCheckBox("  Show Heat Map", true);
        heatToggle.setFont(new Font("Monospaced", Font.BOLD, 12));
        heatToggle.setForeground(new Color(180, 200, 255));
        heatToggle.setOpaque(false);
        heatToggle.setAlignmentX(CENTER_ALIGNMENT);
        heatToggle.addActionListener(e -> { showHeat = heatToggle.isSelected(); updateDisplay(); });

        // buttons
        JButton backBtn  = fancyBtn("← Change Algorithm", new Color(60, 80, 180));
        JButton resetBtn = fancyBtn("New Game", new Color(22,140,110));
        backBtn .setAlignmentX(CENTER_ALIGNMENT);
        resetBtn.setAlignmentX(CENTER_ALIGNMENT);
        backBtn .setMaximumSize(new Dimension(280, 44));
        resetBtn.setMaximumSize(new Dimension(280, 44));
        backBtn .addActionListener(e -> cardLayout.show(rootPanel, "MENU"));
        resetBtn.addActionListener(e -> resetGame());

        // reasoning panel
        JPanel reasonCard = darkCard(280, 260);
        reasonCard.setLayout(new BoxLayout(reasonCard, BoxLayout.Y_AXIS));
        reasonCard.setAlignmentX(CENTER_ALIGNMENT);
        JLabel reasonTitle = styledLabel("CPU  REASONING", 10, MUTED, Font.PLAIN);
        reasonTitle.setAlignmentX(CENTER_ALIGNMENT);
        reasonArea = new JTextArea(10, 22);
        reasonArea.setEditable(false);
        reasonArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBackground(new Color(8, 12, 30));
        reasonArea.setForeground(new Color(180, 210, 255));
        reasonArea.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        reasonArea.setText("Select a cell to begin.\nWatch the CPU think…");
        JScrollPane rs = new JScrollPane(reasonArea);
        rs.setBorder(BorderFactory.createEmptyBorder());
        rs.setOpaque(false); rs.getViewport().setOpaque(false);
        reasonCard.add(Box.createVerticalStrut(8));
        reasonCard.add(reasonTitle);
        reasonCard.add(Box.createVerticalStrut(6));
        reasonCard.add(rs);
        reasonCard.add(Box.createVerticalStrut(8));

        // value picker
        valuePanel = buildValuePanel();
        valuePanel.setAlignmentX(CENTER_ALIGNMENT);
        valuePanel.setVisible(false);

        p.add(badge);
        p.add(Box.createVerticalStrut(10));
        p.add(heatToggle);
        p.add(Box.createVerticalStrut(10));
        p.add(backBtn);
        p.add(Box.createVerticalStrut(6));
        p.add(resetBtn);
        p.add(Box.createVerticalStrut(14));
        p.add(reasonCard);
        p.add(Box.createVerticalStrut(14));
        p.add(valuePanel);
        return p;
    }

    // ── Value picker ──────────────────────────────────────────────────────────
    private JPanel buildValuePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE2);
        p.setBorder(new CompoundBorder(
            new LineBorder(accentColor(), 2, true),
            new EmptyBorder(10, 10, 10, 10)
        ));
        p.setMaximumSize(new Dimension(280, 200));

        JLabel lbl = styledLabel("SELECT  VALUE", 11, accentColor(), Font.BOLD);
        lbl.setAlignmentX(CENTER_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(1, 4, 8, 0));
        grid.setOpaque(false);
        for (int i = 0; i < N; i++) {
            final int val = i+1;
            valBtn[i] = new JButton(String.valueOf(val)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color col = isEnabled() ? accentColor() : new Color(50,55,70);
                    g2.setColor(col);
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            valBtn[i].setFont(new Font("Serif", Font.BOLD, 26));
            valBtn[i].setForeground(Color.WHITE);
            valBtn[i].setContentAreaFilled(false);
            valBtn[i].setBorderPainted(false);
            valBtn[i].setFocusPainted(false);
            valBtn[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            valBtn[i].setPreferredSize(new Dimension(54, 54));
            valBtn[i].addActionListener(e -> handleValueClick(val));
            grid.add(valBtn[i]);
        }

        JButton cancel = fancyBtn("Cancel", new Color(160,40,60));
        cancel.setAlignmentX(CENTER_ALIGNMENT);
        cancel.setMaximumSize(new Dimension(220, 36));
        cancel.addActionListener(e -> { selRow=-1; selCol=-1; valuePanel.setVisible(false); updateDisplay(); });

        p.add(lbl);
        p.add(Box.createVerticalStrut(10));
        p.add(grid);
        p.add(Box.createVerticalStrut(8));
        p.add(cancel);
        return p;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bar.setBackground(new Color(10, 14, 28));
        bar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255,255,255,15)),
            BorderFactory.createEmptyBorder(12, 24, 14, 24)
        ));
        statusLbl = new JLabel("Your turn! Click an empty cell.", SwingConstants.CENTER);
        statusLbl.setFont(new Font("Monospaced", Font.BOLD, 14));
        statusLbl.setForeground(TEXT);
        bar.add(statusLbl);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME LOGIC
    // ════════════════════════════════════════════════════════════════════════
    private void initGame() {
        PuzzleGenerator gen = new PuzzleGenerator();
        PuzzleGenerator.PuzzleData pd = gen.generatePuzzle();
        gameState = new GameState(pd.topClues, pd.rightClues, pd.bottomClues, pd.leftClues);
        stratDP  = new StrategyDP(gameState);
        stratDnC = new StrategyDnC(gameState);
        stratBT  = new StrategyBTForwardCheck(gameState);
        stratBB  = new StrategyBranchBound(gameState);
        refreshAlgoBadge();
    }

    private void refreshAlgoBadge() {
        int idx = currentAlgo.ordinal();
        String[] names = {"Dynamic Programming","Divide & Conquer","Backtracking","Branch & Bound"};
        algoBadgeLbl.setText(names[idx]);
        algoBadgeLbl.setForeground(ACCENT[idx]);
    }

    private Color accentColor() {
        return ACCENT[currentAlgo.ordinal()];
    }

    // ── User clicks a cell ────────────────────────────────────────────────────
    private void handleCellClick(int r, int c) {
        if (!gameState.isHumanTurn() || gameState.isGameOver() || gameState.getGrid()[r][c] != 0) return;
        selRow = r; selCol = c;
        for (int i = 0; i < N; i++)
            valBtn[i].setEnabled(gameState.checkLegalMove(r, c, i+1));
        valuePanel.setBorder(new CompoundBorder(
            new LineBorder(accentColor(), 2, true),
            new EmptyBorder(10,10,10,10)
        ));
        valuePanel.setVisible(true);
        statusLbl.setText("Choose a value for cell (" + (r+1) + ", " + (c+1) + ")");
        updateDisplay();
    }

    private void handleValueClick(int val) {
        if (selRow == -1) return;

        if (gameState.checkForDeadlock(true)) {
            statusLbl.setText("No legal moves! −5 lives, skipping turn.");
            selRow=-1; selCol=-1;
            valuePanel.setVisible(false);
            gameState.setHumanTurn(false);
            updateDisplay();

            // ── FIX: explicit javax.swing.Timer (no anonymous double-brace) ──
            javax.swing.Timer skipTimer = new javax.swing.Timer(1500, e -> {
                if (!checkGameEnd()) { updateHeat(); animateHeat(0); }
            });
            skipTimer.setRepeats(false);
            skipTimer.start();
            return;
        }

        boolean ok = gameState.makeMove(selRow, selCol, val, true);
        selRow=-1; selCol=-1;
        valuePanel.setVisible(false);
        if (ok) gameState.setHumanTurn(false);
        clearHeat();
        updateDisplay();
        if (checkGameEnd()) return;

        if (ok) {
            // ── FIX: explicit javax.swing.Timer ──────────────────────────────
            javax.swing.Timer cpuMoveTimer = new javax.swing.Timer(500, e -> {
                updateHeat();
                animateHeat(0);
            });
            cpuMoveTimer.setRepeats(false);
            cpuMoveTimer.start();
        }
    }

    private void doCPUMove() {
        if (gameState.checkForDeadlock(false)) {
            statusLbl.setText("CPU has no legal moves! Skipping.");
            gameState.setHumanTurn(true);
            updateDisplay();
            return;
        }
        int[] move = switch (currentAlgo) {
            case DP  -> stratDP .findBestMove();
            case DNC -> stratDnC.findBestMove();
            case BT  -> stratBT .findBestMove();
            case BB  -> stratBB .findBestMove();
        };
        if (move == null) { gameState.setStatusMessage("CPU has no valid moves!"); updateDisplay(); return; }
        reasonArea.setText(gameState.getCpuReasoningExplanation());
        gameState.makeMove(move[0], move[1], move[2], false);
        clearHeat();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HEAT MAP
    // ════════════════════════════════════════════════════════════════════════
    private void updateHeat() {
        double max = 0;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (gameState.getGrid()[r][c] == 0) {
                heat[r][c] = switch (currentAlgo) {
                    case DP  -> stratDP .evaluateCell(r, c);
                    case DNC -> stratDnC.evaluateCell(r, c);
                    case BT  -> stratBT .evaluateCell(r, c);
                    case BB  -> stratBB .evaluateCell(r, c);
                };
                max = Math.max(max, heat[r][c]);
            } else heat[r][c] = 0;
        }
        if (max > 0) for (int r=0;r<N;r++) for(int c=0;c<N;c++) heat[r][c] /= max;
    }

    private void animateHeat(int idx) {
        if (idx >= N*N) {
            // ── FIX: explicit javax.swing.Timer ──────────────────────────────
            javax.swing.Timer postHeatTimer = new javax.swing.Timer(1100, e -> {
                if (!gameState.isGameOver()) {
                    doCPUMove();
                    clearHeat();
                    gameState.setHumanTurn(true);
                    updateDisplay();
                    checkGameEnd();
                }
            });
            postHeatTimer.setRepeats(false);
            postHeatTimer.start();
            return;
        }
        int r=idx/N, c=idx%N;
        if (gameState.getGrid()[r][c]==0 && showHeat) {
            cell[r][c].setBackground(heatColor(heat[r][c]));
            cell[r][c].repaint();
        }
        // ── FIX: explicit javax.swing.Timer ──────────────────────────────────
        javax.swing.Timer stepTimer = new javax.swing.Timer(60, e -> animateHeat(idx + 1));
        stepTimer.setRepeats(false);
        stepTimer.start();
    }

    private Color heatColor(double h) {
        if (h < 0.01) return SURFACE;
        Color lo, hi;
        switch (currentAlgo) {
            case DP  -> { lo=new Color(10,30,80);    hi=new Color(56,189,248); }
            case DNC -> { lo=new Color(40,10,80);    hi=new Color(167,139,250); }
            case BT  -> { lo=new Color(5,50,35);     hi=new Color(52,211,153); }
            default  -> { lo=new Color(80,30,5);     hi=new Color(251,146,60); }
        }
        return blend(lo, hi, Math.min(h,1.0));
    }

    private Color blend(Color a, Color b, double t) {
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    private void clearHeat() {
        for (int r=0;r<N;r++) for(int c=0;c<N;c++) {
            heat[r][c]=0;
            if (gameState.getGrid()[r][c]==0)
                cell[r][c].setBackground(SURFACE);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DISPLAY UPDATE
    // ════════════════════════════════════════════════════════════════════════
    private void updateDisplay() {
        int[][] grid = gameState.getGrid();
        for (int r=0;r<N;r++) for(int c=0;c<N;c++) {
            JButton b = cell[r][c];
            int val = grid[r][c];
            boolean sel = (r==selRow && c==selCol);

            if (val != 0) {
                b.setText("");
                b.setBackground(new Color(20, 65, 140));
                b.setEnabled(false);
            } else {
                b.setText("");
                b.setEnabled(gameState.isHumanTurn() && !gameState.isGameOver());
                b.setBackground(sel ? new Color(25, 75, 180)
                        : (showHeat && heat[r][c] > 0.01 ? heatColor(heat[r][c]) : SURFACE));
            }
            b.repaint();
        }

        humanScoreLbl.setText("YOU  ·  Score: " + gameState.getHumanScore());
        humanLivesLbl.setText("Lives: " + gameState.getHumanLives());
        cpuScoreLbl.setText("CPU  ·  Score: " + gameState.getCpuScore());
        cpuLivesLbl.setText("Lives: " + gameState.getCpuLives());

        String msg = gameState.getStatusMessage();
        if (!msg.isEmpty()) statusLbl.setText(msg);
        else if (gameState.isHumanTurn()) statusLbl.setText(selRow==-1 ? "✦ Your turn — click a cell" : "✦ Choose a value");
        else statusLbl.setText("⟳  CPU thinking  [ " + currentAlgo + " ] …");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME END & RESET
    // ════════════════════════════════════════════════════════════════════════
    private boolean checkGameEnd() {
        if (!gameState.isGameOver()) return false;
        String w = gameState.getWinner();
        if (w==null) w = "Game Over";
        statusLbl.setText(w);
        JOptionPane.showMessageDialog(this,
            "═══  GAME OVER  ═══\n\n" + w +
            "\n\nYOU  →  " + gameState.getHumanScore() + " pts  |  " + gameState.getHumanLives() + " lives" +
            "\nCPU  →  " + gameState.getCpuScore() + " pts  |  " + gameState.getCpuLives() + " lives",
            "Game Over", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    private void resetGame() {
        initGame();
        selRow=-1; selCol=-1;
        valuePanel.setVisible(false);
        reasonArea.setText("New game started.\nWatch the CPU think…");
        clearHeat(); updateHeat(); updateDisplay();
        statusLbl.setText("✦ New game — your turn.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  WIDGET HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private JLabel styledLabel(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", style, size));
        l.setForeground(color);
        return l;
    }

    private JPanel darkCard(int maxW, int maxH) {
        JPanel c = new JPanel();
        c.setBackground(SURFACE);
        c.setBorder(new CompoundBorder(
            new LineBorder(BORDER_DIM, 1),
            new EmptyBorder(10,12,10,12)
        ));
        c.setMaximumSize(new Dimension(maxW, maxH));
        return c;
    }

    private JButton fancyBtn(String text, Color col) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? col.brighter() : col);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(180, 40));
        return b;
    }

    private JPanel buildDivider(String label) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BORDER_DIM);
                g2.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Monospaced", Font.PLAIN, 10));
        l.setForeground(MUTED);
        l.setOpaque(true);
        l.setBackground(BG);
        l.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
        p.add(l);
        return p;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ANIMATED BACKGROUND
    // ════════════════════════════════════════════════════════════════════════
    static class AnimatedBG extends JPanel {
        private float t = 0;
        private final javax.swing.Timer anim;   // ── FIX: explicit javax.swing.Timer
        private final float[] dotX, dotY, dotA, dotS;
        private static final int DOTS = 110;

        AnimatedBG() {
            setBackground(BG);
            dotX = new float[DOTS]; dotY = new float[DOTS];
            dotA = new float[DOTS]; dotS = new float[DOTS];
            Random rnd = new Random();
            for (int i=0;i<DOTS;i++) {
                dotX[i]=rnd.nextFloat(); dotY[i]=rnd.nextFloat();
                dotA[i]=rnd.nextFloat(); dotS[i]=(rnd.nextFloat()-.5f)*.003f;
            }
            // ── FIX: explicit javax.swing.Timer (no double-brace init) ────────
            anim = new javax.swing.Timer(28, e -> {
                t += .007f;
                for (int i = 0; i < DOTS; i++) {
                    dotA[i] += dotS[i];
                    if (dotA[i] < 0) { dotA[i] = 0; dotS[i] *= -1; }
                    if (dotA[i] > 1) { dotA[i] = 1; dotS[i] *= -1; }
                }
                repaint();
            });
            anim.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g2.setColor(BG); g2.fillRect(0,0,w,h);

            // blobs
            Color[] bc = {new Color(18,55,150,80),new Color(70,15,160,70),new Color(8,110,190,65)};
            int[]   br = {270,210,190};
            float[] bxv = {(float)(w*(.28+Math.sin(t)*.10)), (float)(w*(.72+Math.cos(t*.8)*.09)), (float)(w*(.50+Math.sin(t*1.2)*.08))};
            float[] byv = {(float)(h*(.32+Math.cos(t)*.09)), (float)(h*(.62+Math.sin(t*.9)*.11)), (float)(h*(.18+Math.cos(t*1.1)*.07))};

            for (int i=0;i<3;i++) {
                RadialGradientPaint rg = new RadialGradientPaint(bxv[i],byv[i],br[i],new float[]{0f,1f},new Color[]{bc[i],new Color(0,0,0,0)});
                g2.setPaint(rg); g2.fillRect(0,0,w,h);
            }
            // grid
            g2.setColor(new Color(255,255,255,12)); g2.setStroke(new BasicStroke(1));
            for(int x=0;x<w;x+=55){g2.drawLine(x,0,x,h);} for(int y=0;y<h;y+=55){g2.drawLine(0,y,w,y);}
            // stars
            for(int i=0;i<DOTS;i++){
                float a=dotA[i]*.5f;
                g2.setColor(new Color(200,215,255,(int)(a*255)));
                float px=dotX[i]*w, py=dotY[i]*h;
                g2.fillOval((int)px-1,(int)py-1,2,2);
            }
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ALGO CARD WIDGET
    // ════════════════════════════════════════════════════════════════════════
    static class AlgoCard extends JPanel {
        private boolean selected = false;
        private final Color accent;
        private float hover = 0f;
        private final javax.swing.Timer hoverT;  // ── FIX: explicit javax.swing.Timer
        private final java.util.List<ActionListener> listeners = new java.util.ArrayList<>();

        AlgoCard(String tag, String code, String name, String desc, String p1, String p2, Color accent) {
            this.accent = accent;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setPreferredSize(new Dimension(300, 170));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // ── FIX: explicit javax.swing.Timer (no double-brace init) ────────
            hoverT = new javax.swing.Timer(16, e -> repaint());

            // top row
            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);

            JPanel iconBox = new JPanel(new GridBagLayout());
            iconBox.setOpaque(false);
            iconBox.setPreferredSize(new Dimension(46, 46));
            iconBox.setBorder(new LineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),80), 1, true));
            JLabel iconLbl = new JLabel(tag, SwingConstants.CENTER);
            iconLbl.setFont(new Font("Monospaced", Font.BOLD, 13));
            iconLbl.setForeground(accent);
            iconBox.add(iconLbl);

            JPanel meta = new JPanel();
            meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
            meta.setOpaque(false);
            JLabel codeLbl = new JLabel("Algorithm " + code);
            codeLbl.setFont(new Font("Monospaced", Font.PLAIN, 9));
            codeLbl.setForeground(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),160));
            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(new Font("Serif", Font.BOLD, 15));
            nameLbl.setForeground(Color.WHITE);
            meta.add(codeLbl);
            meta.add(Box.createVerticalStrut(2));
            meta.add(nameLbl);

            // check circle
            JLabel check = new JLabel("✓") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(selected ? accent : new Color(60,70,100));
                    g2.fillOval(0,0,getWidth()-1,getHeight()-1);
                    if (selected) { g2.setColor(Color.WHITE); g2.setFont(new Font("Monospaced",Font.BOLD,10)); FontMetrics fm=g2.getFontMetrics(); g2.drawString("✓",(getWidth()-fm.stringWidth("✓"))/2,(getHeight()+fm.getAscent())/2-2); }
                    g2.dispose();
                }
            };
            check.setPreferredSize(new Dimension(20,20));

            top.add(iconBox, BorderLayout.WEST);
            top.add(meta, BorderLayout.CENTER);
            top.add(check, BorderLayout.EAST);

            JTextArea descA = new JTextArea(desc);
            descA.setEditable(false); descA.setOpaque(false);
            descA.setFont(new Font("Monospaced", Font.PLAIN, 11));
            descA.setForeground(new Color(200, 215, 235));
            descA.setLineWrap(true); descA.setWrapStyleWord(true);
            descA.setMaximumSize(new Dimension(260, 50));

            // pills
            JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            pills.setOpaque(false);
            pills.add(pill(p1, accent)); pills.add(pill(p2, accent));

            add(Box.createVerticalStrut(12));
            add(top);
            add(Box.createVerticalStrut(8));
            add(descA);
            add(Box.createVerticalStrut(6));
            add(pills);
            add(Box.createVerticalStrut(10));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover=1f; hoverT.start(); }
                public void mouseExited (MouseEvent e) { hover=0f; hoverT.start(); }
                public void mouseClicked(MouseEvent e) { selected=true; repaint(); listeners.forEach(l->l.actionPerformed(null)); }
            });
            setBorder(BorderFactory.createEmptyBorder(0,14,0,14));
        }

        private JLabel pill(String text, Color ac) {
            JLabel l = new JLabel("· " + text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),30));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                    g2.dispose(); super.paintComponent(g);
                }
            };
            l.setFont(new Font("Monospaced",Font.PLAIN,10));
            l.setForeground(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),210));
            l.setBorder(new CompoundBorder(new LineBorder(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),60),1,true),new EmptyBorder(2,8,2,8)));
            return l;
        }

        void addActionListener(ActionListener l) { listeners.add(l); }
        void setSelected(boolean s) { selected=s; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(1,1,w-2,h-2,14,14);
            Color fill = selected ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),45)
                       : new Color(13,18,32, 200);
            g2.setColor(fill); g2.fill(rr);
            float bw = selected ? 2.5f : (hover>.1f ? 1.8f : 1.2f);
            int alpha = selected ? 255 : (int)(70+120*hover);
            g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),alpha));
            g2.setStroke(new BasicStroke(bw));
            g2.draw(rr);
            if (selected) {
                g2.setColor(accent);
                g2.fillRoundRect(1,16,3,h-32,3,3);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowersGameGUI().setVisible(true));
    }
}
