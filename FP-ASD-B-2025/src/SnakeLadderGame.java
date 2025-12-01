import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.sound.sampled.*;
import javax.swing.Timer;

// ===========================
// 1. DATA MODELS (TIDAK BERUBAH)
// ===========================

class Player implements Comparable<Player> {
    private String name;
    private int id;
    private int position;
    private int score;
    private int wins;
    private Color color;

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.position = 1;
        this.score = 0;
        this.wins = 0;
    }

    public void move(int steps) { this.position += steps; }
    public void setPosition(int pos) { this.position = pos; }
    public void addScore(int points) { this.score += points; }
    public void addWin() { this.wins++; }

    // Getters
    public int getPosition() { return position; }
    public String getName() { return name; }
    public Color getColor() { return color; }
    public int getScore() { return score; }

    @Override
    public int compareTo(Player other) {
        return other.score - this.score; // Sort descending by score
    }

    @Override
    public String toString() {
        return name;
    }
}

class GameBoard {
    private Map<Integer, Integer> snakes = new HashMap<>();
    private Map<Integer, Integer> ladders = new HashMap<>();
    private Map<Integer, Integer> portals = new HashMap<>();
    private Map<Integer, Integer> cellScores = new HashMap<>();

    public GameBoard() {
        initObstacles();
        initRandomLinks();
        initCellScores();
    }

    private void initObstacles() {
        snakes.put(17, 7); snakes.put(54, 34); snakes.put(62, 19); snakes.put(98, 79);
        ladders.put(3, 38); ladders.put(24, 87); ladders.put(57, 76); ladders.put(80, 100);
    }

    private void initRandomLinks() {
        Random rand = new Random();
        int count = 0;
        while (count < 5) {
            int start = rand.nextInt(90) + 2;
            int end = rand.nextInt(98) + 1;

            if (start != end && !snakes.containsKey(start) && !ladders.containsKey(start) && !portals.containsKey(start)) {
                portals.put(start, end);
                count++;
            }
        }
    }

    private void initCellScores() {
        Random rand = new Random();
        for (int i = 1; i <= 100; i++) {
            cellScores.put(i, rand.nextInt(91) + 10);
        }
    }

    public int checkJump(int pos) {
        if (snakes.containsKey(pos)) return snakes.get(pos);
        if (ladders.containsKey(pos)) return ladders.get(pos);
        if (portals.containsKey(pos)) return portals.get(pos);
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
    public Map<Integer, Integer> getPortals() { return portals; }
}

// ===========================
// 2. SOUND MANAGER (TIDAK BERUBAH)
// ===========================
class SoundManager {
    private Clip bgClip;

    public void playBackground(String filepath) {
        try {
            File musicPath = new File(filepath);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                bgClip = AudioSystem.getClip();
                bgClip.open(audioInput);
                bgClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgClip.start();
            }
        } catch (Exception e) {
            System.out.println("Audio Error: " + e.getMessage());
        }
    }
}

// ===========================
// 3. MAIN GAME CLASS
// ===========================

public class SnakeLadderGame extends JFrame {

    // Core Logic
    private GameBoard board;
    private Queue<Player> turnQueue;
    private List<Player> allPlayers;
    private boolean extraTurnPending = false;
    private List<Integer> currentShortestPath = new ArrayList<>();

    // UI Layout Management
    private CardLayout cardLayout;
    private JPanel mainContainer;

    // Game UI Components
    private BoardPanel boardPanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton rollButton;
    private JTextArea leaderboardArea;

    // Menu Components
    private DefaultListModel<String> playerListModel;
    private JList<String> playerListUI;
    private ArrayList<Player> tempPlayerList;

    private final Color[] colorPool = {Color.RED, Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.PINK, new Color(0,100,0)};

    public SnakeLadderGame() {
        setTitle("Snake & Ladder: Spiral Edition");
        setSize(1250, 850); // Sedikit diperbesar agar spiral terlihat jelas
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        JPanel menuPanel = createMenuPanel();
        mainContainer.add(menuPanel, "MENU");

        add(mainContainer);

        new SoundManager().playBackground("bgm.wav");
    }

    // ===========================
    // A. MENU SCREEN (SETUP) - TIDAK BERUBAH
    // ===========================
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 240, 255));

        JLabel title = new JLabel("SNAKE & LADDER SETUP", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setBorder(new EmptyBorder(30, 0, 30, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 100, 0, 100));

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setOpaque(false);
        JTextField nameField = new JTextField(15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 18));
        JButton addBtn = new JButton("Tambah Player");
        addBtn.setFont(new Font("Arial", Font.BOLD, 14));

        tempPlayerList = new ArrayList<>();
        playerListModel = new DefaultListModel<>();
        playerListUI = new JList<>(playerListModel);
        playerListUI.setFont(new Font("Arial", Font.BOLD, 16));

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                if (tempPlayerList.size() >= colorPool.length) {
                    JOptionPane.showMessageDialog(this, "Maksimal player tercapai!");
                    return;
                }
                Color c = colorPool[tempPlayerList.size()];
                Player newP = new Player(tempPlayerList.size(), name, c);
                tempPlayerList.add(newP);
                playerListModel.addElement("👤 " + name);
                nameField.setText("");
            }
        });

        JButton removeBtn = new JButton("Hapus Selected");
        removeBtn.addActionListener(e -> {
            int idx = playerListUI.getSelectedIndex();
            if (idx != -1) {
                tempPlayerList.remove(idx);
                playerListModel.remove(idx);
            }
        });

        inputPanel.add(new JLabel("Nama: "));
        inputPanel.add(nameField);
        inputPanel.add(addBtn);
        inputPanel.add(removeBtn);

        centerPanel.add(inputPanel);
        centerPanel.add(new JScrollPane(playerListUI));
        panel.add(centerPanel, BorderLayout.CENTER);

        JButton startBtn = new JButton("MULAI PERMAINAN 🚀");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 24));
        startBtn.setBackground(new Color(50, 205, 50));
        startBtn.setForeground(Color.WHITE);
        startBtn.setPreferredSize(new Dimension(200, 80));
        startBtn.addActionListener(e -> startGame());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        bottomPanel.add(startBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

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

        JPanel sidePanel = createGameSidePanel();
        gameContainer.add(sidePanel, BorderLayout.EAST);

        mainContainer.add(gameContainer, "GAME");
        cardLayout.show(mainContainer, "GAME");

        updateTurnLabel();
        updateLeaderboard();
        log("🎮 PERMAINAN DIMULAI! Papan Spiral.");
    }

    // ===========================
    // B. GAME SCREEN (PLAY) - TIDAK BERUBAH
    // ===========================

    private JPanel createGameSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(320, 800));
        sidePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidePanel.setBackground(new Color(245, 245, 255));

        JPanel controlBox = new JPanel(new GridLayout(2, 1, 5, 5));
        controlBox.setOpaque(false);

        statusLabel = new JLabel("Giliran...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        rollButton = new JButton("🎲 KOCOK DADU");
        rollButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        rollButton.setBackground(new Color(100, 149, 237));
        rollButton.setForeground(Color.WHITE);
        rollButton.setFocusPainted(false);
        rollButton.addActionListener(e -> playTurn());

        controlBox.add(statusLabel);
        controlBox.add(rollButton);

        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.BOLD, 12));
        leaderboardArea.setBorder(BorderFactory.createTitledBorder("🏆 KLASEMEN"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Riwayat Permainan"));
        logScroll.setPreferredSize(new Dimension(300, 300));

        sidePanel.add(controlBox, BorderLayout.NORTH);
        sidePanel.add(new JScrollPane(leaderboardArea), BorderLayout.CENTER);
        sidePanel.add(logScroll, BorderLayout.SOUTH);

        return sidePanel;
    }

    // ===========================
    // GAME LOGIC - TIDAK BERUBAH
    // ===========================

    private void playTurn() {
        if (turnQueue.isEmpty()) return;
        rollButton.setEnabled(false);

        Player currentPlayer = turnQueue.peek();
        currentShortestPath.clear();

        if (board.isPrime(currentPlayer.getPosition())) {
            log("✨ " + currentPlayer.getName() + " di posisi PRIMA! Jalur terpendek ditampilkan.");
            calculateShortestPath(currentPlayer.getPosition());
            boardPanel.repaint();
        }

        int diceValue = (int) (Math.random() * 6) + 1;
        boolean isGreen = Math.random() < 0.75;

        int movement = isGreen ? diceValue : -diceValue;
        String diceColor = isGreen ? "🟢 HIJAU (Maju)" : "🔴 MERAH (Mundur)";

        log(currentPlayer.getName() + " dapat dadu: " + diceValue + " " + diceColor);

        int startPos = currentPlayer.getPosition();
        int calculatedTarget = startPos + movement;

        if (calculatedTarget > 100) calculatedTarget = startPos;
        if (calculatedTarget < 1) calculatedTarget = 1;

        final int finalTarget = calculatedTarget;

        animateMove(currentPlayer, finalTarget, () -> {
            int obstacleDest = board.checkJump(finalTarget);

            if (obstacleDest != finalTarget) {
                String type = (obstacleDest > finalTarget) ? "Naik Tangga/Portal 🚀" : "Digigit Ular 🐍";
                log(type + "! Pindah ke " + obstacleDest);
                currentPlayer.setPosition(obstacleDest);
                boardPanel.repaint();
            }

            int nodeScore = board.getScoreForCell(currentPlayer.getPosition());
            currentPlayer.addScore(nodeScore);

            if (currentPlayer.getPosition() == 100) {
                currentPlayer.addWin();
                JOptionPane.showMessageDialog(this, "SELAMAT! " + currentPlayer.getName() + " MENANG!");
                resetGameToMenu();
            } else {
                if (currentPlayer.getPosition() % 5 == 0) {
                    log("⭐ Berhenti di Bintang! Main lagi!");
                    extraTurnPending = true;
                } else {
                    extraTurnPending = false;
                }
                finishTurn(currentPlayer);
            }
        });
    }

    private void finishTurn(Player p) {
        updateLeaderboard();
        if (!extraTurnPending) {
            turnQueue.poll();
            turnQueue.add(p);
        }
        updateTurnLabel();
        rollButton.setEnabled(true);
        boardPanel.repaint();
    }

    private void resetGameToMenu() {
        cardLayout.show(mainContainer, "MENU");
        tempPlayerList.clear();
        playerListModel.clear();
        logArea.setText("");
    }

    private void updateLeaderboard() {
        PriorityQueue<Player> pq = new PriorityQueue<>(allPlayers);
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        while (!pq.isEmpty()) {
            Player p = pq.poll();
            sb.append(rank).append(". ").append(p.getName())
                    .append(" [Pos:").append(p.getPosition()).append("]")
                    .append(" : ").append(p.getScore()).append("\n");
            rank++;
        }
        leaderboardArea.setText(sb.toString());
    }

    private void updateTurnLabel() {
        if (turnQueue.isEmpty()) return;
        Player p = turnQueue.peek();
        statusLabel.setText("Giliran: " + p.getName());
        statusLabel.setForeground(p.getColor());
    }

    private void log(String msg) {
        if (logArea != null) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            System.out.println(msg);
        }
    }

    // ===========================
    // UTILS (BFS & ANIMATION) - TIDAK BERUBAH
    // ===========================

    private void calculateShortestPath(int startNode) {
        Queue<List<Integer>> q = new LinkedList<>();
        boolean[] visited = new boolean[101];

        List<Integer> initialPath = new ArrayList<>();
        initialPath.add(startNode);
        q.add(initialPath);
        visited[startNode] = true;

        while(!q.isEmpty()) {
            List<Integer> path = q.poll();
            int last = path.get(path.size()-1);
            if (last == 100) {
                currentShortestPath = path;
                return;
            }
            for(int i=1; i<=6; i++) {
                int next = last + i;
                if (next <= 100) {
                    int dest = board.checkJump(next);
                    if (!visited[dest]) {
                        visited[dest] = true;
                        List<Integer> newPath = new ArrayList<>(path);
                        newPath.add(dest);
                        q.add(newPath);
                    }
                }
            }
        }
    }

    private void animateMove(Player p, int target, Runnable onComplete) {
        Timer timer = new Timer(200, null);
        timer.addActionListener(e -> {
            int current = p.getPosition();
            if (current < target) {
                p.move(1);
            } else if (current > target) {
                p.move(-1);
            } else {
                timer.stop();
                onComplete.run();
            }
            boardPanel.repaint();
        });
        timer.start();
    }

    // ===========================
    // VISUAL BOARD (SPIRAL MODE - DIROMBAK TOTAL)
    // ===========================
    class BoardPanel extends JPanel {
        // Menyimpan posisi tengah dari setiap cell (index 0 = cell 1, index 99 = cell 100)
        private List<Point> cellPositions;
        private final int CELL_SIZE = 35; // Ukuran tetap untuk lingkaran cell

        public BoardPanel() {
            setBackground(new Color(240, 248, 255)); // Alice Blue background
            cellPositions = new ArrayList<>();
        }

        // Fungsi untuk menghitung posisi spiral berdasarkan ukuran panel saat ini
        private void calculateSpiralPositions(int w, int h) {
            cellPositions.clear();
            double centerX = w / 2.0;
            double centerY = h / 2.0;

            // Radius maksimum agar spiral muat di layar dengan sedikit padding
            double maxRadius = Math.min(w, h) / 2.0 * 0.85;
            // Radius minimum agar angka 100 tidak terlalu rapat di tengah
            double minRadius = CELL_SIZE * 1.5;

            // Jumlah putaran spiral
            double rotations = 4.5;
            double totalAngle = Math.PI * 2 * rotations;
            double startAngle = -Math.PI / 2; // Mulai dari atas (jam 12)

            for (int i = 0; i < 100; i++) {
                // Normalisasi i dari 0.0 ke 1.0
                double ratio = i / 99.0;

                // Radius mengecil saat mendekati 100 (i mendekati 99)
                double radius = maxRadius - (maxRadius - minRadius) * ratio;

                // Sudut bertambah seiring bertambahnya i
                double angle = startAngle + totalAngle * ratio;

                // Rumus Matematika Spiral (Polar ke Cartesian)
                int x = (int) (centerX + radius * Math.cos(angle));
                int y = (int) (centerY + radius * Math.sin(angle));

                cellPositions.add(new Point(x, y));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Hitung ulang posisi jika ukuran window berubah
            if (cellPositions.isEmpty() || w != getWidth() || h != getHeight()) {
                calculateSpiralPositions(w, h);
            }

            // 1. Gambar Garis Lintasan Spiral
            g2.setColor(new Color(200, 200, 200));
            g2.setStroke(new BasicStroke(3f));
            if (cellPositions.size() > 1) {
                Point2D prev = cellPositions.get(0);
                for (int i = 1; i < 100; i++) {
                    Point2D curr = cellPositions.get(i);
                    g2.draw(new Line2D.Double(prev, curr));
                    prev = curr;
                }
            }

            // 2. Gambar Cell (Lingkaran Angka)
            for (int i = 0; i < 100; i++) {
                Point p = cellPositions.get(i);
                int num = i + 1;
                int x = p.x - CELL_SIZE / 2;
                int y = p.y - CELL_SIZE / 2;

                // Warna dasar cell
                if (num % 5 == 0) g2.setColor(new Color(255, 215, 0)); // Emas untuk bintang
                else g2.setColor(Color.WHITE);

                // Highlight jalur terpendek (jika ada)
                if (currentShortestPath.contains(num)) {
                    g2.setColor(new Color(135, 206, 250)); // Sky Blue
                }

                g2.fillOval(x, y, CELL_SIZE, CELL_SIZE);
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x, y, CELL_SIZE, CELL_SIZE);

                // Gambar Nomor
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String numStr = String.valueOf(num);
                int textX = p.x - fm.stringWidth(numStr) / 2;
                int textY = p.y + fm.getAscent() / 2 - 2;
                g2.drawString(numStr, textX, textY);

                // Icon Bintang
                if (num % 5 == 0) {
                    g2.setColor(new Color(255, 69, 0));
                    g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                    g2.drawString("★", p.x + 8, p.y - 8);
                }
            }

            // 3. Gambar Koneksi (Ular/Tangga/Portal)
            // Menggunakan posisi dari cellPositions
            drawConnections(g2, board.getSnakes(), new Color(220, 20, 60, 180), 4); // Merah transparan
            drawConnections(g2, board.getLadders(), new Color(34, 139, 34, 180), 4); // Hijau transparan
            drawConnections(g2, board.getPortals(), new Color(30, 144, 255, 180), 3); // Biru transparan

            // 4. Gambar Player
            if (allPlayers != null) {
                for (Player player : allPlayers) {
                    drawPlayer(g2, player);
                }
            }
        }

        private void drawPlayer(Graphics2D g2, Player p) {
            int posIndex = p.getPosition() - 1;
            if (posIndex < 0 || posIndex >= cellPositions.size()) return;

            Point center = cellPositions.get(posIndex);

            int playerSize = CELL_SIZE - 10;

            // Offset agar player tidak menumpuk persis di tengah jika di posisi sama
            int offsetVal = 6;
            int offsetX = (p.hashCode() % 3 - 1) * offsetVal;
            int offsetY = ((p.hashCode() / 3) % 3 - 1) * offsetVal;

            int x = center.x - playerSize / 2 + offsetX;
            int y = center.y - playerSize / 2 + offsetY;

            // Efek bayangan sedikit
            g2.setColor(new Color(0,0,0,50));
            g2.fillOval(x+2, y+2, playerSize, playerSize);

            g2.setColor(p.getColor());
            g2.fillOval(x, y, playerSize, playerSize);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x, y, playerSize, playerSize);
        }

        private void drawConnections(Graphics2D g2, Map<Integer, Integer> map, Color c, int thick) {
            g2.setColor(c);
            g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                int startIdx = entry.getKey() - 1;
                int endIdx = entry.getValue() - 1;

                if (startIdx >= 0 && startIdx < 100 && endIdx >= 0 && endIdx < 100) {
                    Point p1 = cellPositions.get(startIdx);
                    Point p2 = cellPositions.get(endIdx);
                    g2.draw(new Line2D.Double(p1, p2));

                    // Gambar panah/lingkaran di tujuan
                    g2.fillOval(p2.x - thick*2, p2.y - thick*2, thick*4, thick*4);
                }
            }
        }
    }

    public static void main(String[] args) {
        // Menggunakan Nimbus Look and Feel agar UI lebih modern
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to default
        }
        SwingUtilities.invokeLater(() -> new SnakeLadderGame().setVisible(true));
    }
}