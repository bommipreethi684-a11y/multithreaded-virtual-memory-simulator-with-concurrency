import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Main GUI for Multithreaded Demand Paging Simulation with Concurrency.
 * Features tabbed interface for configuration, simulation, and statistics.
 */
public class MultiThreadGUI extends JFrame {
    
    // Core components
    private MultiThreadSimulator simulator;
    private ThreadScheduler scheduler;
    private List<ProcessThread> threads;
    
    // GUI Components
    private JTabbedPane tabbedPane;
    
    // Tab 1: Configuration
    private JSpinner threadCountSpinner;
    private JComboBox<String> schedulingCombo;
    private JSlider timeQuantumSlider;
    private JLabel quantumLabel;
    private JComboBox<String> syncTypeCombo;
    private JSpinner semaphorePermitsSpinner;
    private JComboBox<String> pageAlgoCombo;
    private JSpinner frameCountSpinner;
    private JComboBox<String> scenarioCombo;
    private JPanel threadConfigPanel;
    private List<ThreadConfigRow> threadRows;
    
    // Tab 2: Simulation
    private JPanel frameDisplayPanel;
    private JPanel threadStatusPanel;
    private JPanel timelinePanel;
    private JPanel lockStatusPanel;
    private JTextArea logArea;
    private JButton startButton, pauseButton, stepButton, resetButton;
    private JLabel stepLabel;
    private JProgressBar progressBar;
    
    // Tab 3: Statistics
    private JTextArea statsArea;
    private JPanel chartPanel;
    
    // Simulation state
    private javax.swing.Timer simulationTimer;
    private boolean isSimulationRunning;
    
    /**
     * Represents a row for configuring a thread.
     */
    private class ThreadConfigRow {
        JTextField refStringField;
        JSpinner prioritySpinner;
        JPanel panel;
        
        ThreadConfigRow(int threadNum) {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setBorder(BorderFactory.createEtchedBorder());
            
            panel.add(new JLabel("Thread-" + threadNum + ":"));
            panel.add(new JLabel("  Ref String:"));
            refStringField = new JTextField("1 2 3 4 5", 15);
            panel.add(refStringField);
            
            panel.add(new JLabel("  Priority:"));
            prioritySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
            panel.add(prioritySpinner);
        }
    }
    
    /**
     * Constructor - Initializes the GUI.
     */
    public MultiThreadGUI() {
        setTitle("Multithreaded Demand Paging Simulator with Concurrency");
        setSize(1200, 800);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        threads = new ArrayList<>();
        threadRows = new ArrayList<>();
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Add tabs
        tabbedPane.addTab("⚙️ Configuration", createConfigurationTab());
        tabbedPane.addTab("▶️ Simulation", createSimulationTab());
        tabbedPane.addTab("📊 Statistics", createStatisticsTab());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        setVisible(true);
    }
    
    /**
     * Creates the Configuration tab.
     */
    private JPanel createConfigurationTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Top panel - Global settings
        JPanel globalPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        globalPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLUE, 2), 
            "Global Simulation Settings",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        
        // Number of threads
        globalPanel.add(new JLabel("Number of Threads (3-8):"));
        threadCountSpinner = new JSpinner(new SpinnerNumberModel(3, 3, 8, 1));
        threadCountSpinner.addChangeListener(e -> updateThreadConfigPanel());
        globalPanel.add(threadCountSpinner);
        
        // Scheduling algorithm
        globalPanel.add(new JLabel("Scheduling Algorithm:"));
        schedulingCombo = new JComboBox<>(new String[]{"FCFS", "Round-Robin", "Priority"});
        schedulingCombo.addActionListener(e -> updateQuantumVisibility());
        globalPanel.add(schedulingCombo);
        
        // Time quantum
        globalPanel.add(new JLabel("Time Quantum (for Round-Robin):"));
        JPanel quantumPanel = new JPanel(new BorderLayout());
        timeQuantumSlider = new JSlider(1, 10, 3);
        timeQuantumSlider.setMajorTickSpacing(1);
        timeQuantumSlider.setPaintTicks(true);
        timeQuantumSlider.setPaintLabels(true);
        timeQuantumSlider.addChangeListener(e -> quantumLabel.setText("Value: " + timeQuantumSlider.getValue()));
        quantumPanel.add(timeQuantumSlider, BorderLayout.CENTER);
        quantumLabel = new JLabel("Value: 3");
        quantumLabel.setFont(new Font("Arial", Font.BOLD, 12));
        quantumPanel.add(quantumLabel, BorderLayout.EAST);
        globalPanel.add(quantumPanel);
        
        // Synchronization type
        globalPanel.add(new JLabel("Synchronization Type:"));
        syncTypeCombo = new JComboBox<>(new String[]{"None", "Mutex", "Semaphore"});
        syncTypeCombo.addActionListener(e -> updateSemaphoreVisibility());
        globalPanel.add(syncTypeCombo);
        
        // Semaphore permits
        globalPanel.add(new JLabel("Semaphore Permits (if enabled):"));
        semaphorePermitsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 5, 1));
        globalPanel.add(semaphorePermitsSpinner);
        
        // Page replacement algorithm
        globalPanel.add(new JLabel("Page Replacement Algorithm:"));
        pageAlgoCombo = new JComboBox<>(new String[]{"FIFO", "LRU", "MRU", "OPT"});
        globalPanel.add(pageAlgoCombo);
        
        // Frame count
        globalPanel.add(new JLabel("Number of Memory Frames:"));
        frameCountSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
        globalPanel.add(frameCountSpinner);
        
        // Scenario loader
        globalPanel.add(new JLabel("Load Pre-configured Scenario:"));
        JPanel scenarioPanel = new JPanel(new BorderLayout());
        scenarioCombo = new JComboBox<>(new String[]{
            "Custom",
            "Race Condition Demo",
            "Deadlock Scenario",
            "Priority Inversion",
            "Starvation Example",
            "Thrashing Simulation"
        });
        scenarioPanel.add(scenarioCombo, BorderLayout.CENTER);
        JButton loadScenarioButton = new JButton("Load");
        loadScenarioButton.addActionListener(e -> loadScenario());
        scenarioPanel.add(loadScenarioButton, BorderLayout.EAST);
        globalPanel.add(scenarioPanel);
        
        mainPanel.add(globalPanel, BorderLayout.NORTH);
        
        // Center panel - Thread configuration
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GREEN, 2),
            "Thread Configuration",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        
        threadConfigPanel = new JPanel();
        threadConfigPanel.setLayout(new BoxLayout(threadConfigPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(threadConfigPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Initialize thread config rows
        updateThreadConfigPanel();
        updateQuantumVisibility();
        updateSemaphoreVisibility();
        
        return mainPanel;
    }
    
    /**
     * Creates the Simulation tab.
     */
    private JPanel createSimulationTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createEtchedBorder());
        
        startButton = new JButton("▶ Start");
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.addActionListener(e -> startSimulation());
        controlPanel.add(startButton);
        
        pauseButton = new JButton("⏸ Pause");
        pauseButton.setBackground(new Color(230, 126, 34));
        pauseButton.setForeground(Color.WHITE);
        pauseButton.setFont(new Font("Arial", Font.BOLD, 14));
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(e -> togglePause());
        controlPanel.add(pauseButton);
        
        stepButton = new JButton("⏭ Step");
        stepButton.setBackground(new Color(52, 152, 219));
        stepButton.setForeground(Color.WHITE);
        stepButton.setFont(new Font("Arial", Font.BOLD, 14));
        stepButton.setEnabled(false);
        stepButton.addActionListener(e -> executeStep());
        controlPanel.add(stepButton);
        
        resetButton = new JButton("🔄 Reset");
        resetButton.setBackground(new Color(231, 76, 60));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFont(new Font("Arial", Font.BOLD, 14));
        resetButton.addActionListener(e -> resetSimulation());
        controlPanel.add(resetButton);
        
        controlPanel.add(Box.createHorizontalStrut(30));
        
        stepLabel = new JLabel("Step: 0 / 0");
        stepLabel.setFont(new Font("Arial", Font.BOLD, 14));
        controlPanel.add(stepLabel);
        
        controlPanel.add(Box.createHorizontalStrut(20));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(200, 25));
        progressBar.setStringPainted(true);
        controlPanel.add(progressBar);
        
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Center split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        
        // Top section - Visualization
        JPanel visualPanel = new JPanel(new BorderLayout(5, 5));
        
        // Memory frames
        JPanel framesPanel = new JPanel(new BorderLayout());
        framesPanel.setBorder(BorderFactory.createTitledBorder("Shared Memory Frames"));
        frameDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        frameDisplayPanel.setBackground(Color.WHITE);
        framesPanel.add(new JScrollPane(frameDisplayPanel), BorderLayout.CENTER);
        visualPanel.add(framesPanel, BorderLayout.NORTH);
        
        // Thread status and locks
        JPanel statusPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        
        threadStatusPanel = new JPanel();
        threadStatusPanel.setLayout(new BoxLayout(threadStatusPanel, BoxLayout.Y_AXIS));
        threadStatusPanel.setBackground(Color.WHITE);
        JScrollPane threadScroll = new JScrollPane(threadStatusPanel);
        threadScroll.setBorder(BorderFactory.createTitledBorder("Thread Status"));
        statusPanel.add(threadScroll);
        
        lockStatusPanel = new JPanel();
        lockStatusPanel.setLayout(new BoxLayout(lockStatusPanel, BoxLayout.Y_AXIS));
        lockStatusPanel.setBackground(Color.WHITE);
        JScrollPane lockScroll = new JScrollPane(lockStatusPanel);
        lockScroll.setBorder(BorderFactory.createTitledBorder("Lock Status"));
        statusPanel.add(lockScroll);
        
        visualPanel.add(statusPanel, BorderLayout.CENTER);
        
        splitPane.setTopComponent(visualPanel);
        
        // Bottom section - Log and timeline
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Execution Log"));
        bottomPanel.add(logScroll, BorderLayout.CENTER);
        
        splitPane.setBottomComponent(bottomPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Creates the Statistics tab.
     */
    private JPanel createStatisticsTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Statistics text area
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(statsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Detailed Statistics"));
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Chart panel
        chartPanel = new JPanel();
        chartPanel.setBorder(BorderFactory.createTitledBorder("Performance Comparison"));
        chartPanel.setPreferredSize(new Dimension(1180, 250));
        chartPanel.setBackground(Color.WHITE);
        
        mainPanel.add(chartPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    /**
     * Updates thread configuration panel based on thread count.
     */
    private void updateThreadConfigPanel() {
        int count = (int) threadCountSpinner.getValue();
        threadConfigPanel.removeAll();
        threadRows.clear();
        
        for (int i = 1; i <= count; i++) {
            ThreadConfigRow row = new ThreadConfigRow(i);
            threadRows.add(row);
            threadConfigPanel.add(row.panel);
        }
        
        threadConfigPanel.revalidate();
        threadConfigPanel.repaint();
    }
    
    /**
     * Updates time quantum slider visibility.
     */
    private void updateQuantumVisibility() {
        boolean isRoundRobin = "Round-Robin".equals(schedulingCombo.getSelectedItem());
        timeQuantumSlider.setEnabled(isRoundRobin);
        quantumLabel.setEnabled(isRoundRobin);
    }
    
    /**
     * Updates semaphore spinner visibility.
     */
    private void updateSemaphoreVisibility() {
        boolean isSemaphore = "Semaphore".equals(syncTypeCombo.getSelectedItem());
        semaphorePermitsSpinner.setEnabled(isSemaphore);
    }
    
    /**
     * Loads a pre-configured scenario.
     */
    private void loadScenario() {
        String scenario = (String) scenarioCombo.getSelectedItem();
        
        switch (scenario) {
            case "Race Condition Demo":
                loadRaceConditionScenario();
                break;
            case "Deadlock Scenario":
                loadDeadlockScenario();
                break;
            case "Priority Inversion":
                loadPriorityInversionScenario();
                break;
            case "Starvation Example":
                loadStarvationScenario();
                break;
            case "Thrashing Simulation":
                loadThrashingScenario();
                break;
        }
    }
    
    private void loadRaceConditionScenario() {
        threadCountSpinner.setValue(3);
        updateThreadConfigPanel();
        threadRows.get(0).refStringField.setText("1 2 3 1 2");
        threadRows.get(0).prioritySpinner.setValue(5);
        threadRows.get(1).refStringField.setText("1 2 3 1 2");
        threadRows.get(1).prioritySpinner.setValue(5);
        threadRows.get(2).refStringField.setText("1 2 3 1 2");
        threadRows.get(2).prioritySpinner.setValue(5);
        schedulingCombo.setSelectedItem("FCFS");
        syncTypeCombo.setSelectedItem("None");
        frameCountSpinner.setValue(3);
        JOptionPane.showMessageDialog(this, 
            "Race Condition Scenario Loaded!\nAll threads access same pages without synchronization.",
            "Scenario Loaded", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void loadDeadlockScenario() {
        threadCountSpinner.setValue(3);
        updateThreadConfigPanel();
        threadRows.get(0).refStringField.setText("1 2 3 4 5");
        threadRows.get(0).prioritySpinner.setValue(5);
        threadRows.get(1).refStringField.setText("5 4 3 2 1");
        threadRows.get(1).prioritySpinner.setValue(5);
        threadRows.get(2).refStringField.setText("2 3 4 5 1");
        threadRows.get(2).prioritySpinner.setValue(5);
        schedulingCombo.setSelectedItem("FCFS");
        syncTypeCombo.setSelectedItem("Mutex");
        frameCountSpinner.setValue(3);
        JOptionPane.showMessageDialog(this,
            "Deadlock Scenario Loaded!\nThreads may create circular wait with mutexes.",
            "Scenario Loaded", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void loadPriorityInversionScenario() {
        threadCountSpinner.setValue(3);
        updateThreadConfigPanel();
        threadRows.get(0).refStringField.setText("1 2 3 4 5");
        threadRows.get(0).prioritySpinner.setValue(10);
        threadRows.get(1).refStringField.setText("2 3 4 5 6");
        threadRows.get(1).prioritySpinner.setValue(5);
        threadRows.get(2).refStringField.setText("3 4 5 6 7");
        threadRows.get(2).prioritySpinner.setValue(1);
        schedulingCombo.setSelectedItem("Priority");
        syncTypeCombo.setSelectedItem("Mutex");
        frameCountSpinner.setValue(4);
        JOptionPane.showMessageDialog(this,
            "Priority Inversion Scenario Loaded!\nHigh priority thread blocked by low priority thread.",
            "Scenario Loaded", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void loadStarvationScenario() {
        threadCountSpinner.setValue(4);
        updateThreadConfigPanel();
        threadRows.get(0).refStringField.setText("1 2 3 4 5 6 7");
        threadRows.get(0).prioritySpinner.setValue(10);
        threadRows.get(1).refStringField.setText("2 3 4 5 6 7 8");
        threadRows.get(1).prioritySpinner.setValue(8);
        threadRows.get(2).refStringField.setText("3 4 5 6 7 8 9");
        threadRows.get(2).prioritySpinner.setValue(6);
        threadRows.get(3).refStringField.setText("4 5 6 7 8 9 10");
        threadRows.get(3).prioritySpinner.setValue(1);
        schedulingCombo.setSelectedItem("Priority");
        syncTypeCombo.setSelectedItem("None");
        frameCountSpinner.setValue(4);
        JOptionPane.showMessageDialog(this,
            "Starvation Scenario Loaded!\nLow priority thread may never get CPU time.",
            "Scenario Loaded", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void loadThrashingScenario() {
        threadCountSpinner.setValue(6);
        updateThreadConfigPanel();
        for (int i = 0; i < 6; i++) {
            threadRows.get(i).refStringField.setText("1 2 3 4 5 6 7 8 9 10");
            threadRows.get(i).prioritySpinner.setValue(5);
        }
        schedulingCombo.setSelectedItem("Round-Robin");
        timeQuantumSlider.setValue(2);
        syncTypeCombo.setSelectedItem("None");
        frameCountSpinner.setValue(3);
        JOptionPane.showMessageDialog(this,
            "Thrashing Scenario Loaded!\nMany threads competing for few frames causes excessive page faults.",
            "Scenario Loaded", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Starts the simulation.
     */
    private void startSimulation() {
        try {
            // Parse thread configurations
            threads.clear();
            for (ThreadConfigRow row : threadRows) {
                String refString = row.refStringField.getText().trim();
                int priority = (int) row.prioritySpinner.getValue();
                
                String[] parts = refString.split("\\s+");
                int[] pages = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
                
                ProcessThread thread = new ProcessThread(pages, priority);
                threads.add(thread);
            }
            
            // Create scheduler
            String schedAlgo = (String) schedulingCombo.getSelectedItem();
            ThreadScheduler.SchedulingAlgorithm algorithm = ThreadScheduler.SchedulingAlgorithm.valueOf(
                schedAlgo.toUpperCase().replace("-", "_")
            );
            int quantum = timeQuantumSlider.getValue();
            scheduler = new ThreadScheduler(algorithm, quantum);
            
            // Create simulator
            int frames = (int) frameCountSpinner.getValue();
            String pageAlgo = (String) pageAlgoCombo.getSelectedItem();
            simulator = new MultiThreadSimulator(frames, pageAlgo);
            
            // Initialize simulation
            String syncType = (String) syncTypeCombo.getSelectedItem();
            boolean useSync = !"None".equals(syncType);
            int semPermits = (int) semaphorePermitsSpinner.getValue();
            simulator.initialize(threads, scheduler, useSync, syncType.toUpperCase(), semPermits);
            
            // Switch to simulation tab
            tabbedPane.setSelectedIndex(1);
            
            // Update UI
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stepButton.setEnabled(true);
            isSimulationRunning = true;
            
            // Initialize visualization
            updateVisualization();
            
            // Start automatic execution
            simulationTimer = new javax.swing.Timer(500, e -> {
                if (!simulator.executeStep()) {
                    stopSimulation();
                } else {
                    updateVisualization();
                }
            });
            simulationTimer.start();
            
            logArea.append("Simulation started!\n");
            logArea.append("Algorithm: " + schedAlgo + ", Sync: " + syncType + "\n\n");
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error starting simulation: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    /**
     * Toggles pause/resume.
     */
    private void togglePause() {
        if (simulator == null) return;
        
        boolean isPaused = simulator.isPaused();
        simulator.setPaused(!isPaused);
        
        if (!isPaused) {
            simulationTimer.stop();
            pauseButton.setText("▶ Resume");
            logArea.append("Simulation paused.\n");
        } else {
            simulationTimer.start();
            pauseButton.setText("⏸ Pause");
            logArea.append("Simulation resumed.\n");
        }
    }
    
    /**
     * Executes one simulation step.
     */
    private void executeStep() {
        if (simulator == null) return;
        
        if (!simulator.executeStep()) {
            stopSimulation();
        } else {
            updateVisualization();
        }
    }
    
    /**
     * Stops the simulation.
     */
    private void stopSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        isSimulationRunning = false;
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stepButton.setEnabled(false);
        
        if (simulator.isDeadlockDetected()) {
            logArea.append("\n⚠️ DEADLOCK DETECTED! ⚠️\n");
            logArea.append("Deadlocked threads: ");
            for (ProcessThread t : simulator.getDeadlockedThreads()) {
                logArea.append(t.getThreadName() + " ");
            }
            logArea.append("\n");
            
            JOptionPane.showMessageDialog(this,
                "Deadlock detected! Check the execution log for details.",
                "Deadlock Detected", JOptionPane.WARNING_MESSAGE);
        } else {
            logArea.append("\nSimulation completed successfully!\n");
        }
        
        updateStatistics();
    }
    
    /**
     * Resets the simulation.
     */
    private void resetSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        
        simulator = null;
        scheduler = null;
        isSimulationRunning = false;
        
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stepButton.setEnabled(false);
        
        frameDisplayPanel.removeAll();
        threadStatusPanel.removeAll();
        lockStatusPanel.removeAll();
        logArea.setText("");
        statsArea.setText("");
        stepLabel.setText("Step: 0 / 0");
        progressBar.setValue(0);
        
        frameDisplayPanel.revalidate();
        frameDisplayPanel.repaint();
        threadStatusPanel.revalidate();
        threadStatusPanel.repaint();
        lockStatusPanel.revalidate();
        lockStatusPanel.repaint();
        
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * Updates visualization panels.
     */
    private void updateVisualization() {
        if (simulator == null) return;
        
        // Update frames
        frameDisplayPanel.removeAll();
        for (MultiThreadSimulator.FrameEntry entry : simulator.getFrames()) {
            JPanel frameBox = new JPanel(new BorderLayout());
            frameBox.setPreferredSize(new Dimension(80, 80));
            frameBox.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            frameBox.setBackground(getThreadColor(entry.owner));
            
            JLabel pageLabel = new JLabel(String.valueOf(entry.pageNumber), SwingConstants.CENTER);
            pageLabel.setFont(new Font("Arial", Font.BOLD, 24));
            pageLabel.setForeground(Color.WHITE);
            frameBox.add(pageLabel, BorderLayout.CENTER);
            
            JLabel ownerLabel = new JLabel(entry.owner.getThreadName(), SwingConstants.CENTER);
            ownerLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            ownerLabel.setForeground(Color.WHITE);
            frameBox.add(ownerLabel, BorderLayout.SOUTH);
            
            frameDisplayPanel.add(frameBox);
        }
        frameDisplayPanel.revalidate();
        frameDisplayPanel.repaint();
        
        // Update thread status
        threadStatusPanel.removeAll();
        for (ProcessThread thread : simulator.getThreads()) {
            JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            // State indicator
            JLabel stateLabel = new JLabel(getStateEmoji(thread.getState()));
            stateLabel.setFont(new Font("Arial", Font.PLAIN, 20));
            statusRow.add(stateLabel);
            
            // Thread info
            JLabel infoLabel = new JLabel(String.format("%s: %s [%d/%d] H:%d F:%d",
                thread.getThreadName(),
                thread.getState(),
                thread.getCurrentIndex(),
                thread.getReferenceString().length,
                thread.getPageHits(),
                thread.getPageFaults()
            ));
            infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
            statusRow.add(infoLabel);
            
            threadStatusPanel.add(statusRow);
        }
        threadStatusPanel.revalidate();
        threadStatusPanel.repaint();
        
        // Update lock status
        lockStatusPanel.removeAll();
        for (LockResource lock : simulator.getLocks()) {
            JPanel lockRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            String lockIcon = lock.isAvailable() ? "🔓" : "🔒";
            JLabel lockLabel = new JLabel(lockIcon + " " + lock.toString());
            lockLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
            lockRow.add(lockLabel);
            
            lockStatusPanel.add(lockRow);
        }
        lockStatusPanel.revalidate();
        lockStatusPanel.repaint();
        
        // Update progress
        int current = simulator.getCurrentStep();
        int total = simulator.getTotalSteps();
        stepLabel.setText("Step: " + current + " / " + total);
        progressBar.setValue(total > 0 ? (current * 100 / total) : 0);
        
        // Update log with recent timeline events
        List<MultiThreadSimulator.TimelineEvent> timeline = simulator.getTimeline();
        if (!timeline.isEmpty()) {
            MultiThreadSimulator.TimelineEvent lastEvent = timeline.get(timeline.size() - 1);
            logArea.append(String.format("[Step %d] %s: %s - %s\n",
                lastEvent.step,
                lastEvent.thread.getThreadName(),
                lastEvent.event,
                lastEvent.details
            ));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
    
    /**
     * Updates statistics panel.
     */
    private void updateStatistics() {
        if (simulator == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(60)).append("\n");
        sb.append("        MULTITHREADED SIMULATION STATISTICS\n");
        sb.append("=".repeat(60)).append("\n\n");
        
        // Global statistics
        sb.append("GLOBAL STATISTICS:\n");
        sb.append("-".repeat(60)).append("\n");
        sb.append(String.format("Total Steps: %d\n", simulator.getCurrentStep()));
        sb.append(String.format("Total Context Switches: %d\n", scheduler.getTotalContextSwitches()));
        
        int totalFaults = simulator.getThreads().stream().mapToInt(ProcessThread::getPageFaults).sum();
        int totalHits = simulator.getThreads().stream().mapToInt(ProcessThread::getPageHits).sum();
        double globalHitRatio = totalHits * 100.0 / (totalHits + totalFaults);
        
        sb.append(String.format("Total Page Faults: %d\n", totalFaults));
        sb.append(String.format("Total Page Hits: %d\n", totalHits));
        sb.append(String.format("Global Hit Ratio: %.2f%%\n\n", globalHitRatio));
        
        // Per-thread statistics
        sb.append("PER-THREAD STATISTICS:\n");
        sb.append("-".repeat(60)).append("\n");
        
        for (ProcessThread thread : simulator.getThreads()) {
            sb.append(String.format("\n%s (Priority: %d):\n", thread.getThreadName(), thread.getPriority()));
            sb.append(String.format("  Reference String Length: %d\n", thread.getReferenceString().length));
            sb.append(String.format("  Page Faults: %d\n", thread.getPageFaults()));
            sb.append(String.format("  Page Hits: %d\n", thread.getPageHits()));
            
            int threadTotal = thread.getPageFaults() + thread.getPageHits();
            double hitRatio = threadTotal > 0 ? (thread.getPageHits() * 100.0 / threadTotal) : 0;
            sb.append(String.format("  Hit Ratio: %.2f%%\n", hitRatio));
            sb.append(String.format("  Context Switches: %d\n", thread.getContextSwitches()));
            sb.append(String.format("  State: %s\n", thread.getState()));
        }
        
        // Lock statistics
        if (!simulator.getLocks().isEmpty()) {
            sb.append("\n\nLOCK STATISTICS:\n");
            sb.append("-".repeat(60)).append("\n");
            for (LockResource lock : simulator.getLocks()) {
                sb.append(String.format("%s: %d acquisitions\n", 
                    lock.getLockName(), lock.getAcquisitionCount()));
            }
        }
        
        // Deadlock info
        if (simulator.isDeadlockDetected()) {
            sb.append("\n\n⚠️ DEADLOCK DETECTED ⚠️\n");
            sb.append("-".repeat(60)).append("\n");
            sb.append("Deadlocked threads: ");
            for (ProcessThread t : simulator.getDeadlockedThreads()) {
                sb.append(t.getThreadName()).append(" ");
            }
            sb.append("\n");
        }
        
        statsArea.setText(sb.toString());
        
        // Switch to statistics tab
        tabbedPane.setSelectedIndex(2);
    }
    
    /**
     * Gets color for a thread.
     */
    private Color getThreadColor(ProcessThread thread) {
        Color[] colors = {
            new Color(52, 152, 219),   // Blue
            new Color(46, 204, 113),   // Green
            new Color(155, 89, 182),   // Purple
            new Color(230, 126, 34),   // Orange
            new Color(231, 76, 60),    // Red
            new Color(26, 188, 156),   // Turquoise
            new Color(241, 196, 15),   // Yellow
            new Color(149, 165, 166)   // Gray
        };
        return colors[thread.getThreadId() % colors.length];
    }
    
    /**
     * Gets emoji for thread state.
     */
    private String getStateEmoji(ProcessThread.State state) {
        switch (state) {
            case RUNNING: return "🟢";
            case READY: return "⚪";
            case WAITING: return "🟡";
            case BLOCKED: return "🔴";
            case COMPLETED: return "✅";
            default: return "⚫";
        }
    }
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(MultiThreadGUI::new);
    }
}
