import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimulationDashboard extends JFrame {
    private final SimulationStore store = new SimulationStore();

    private DefaultTableModel managerTableModel;
    private DefaultTableModel pageMetricsModel;
    private DefaultTableModel pageStepModel;
    private DefaultTableModel cpuMetricsModel;

    private JTextField managerNameField;
    private JComboBox<String> managerCategory;
    private JTextField managerAlgorithmField;
    private JTextField managerSummaryField;

    private JTextField refStringField;
    private JSpinner frameCountSpinner;
    private JComboBox<String> stepAlgoCombo;
    private JLabel pageSummaryLabel;
    private SimpleBarChartPanel pageChartPanel;

    private JTextArea processInputArea;
    private JSpinner quantumSpinner;
    private JTextArea ganttOutputArea;
    private JLabel cpuSummaryLabel;
    private SimpleBarChartPanel cpuChartPanel;

    private JComboBox<String> aiModeCombo;
    private JComboBox<String> aiDifficultyCombo;
    private JTextArea aiOutputArea;

    private final List<PageReplacementAnalyzer.PageMetrics> lastPageMetrics = new ArrayList<>();
    private final List<CpuSchedulingAnalyzer.CpuMetrics> lastCpuMetrics = new ArrayList<>();
    private AIResearchAssistant aiAssistant;

    public SimulationDashboard() {
        setTitle("Integrated Simulation Dashboard - Existing Project + Analysis");
        setSize(1300, 840);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Launch Existing Simulators", createLauncherTab());
        tabs.addTab("Simulation Manager", createManagerTab());
        tabs.addTab("Page Replacement Analysis", createPageAnalysisTab());
        tabs.addTab("CPU Scheduling Analysis", createCpuAnalysisTab());
        tabs.addTab("AI Research Copilot", createAITab());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createLauncherTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel info = new JPanel(new GridLayout(4, 1, 8, 8));
        info.setBorder(BorderFactory.createTitledBorder("Integrated Existing Project Tools"));
        info.add(new JLabel("Use this dashboard to run analysis and launch your original simulators."));
        info.add(new JLabel("Original files integrated: DemandPagingGUI, MultiThreadGUI, DemandPaging, ThreadScheduler."));
        info.add(new JLabel("All simulation records are managed in-memory with Java Collections."));
        info.add(new JLabel("AI integrated with Gemini 3 Flash for generation, explanation, recommendation, anomaly checks."));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        JButton openDemandPaging = new JButton("Open Existing DemandPagingGUI");
        openDemandPaging.addActionListener(e -> SwingUtilities.invokeLater(DemandPagingGUI::new));
        JButton openMultiThread = new JButton("Open Existing MultiThreadGUI");
        openMultiThread.addActionListener(e -> SwingUtilities.invokeLater(MultiThreadGUI::new));
        aiModeCombo = new JComboBox<>(new String[]{"Concise", "Research"});
        aiDifficultyCombo = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        JButton updateKey = new JButton("Update Gemini API Key");
        updateKey.addActionListener(e -> updateApiKey());

        buttons.add(openDemandPaging);
        buttons.add(openMultiThread);
        buttons.add(new JLabel("AI Output:"));
        buttons.add(aiModeCombo);
        buttons.add(new JLabel("AI Difficulty:"));
        buttons.add(aiDifficultyCombo);
        buttons.add(updateKey);

        panel.add(info, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createManagerTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Save / Manage Simulation Runs"));

        managerNameField = new JTextField("Run-1");
        managerCategory = new JComboBox<>(new String[]{"Page Replacement", "CPU Scheduling"});
        managerAlgorithmField = new JTextField("FIFO / FCFS");
        managerSummaryField = new JTextField("Enter summary before saving");

        form.add(new JLabel("Simulation Name:"));
        form.add(managerNameField);
        form.add(new JLabel("Category:"));
        form.add(managerCategory);
        form.add(new JLabel("Algorithm(s):"));
        form.add(managerAlgorithmField);
        form.add(new JLabel("Summary:"));
        form.add(managerSummaryField);

        JButton saveButton = new JButton("Save Simulation Record");
        saveButton.addActionListener(e -> saveSimulationRecord());
        JButton clearButton = new JButton("Clear Records");
        clearButton.addActionListener(e -> clearRecords());
        form.add(saveButton);
        form.add(clearButton);

        panel.add(form, BorderLayout.NORTH);

        managerTableModel = new DefaultTableModel(
                new Object[]{"Name", "Category", "Algorithm", "Summary", "Saved At"}, 0
        );
        JTable table = new JTable(managerTableModel);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPageAnalysisTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new GridLayout(2, 1, 8, 8));

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        inputRow.setBorder(BorderFactory.createTitledBorder("Reference String Analysis"));
        refStringField = new JTextField("7 0 1 2 0 3 0 4 2 3 0 3 2", 45);
        frameCountSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 15, 1));
        stepAlgoCombo = new JComboBox<>(PageReplacementAnalyzer.ALGORITHMS.toArray(new String[0]));
        JButton compareButton = new JButton("Run Full Comparison");
        compareButton.addActionListener(e -> runPageComparison());
        JButton stepButton = new JButton("Generate Step Table");
        stepButton.addActionListener(e -> runPageStepAnalysis());
        JButton aiGenerateRefButton = new JButton("AI Generate Reference");
        aiGenerateRefButton.addActionListener(e -> aiGenerateReferenceString());
        JButton aiExplainPageButton = new JButton("AI Explain Page Results");
        aiExplainPageButton.addActionListener(e -> aiExplainPageResults());
        JButton aiRecommendPageButton = new JButton("AI Recommend Best Page Algorithm");
        aiRecommendPageButton.addActionListener(e -> aiRecommendPageAlgorithm());

        inputRow.add(new JLabel("Reference String:"));
        inputRow.add(refStringField);
        inputRow.add(new JLabel("Frames:"));
        inputRow.add(frameCountSpinner);
        inputRow.add(new JLabel("Step Algorithm:"));
        inputRow.add(stepAlgoCombo);
        inputRow.add(compareButton);
        inputRow.add(stepButton);
        inputRow.add(aiGenerateRefButton);
        inputRow.add(aiExplainPageButton);
        inputRow.add(aiRecommendPageButton);
        top.add(inputRow);

        pageSummaryLabel = new JLabel("Run comparison to view best algorithm.");
        pageSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summary.add(pageSummaryLabel);
        top.add(summary);
        panel.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        pageMetricsModel = new DefaultTableModel(new Object[]{"Algorithm", "Faults", "Hits", "Hit Ratio %"}, 0);
        JTable metricsTable = new JTable(pageMetricsModel);
        metricsTable.setRowHeight(24);

        pageChartPanel = new SimpleBarChartPanel();
        pageChartPanel.setPreferredSize(new Dimension(500, 300));

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Algorithm Metrics"));
        left.add(new JScrollPane(metricsTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Fault Count Chart"));
        right.add(pageChartPanel, BorderLayout.CENTER);

        center.add(left);
        center.add(right);
        panel.add(center, BorderLayout.CENTER);

        pageStepModel = new DefaultTableModel(new Object[]{"Step", "Page", "Frames", "Event", "Evicted"}, 0);
        JTable stepTable = new JTable(pageStepModel);
        stepTable.setRowHeight(22);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Step-by-Step Table"));
        bottom.add(new JScrollPane(stepTable), BorderLayout.CENTER);
        bottom.setPreferredSize(new Dimension(1200, 280));
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCpuAnalysisTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createTitledBorder("CPU Scheduling Input"));

        processInputArea = new JTextArea(
                "P1,0,6,2\n" +
                "P2,1,4,4\n" +
                "P3,2,8,1\n" +
                "P4,3,5,3", 6, 40
        );
        processInputArea.setLineWrap(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        quantumSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JButton runButton = new JButton("Compare CPU Algorithms");
        runButton.addActionListener(e -> runCpuComparison());
        JButton aiGenerateProcessesButton = new JButton("AI Generate Process Set");
        aiGenerateProcessesButton.addActionListener(e -> aiGenerateCpuProcesses());
        JButton aiExplainCpuButton = new JButton("AI Explain CPU Results");
        aiExplainCpuButton.addActionListener(e -> aiExplainCpuResults());
        JButton aiDetectButton = new JButton("AI Detect OS Anomalies");
        aiDetectButton.addActionListener(e -> aiDetectAnomalies());
        controls.add(new JLabel("Round Robin Quantum:"));
        controls.add(quantumSpinner);
        controls.add(runButton);
        controls.add(aiGenerateProcessesButton);
        controls.add(aiExplainCpuButton);
        controls.add(aiDetectButton);
        controls.add(new JLabel("Format: ID,Arrival,Burst,Priority (higher value = higher priority)"));

        top.add(new JScrollPane(processInputArea), BorderLayout.CENTER);
        top.add(controls, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));

        cpuMetricsModel = new DefaultTableModel(
                new Object[]{"Algorithm", "Avg Waiting", "Avg Turnaround", "Throughput"}, 0
        );
        JTable cpuTable = new JTable(cpuMetricsModel);
        cpuTable.setRowHeight(24);
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("CPU Metrics"));
        left.add(new JScrollPane(cpuTable), BorderLayout.CENTER);

        cpuChartPanel = new SimpleBarChartPanel();
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Average Waiting Time Chart"));
        right.add(cpuChartPanel, BorderLayout.CENTER);

        center.add(left);
        center.add(right);
        panel.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 8, 8));
        cpuSummaryLabel = new JLabel("Run comparison to view best algorithm.");
        cpuSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        ganttOutputArea = new JTextArea(8, 40);
        ganttOutputArea.setEditable(false);
        JScrollPane ganttScroll = new JScrollPane(ganttOutputArea);
        ganttScroll.setBorder(BorderFactory.createTitledBorder("Gantt Timelines"));

        bottom.add(cpuSummaryLabel);
        bottom.add(ganttScroll);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAITab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel guide = new JPanel(new GridLayout(4, 1, 8, 8));
        guide.setBorder(BorderFactory.createTitledBorder("AI Research Assistant (Gemini 3 Flash)"));
        guide.add(new JLabel("Use Page/CPU tabs to trigger AI generation, explanations, recommendations, and anomaly detection."));
        guide.add(new JLabel("Output Mode toggle: Concise or Research (IEEE-friendly structured analysis)."));
        guide.add(new JLabel("Difficulty controls AI-generated benchmark datasets."));
        guide.add(new JLabel("Key storage path: ai_config.properties"));

        aiOutputArea = new JTextArea();
        aiOutputArea.setEditable(false);
        aiOutputArea.setLineWrap(true);
        aiOutputArea.setWrapStyleWord(true);
        aiOutputArea.setFont(new Font("Serif", Font.PLAIN, 14));

        panel.add(guide, BorderLayout.NORTH);
        panel.add(new JScrollPane(aiOutputArea), BorderLayout.CENTER);
        return panel;
    }

    private void saveSimulationRecord() {
        String name = managerNameField.getText().trim();
        String category = (String) managerCategory.getSelectedItem();
        String algorithm = managerAlgorithmField.getText().trim();
        String summary = managerSummaryField.getText().trim();

        if (name.isEmpty() || algorithm.isEmpty() || summary.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill name, algorithm, and summary.");
            return;
        }

        store.addRecord(name, category, algorithm, summary);
        refreshManagerTable();
        JOptionPane.showMessageDialog(this, "Simulation saved in memory successfully.");
    }

    private void clearRecords() {
        store.clear();
        refreshManagerTable();
    }

    private void refreshManagerTable() {
        managerTableModel.setRowCount(0);
        for (SimulationStore.SimulationRecord record : store.getRecords()) {
            managerTableModel.addRow(new Object[]{
                    record.name,
                    record.category,
                    record.algorithm,
                    record.summary,
                    record.timestamp
            });
        }
    }

    private void runPageComparison() {
        try {
            int[] reference = parseReferenceString(refStringField.getText());
            int frameCount = (int) frameCountSpinner.getValue();
            String[] algorithms = {"FIFO", "LRU", "MRU", "OPT"};
            pageMetricsModel.setRowCount(0);
            lastPageMetrics.clear();

            PageReplacementAnalyzer.PageMetrics best = null;
            java.util.LinkedHashMap<String, Double> faultMap = new java.util.LinkedHashMap<>();

            for (String algorithmName : algorithms) {
                int faults = DemandPaging.simulate(algorithmName, reference, frameCount);
                int hits = reference.length - faults;
                double ratio = reference.length == 0 ? 0 : hits * 100.0 / reference.length;
                PageReplacementAnalyzer.PageMetrics metric =
                        new PageReplacementAnalyzer.PageMetrics(algorithmName, hits, faults, ratio);
                faultMap.put(algorithmName, (double) faults);
                lastPageMetrics.add(metric);

                pageMetricsModel.addRow(new Object[]{
                        metric.algorithm,
                        metric.faults,
                        metric.hits,
                        String.format("%.2f", metric.hitRatio)
                });

                if (best == null || metric.faults < best.faults) {
                    best = metric;
                }
            }

            pageChartPanel.setChartData(
                    faultMap,
                    "Page Fault Comparison",
                    "Faults"
            );

            String summary = "Best by least faults: " + best.algorithm + " (faults=" + best.faults + ")";
            pageSummaryLabel.setText(summary);
            managerCategory.setSelectedItem("Page Replacement");
            managerAlgorithmField.setText("FIFO, LRU, MRU, OPT");
            managerSummaryField.setText(summary);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Error: " + exception.getMessage());
        }
    }

    private void runPageStepAnalysis() {
        try {
            int[] reference = parseReferenceString(refStringField.getText());
            int frameCount = (int) frameCountSpinner.getValue();
            String algorithm = (String) stepAlgoCombo.getSelectedItem();

            PageReplacementAnalyzer.DetailedPageRun run = PageReplacementAnalyzer.simulateDetailed(algorithm, reference, frameCount);
            pageStepModel.setRowCount(0);
            for (PageReplacementAnalyzer.PageStep step : run.steps) {
                pageStepModel.addRow(new Object[]{step.step, step.page, step.frames, step.event, step.evicted});
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Error: " + exception.getMessage());
        }
    }

    private void runCpuComparison() {
        try {
            List<CpuSchedulingAnalyzer.ProcessSpec> processes = CpuSchedulingAnalyzer.parseProcesses(processInputArea.getText());
            int quantum = (int) quantumSpinner.getValue();
            List<CpuSchedulingAnalyzer.CpuMetrics> metrics = CpuSchedulingAnalyzer.compareAll(processes, quantum);
            lastCpuMetrics.clear();
            lastCpuMetrics.addAll(metrics);

            cpuMetricsModel.setRowCount(0);
            ganttOutputArea.setText("");

            for (CpuSchedulingAnalyzer.CpuMetrics metric : metrics) {
                cpuMetricsModel.addRow(new Object[]{
                        metric.algorithm,
                        String.format("%.2f", metric.avgWaiting),
                        String.format("%.2f", metric.avgTurnaround),
                        String.format("%.3f", metric.throughput)
                });
                ganttOutputArea.append(metric.algorithm + ": " + formatSlots(metric.slots) + "\n\n");
            }

            cpuChartPanel.setChartData(
                    CpuSchedulingAnalyzer.waitingForChart(metrics),
                    "Average Waiting Time",
                    "Time Units"
            );

            CpuSchedulingAnalyzer.CpuMetrics best = metrics.stream()
                    .min(Comparator.comparingDouble(m -> m.avgWaiting))
                    .orElse(metrics.get(0));
            String summary = "Best by least avg waiting: " + best.algorithm + " (" + String.format("%.2f", best.avgWaiting) + ")";
            cpuSummaryLabel.setText(summary);

            managerCategory.setSelectedItem("CPU Scheduling");
            managerAlgorithmField.setText("FCFS, Priority, Round Robin");
            managerSummaryField.setText(summary);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Error: " + exception.getMessage());
        }
    }

    private int[] parseReferenceString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference string cannot be empty.");
        }
        String[] parts = raw.trim().split("[\\s,]+");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Reference string cannot be empty.");
        }
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i]);
        }
        return values;
    }

    private String formatSlots(List<CpuSchedulingAnalyzer.GanttSlot> slots) {
        StringBuilder builder = new StringBuilder();
        for (CpuSchedulingAnalyzer.GanttSlot slot : slots) {
            builder.append("[")
                    .append(slot.processId)
                    .append(": ")
                    .append(slot.start)
                    .append("-")
                    .append(slot.end)
                    .append("] ");
        }
        return builder.toString().trim();
    }

    private AIResearchAssistant getAssistant() throws Exception {
        if (aiAssistant == null) {
            String key = AIConfigManager.loadGeminiApiKey();
            aiAssistant = new AIResearchAssistant(key);
        }
        return aiAssistant;
    }

    private AIResearchAssistant.OutputMode getOutputMode() {
        return "Research".equals(aiModeCombo.getSelectedItem())
                ? AIResearchAssistant.OutputMode.RESEARCH
                : AIResearchAssistant.OutputMode.CONCISE;
    }

    private String getDifficulty() {
        return String.valueOf(aiDifficultyCombo.getSelectedItem());
    }

    private void aiGenerateReferenceString() {
        runAiTask("AI Generate Reference String", () -> {
            String generated = getAssistant().generateReferenceString(getDifficulty());
            SwingUtilities.invokeLater(() -> refStringField.setText(generated));
            return "Generated reference string (" + getDifficulty() + "):\n" + generated;
        });
    }

    private void aiGenerateCpuProcesses() {
        runAiTask("AI Generate CPU Process Dataset", () -> {
            String generated = getAssistant().generateCpuProcesses(getDifficulty(), 6);
            SwingUtilities.invokeLater(() -> processInputArea.setText(generated));
            return "Generated process dataset (" + getDifficulty() + "):\n" + generated;
        });
    }

    private void aiExplainPageResults() {
        if (lastPageMetrics.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run page comparison first.");
            return;
        }
        runAiTask("AI Explain Page Results", () -> getAssistant().explainPageComparison(
                refStringField.getText(),
                (int) frameCountSpinner.getValue(),
                lastPageMetrics,
                getOutputMode()
        ));
    }

    private void aiRecommendPageAlgorithm() {
        if (lastPageMetrics.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run page comparison first.");
            return;
        }
        runAiTask("AI Recommend Best Page Algorithm", () -> {
            String result = getAssistant().explainPageComparison(
                    refStringField.getText(),
                    (int) frameCountSpinner.getValue(),
                    lastPageMetrics,
                    getOutputMode()
            );
            SwingUtilities.invokeLater(() -> managerSummaryField.setText("AI Recommendation ready. Check AI tab."));
            return result;
        });
    }

    private void aiExplainCpuResults() {
        if (lastCpuMetrics.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run CPU comparison first.");
            return;
        }
        runAiTask("AI Explain CPU Results", () -> getAssistant().explainCpuComparison(
                processInputArea.getText(),
                (int) quantumSpinner.getValue(),
                lastCpuMetrics,
                getOutputMode()
        ));
    }

    private void aiDetectAnomalies() {
        runAiTask("AI Detect OS Anomalies", () -> {
            String context = "CPU Input:\n" + processInputArea.getText()
                    + "\n\nQuantum=" + quantumSpinner.getValue()
                    + "\nCPU Summary=" + cpuSummaryLabel.getText()
                    + "\nPage Summary=" + pageSummaryLabel.getText()
                    + "\nGantt Data:\n" + ganttOutputArea.getText();
            return getAssistant().detectAnomalies(context, getOutputMode());
        });
    }

    private void updateApiKey() {
        String input = JOptionPane.showInputDialog(this,
                "Enter Gemini API key to store in ai_config.properties:",
                "Update Gemini Key",
                JOptionPane.PLAIN_MESSAGE);
        if (input == null) {
            return;
        }
        try {
            AIConfigManager.saveGeminiApiKey(input.trim());
            aiAssistant = null;
            JOptionPane.showMessageDialog(this, "Gemini API key updated.");
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Failed to save key: " + exception.getMessage());
        }
    }

    private interface AiTask {
        String execute() throws Exception;
    }

    private void runAiTask(String title, AiTask task) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (aiOutputArea != null) {
            aiOutputArea.setText("Running: " + title + " ...");
        }
        new Thread(() -> {
            try {
                String result = task.execute();
                SwingUtilities.invokeLater(() -> showAiOutput(title, result));
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> showAiOutput(title, "Error: " + exception.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    private void showAiOutput(String title, String text) {
        if (aiOutputArea != null) {
            aiOutputArea.setText("[" + title + "]\n\n" + text);
            aiOutputArea.setCaretPosition(0);
        }
        JTextArea preview = new JTextArea(text, 18, 70);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(preview), title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            SimulationDashboard dashboard = new SimulationDashboard();
            dashboard.setVisible(true);
        });
    }
}
