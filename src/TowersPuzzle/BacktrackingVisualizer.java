import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 *  BacktrackingVisualizer – Advanced Swing Visualizer
 *  Algorithms: Backtracking · MRV · LCV · Forward Checking
 *
 *  Compile:  javac *.java
 *  Run:      java BacktrackingVisualizer
 * ═══════════════════════════════════════════════════════════════
 */
public class BacktrackingVisualizer extends JFrame implements SolverEngine.SolverCallback {

    // ── Colour palette ─────────────────────────────────────────────────────
    static final Color BG_DARK      = new Color(10,  14,  26);
    static final Color BG_PANEL     = new Color(16,  22,  40);
    static final Color BG_CARD      = new Color(22,  32,  58);
    static final Color ACCENT_BLUE  = new Color(64, 156, 255);
    static final Color ACCENT_CYAN  = new Color(0,  220, 200);
    static final Color ACCENT_GREEN = new Color(50, 220, 100);
    static final Color ACCENT_RED   = new Color(255, 70,  90);
    static final Color ACCENT_AMBER = new Color(255, 190,  50);
    static final Color ACCENT_PURPLE= new Color(180, 100, 255);
    static final Color TEXT_PRIMARY = new Color(230, 235, 255);
    static final Color TEXT_DIM     = new Color(100, 120, 160);
    static final Color GRID_LINE    = new Color(40,  60, 100);

    // ── Cell state colours ─────────────────────────────────────────────────
    static final Color C_IDLE      = BG_CARD;
    static final Color C_SELECT    = new Color(30, 80, 160);
    static final Color C_TRY       = new Color(120, 80, 0);
    static final Color C_PRUNE     = new Color(120, 20, 30);
    static final Color C_BACKTRACK = new Color(40, 40, 55);
    static final Color C_SOLUTION  = new Color(10, 80, 40);

    // ── Single fixed puzzle (taken from the reference image) ──────────────
    //   Top    (↓): 1  3  2  2
    //   Bottom (↑): 3  1  2  2
    //   Left   (→): 1  3  2  2
    //   Right  (←): 3  2  1  2
    private static final int[] PUZZLE_TOP    = {1, 3, 2, 2};
    private static final int[] PUZZLE_BOTTOM = {3, 1, 2, 2};
    private static final int[] PUZZLE_LEFT   = {1, 3, 2, 2};
    private static final int[] PUZZLE_RIGHT  = {3, 2, 1, 2};

    // ── Panels ────────────────────────────────────────────────────────────
    private final GridPanel    gridPanel;
    private final LogPanel     logPanel;
    private final StatsPanel   statsPanel;
    private final TreePanel    treePanel;
    private final ControlPanel controls;

    // ── Solver state ──────────────────────────────────────────────────────
    private final StatsTracker stats = new StatsTracker();
    private SolverEngine solver;
    private Thread solverThread;

    // ── Current visual state ──────────────────────────────────────────────
    private volatile int[][] currentGrid = new int[SolverEngine.N][SolverEngine.N];
    private volatile int     hlRow = -1, hlCol = -1;
    private volatile EventType currentEvent = null;
    private volatile String    currentMsg   = "";

    // ── Constructor ───────────────────────────────────────────────────────

    public BacktrackingVisualizer() {
        super("Towers Puzzle  ·  Backtracking Visualizer  ·  MRV + LCV");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(BG_DARK);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        gridPanel  = new GridPanel();
        controls   = new ControlPanel();
        JPanel left = vstack(gridPanel, controls);

        statsPanel = new StatsPanel();
        logPanel   = new LogPanel();
        JPanel mid  = vstack(statsPanel, logPanel);
        mid.setPreferredSize(new Dimension(310, 0));

        treePanel  = new TreePanel();
        treePanel.setPreferredSize(new Dimension(270, 0));

        root.add(left,      BorderLayout.CENTER);
        root.add(mid,       BorderLayout.EAST);
        root.add(treePanel, BorderLayout.WEST);

        // Pre-load clues so the grid shows the puzzle on startup
        gridPanel.setClues(PUZZLE_TOP, PUZZLE_BOTTOM, PUZZLE_LEFT, PUZZLE_RIGHT);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── SolverCallback ────────────────────────────────────────────────────

    @Override
    public void onEvent(EventType type, int row, int col, int value,
                        int[][] snap, StatsTracker st, String message) {
        currentGrid  = snap;
        hlRow        = row;
        hlCol        = col;
        currentEvent = type;
        currentMsg   = message;

        SwingUtilities.invokeLater(() -> {
            gridPanel.update(snap, row, col, type);
            statsPanel.update(st);
            logPanel.addEntry(type, message, st.getCurrentDepth());
            treePanel.addNode(type, row, col, value, st.getCurrentDepth());
        });
    }

    // ── Solver control ────────────────────────────────────────────────────

    private void startSolver() {
        if (solverThread != null && solverThread.isAlive()) {
            solver.running = false;
            solverThread.interrupt();
        }
        treePanel.reset();
        logPanel.clear();

        solver = new SolverEngine(stats, this,
                    PUZZLE_TOP.clone(), PUZZLE_BOTTOM.clone(),
                    PUZZLE_LEFT.clone(), PUZZLE_RIGHT.clone());
        solver.stepDelay    = controls.getDelay();
        solver.maxSolutions = 1;   // stop after the first solution

        gridPanel.setClues(PUZZLE_TOP, PUZZLE_BOTTOM, PUZZLE_LEFT, PUZZLE_RIGHT);
        gridPanel.reset();

        solverThread = new Thread(solver, "Solver");
        solverThread.setDaemon(true);
        solverThread.start();
    }

    private void pauseResume() {
        if (solver != null) solver.paused = !solver.paused;
    }

    private void stopSolver() {
        if (solver != null) solver.running = false;
        if (solverThread != null) solverThread.interrupt();
    }

    // ── Layout helpers ────────────────────────────────────────────────────

    private static JPanel vstack(JComponent a, JComponent b) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(BG_DARK);
        p.add(a, BorderLayout.CENTER);
        p.add(b, BorderLayout.SOUTH);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GridPanel – animated 4×4 board
    // ══════════════════════════════════════════════════════════════════════

    class GridPanel extends JPanel {
        private static final int CELL = 90;
        private static final int GAP  = 4;
        private static final int CLUE = 36;
        private static final int PAD  = 20;

        private int[]   top, bot, left, right;
        private int[][] grid = new int[SolverEngine.N][SolverEngine.N];

        private Color[][]  cellBg     = new Color[SolverEngine.N][SolverEngine.N];
        private float[][]  cellAlpha  = new float[SolverEngine.N][SolverEngine.N];
        private Color[][]  cellTarget = new Color[SolverEngine.N][SolverEngine.N];
        private int        hlR = -1, hlC = -1;
        private EventType  evt = null;

        private javax.swing.Timer animTimer;
        private float pulsePhase = 0;

        GridPanel() {
            setBackground(BG_DARK);
            setPreferredSize(new Dimension(
                PAD*2 + CLUE + SolverEngine.N*(CELL+GAP) + GAP + CLUE,
                PAD*2 + CLUE + SolverEngine.N*(CELL+GAP) + GAP + CLUE + 20
            ));
            for (int r = 0; r < SolverEngine.N; r++)
                for (int c = 0; c < SolverEngine.N; c++) {
                    cellBg[r][c]     = C_IDLE;
                    cellAlpha[r][c]  = 1f;
                    cellTarget[r][c] = C_IDLE;
                }
            top = left = bot = right = new int[SolverEngine.N];

            animTimer = new javax.swing.Timer(16, e -> {
                pulsePhase += 0.08f;
                boolean changed = false;
                for (int r = 0; r < SolverEngine.N; r++)
                    for (int cc = 0; cc < SolverEngine.N; cc++) {
                        Color cur = cellBg[r][cc];
                        Color tar = cellTarget[r][cc];
                        if (!cur.equals(tar)) {
                            cellBg[r][cc] = lerpColor(cur, tar, 0.12f);
                            changed = true;
                        }
                    }
                if (changed || true) repaint();
            });
            animTimer.start();
        }

        void setClues(int[] t, int[] b, int[] l, int[] r) {
            top = t; bot = b; left = l; right = r;
        }

        void reset() {
            grid = new int[SolverEngine.N][SolverEngine.N];
            hlR = -1; hlC = -1; evt = null;
            for (int r = 0; r < SolverEngine.N; r++)
                for (int c = 0; c < SolverEngine.N; c++)
                    cellTarget[r][c] = C_IDLE;
            repaint();
        }

        void update(int[][] newGrid, int row, int col, EventType type) {
            grid = newGrid;
            hlR  = row; hlC = col; evt = type;
            for (int r = 0; r < SolverEngine.N; r++)
                for (int c = 0; c < SolverEngine.N; c++) {
                    Color target;
                    if (r == row && c == col) {
                        target = switch(type) {
                            case SELECT    -> C_SELECT;
                            case TRY       -> C_TRY;
                            case PRUNE     -> C_PRUNE;
                            case BACKTRACK -> C_BACKTRACK;
                            case SOLUTION  -> C_SOLUTION;
                            default        -> C_IDLE;
                        };
                    } else {
                        if (type == EventType.SOLUTION && newGrid[r][c] != 0) {
                            target = C_SOLUTION;
                        } else if (newGrid[r][c] != 0) {
                            target = new Color(28, 40, 72);
                        } else {
                            target = C_IDLE;
                        }
                    }
                    cellTarget[r][c] = target;
                }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int N  = SolverEngine.N;
            int ox = PAD + CLUE;
            int oy = PAD + CLUE;

            GradientPaint gp = new GradientPaint(0, 0, new Color(10,14,28),
                                                 getWidth(), getHeight(), new Color(16,20,42));
            g.setPaint(gp);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(ACCENT_CYAN);
            g.drawString("4 × 4  SKYSCRAPER  PUZZLE", ox, PAD - 4);

            drawClues(g, ox, oy, N);

            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++)
                    drawCell(g, ox + c*(CELL+GAP), oy + r*(CELL+GAP), r, c);

            g.setColor(GRID_LINE);
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i <= N; i++)
                g.drawLine(ox - 1,               oy + i*(CELL+GAP) - GAP/2,
                           ox + N*(CELL+GAP) - GAP - 1, oy + i*(CELL+GAP) - GAP/2);

            if (evt != null && hlR >= 0) {
                float alpha = 0.7f + 0.3f * (float)Math.sin(pulsePhase);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setFont(new Font("Monospaced", Font.BOLD, 11));
                g.setColor(eventColor(evt));
                drawCenteredString(g, eventLabel(evt),
                    ox + hlC*(CELL+GAP) + CELL/2,
                    oy + hlR*(CELL+GAP) - 4);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }

        private void drawCell(Graphics2D g, int cx, int cy, int r, int c) {
            Color bg = cellBg[r][c];

            if (r == hlR && c == hlC && evt != null) {
                Color glowC = eventColor(evt);
                float glowA = 0.15f + 0.12f * (float)Math.sin(pulsePhase);
                for (int k = 8; k >= 1; k--) {
                    g.setColor(new Color(glowC.getRed(), glowC.getGreen(), glowC.getBlue(),
                                         (int)(glowA * 255 / k)));
                    g.fillRoundRect(cx - k*3, cy - k*3, CELL + k*6, CELL + k*6, 18, 18);
                }
            }

            g.setColor(bg);
            g.fillRoundRect(cx, cy, CELL, CELL, 12, 12);

            Color borderC = (r == hlR && c == hlC && evt != null) ? eventColor(evt) : GRID_LINE;
            float borderW = (r == hlR && c == hlC) ? 2.5f : 1f;
            g.setColor(borderC);
            g.setStroke(new BasicStroke(borderW));
            g.drawRoundRect(cx, cy, CELL, CELL, 12, 12);
            g.setStroke(new BasicStroke(1f));

            int val = grid[r][c];
            if (val != 0) {
                drawBuilding(g, cx, cy, val,
                    r == hlR && c == hlC ? eventColor(evt) : TEXT_PRIMARY);
                g.setFont(new Font("Monospaced", Font.BOLD, 30));
                g.setColor(r == hlR && c == hlC ? eventColor(evt) : TEXT_PRIMARY);
                drawCenteredString(g, String.valueOf(val), cx + CELL/2, cy + CELL/2 + 10);
            } else {
                g.setColor(TEXT_DIM);
                g.setFont(new Font("Monospaced", Font.PLAIN, 14));
                drawCenteredString(g, "·", cx + CELL/2, cy + CELL/2 + 5);
            }
        }

        private void drawBuilding(Graphics2D g, int cx, int cy, int h, Color c) {
            int bw = 10, maxH = 20, gap = 4;
            int totalW = SolverEngine.N * (bw + gap) - gap;
            int startX = cx + (CELL - totalW) / 2;
            int baseY  = cy + CELL - 10;
            for (int i = 0; i < SolverEngine.N; i++) {
                int bh = (int)(maxH * (i + 1.0) / SolverEngine.N);
                int by = baseY - bh;
                Color fill = (i < h)
                    ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 80)
                    : new Color(30, 40, 60, 100);
                g.setColor(fill);
                g.fillRect(startX + i*(bw+gap), by, bw, bh);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
                g.drawRect(startX + i*(bw+gap), by, bw, bh);
            }
        }

        private void drawClues(Graphics2D g, int ox, int oy, int N) {
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            for (int i = 0; i < N; i++) {
                int cc = ox + i*(CELL+GAP) + CELL/2;
                int rr = oy + i*(CELL+GAP) + CELL/2 + 6;

                g.setColor(top[i]   > 0 ? ACCENT_CYAN : TEXT_DIM);
                drawCenteredString(g, top[i]   > 0 ? String.valueOf(top[i])   : "·", cc, oy - 12);

                g.setColor(bot[i]   > 0 ? ACCENT_CYAN : TEXT_DIM);
                drawCenteredString(g, bot[i]   > 0 ? String.valueOf(bot[i])   : "·", cc,
                                   oy + N*(CELL+GAP) + 24);

                g.setColor(left[i]  > 0 ? ACCENT_CYAN : TEXT_DIM);
                drawCenteredString(g, left[i]  > 0 ? String.valueOf(left[i])  : "·", ox - 20, rr);

                g.setColor(right[i] > 0 ? ACCENT_CYAN : TEXT_DIM);
                drawCenteredString(g, right[i] > 0 ? String.valueOf(right[i]) : "·",
                                   ox + N*(CELL+GAP) + 8, rr);
            }
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(60, 90, 130));
            g.drawString("↓", ox + N*(CELL+GAP)/2, oy - 28);
            g.drawString("↑", ox + N*(CELL+GAP)/2, oy + N*(CELL+GAP) + 38);
            g.drawString("→", ox - 36,              oy + N*(CELL+GAP)/2);
            g.drawString("←", ox + N*(CELL+GAP) + 14, oy + N*(CELL+GAP)/2);
        }

        private void drawCenteredString(Graphics2D g, String s, int cx, int cy) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, cx - fm.stringWidth(s)/2, cy);
        }

        private String eventLabel(EventType e) {
            return switch(e) {
                case SELECT    -> "MRV SELECT";
                case TRY       -> "TRYING";
                case PRUNE     -> "PRUNED";
                case BACKTRACK -> "BACKTRACK";
                case SOLUTION  -> "SOLUTION ✦";
                default        -> "";
            };
        }

        private Color eventColor(EventType e) {
            return switch(e) {
                case SELECT    -> ACCENT_BLUE;
                case TRY       -> ACCENT_AMBER;
                case PRUNE     -> ACCENT_RED;
                case BACKTRACK -> new Color(120, 130, 160);
                case SOLUTION  -> ACCENT_GREEN;
                default        -> TEXT_PRIMARY;
            };
        }

        private Color lerpColor(Color a, Color b, float t) {
            int r  = (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t);
            int g  = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bv = (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
            return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, bv))
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  StatsPanel
    // ══════════════════════════════════════════════════════════════════════

    class StatsPanel extends JPanel {
        private long   nodes, pruned, backtracks, solutions, assignments;
        private int    curDepth, maxDepth;
        private double branchFactor;
        private long   elapsed;

        StatsPanel() {
            setBackground(BG_PANEL);
            setBorder(new CompoundBorder(
                new LineBorder(GRID_LINE, 1, true),
                new EmptyBorder(14, 14, 14, 14)
            ));
            setPreferredSize(new Dimension(310, 300));
        }

        void update(StatsTracker st) {
            nodes        = st.getNodesExplored();
            pruned       = st.getBranchesPruned();
            backtracks   = st.getBacktrackCount();
            solutions    = st.getSolutionsFound();
            assignments  = st.getTotalAssignments();
            curDepth     = st.getCurrentDepth();
            maxDepth     = st.getMaxDepth();
            branchFactor = st.estimatedBranchingFactor();
            elapsed      = st.elapsedMs();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int y = 24;

            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(ACCENT_CYAN);
            g.drawString("LIVE  STATISTICS", 10, y); y += 20;

            g.setColor(GRID_LINE);
            g.drawLine(10, y, getWidth()-10, y); y += 16;

            Object[][] rows = {
                {"Nodes Explored",  nodes,       ACCENT_BLUE},
                {"Branches Pruned", pruned,      ACCENT_RED},
                {"Backtracks",      backtracks,  ACCENT_AMBER},
                {"Assignments",     assignments, TEXT_DIM},
                {"Solutions Found", solutions,   ACCENT_GREEN},
                {"Current Depth",   curDepth,    ACCENT_PURPLE},
                {"Max Depth",       maxDepth,    ACCENT_PURPLE},
            };
            for (Object[] row : rows) {
                drawStatRow(g, (String)row[0], row[1].toString(), (Color)row[2], y);
                y += 26;
            }

            y += 4;
            g.setColor(GRID_LINE);
            g.drawLine(10, y, getWidth()-10, y); y += 14;

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(TEXT_DIM);
            g.drawString("Est. Branching Factor", 10, y);
            g.setColor(ACCENT_AMBER);
            g.drawString(String.format("%.2f", branchFactor), 200, y);
            y += 17;

            double complexity = Math.pow(Math.max(branchFactor, 1), Math.max(maxDepth, 1));
            g.setColor(TEXT_DIM);
            g.drawString("O(b^d) estimate", 10, y);
            g.setColor(ACCENT_AMBER);
            g.drawString(String.format("≈ %.0f", complexity), 200, y);
            y += 17;

            float pct = Math.min(1f, (float)nodes / 10_000f);
            drawProgressBar(g, 10, y, getWidth()-20, 10, pct, ACCENT_BLUE, "Nodes/10k");
            y += 20;

            g.setColor(TEXT_DIM);
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.drawString(String.format("vs brute-force 4^16 = 4.3B  |  elapsed: %dms", elapsed), 10, y);
        }

        private void drawStatRow(Graphics2D g, String label, String val, Color valColor, int y) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(TEXT_DIM);
            g.drawString(label, 10, y);
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(valColor);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(val, getWidth() - 10 - fm.stringWidth(val), y);
        }

        private void drawProgressBar(Graphics2D g, int x, int y, int w, int h,
                                     float pct, Color c, String lbl) {
            g.setColor(new Color(30, 40, 60));
            g.fillRoundRect(x, y, w, h, 4, 4);
            g.setColor(c);
            g.fillRoundRect(x, y, (int)(w*pct), h, 4, 4);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.drawRoundRect(x, y, w, h, 4, 4);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LogPanel – scrollable event log
    // ══════════════════════════════════════════════════════════════════════

    class LogPanel extends JPanel {
        private final List<LogEntry> entries = new ArrayList<>();
        private final JScrollPane    scroll;
        private final JPanel         inner;

        LogPanel() {
            setLayout(new BorderLayout());
            setBackground(BG_PANEL);

            inner = new JPanel();
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setBackground(new Color(12, 18, 32));

            scroll = new JScrollPane(inner);
            scroll.setBackground(new Color(12, 18, 32));
            scroll.getViewport().setBackground(new Color(12, 18, 32));
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setBackground(BG_PANEL);

            JLabel title = new JLabel("  ALGORITHM LOG");
            title.setFont(new Font("Monospaced", Font.BOLD, 12));
            title.setForeground(ACCENT_CYAN);
            title.setBackground(BG_PANEL);
            title.setOpaque(true);
            title.setBorder(new EmptyBorder(8, 6, 8, 6));

            add(title,  BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            setPreferredSize(new Dimension(310, 350));
        }

        void addEntry(EventType type, String msg, int depth) {
            if (entries.size() > 500) {
                entries.remove(0);
                if (inner.getComponentCount() > 0) inner.remove(0);
            }
            Color c = switch(type) {
                case SELECT    -> ACCENT_BLUE;
                case TRY       -> ACCENT_AMBER;
                case PRUNE     -> ACCENT_RED;
                case BACKTRACK -> new Color(120, 130, 160);
                case SOLUTION  -> ACCENT_GREEN;
                case COMPLETE  -> ACCENT_CYAN;
                default        -> TEXT_DIM;
            };
            String icon = switch(type) {
                case SELECT    -> "◈";
                case TRY       -> "▶";
                case PRUNE     -> "✕";
                case BACKTRACK -> "↩";
                case SOLUTION  -> "★";
                default        -> "·";
            };
            String indent = "  ".repeat(Math.min(depth, 6));
            JLabel lbl = new JLabel(String.format(
                "<html><font color='%s'>%s%s</font> <font color='%s'>%s</font></html>",
                toHex(TEXT_DIM), indent, icon, toHex(c), truncate(msg, 38)));
            lbl.setFont(new Font("Monospaced", Font.PLAIN, 10));
            lbl.setBackground(new Color(12, 18, 32));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(1, 4, 1, 4));
            inner.add(lbl);
            inner.revalidate();
            SwingUtilities.invokeLater(() -> {
                JScrollBar bar = scroll.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            });
        }

        void clear() {
            entries.clear();
            inner.removeAll();
            inner.revalidate();
            inner.repaint();
        }

        private String toHex(Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
        private String truncate(String s, int max) {
            return s.length() <= max ? s : s.substring(0, max-1) + "…";
        }
        record LogEntry(EventType type, String msg, int depth) {}
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TreePanel – live recursion tree
    // ══════════════════════════════════════════════════════════════════════

    class TreePanel extends JPanel {
        private final List<TreeNode> nodes = new ArrayList<>();
        private final JScrollPane    scroll;
        private final TreeCanvas     canvas;

        TreePanel() {
            setBackground(BG_PANEL);
            setLayout(new BorderLayout());

            JLabel title = new JLabel("  RECURSION TREE");
            title.setFont(new Font("Monospaced", Font.BOLD, 12));
            title.setForeground(ACCENT_CYAN);
            title.setBackground(BG_PANEL);
            title.setOpaque(true);
            title.setBorder(new EmptyBorder(8, 6, 8, 6));

            canvas = new TreeCanvas(nodes);
            canvas.setBackground(new Color(10, 14, 26));

            scroll = new JScrollPane(canvas);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(new Color(10, 14, 26));
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            add(title,  BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
        }

        void addNode(EventType type, int r, int c, int v, int depth) {
            if (nodes.size() > 300) nodes.remove(0);
            nodes.add(new TreeNode(type, r, c, v, depth));
            canvas.setPreferredSize(new Dimension(250, Math.max(400, nodes.size()*18 + 30)));
            canvas.revalidate();
            canvas.repaint();
            JScrollBar bar = scroll.getVerticalScrollBar();
            SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
        }

        void reset() { nodes.clear(); canvas.repaint(); }

        record TreeNode(EventType type, int r, int c, int v, int depth) {}

        class TreeCanvas extends JPanel {
            private final List<TreeNode> ns;
            TreeCanvas(List<TreeNode> ns) { this.ns = ns; }

            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int y = 20;
                for (int i = 0; i < ns.size(); i++) {
                    TreeNode n = ns.get(i);
                    int x = 10 + n.depth() * 14;

                    if (i > 0 && n.depth() > 0) {
                        g.setColor(new Color(40, 55, 80));
                        g.setStroke(new BasicStroke(1f));
                        g.drawLine(x - 10, y - 9, x - 10, y - 5);
                        g.drawLine(x - 10, y - 5, x,      y - 5);
                    }

                    Color c = switch(n.type()) {
                        case SELECT    -> ACCENT_BLUE;
                        case TRY       -> ACCENT_AMBER;
                        case PRUNE     -> ACCENT_RED;
                        case BACKTRACK -> new Color(100, 110, 140);
                        case SOLUTION  -> ACCENT_GREEN;
                        default        -> TEXT_DIM;
                    };
                    String icon = switch(n.type()) {
                        case SELECT    -> "◈";
                        case TRY       -> "▷";
                        case PRUNE     -> "✕";
                        case BACKTRACK -> "↩";
                        case SOLUTION  -> "★";
                        default        -> "·";
                    };

                    g.setColor(c);
                    g.fillOval(x-3, y-8, 7, 7);
                    g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                    g.setColor(c);
                    String label = n.v() > 0
                        ? String.format("%s (%d,%d)=%d", icon, n.r(), n.c(), n.v())
                        : String.format("%s (%d,%d)",    icon, n.r(), n.c());
                    g.drawString(label, x + 6, y);
                    y += 17;
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ControlPanel – speed + START / PAUSE / STOP  (no puzzle/solution pickers)
    // ══════════════════════════════════════════════════════════════════════

    class ControlPanel extends JPanel {
        private final JSlider speedSlider;
        private final JButton btnStart, btnPause, btnStop;
        private final JLabel  statusLabel;

        ControlPanel() {
            setBackground(BG_PANEL);
            setBorder(new CompoundBorder(
                new LineBorder(GRID_LINE, 1, true),
                new EmptyBorder(10, 10, 10, 10)
            ));
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets  = new Insets(4, 4, 4, 4);
            gbc.fill    = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            // ── Row 0: Speed ──────────────────────────────────────────────
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
            add(label("SPEED"), gbc);
            gbc.gridx = 1; gbc.gridwidth = 2;
            speedSlider = new JSlider(0, 1000, 700);
            speedSlider.setBackground(BG_PANEL);
            speedSlider.setForeground(ACCENT_CYAN);
            add(speedSlider, gbc);

            // ── Row 1: Buttons ────────────────────────────────────────────
            gbc.gridy = 1; gbc.gridwidth = 1;
            btnStart = button("▶  START",  ACCENT_GREEN);
            btnPause = button("⏸  PAUSE",  ACCENT_AMBER);
            btnStop  = button("■  STOP",   ACCENT_RED);
            gbc.gridx = 0; add(btnStart, gbc);
            gbc.gridx = 1; add(btnPause, gbc);
            gbc.gridx = 2; add(btnStop,  gbc);

            // ── Row 2: Status ─────────────────────────────────────────────
            gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 3;
            statusLabel = new JLabel("Ready  ·  Press START to solve");
            statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            statusLabel.setForeground(TEXT_DIM);
            add(statusLabel, gbc);

            // ── Row 3: Legend ─────────────────────────────────────────────
            gbc.gridy = 3;
            add(legendPanel(), gbc);

            // ── Actions ───────────────────────────────────────────────────
            btnStart.addActionListener(e -> {
                startSolver();
                statusLabel.setText("Solving…");
            });
            btnPause.addActionListener(e -> {
                pauseResume();
                if (solver != null)
                    statusLabel.setText(solver.paused ? "PAUSED" : "Resumed…");
            });
            btnStop.addActionListener(e -> {
                stopSolver();
                statusLabel.setText("Stopped.");
            });
        }

        /** Slider 0..1000  →  delay 800ms..10ms (inverse) */
        int getDelay() {
            return (int)(800 - speedSlider.getValue() * 0.79f);
        }

        private JLabel label(String t) {
            JLabel l = new JLabel(t);
            l.setFont(new Font("Monospaced", Font.BOLD, 10));
            l.setForeground(TEXT_DIM);
            return l;
        }

        private JButton button(String text, Color c) {
            JButton b = new JButton(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = getModel().isPressed()  ? c.darker()
                             : getModel().isRollover() ? c.brighter()
                             : new Color(c.getRed()/3, c.getGreen()/3, c.getBlue()/3);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.setFont(getFont());
                    g2.setColor(c);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                }
            };
            b.setFont(new Font("Monospaced", Font.BOLD, 11));
            b.setPreferredSize(new Dimension(110, 30));
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        private JComboBox<String> combo(String[] items) {
            JComboBox<String> cb = new JComboBox<>(items);
            cb.setBackground(BG_CARD);
            cb.setForeground(TEXT_PRIMARY);
            cb.setFont(new Font("Monospaced", Font.PLAIN, 11));
            return cb;
        }

        private JPanel legendPanel() {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            p.setBackground(BG_PANEL);
            Object[][] items = {
                {"MRV Select", ACCENT_BLUE},
                {"Try Value",  ACCENT_AMBER},
                {"Pruned",     ACCENT_RED},
                {"Backtrack",  new Color(120, 130, 160)},
                {"Solution",   ACCENT_GREEN},
            };
            for (Object[] item : items) {
                JLabel l = new JLabel("■ " + item[0]) {
                    @Override protected void paintComponent(Graphics g) {
                        g.setColor((Color) item[1]);
                        g.fillRect(0, getHeight()/2 - 4, 8, 8);
                        g.setFont(getFont());
                        g.drawString((String) item[0], 12, getHeight()/2 + 4);
                    }
                };
                l.setFont(new Font("Monospaced", Font.PLAIN, 9));
                l.setPreferredSize(new Dimension(80, 16));
                p.add(l);
            }
            return p;
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BacktrackingVisualizer::new);
    }
}