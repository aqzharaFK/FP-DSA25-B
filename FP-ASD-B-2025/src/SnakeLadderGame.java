import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.swing.Timer;
import java.awt.geom.AffineTransform;

// ===========================
// 1. DATA MODELS (KODE ASLI ANDA)
// ===========================
class Player implements Comparable<Player> {
    private String name;
    private int id;
    private int position;
    private int score;
    private int wins;
    private Color color;
    private String characterType;
    private ImageIcon avatarIcon;
    private int lastClimbedLadderStart = -1;
    private int lastClimbedLadderEnd = -1;

    public Player(int id, String name, Color color, String charType, ImageIcon icon) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.characterType = charType;
        this.avatarIcon = icon;
        this.position = 1;
        this.score = 0;
        this.wins = 0;
    }

    public void move(int steps) { this.position += steps; }
    public void setPosition(int pos) { this.position = pos; }
    public void addScore(int points) { this.score += points; }
    public void addWin() { this.wins++; }
    public int getPosition() { return position; }
    public String getName() { return name; }
    public Color getColor() { return color; }
    public int getScore() { return score; }
    public ImageIcon getAvatarIcon() { return avatarIcon; }
    public String getCharacterType() { return characterType; }
    public void setLastClimbedLadder(int start, int end) {
        this.lastClimbedLadderStart = start;
        this.lastClimbedLadderEnd = end;
    }
    public void clearLastClimbedLadder() {
        this.lastClimbedLadderStart = -1;
        this.lastClimbedLadderEnd = -1;
    }
    public int getLastClimbedLadderStart() {return lastClimbedLadderStart;}
    public int getLastClimbedLadderEnd() {return lastClimbedLadderEnd;}

    @Override
    public int compareTo(Player other) { return Integer.compare(other.score, this.score); }
}

class GameBoard {
    private Map<Integer, Integer> snakes = new HashMap<>();
    private Map<Integer, Integer> ladders = new HashMap<>();
    private Map<Integer, Integer> cellScores = new HashMap<>();

    public GameBoard() {
        initObstacles();
        initCellScores();
    }

    private void initObstacles() {
        ladders.put(4, 14);
        ladders.put(9, 31);
        ladders.put(20, 38);
        ladders.put(28, 84);
        ladders.put(40, 59);
        ladders.put(51, 67);
        ladders.put(63, 81);
        ladders.put(71, 91);
    }

    private void initCellScores() {
        Random rand = new Random();
        for (int i = 1; i <= 100; i++) {
            cellScores.put(i, rand.nextInt(50) + 10);
        }
    }

    public int checkJump(int pos) {
        if (snakes.containsKey(pos)) return snakes.get(pos);
        if (ladders.containsKey(pos)) return ladders.get(pos);
        return pos;
    }

    public boolean isPrime(int num) {
        if (num <= 1) return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) return false;
        }
        return true;
    }

    public int getScoreForCell(int pos) { return cellScores.getOrDefault(pos, 0); }
    public Map<Integer, Integer> getSnakes() { return snakes; }
    public Map<Integer, Integer> getLadders() { return ladders; }
}

// ===========================
// 2. MAIN GAME CLASS
// ===========================
public class SnakeLadderGame extends JFrame {
    private GameBoard board;
    private Queue<Player> turnQueue;
    private List<Player> allPlayers;
    private boolean extraTurnPending = false;
    private List<Integer> currentShortestPath = new ArrayList<>();
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private BoardPanel boardPanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton rollButton;
    private JLabel diceImageLabel;
    private JTextArea leaderboardArea;

    private DefaultListModel<String> playerListModel;
    private ArrayList<Player> tempPlayerList;
    private JLabel playerCountLabel;
    private JComboBox<String> charSelector;
    private ImageIcon[] diceIcons;

    private final Color COLOR_GREEN_MOVE = new Color(34, 139, 34);
    private final Color COLOR_RED_MOVE = new Color(220, 20, 60);

    public SnakeLadderGame() {
        setTitle("Snake & Ladder: Deluxe Edition");
        setSize(1280, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadDiceImages();
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // MENGGUNAKAN PANEL MENU BARU
        mainContainer.add(createModernMenuPanel(), "MENU");

        add(mainContainer);
        setVisible(true);
    }

    private void loadDiceImages() {
        diceIcons = new ImageIcon[6];
        for (int i = 0; i < 6; i++) {
            try {
                BufferedImage img = ImageIO.read(getClass().getResource("/" + (i + 1) + ".png"));
                diceIcons[i] = new ImageIcon(img.getScaledInstance(90, 90, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                diceIcons[i] = createPlaceholderDice(i + 1);
            }
        }
    }

    private ImageIcon createPlaceholderDice(int val) {
        BufferedImage img = new BufferedImage(90, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0,0,90,90);
        g.setColor(Color.BLACK); g.drawRect(0,0,89,89);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString(String.valueOf(val), 35, 60);
        g.dispose();
        return new ImageIcon(img);
    }

    // ==========================================
    // PERUBAHAN KHUSUS: MODERN MAIN MENU SAJA
    // ==========================================
    private JPanel createModernMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(41, 128, 185), 0, getHeight(), new Color(109, 213, 250));
                g2d.setPaint(gp); g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        JPanel glassBox = new JPanel();
        glassBox.setLayout(new BoxLayout(glassBox, BoxLayout.Y_AXIS));
        glassBox.setBackground(new Color(255, 255, 255, 210));
        glassBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 2),
                new EmptyBorder(40, 60, 40, 60)));

        JLabel title = new JLabel("SNAKE & LADDER");
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 48));
        title.setForeground(new Color(44, 62, 80));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Doraemon Adventure");
        subtitle.setFont(new Font("Segoe UI Semilight", Font.ITALIC, 20));
        subtitle.setForeground(new Color(52, 152, 219));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(0, 0, 40, 0));

        JPanel inputArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        inputArea.setOpaque(false);

        JTextField nameField = new JTextField(12);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nameField.setBorder(BorderFactory.createTitledBorder("Nama"));

        charSelector = new JComboBox<>(new String[]{"Doraemon", "Nobita", "Shizuka", "Giant", "Suneo", "Avatar X", "Avatar O"});
        charSelector.setBorder(BorderFactory.createTitledBorder("Karakter"));

        JButton addBtn = createStyledButton("Tambah", new Color(46, 204, 113));
        JButton remBtn = createStyledButton("Hapus", new Color(231, 76, 60));

        inputArea.add(nameField); inputArea.add(charSelector);
        inputArea.add(addBtn); inputArea.add(remBtn);

        tempPlayerList = new ArrayList<>();
        playerListModel = new DefaultListModel<>();
        JList<String> playerListUI = new JList<>(playerListModel);
        playerListUI.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        JScrollPane scroll = new JScrollPane(playerListUI);
        scroll.setPreferredSize(new Dimension(450, 150));

        playerCountLabel = new JLabel("Pemain Terdaftar: 0/4");
        playerCountLabel.setFont(new Font("Segoe UI Bold", Font.PLAIN, 14));
        playerCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerCountLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JButton startBtn = createStyledButton("MULAI MAIN ▶", new Color(52, 73, 94));
        startBtn.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        startBtn.setPreferredSize(new Dimension(350, 70));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String selChar = (String) charSelector.getSelectedItem();
            if (!name.isEmpty() && tempPlayerList.size() < 4) {
                for (Player p : tempPlayerList) {
                    if (p.getCharacterType().equals(selChar)) {
                        JOptionPane.showMessageDialog(this, "Karakter ini sudah dipilih!");
                        return;
                    }
                }
                Color c = getCharacterColor(selChar);
                tempPlayerList.add(new Player(tempPlayerList.size(), name, c, selChar, createCharacterAvatar(selChar, c)));
                playerListModel.addElement(name + " (" + selChar + ")");
                nameField.setText("");
                updatePlayerCountUI();
            }
        });

        remBtn.addActionListener(e -> {
            int i = playerListUI.getSelectedIndex();
            if (i != -1) { tempPlayerList.remove(i); playerListModel.remove(i); updatePlayerCountUI(); }
        });

        startBtn.addActionListener(e -> startGame());

        glassBox.add(title); glassBox.add(subtitle);
        glassBox.add(inputArea); glassBox.add(scroll);
        glassBox.add(playerCountLabel); glassBox.add(Box.createVerticalStrut(20));
        glassBox.add(startBtn);
        panel.add(glassBox);
        return panel;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private void updatePlayerCountUI() {
        playerCountLabel.setText("Pemain Terdaftar: " + tempPlayerList.size() + "/4");
    }

    private Color getCharacterColor(String type) {
        return switch (type) {
            case "Doraemon" -> Color.BLUE;
            case "Nobita" -> new Color(255, 215, 0);
            case "Shizuka" -> Color.PINK;
            case "Giant" -> new Color(255, 140, 0);
            case "Suneo" -> new Color(0, 255, 255);
            case "Avatar X" -> Color.RED;
            case "Avatar O" -> new Color(50, 205, 50);
            default -> Color.GRAY;
        };
    }

    private ImageIcon createCharacterAvatar(String type, Color c) {
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (type.equals("Avatar X")) {
            g2.setColor(Color.RED); g2.setStroke(new BasicStroke(4));
            g2.drawLine(5, 5, 25, 25); g2.drawLine(25, 5, 5, 25);
        } else if (type.equals("Avatar O")) {
            g2.setColor(Color.GREEN); g2.setStroke(new BasicStroke(4));
            g2.drawOval(5, 5, 20, 20);
        } else {
            g2.setColor(c); g2.fillOval(2, 2, 26, 26);
            g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString(type.substring(0, 1), 10, 20);
        }
        g2.dispose();
        return new ImageIcon(img);
    }

    // ===========================
    // GAME LOGIC (KODE ASLI ANDA)
    // ===========================
    private void startGame() {
        if (tempPlayerList.size() < 2) {
            JOptionPane.showMessageDialog(this, "Minimal 2 pemain diperlukan!");
            return;
        }
        board = new GameBoard();
        allPlayers = new ArrayList<>(tempPlayerList);
        turnQueue = new LinkedList<>(tempPlayerList);

        JPanel gameContainer = new JPanel(new BorderLayout());
        boardPanel = new BoardPanel();
        gameContainer.add(boardPanel, BorderLayout.CENTER);
        gameContainer.add(createGameSidePanel(), BorderLayout.EAST);

        mainContainer.add(gameContainer, "GAME");
        cardLayout.show(mainContainer, "GAME");
        updateTurnLabel();
        updateLeaderboard();
    }

    private JPanel createGameSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(320, 800));
        sidePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidePanel.setBackground(new Color(230, 245, 255));

        JPanel controlBox = new JPanel();
        controlBox.setLayout(new BoxLayout(controlBox, BoxLayout.Y_AXIS));
        controlBox.setOpaque(false);
        controlBox.setBorder(BorderFactory.createTitledBorder("Kontrol"));

        statusLabel = new JLabel("Giliran...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceImageLabel = new JLabel(diceIcons[0]);
        diceImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        diceImageLabel.setBorder(new EmptyBorder(10,0,10,0));

        rollButton = createStyledButton("KOCOK DADU", new Color(70, 130, 180));
        rollButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollButton.addActionListener(e -> startDiceRollAnimation());

        controlBox.add(statusLabel);
        controlBox.add(Box.createVerticalStrut(10));
        controlBox.add(diceImageLabel);
        controlBox.add(Box.createVerticalStrut(10));
        controlBox.add(rollButton);

        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.BOLD, 12));
        JScrollPane leaderScroll = new JScrollPane(leaderboardArea);
        leaderScroll.setBorder(BorderFactory.createTitledBorder("🏆 KLASEMEN"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Riwayat"));
        logScroll.setPreferredSize(new Dimension(280, 200));

        sidePanel.add(controlBox, BorderLayout.NORTH);
        sidePanel.add(leaderScroll, BorderLayout.CENTER);
        sidePanel.add(logScroll, BorderLayout.SOUTH);

        return sidePanel;
    }

    private void startDiceRollAnimation() {
        rollButton.setEnabled(false);
        Timer animTimer = new Timer(50, null);
        final int[] cycles = {0};
        animTimer.addActionListener(e -> {
            diceImageLabel.setIcon(diceIcons[(int)(Math.random() * 6)]);
            if (cycles[0]++ > 10) {
                animTimer.stop();
                finalizeDiceRoll();
            }
        });
        animTimer.start();
    }

    private void finalizeDiceRoll() {
        Player currentPlayer = turnQueue.peek();
        int diceValue = (int) (Math.random() * 6) + 1;
        diceImageLabel.setIcon(diceIcons[diceValue - 1]);
        boolean isGreen = Math.random() < 0.7;
        int steps = isGreen ? diceValue : -diceValue;
        log(currentPlayer.getName() + ": Dadu " + diceValue + (isGreen ? " (Maju)" : " (Mundur)"));
        processMovement(currentPlayer, steps);
    }

    private void processMovement(Player currentPlayer, int steps) {
        int targetPos = Math.max(1, Math.min(100, currentPlayer.getPosition() + steps));
        animateMove(currentPlayer, targetPos, () -> {
            // ATURAN MUNDUR TANGGA
            if (steps < 0 && currentPlayer.getPosition() == currentPlayer.getLastClimbedLadderEnd()) {
                currentPlayer.setPosition(currentPlayer.getLastClimbedLadderStart());
                currentPlayer.clearLastClimbedLadder();
            }

            int obstacleDest = board.checkJump(currentPlayer.getPosition());
            if (obstacleDest != currentPlayer.getPosition()) {
                if (obstacleDest > currentPlayer.getPosition()) {
                    currentPlayer.setLastClimbedLadder(currentPlayer.getPosition(), obstacleDest);
                }
                currentPlayer.setPosition(obstacleDest);
            }

            currentPlayer.addScore(board.getScoreForCell(currentPlayer.getPosition()));
            if (currentPlayer.getPosition() == 100) {
                JOptionPane.showMessageDialog(this, currentPlayer.getName() + " MENANG!");
                cardLayout.show(mainContainer, "MENU");
            } else {
                turnQueue.add(turnQueue.poll());
                updateTurnLabel();
                updateLeaderboard();
                rollButton.setEnabled(true);
            }
            boardPanel.repaint();
        });
    }

    private void animateMove(Player p, int target, Runnable onComplete) {
        Timer timer = new Timer(100, null);
        timer.addActionListener(e -> {
            if (p.getPosition() < target) p.move(1);
            else if (p.getPosition() > target) p.move(-1);
            else { timer.stop(); onComplete.run(); }
            boardPanel.repaint();
        });
        timer.start();
    }

    private void updateTurnLabel() { statusLabel.setText("Giliran: " + turnQueue.peek().getName()); statusLabel.setForeground(turnQueue.peek().getColor()); }
    private void updateLeaderboard() {
        StringBuilder sb = new StringBuilder();
        List<Player> sorted = new ArrayList<>(allPlayers);
        Collections.sort(sorted);
        for(Player p : sorted) sb.append(p.getName()).append(": ").append(p.getScore()).append(" pts\n");
        leaderboardArea.setText(sb.toString());
    }
    private void log(String msg) { logArea.append("• " + msg + "\n"); }

    // ===========================
    // BOARD PANEL (KODE ASLI ANDA)
    // ===========================
    class BoardPanel extends JPanel {
        private final Point[] tileCoords = new Point[101];
        private final int TILE_RADIUS = 22;
        private static final double SCALE = 1.35;
        private static final int OFFSET_X = -30;
        private static final int OFFSET_Y = -193;
        private BufferedImage backgroundImage;

        public BoardPanel() {
            setOpaque(true);
            initPathCoords();
            try { backgroundImage = ImageIO.read(getClass().getResource("/Background_Doraemon.jpeg")); } catch (Exception e) {}
        }

        private void initPathCoords() {
            int startX = 100, startY = 720, size = 60;
            boolean leftToRight = true;
            int num = 1;
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    int x = leftToRight ? startX + col * size : startX + (9 - col) * size;
                    int y = startY - row * size;
                    tileCoords[num++] = new Point(x, y);
                }
                leftToRight = !leftToRight;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            g2.setColor(new Color(255, 255, 255, 120)); g2.fillRect(0, 0, getWidth(), getHeight());

            AffineTransform original = g2.getTransform();
            g2.translate(OFFSET_X, OFFSET_Y);
            g2.scale(SCALE, SCALE);

            drawPath(g2);
            drawObstacles(g2);
            drawTiles(g2);

            if (allPlayers != null) {
                for (int i = 0; i < allPlayers.size(); i++) drawPlayer(g2, allPlayers.get(i), i, allPlayers.size());
            }
            g2.setTransform(original);
        }

        private void drawPath(Graphics2D g2) {
            g2.setColor(new Color(255, 255, 255, 150));
            g2.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Float();
            path.moveTo(tileCoords[1].x, tileCoords[1].y);
            for (int i = 2; i <= 100; i++) path.lineTo(tileCoords[i].x, tileCoords[i].y);
            g2.draw(path);
        }

        private void drawTiles(Graphics2D g2) {
            for (int i = 1; i <= 100; i++) {
                Point p = tileCoords[i];
                Polygon hex = new Polygon();
                for (int j = 0; j < 6; j++) {
                    hex.addPoint((int) (p.x + TILE_RADIUS * Math.cos(j * Math.PI / 3)),
                            (int) (p.y + TILE_RADIUS * Math.sin(j * Math.PI / 3)));
                }
                g2.setColor(new Color(240, 240, 240, 200)); g2.fillPolygon(hex);
                g2.setColor(Color.DARK_GRAY); g2.drawPolygon(hex);
                g2.drawString(String.valueOf(i), p.x - 5, p.y + 5);
            }
        }

        private void drawObstacles(Graphics2D g2) {
            g2.setStroke(new BasicStroke(6f));
            for (Map.Entry<Integer, Integer> entry : board.getLadders().entrySet()) {
                g2.setColor(new Color(218, 165, 32, 200));
                Point p1 = tileCoords[entry.getKey()], p2 = tileCoords[entry.getValue()];
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        private void drawPlayer(Graphics2D g2, Player p, int index, int total) {
            Point center = tileCoords[p.getPosition()];
            g2.drawImage(p.getAvatarIcon().getImage(), center.x-15, center.y-15, null);
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(SnakeLadderGame::new); }
}