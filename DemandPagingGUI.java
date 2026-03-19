import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Main GUI application for Demand Paging simulation.
 * Provides an interactive interface to simulate various page replacement algorithms.
 */
public class DemandPagingGUI extends JFrame {
    private JTextField frameInput, refStringInput;
    private JButton startButton, nextButton, playPauseButton, chartButton, resetButton;
    private JLabel statusLabel, statsLabel, stepLabel, evictedLabel;
    private JPanel framePanel;

    private List<Integer> frames = new ArrayList<>();
    private Queue<Integer> fifoQueue = new LinkedList<>();
    private JLabel[] frameLabels;

    private int[] pages;
    private int frameCount;
    private int currentStep = 0, hits = 0, faults = 0;
    private String algorithm = "FIFO";

    private JComboBox<String> algorithmSelector;

    private javax.swing.Timer autoTimer;
    private boolean isPlaying = false;

    /**
     * Constructs and initializes the GUI.
     */
    public DemandPagingGUI() {
        setTitle("Demand Paging Simulation - FIFO, LRU, MRU, OPT");
        setSize(900, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Simulation Settings"));

        inputPanel.add(new JLabel("Number of Frames:"));
        frameInput = new JTextField("3");
        inputPanel.add(frameInput);

        inputPanel.add(new JLabel("Reference String (space-separated):"));
        refStringInput = new JTextField("7 0 1 2 0 3 0 4 2 3 0 3 2");
        inputPanel.add(refStringInput);

        inputPanel.add(new JLabel("Algorithm:"));
        algorithmSelector = new JComboBox<>(new String[]{"FIFO", "LRU", "MRU", "OPT"});
        algorithmSelector.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(algorithmSelector);

        startButton = new JButton("Start Simulation");
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        inputPanel.add(startButton);

        nextButton = new JButton("Next Step");
        nextButton.setEnabled(false);
        nextButton.setFocusPainted(false);
        inputPanel.add(nextButton);

        playPauseButton = new JButton("Play");
        playPauseButton.setEnabled(false);
        playPauseButton.setBackground(new Color(52, 152, 219));
        playPauseButton.setForeground(Color.WHITE);
        playPauseButton.setFocusPainted(false);
        inputPanel.add(playPauseButton);

        chartButton = new JButton("Show Bar Chart");
        chartButton.setEnabled(false);
        chartButton.setBackground(new Color(155, 89, 182));
        chartButton.setForeground(Color.WHITE);
        chartButton.setFocusPainted(false);
        inputPanel.add(chartButton);

        resetButton = new JButton("Reset");
        resetButton.setBackground(new Color(231, 76, 60));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        inputPanel.add(resetButton);

        add(inputPanel, BorderLayout.NORTH);

        // Center Panel - Frame Display
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Memory Frames"));
        
        framePanel = new JPanel();
        framePanel.setPreferredSize(new Dimension(800, 150));
        centerPanel.add(framePanel, BorderLayout.CENTER);

        evictedLabel = new JLabel("Evicted Page: -", SwingConstants.CENTER);
        evictedLabel.setFont(new Font("Arial", Font.BOLD, 18));
        evictedLabel.setForeground(new Color(192, 57, 43));
        centerPanel.add(evictedLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Info Panel - Statistics
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Simulation Status"));
        
        stepLabel = new JLabel("Step: 0 / 0", SwingConstants.CENTER);
        stepLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        statusLabel = new JLabel("Enter input and click 'Start Simulation'", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        statsLabel = new JLabel("Hits: 0 | Faults: 0 | Hit Ratio: 0.00%", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        infoPanel.add(stepLabel);
        infoPanel.add(statusLabel);
        infoPanel.add(statsLabel);

        add(infoPanel, BorderLayout.SOUTH);

        // Event Listeners
        startButton.addActionListener(e -> startSimulation());
        nextButton.addActionListener(e -> runStep());
        playPauseButton.addActionListener(e -> toggleAutoPlay());
        chartButton.addActionListener(e -> showBarChart());
        resetButton.addActionListener(e -> resetSimulation());

        setVisible(true);
    }

    /**
     * Initializes and starts a new simulation.
     */
    private void startSimulation() {
        try {
            frameCount = Integer.parseInt(frameInput.getText().trim());
            if (frameCount <= 0) {
                throw new IllegalArgumentException("Frame count must be positive");
            }
            
            String[] parts = refStringInput.getText().trim().split("\\s+");
            pages = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            
            if (pages.length == 0) {
                throw new IllegalArgumentException("Reference string cannot be empty");
            }
            
            algorithm = (String) algorithmSelector.getSelectedItem();

            frames.clear();
            fifoQueue.clear();
            currentStep = 0;
            hits = 0;
            faults = 0;

            // Initialize frame display
            framePanel.removeAll();
            framePanel.setLayout(new GridLayout(1, frameCount, 15, 10));
            frameLabels = new JLabel[frameCount];
            
            for (int i = 0; i < frameCount; i++) {
                frameLabels[i] = new JLabel("-", SwingConstants.CENTER);
                frameLabels[i].setFont(new Font("Arial", Font.BOLD, 32));
                frameLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                frameLabels[i].setOpaque(true);
                frameLabels[i].setBackground(Color.WHITE);
                frameLabels[i].setPreferredSize(new Dimension(100, 100));
                framePanel.add(frameLabels[i]);
            }

            framePanel.revalidate();
            framePanel.repaint();

            stepLabel.setText("Step: 0 / " + pages.length);
            statusLabel.setText("Simulation ready. Click 'Next Step' or 'Play'.");
            statsLabel.setText("Hits: 0 | Faults: 0 | Hit Ratio: 0.00%");
            evictedLabel.setText("Evicted Page: -");

            nextButton.setEnabled(true);
            playPauseButton.setEnabled(true);
            playPauseButton.setText("Play");
            isPlaying = false;
            chartButton.setEnabled(false);

            if (autoTimer != null && autoTimer.isRunning()) {
                autoTimer.stop();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Invalid input: " + ex.getMessage() + "\nPlease enter valid integers.", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Executes one step of the simulation.
     */
    private void runStep() {
        if (currentStep >= pages.length) {
            statusLabel.setText("Simulation complete!");
            nextButton.setEnabled(false);
            playPauseButton.setEnabled(false);
            chartButton.setEnabled(true);

            if (autoTimer != null && autoTimer.isRunning()) {
                autoTimer.stop();
                isPlaying = false;
                playPauseButton.setText("Play");
            }

            double hitRatio = (hits / (double) pages.length) * 100;
            double missRatio = (faults / (double) pages.length) * 100;

            String summary = String.format(
                "Simulation Finished!\n\n" +
                "Algorithm: %s\n" +
                "Total References: %d\n" +
                "Hits: %d\n" +
                "Faults: %d\n" +
                "Hit Ratio: %.2f%%\n" +
                "Miss Ratio: %.2f%%",
                algorithm, pages.length, hits, faults, hitRatio, missRatio
            );

            JOptionPane.showMessageDialog(this, summary, "Final Statistics", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int page = pages[currentStep];
        boolean hit = frames.contains(page);

        // Reset colors
        for (JLabel label : frameLabels) {
            label.setBackground(Color.WHITE);
        }

        if (hit) {
            hits++;
            if (algorithm.equals("LRU") || algorithm.equals("MRU")) {
                frames.remove(Integer.valueOf(page));
                frames.add(page);
            }
            statusLabel.setText("✓ HIT → Page " + page + " is already in memory");
            statusLabel.setForeground(new Color(39, 174, 96));
            evictedLabel.setText("Evicted Page: -");
        } else {
            faults++;
            statusLabel.setText("✗ FAULT → Page " + page + " not in memory");
            statusLabel.setForeground(new Color(192, 57, 43));
            
            if (frames.size() < frameCount) {
                frames.add(page);
                evictedLabel.setText("Evicted Page: - (frame available)");
            } else {
                int indexToRemove = switch (algorithm) {
                    case "FIFO" -> frames.indexOf(fifoQueue.poll());
                    case "OPT" -> findOptEviction(frames, pages, currentStep);
                    case "MRU" -> frames.size() - 1;
                    case "LRU" -> 0;
                    default -> 0;
                };
                int evicted = frames.get(indexToRemove);
                frames.remove(indexToRemove);
                frames.add(page);
                evictedLabel.setText("Evicted Page: " + evicted);
                if (!algorithm.equals("FIFO")) {
                    fifoQueue.remove(evicted);
                }
            }
            fifoQueue.add(page);
        }

        // Update frame display
        for (int i = 0; i < frameCount; i++) {
            if (i < frames.size()) {
                int val = frames.get(i);
                frameLabels[i].setText(String.valueOf(val));
                if (val == page) {
                    frameLabels[i].setBackground(hit ? new Color(46, 204, 113) : new Color(231, 76, 60));
                    frameLabels[i].setForeground(Color.WHITE);
                } else {
                    frameLabels[i].setForeground(Color.BLACK);
                }
            } else {
                frameLabels[i].setText("-");
                frameLabels[i].setBackground(Color.WHITE);
            }
        }

        currentStep++;
        stepLabel.setText("Step: " + currentStep + " / " + pages.length);
        double hitRatio = (hits / (double) currentStep) * 100;
        statsLabel.setText(String.format("Hits: %d | Faults: %d | Hit Ratio: %.2f%%",
                hits, faults, hitRatio));
    }

    /**
     * Displays a bar chart comparing all algorithms.
     */
    private void showBarChart() {
        int[] faultResults = {
            DemandPaging.simulate("FIFO", pages, frameCount),
            DemandPaging.simulate("LRU", pages, frameCount),
            DemandPaging.simulate("MRU", pages, frameCount),
            DemandPaging.simulate("OPT", pages, frameCount)
        };
        String[] algos = {"FIFO", "LRU", "MRU", "OPT"};

        // Sort by fault count (ascending)
        Integer[] indices = IntStream.range(0, faultResults.length).boxed()
                .sorted(Comparator.comparingInt(i -> faultResults[i]))
                .toArray(Integer[]::new);

        int[] sortedFaults = new int[faultResults.length];
        String[] sortedAlgos = new String[algos.length];
        for (int i = 0; i < indices.length; i++) {
            sortedFaults[i] = faultResults[indices[i]];
            sortedAlgos[i] = algos[indices[i]];
        }

        new BarChartFrame(sortedAlgos, sortedFaults);
    }

    /**
     * Toggles automatic step-through mode.
     */
    private void toggleAutoPlay() {
        if (isPlaying) {
            autoTimer.stop();
            playPauseButton.setText("Play");
            playPauseButton.setBackground(new Color(52, 152, 219));
            isPlaying = false;
        } else {
            autoTimer = new javax.swing.Timer(1000, e -> runStep());
            autoTimer.start();
            playPauseButton.setText("Pause");
            playPauseButton.setBackground(new Color(230, 126, 34));
            isPlaying = true;
        }
    }

    /**
     * Resets the simulation to initial state.
     */
    private void resetSimulation() {
        if (autoTimer != null && autoTimer.isRunning()) {
            autoTimer.stop();
        }
        
        frames.clear();
        fifoQueue.clear();
        currentStep = 0;
        hits = 0;
        faults = 0;
        isPlaying = false;
        
        framePanel.removeAll();
        framePanel.revalidate();
        framePanel.repaint();
        
        stepLabel.setText("Step: 0 / 0");
        statusLabel.setText("Enter input and click 'Start Simulation'");
        statusLabel.setForeground(Color.BLACK);
        statsLabel.setText("Hits: 0 | Faults: 0 | Hit Ratio: 0.00%");
        evictedLabel.setText("Evicted Page: -");
        
        nextButton.setEnabled(false);
        playPauseButton.setEnabled(false);
        playPauseButton.setText("Play");
        playPauseButton.setBackground(new Color(52, 152, 219));
        chartButton.setEnabled(false);
    }

    /**
     * Finds the optimal page to evict using the OPT algorithm.
     */
    private int findOptEviction(List<Integer> frames, int[] pages, int currentIndex) {
        int indexToRemove = 0;
        int farthest = -1;
        
        for (int j = 0; j < frames.size(); j++) {
            int curr = frames.get(j);
            int nextUse = Integer.MAX_VALUE;
            
            for (int k = currentIndex + 1; k < pages.length; k++) {
                if (pages[k] == curr) {
                    nextUse = k;
                    break;
                }
            }
            
            if (nextUse > farthest) {
                farthest = nextUse;
                indexToRemove = j;
            }
        }
        
        return indexToRemove;
    }

    /**
     * Main entry point of the application.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(DemandPagingGUI::new);
    }
}
