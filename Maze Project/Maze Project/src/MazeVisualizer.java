import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MazeVisualizer extends JFrame {

    private final int ROWS = 20;
    private final int COLS = 20;
    private final int CELL_SIZE = 30;
    private MazePanel panel;
    private JLabel statusLabel;

    public MazeVisualizer() {
        setTitle("Ultimate Maze: BFS, DFS, Dijkstra, A*");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        // Status Bar untuk menampilkan Cost/Steps
        statusLabel = new JLabel("Ready. Generate Maze first!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(statusLabel, BorderLayout.NORTH);

        panel = new MazePanel(ROWS, COLS, CELL_SIZE, statusLabel);
        add(panel, BorderLayout.CENTER);

        // Control Panel
        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(2, 1)); // 2 Baris tombol

        JPanel row1 = new JPanel();
        JButton btnGen = new JButton("1. Generate (Prim)");
        JButton btnWeights = new JButton("2. Randomize Terrain");
        JButton btnReset = new JButton("Reset");
        row1.add(btnGen);
        row1.add(btnWeights);
        row1.add(btnReset);

        JPanel row2 = new JPanel();
        JButton btnBFS = new JButton("BFS");
        JButton btnDFS = new JButton("DFS");
        JButton btnDijkstra = new JButton("Dijkstra");
        JButton btnAStar = new JButton("A*");

        row2.add(btnBFS);
        row2.add(btnDFS);
        row2.add(btnDijkstra);
        row2.add(btnAStar);

        controls.add(row1);
        controls.add(row2);

        // Action Listeners
        btnGen.addActionListener(e -> panel.generateMazePrim());
        btnWeights.addActionListener(e -> panel.randomizeWeights());
        btnReset.addActionListener(e -> panel.resetGrid());

        btnBFS.addActionListener(e -> panel.solveBFS());
        btnDFS.addActionListener(e -> panel.solveDFS());
        btnDijkstra.addActionListener(e -> panel.solveDijkstra());
        btnAStar.addActionListener(e -> panel.solveAStar());

        add(controls, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {}
        SwingUtilities.invokeLater(MazeVisualizer::new);
    }
}

class MazePanel extends JPanel {
    private final int rows, cols, cellSize;
    private final Cell[][] grid;
    private Cell start, end;
    private boolean isWorking = false;
    private final int DELAY = 15;
    private JLabel statusLabel;

    // Costs
    public static final int COST_GRASS = 1;
    public static final int COST_MUD = 5;
    public static final int COST_WATER = 10;

    public MazePanel(int rows, int cols, int cellSize, JLabel statusLabel) {
        this.rows = rows;
        this.cols = cols;
        this.cellSize = cellSize;
        this.statusLabel = statusLabel;
        this.grid = new Cell[rows][cols];
        setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));
        setBackground(Color.BLACK);
        resetGrid();
    }

    public void resetGrid() {
        if (isWorking) return;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
        start = grid[0][0];
        end = grid[rows - 1][cols - 1];
        statusLabel.setText("Grid Reset.");
        repaint();
    }

    public void randomizeWeights() {
        if (isWorking) return;
        Random rand = new Random();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == start || grid[r][c] == end) {
                    grid[r][c].weight = COST_GRASS;
                    continue;
                }
                int p = rand.nextInt(100);
                if (p < 60) grid[r][c].weight = COST_GRASS;
                else if (p < 90) grid[r][c].weight = COST_MUD;
                else grid[r][c].weight = COST_WATER;
            }
        }
        statusLabel.setText("Terrain Randomized.");
        repaint();
    }

    // --- 1. PRIM'S GENERATOR ---
    public void generateMazePrim() {
        if (isWorking) return;
        resetGrid();
        isWorking = true;
        statusLabel.setText("Generating Maze...");

        new Thread(() -> {
            Cell current = grid[0][0];
            current.visited = true;
            ArrayList<Wall> walls = new ArrayList<>();
            addWalls(current, walls);

            while (!walls.isEmpty()) {
                int index = new Random().nextInt(walls.size());
                Wall w = walls.remove(index);
                Cell next = w.to;
                if (!next.visited) {
                    removeWall(w.from, next);
                    next.visited = true;
                    addWalls(next, walls);
                    repaint();
                    sleep(5);
                }
            }
            clearSolverState();
            isWorking = false;
            SwingUtilities.invokeLater(() -> statusLabel.setText("Maze Generated."));
        }).start();
    }

    // --- 2a. BFS (Unweighted / Steps Only) ---
    public void solveBFS() {
        if (isWorking) return;
        clearSolverState();
        isWorking = true;
        statusLabel.setText("Running BFS...");

        new Thread(() -> {
            Queue<Cell> queue = new LinkedList<>();
            queue.add(start);
            start.visited = true;

            boolean found = false;
            while (!queue.isEmpty()) {
                Cell current = queue.poll();
                if (current == end) { found = true; break; }

                for (Cell neighbor : getNeighbors(current)) {
                    if (!neighbor.visited) {
                        neighbor.visited = true;
                        neighbor.parent = current;
                        queue.add(neighbor);
                    }
                }
                repaint();
                sleep(DELAY);
            }
            if(found) reconstructPath("BFS");
            else stopWorking("No Path Found");
        }).start();
    }

    // --- 2b. DFS (Unweighted / Random Path) ---
    public void solveDFS() {
        if (isWorking) return;
        clearSolverState();
        isWorking = true;
        statusLabel.setText("Running DFS...");

        new Thread(() -> {
            Stack<Cell> stack = new Stack<>();
            stack.push(start);
            start.visited = true;

            boolean found = false;
            while (!stack.isEmpty()) {
                Cell current = stack.pop();
                if (current == end) { found = true; break; }

                for (Cell neighbor : getNeighbors(current)) {
                    if (!neighbor.visited) {
                        neighbor.visited = true;
                        neighbor.parent = current;
                        stack.push(neighbor);
                    }
                }
                repaint();
                sleep(DELAY);
            }
            if(found) reconstructPath("DFS");
            else stopWorking("No Path Found");
        }).start();
    }

    // --- 2c. DIJKSTRA (Weighted Cost) ---
    public void solveDijkstra() {
        if (isWorking) return;
        clearSolverState();
        isWorking = true;
        statusLabel.setText("Running Dijkstra...");

        new Thread(() -> {
            PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> c.gCost));
            start.gCost = 0;
            pq.add(start);

            boolean found = false;
            while (!pq.isEmpty()) {
                Cell current = pq.poll();
                if (current.visited) continue;
                current.visited = true;

                if (current == end) { found = true; break; }

                for (Cell neighbor : getNeighbors(current)) {
                    if (!neighbor.visited) {
                        int newCost = current.gCost + neighbor.weight;
                        if (newCost < neighbor.gCost) {
                            neighbor.gCost = newCost;
                            neighbor.parent = current;
                            pq.add(neighbor);
                        }
                    }
                }
                repaint();
                sleep(DELAY);
            }
            if(found) reconstructPath("Dijkstra");
            else stopWorking("No Path Found");
        }).start();
    }

    // --- 2d. A* (Weighted Heuristic) ---
    public void solveAStar() {
        if (isWorking) return;
        clearSolverState();
        isWorking = true;
        statusLabel.setText("Running A*...");

        new Thread(() -> {
            PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> c.fCost));
            start.gCost = 0;
            start.hCost = calculateHeuristic(start, end);
            start.fCost = start.gCost + start.hCost;
            pq.add(start);

            boolean found = false;
            while (!pq.isEmpty()) {
                Cell current = pq.poll();
                if (current.visited) continue;
                current.visited = true;

                if (current == end) { found = true; break; }

                for (Cell neighbor : getNeighbors(current)) {
                    if (!neighbor.visited) {
                        int tentativeGCost = current.gCost + neighbor.weight;
                        if (tentativeGCost < neighbor.gCost) {
                            neighbor.parent = current;
                            neighbor.gCost = tentativeGCost;
                            neighbor.hCost = calculateHeuristic(neighbor, end);
                            neighbor.fCost = neighbor.gCost + neighbor.hCost;
                            pq.add(neighbor);
                        }
                    }
                }
                repaint();
                sleep(DELAY);
            }
            if(found) reconstructPath("A*");
            else stopWorking("No Path Found");
        }).start();
    }

    // --- HELPERS ---
    private int calculateHeuristic(Cell a, Cell b) {
        return (Math.abs(a.row - b.row) + Math.abs(a.col - b.col)) * COST_GRASS;
    }

    private void reconstructPath(String algoName) {
        Cell current = end;
        int steps = 0;
        int totalWeight = 0;

        while (current != null) {
            current.isPath = true;
            steps++;
            totalWeight += current.weight;
            current = current.parent;
            repaint();
            sleep(15);
        }

        // Final Status Update
        String result = String.format("[%s] Path Found! Steps: %d | Total Cost: %d", algoName, steps, totalWeight);
        SwingUtilities.invokeLater(() -> statusLabel.setText(result));
        isWorking = false;
    }

    private void stopWorking(String msg) {
        isWorking = false;
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    private void clearSolverState() {
        for(int r=0; r<rows; r++) {
            for(int c=0; c<cols; c++) {
                grid[r][c].visited = false;
                grid[r][c].parent = null;
                grid[r][c].isPath = false;
                grid[r][c].gCost = Integer.MAX_VALUE;
                grid[r][c].fCost = Integer.MAX_VALUE;
            }
        }
        repaint();
    }

    private void addWalls(Cell c, ArrayList<Wall> walls) {
        int r = c.row; int col = c.col;
        if (r > 0 && !grid[r-1][col].visited) walls.add(new Wall(c, grid[r-1][col]));
        if (r < rows-1 && !grid[r+1][col].visited) walls.add(new Wall(c, grid[r+1][col]));
        if (col > 0 && !grid[r][col-1].visited) walls.add(new Wall(c, grid[r][col-1]));
        if (col < cols-1 && !grid[r][col+1].visited) walls.add(new Wall(c, grid[r][col+1]));
    }

    private void removeWall(Cell a, Cell b) {
        if (a.row == b.row) {
            if (a.col < b.col) { a.walls[1] = false; b.walls[3] = false; }
            else { a.walls[3] = false; b.walls[1] = false; }
        } else {
            if (a.row < b.row) { a.walls[2] = false; b.walls[0] = false; }
            else { a.walls[0] = false; b.walls[2] = false; }
        }
    }

    private List<Cell> getNeighbors(Cell c) {
        List<Cell> neighbors = new ArrayList<>();
        int r = c.row; int col = c.col;
        if (!c.walls[0] && r > 0) neighbors.add(grid[r-1][col]);
        if (!c.walls[1] && col < cols-1) neighbors.add(grid[r][col+1]);
        if (!c.walls[2] && r < rows-1) neighbors.add(grid[r+1][col]);
        if (!c.walls[3] && col > 0) neighbors.add(grid[r][col-1]);
        return neighbors;
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].draw(g2, cellSize);
            }
        }

        // Label Start & End
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("S", start.col * cellSize + 10, start.row * cellSize + 20);
        g2.drawString("E", end.col * cellSize + 10, end.row * cellSize + 20);
    }

    private static class Wall {
        Cell from, to;
        public Wall(Cell f, Cell t) { from = f; to = t; }
    }

    private static class Cell {
        int row, col;
        boolean[] walls = {true, true, true, true};
        boolean visited = false;
        boolean isPath = false;
        Cell parent = null;

        int weight = 1;
        int gCost = Integer.MAX_VALUE;
        int hCost = 0;
        int fCost = Integer.MAX_VALUE;

        public Cell(int r, int c) { this.row = r; this.col = c; }

        public void draw(Graphics2D g, int size) {
            int x = col * size;
            int y = row * size;

            // Terrain Colors
            if (weight == MazePanel.COST_WATER) g.setColor(new Color(102, 205, 255)); // Water
            else if (weight == MazePanel.COST_MUD) g.setColor(new Color(205, 133, 63)); // Mud
            else g.setColor(new Color(144, 238, 144)); // Grass
            g.fillRect(x, y, size, size);

            // Visited Animation
            if (visited && !isPath) {
                g.setColor(new Color(255, 255, 255, 120));
                g.fillRect(x, y, size, size);
            }

            // Path
            if (isPath) {
                g.setColor(new Color(220, 20, 60, 200)); // Crimson Red
                g.fillRect(x + 8, y + 8, size - 16, size - 16);
            }

            // Walls
            g.setColor(Color.BLACK);
            if (walls[0]) g.drawLine(x, y, x + size, y);
            if (walls[1]) g.drawLine(x + size, y, x + size, y + size);
            if (walls[2]) g.drawLine(x + size, y + size, x, y + size);
            if (walls[3]) g.drawLine(x, y + size, x, y);
        }
    }
}