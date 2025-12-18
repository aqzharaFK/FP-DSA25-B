import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.swing.Timer;
import java.awt.geom.AffineTransform;
import javax.sound.sampled.*;
import java.io.InputStream;



// ===========================
// 1. DATA MODELS
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

    // Getters
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
    @Override
    public String toString() { return name; }
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
        snakes.clear(); // ❌ TIDAK ADA ULAR

        // Tangga (Naik)
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

    // ===========================
    // SOUND SYSTEM
    // ===========================
    private boolean soundEnabled = true;
    private Clip backgroundClip;

    private void toggleBackgroundMusic(boolean enable) {
        soundEnabled = enable;

        if (soundEnabled) {
            if (backgroundClip == null || !backgroundClip.isRunning()) {
                playBackgroundMusic();
            }
        } else {
            stopBackgroundMusic();
        }
    }


    // ===========================
// BACKGROUND MUSIC ONLY
// ===========================
    private void playBackgroundMusic() {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                    Objects.requireNonNull(
                            getClass().getResource("/BackgroundSound.wav")

                    )
            );
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioIn);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
        } catch (Exception e) {
            System.out.println("Background music gagal dimainkan");
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
        }
    }



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

    // Menu Components
    private DefaultListModel<String> playerListModel;
    private ArrayList<Player> tempPlayerList;
    private JLabel playerCountLabel;
    private JButton addPlayerBtn;
    private JComboBox<String> charSelector; // Dropdown pemilihan karakter

    // Asset Dadu
    private ImageIcon[] diceIcons;

    // Warna untuk dadu status
    private final Color COLOR_GREEN_MOVE = new Color(34, 139, 34);
    private final Color COLOR_RED_MOVE = new Color(220, 20, 60);

    public SnakeLadderGame() {
        setTitle("Snake & Ladder: Red/Green Dice Edition");
        setSize(1280, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadDiceImages();
        playBackgroundMusic(); // 🔊 INI YANG KURANG

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        JPanel menuPanel = createMenuPanel();
        mainContainer.add(menuPanel, "MENU");

        add(mainContainer);
    }


    private void loadDiceImages() {
        diceIcons = new ImageIcon[6];

        for (int i = 0; i < 6; i++) {
            try {
                BufferedImage img = ImageIO.read(
                        Objects.requireNonNull(
                                getClass().getResource("/" + (i + 1) + ".png")
                        )
                );

                Image scaled = img.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                diceIcons[i] = new ImageIcon(scaled);

            } catch (Exception e) {
                diceIcons[i] = new ImageIcon(); // fallback kosong
            }
        }
    }



    private ImageIcon loadScaledImage(String path, int w, int h, int val) {
        File f = new File(path);
        if(f.exists()) {
            ImageIcon ii = new ImageIcon(path);
            Image img = ii.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } else {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE); g.fillRect(0,0,w,h);
            g.setColor(Color.BLACK); g.drawRect(0,0,w-1,h-1);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString(String.valueOf(val), w/2-10, h/2+10);
            g.dispose();
            return new ImageIcon(img);
        }
    }

    // ===========================
    // UI MENU (INTEGRASI PEMILIHAN KARAKTER)
    // ===========================
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth(); int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(135, 206, 235), 0, h, new Color(224, 255, 255));
                g2d.setPaint(gp); g2d.fillRect(0, 0, w, h);
            }
        };

        JPanel contentBox = new JPanel(new BorderLayout());
        contentBox.setOpaque(false);

        JLabel title = new JLabel("SETUP PEMAIN", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(new Color(0, 102, 204));
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Area Input
        JPanel inputArea = new JPanel(new BorderLayout());
        inputArea.setOpaque(false);

        JPanel topInput = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topInput.setOpaque(false);

        JTextField nameField = new JTextField(10);
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nameField.setBorder(BorderFactory.createTitledBorder("Nama"));

        // Dropdown Pilihan Karakter (Termasuk X dan O)
        String[] characters = {"Doraemon", "Nobita", "Shizuka", "Giant", "Suneo", "Avatar X", "Avatar O"};
        charSelector = new JComboBox<>(characters);
        charSelector.setBorder(BorderFactory.createTitledBorder("Pilih Karakter"));
        charSelector.setBackground(Color.WHITE);

        addPlayerBtn = new JButton("Tambah");
        addPlayerBtn.setBackground(new Color(100, 149, 237));
        addPlayerBtn.setForeground(Color.WHITE);

        playerCountLabel = new JLabel("Players: 0/4");
        playerCountLabel.setFont(new Font("Arial", Font.BOLD, 14));

        tempPlayerList = new ArrayList<>();
        playerListModel = new DefaultListModel<>();
        JList<String> playerListUI = new JList<>(playerListModel);
        playerListUI.setFont(new Font("Arial", Font.BOLD, 14));
        JScrollPane listScroll = new JScrollPane(playerListUI);
        listScroll.setPreferredSize(new Dimension(300, 120));

        // LOGIKA TAMBAH PLAYER
        addPlayerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String selectedChar = (String) charSelector.getSelectedItem();

            if (!name.isEmpty() && selectedChar != null) {
                if (tempPlayerList.size() >= 4) {
                    JOptionPane.showMessageDialog(this, "Maksimal 4 pemain!");
                    return;
                }

                // Cek duplikasi karakter
                for (Player p : tempPlayerList) {
                    if (p.getCharacterType().equals(selectedChar)) {
                        JOptionPane.showMessageDialog(this, "Karakter " + selectedChar + " sudah dipilih orang lain!");
                        return;
                    }
                }

                // Tentukan warna dan icon berdasarkan pilihan
                Color c = getCharacterColor(selectedChar);
                ImageIcon icon = createCharacterAvatar(selectedChar, c);

                Player newP = new Player(tempPlayerList.size(), name, c, selectedChar, icon);
                tempPlayerList.add(newP);
                playerListModel.addElement(name + " (" + selectedChar + ")");
                nameField.setText("");
                updatePlayerCountUI();
            }
        });

        JButton removeBtn = new JButton("Hapus");
        removeBtn.setBackground(Color.RED);
        removeBtn.setForeground(Color.WHITE);
        removeBtn.addActionListener(e -> {
            int idx = playerListUI.getSelectedIndex();
            if (idx != -1) {
                tempPlayerList.remove(idx);
                playerListModel.remove(idx);
                updatePlayerCountUI();
            }
        });

        topInput.add(nameField);
        topInput.add(charSelector);
        topInput.add(addPlayerBtn);
        topInput.add(removeBtn);

        inputArea.add(topInput, BorderLayout.NORTH);
        JPanel listContainer = new JPanel(new BorderLayout());
        listContainer.setOpaque(false);
        listContainer.add(playerCountLabel, BorderLayout.NORTH);
        listContainer.add(listScroll, BorderLayout.CENTER);
        inputArea.add(listContainer, BorderLayout.SOUTH);

        JButton startBtn = new JButton("MULAI MAIN ▶");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 22));
        startBtn.setBackground(new Color(50, 205, 50));
        startBtn.setForeground(Color.WHITE);
        startBtn.setPreferredSize(new Dimension(200, 60));
        startBtn.addActionListener(e -> startGame());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; panel.add(title, gbc);
        gbc.gridy = 1; panel.add(inputArea, gbc);
        gbc.gridy = 2; panel.add(Box.createVerticalStrut(20), gbc);
        gbc.gridy = 3; panel.add(startBtn, gbc);

        return panel;
    }

    private Color getCharacterColor(String type) {
        switch (type) {
            case "Doraemon": return Color.BLUE;
            case "Nobita": return new Color(255, 215, 0);
            case "Shizuka": return Color.PINK;
            case "Giant": return new Color(255, 140, 0);
            case "Suneo": return new Color(0, 255, 255);
            case "Avatar X": return Color.RED;
            case "Avatar O": return new Color(50, 205, 50);
            default: return Color.GRAY;
        }
    }

    // Helper membuat icon (termasuk menggambar X atau O jika dipilih)
    private ImageIcon createCharacterAvatar(String type, Color c) {
        // Coba load file gambar dulu
        String filename = type.toLowerCase().replace(" ", "") + ".png"; // misal "avatarx.png"
        File f = new File(filename);
        if (!f.exists()) f = new File("src/" + filename);

        if (f.exists()) {
            return new ImageIcon(new ImageIcon(f.getPath()).getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
        }

        // Jika gambar tidak ada, buat secara manual
        int size = 30;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (type.equals("Avatar X")) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(5, 5, 25, 25);
            g2.drawLine(25, 5, 5, 25);
        } else if (type.equals("Avatar O")) {
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(5, 5, 20, 20);
        } else {
            // Default wajah bulat berwarna
            g2.setColor(c);
            g2.fillOval(2, 2, size-4, size-4);
            g2.setColor(Color.WHITE);
            g2.drawOval(2, 2, size-4, size-4);
            // Inisial
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString(type.substring(0,1), 10, 20);
        }
        g2.dispose();
        return new ImageIcon(img);
    }

    private void updatePlayerCountUI() {
        int count = tempPlayerList.size();
        playerCountLabel.setText("Players: " + count + "/4");
        addPlayerBtn.setEnabled(count < 4);
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
        log("🎮 Permainan Dimulai!");
        log("ℹ️ INFO DADU: Hijau = Maju, Merah = Mundur");
    }

    // ===========================
    // GAME LOGIC (DADU MERAH & HIJAU)
    // ===========================

    private void startDiceRollAnimation() {
        if (turnQueue.isEmpty()) return;
        rollButton.setEnabled(false);
        log("🎲 Mengocok dadu...");

        Timer animTimer = new Timer(50, null);
        final int[] cycles = {0};
        animTimer.addActionListener(e -> {
            int randomFace = (int)(Math.random() * 6);
            diceImageLabel.setIcon(diceIcons[randomFace]);
            cycles[0]++;
            if (cycles[0] > 10) {
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

        // LOGIKA DADU MERAH / HIJAU
        // 70% Kemungkinan Hijau (Maju), 30% Merah (Mundur)
        boolean isGreen = Math.random() < 0.7;

        int steps = diceValue;
        String colorText;
        Color logColor;

        if (isGreen) {
            colorText = "HIJAU (MAJU)";
            logColor = COLOR_GREEN_MOVE;
        } else {
            steps = -diceValue; // Mundur
            colorText = "MERAH (MUNDUR)";
            logColor = COLOR_RED_MOVE;
        }

        log(currentPlayer.getName() + ": Dadu " + diceValue + " -> " + colorText);

        processMovement(currentPlayer, steps);
    }

    private void processMovement(Player currentPlayer, int steps) {
        currentShortestPath.clear();
        if (board.isPrime(currentPlayer.getPosition())) {
            calculateShortestPath(currentPlayer.getPosition());
        }

        int startPos = currentPlayer.getPosition();
        int targetPos = startPos + steps;

        // Aturan batas bawah dan atas
        if (targetPos < 1) targetPos = 1;
        if (targetPos > 100) targetPos = 200 - targetPos;
        if (startPos + steps > 100) targetPos = startPos;

        final int finalTarget = targetPos;

        animateMove(currentPlayer, finalTarget, () -> {

            // =====================================
            // ATURAN BARU: MUNDUR KE TANGGA TERAKHIR
            // =====================================
            if (steps < 0) {
                if (currentPlayer.getPosition() ==
                        currentPlayer.getLastClimbedLadderEnd()) {

                    log("⬇️ MUNDUR KE TANGGA TERAKHIR! TURUN KEMBALI!");

                    currentPlayer.setPosition(
                            currentPlayer.getLastClimbedLadderStart()
                    );
                    currentPlayer.clearLastClimbedLadder();
                    boardPanel.repaint();
                    finishTurn(currentPlayer);
                    return;
                }
            }

            // ================================
            // CEK TANGGA / ULAR
            // ================================
            int obstacleDest = board.checkJump(currentPlayer.getPosition());

            if (obstacleDest != currentPlayer.getPosition()) {

                if (obstacleDest > currentPlayer.getPosition()) {
                    int start = currentPlayer.getPosition();
                    int end = obstacleDest;

                    log("NAIK TANGGA! 🪜 Dari " + start + " ke " + end);

                    currentPlayer.setPosition(end);
                    currentPlayer.setLastClimbedLadder(start, end);
                } else {
                    log("DITANGKAP NAGA! 🐉 Turun ke " + obstacleDest);
                    currentPlayer.setPosition(obstacleDest);
                    currentPlayer.clearLastClimbedLadder();
                }

                boardPanel.repaint();
            }

            currentPlayer.addScore(
                    board.getScoreForCell(currentPlayer.getPosition())
            );

            if (currentPlayer.getPosition() == 100) {
                currentPlayer.addWin();
                JOptionPane.showMessageDialog(
                        this,
                        "SELAMAT! " + currentPlayer.getName() + " MENANG!"
                );
                resetGameToMenu();
            } else {
                extraTurnPending =
                        (currentPlayer.getPosition() % 10 == 0 &&
                                currentPlayer.getPosition() != 100);

                if (extraTurnPending) {
                    log("⭐ Bonus Giliran (Kelipatan 10)!");
                }

                finishTurn(currentPlayer);
            }
        });
    }

    // ===========================
    // UI SIDE PANEL
    // ===========================

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

        rollButton = new JButton("KOCOK DADU");
        rollButton.setFont(new Font("Segoe UI Black", Font.BOLD, 18));
        rollButton.setBackground(new Color(70, 130, 180));
        rollButton.setForeground(Color.WHITE);
        rollButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollButton.addActionListener(e -> startDiceRollAnimation());

        controlBox.add(statusLabel);
        controlBox.add(Box.createVerticalStrut(10));
        controlBox.add(diceImageLabel);
        controlBox.add(Box.createVerticalStrut(10));
        controlBox.add(rollButton);

        JCheckBox musicToggle = new JCheckBox("🎵 Background Music", true);
        musicToggle.setOpaque(false);
        musicToggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        musicToggle.setAlignmentX(Component.CENTER_ALIGNMENT);

        musicToggle.addActionListener(e -> {
            toggleBackgroundMusic(musicToggle.isSelected());
        });

        controlBox.add(Box.createVerticalStrut(10));
        controlBox.add(musicToggle);


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
        updatePlayerCountUI();
    }

    private void updateLeaderboard() {
        PriorityQueue<Player> pq = new PriorityQueue<>(allPlayers);
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        while (!pq.isEmpty()) {
            Player p = pq.poll();
            sb.append(rank).append(". ").append(p.getName())
                    .append(" (").append(p.getCharacterType()).append(")\n")
                    .append("   Pos: ").append(p.getPosition()).append(" | Skor: ").append(p.getScore()).append("\n");
            rank++;
        }
        leaderboardArea.setText(sb.toString());
    }

    private void updateTurnLabel() {
        if (turnQueue.isEmpty()) return;
        Player p = turnQueue.peek();
        statusLabel.setText("Giliran: " + p.getName());
        statusLabel.setForeground(p.getColor());
        // Jika ada icon X atau O, tampilkan di label juga bisa (opsional)
    }

    private void log(String msg) {
        logArea.append("• " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void animateMove(Player p, int target, Runnable onComplete) {
        Timer timer = new Timer(100, null);
        timer.addActionListener(e -> {
            int current = p.getPosition();
            if (current < target) p.move(1);
            else if (current > target) p.move(-1);
            else {
                timer.stop();
                onComplete.run();
            }
            boardPanel.repaint();
        });
        timer.start();
    }

    private void calculateShortestPath(int startNode) {
        Queue<List<Integer>> q = new LinkedList<>();
        boolean[] visited = new boolean[101];
        List<Integer> initialPath = new ArrayList<>();
        initialPath.add(startNode); q.add(initialPath); visited[startNode] = true;
        while(!q.isEmpty()) {
            List<Integer> path = q.poll();
            int last = path.get(path.size()-1);
            if (last == 100) { currentShortestPath = path; return; }
            for(int i=1; i<=6; i++) {
                int next = last + i;
                if (next <= 100) {
                    int dest = board.checkJump(next);
                    if (!visited[dest]) {
                        visited[dest] = true;
                        List<Integer> newPath = new ArrayList<>(path); newPath.add(dest); q.add(newPath);
                    }
                }
            }
        }
    }

    // ===========================
// BOARD PANEL (DENGAN BACKGROUND DORAEMON)
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

            try {
                backgroundImage = ImageIO.read(
                        Objects.requireNonNull(
                                getClass().getResource("/Background_Doraemon.jpeg")
                        )
                );
            } catch (Exception e) {
                backgroundImage = null;
            }
        }

        private void initPathCoords() {
            int startX = 100;
            int startY = 720;
            int size   = 60;

            boolean leftToRight = true;
            int num = 1;

            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {

                    int x = leftToRight
                            ? startX + col * size
                            : startX + (9 - col) * size;

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

            // BACKGROUND IMAGE
            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            }

            // OVERLAY AGAR PAPAN JELAS
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());

            AffineTransform original = g2.getTransform();

            g2.translate(OFFSET_X, OFFSET_Y);
            g2.scale(SCALE, SCALE);

            drawPath(g2);
            drawObstacles(g2);
            drawTiles(g2);

            if (allPlayers != null) {
                for (int i = 0; i < allPlayers.size(); i++) {
                    drawPlayer(g2, allPlayers.get(i), i, allPlayers.size());
                }
            }

            drawFinishGate(g2, tileCoords[100]);
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
                Color baseColor;
                int colorPattern = i % 6;
                if (i == 100) baseColor = new Color(255, 215, 0);
                else if (i == 1) baseColor = new Color(144, 238, 144);
                else if (colorPattern == 0 || colorPattern == 3) baseColor = new Color(173, 255, 47);
                else if (colorPattern == 1) baseColor = new Color(255, 250, 205);
                else if (colorPattern == 2 || colorPattern == 5) baseColor = new Color(255, 182, 193);
                else baseColor = new Color(176, 224, 230);

                if (currentShortestPath.contains(i)) baseColor = new Color(135, 206, 250);

                Polygon hex = new Polygon();
                for (int j = 0; j < 6; j++) {
                    hex.addPoint((int) (p.x + TILE_RADIUS * Math.cos(j * Math.PI / 3)),
                            (int) (p.y + TILE_RADIUS * Math.sin(j * Math.PI / 3)));
                }

                g2.setColor(baseColor);
                g2.fillPolygon(hex);
                g2.setColor(new Color(0,0,0,50));
                g2.setStroke(new BasicStroke(2));
                g2.drawPolygon(hex);

                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String numStr = String.valueOf(i);
                g2.drawString(numStr, p.x - fm.stringWidth(numStr)/2, p.y + fm.getAscent()/2 - 2);
            }
        }

        private void drawObstacles(Graphics2D g2) {
            // Tangga
            g2.setColor(new Color(218, 165, 32, 200));
            g2.setStroke(new BasicStroke(6f));
            for (Map.Entry<Integer, Integer> entry : board.getLadders().entrySet()) {
                Point p1 = tileCoords[entry.getKey()];
                Point p2 = tileCoords[entry.getValue()];
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            // Ular
            g2.setColor(new Color(0, 128, 128, 180));
            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Map.Entry<Integer, Integer> entry : board.getSnakes().entrySet()) {
                Point start = tileCoords[entry.getKey()];
                Point end = tileCoords[entry.getValue()];
                Path2D path = new Path2D.Float();
                path.moveTo(start.x, start.y);
                double ctrlX = start.x + (end.x - start.x) / 2.0 + (new Random().nextInt(100)-50);
                double ctrlY = start.y + (end.y - start.y) / 2.0 - 100;
                path.quadTo(ctrlX, ctrlY, end.x, end.y);
                g2.draw(path);
                g2.fillOval(start.x-8, start.y-8, 16, 16);
            }
        }

        private void drawFinishGate(Graphics2D g2, Point p) {
            g2.setColor(new Color(139, 69, 19));
            g2.fillRect(p.x - 25, p.y - 30, 5, 40);
            g2.fillRect(p.x + 20, p.y - 30, 5, 40);
            g2.fillRect(p.x - 30, p.y - 35, 60, 10);
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.drawString("FINISH", p.x - 18, p.y - 27);
        }

        private void drawPlayer(Graphics2D g2, Player p, int index, int total) {
            int posIdx = p.getPosition();
            if (posIdx < 1 || posIdx > 100) return;

            Point center = tileCoords[posIdx];
            int size = 28;
            int offsetX = 0, offsetY = 0;
            if (total > 1) {
                offsetX = (index % 2 == 0 ? -1 : 1) * 8;
                offsetY = (index < 2 ? -1 : 1) * 8;
            }

            int x = center.x - size/2 + offsetX;
            int y = center.y - size/2 + offsetY;

            // Gambar avatar khusus (X, O, atau gambar doraemon)
            if (p.getAvatarIcon() != null) {
                g2.drawImage(p.getAvatarIcon().getImage(), x, y, size, size, null);
                // Border warna pemain
                g2.setColor(p.getColor());
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(x-1, y-1, size+2, size+2);
            } else {
                // Fallback bulat biasa
                g2.setColor(p.getColor());
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x, y, size, size);
            }
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new SnakeLadderGame().setVisible(true));
    }
}