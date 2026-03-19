import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AIResearchAssistant {
    public enum OutputMode {
        CONCISE,
        RESEARCH
    }

    private final GeminiAIClient client;

    public AIResearchAssistant(String apiKey) {
        this.client = new GeminiAIClient(apiKey);
    }

    public String generateReferenceString(String difficulty) throws IOException, InterruptedException {
        String prompt = "You are generating benchmark reference strings for operating system page replacement simulations. "
                + "Difficulty=" + difficulty + ". "
                + "Return ONLY one line with exactly 24 integers between 0 and 9, space-separated, no labels.";
        String raw = client.generateText(prompt);
        String cleaned = raw.replaceAll("[^0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? "7 0 1 2 0 3 0 4 2 3 0 3 2" : cleaned;
    }

    public String generateCpuProcesses(String difficulty, int count) throws IOException, InterruptedException {
        String prompt = "Create CPU scheduling dataset. Difficulty=" + difficulty + ". Process count=" + count + ". "
                + "Return ONLY " + count + " lines in exact format: PID,Arrival,Burst,Priority. "
                + "PID should be P1..Pn, arrival non-negative, burst > 0, priority 1..10.";
        String raw = client.generateText(prompt);
        String[] lines = raw.split("\\R");
        StringBuilder builder = new StringBuilder();
        int accepted = 0;
        for (String line : lines) {
            String trimmed = line.trim().replaceAll("`", "");
            String[] parts = trimmed.split("\\s*,\\s*");
            if (parts.length == 4) {
                builder.append(parts[0]).append(",")
                        .append(parts[1]).append(",")
                        .append(parts[2]).append(",")
                        .append(parts[3]).append("\n");
                accepted++;
            }
            if (accepted == count) {
                break;
            }
        }
        if (accepted == 0) {
            return "P1,0,6,2\nP2,1,4,4\nP3,2,8,1\nP4,3,5,3";
        }
        return builder.toString().trim();
    }

    public String explainPageComparison(String reference, int frames, List<PageReplacementAnalyzer.PageMetrics> metrics, OutputMode mode)
            throws IOException, InterruptedException {
        String metricText = metrics.stream()
                .map(m -> m.algorithm + ": faults=" + m.faults + ", hits=" + m.hits + ", hitRatio=" + String.format("%.2f", m.hitRatio))
                .collect(Collectors.joining(" | "));

        String prompt = "You are an OS research assistant. Analyze page replacement experiment. "
                + "ReferenceString=" + reference + ", Frames=" + frames + ", Metrics=" + metricText + ". "
                + modeInstruction(mode)
                + " Include best algorithm recommendation and reason grounded in locality/stack behavior.";
        return client.generateText(prompt);
    }

    public String explainCpuComparison(String processInput, int quantum, List<CpuSchedulingAnalyzer.CpuMetrics> metrics, OutputMode mode)
            throws IOException, InterruptedException {
        String metricText = metrics.stream()
                .map(m -> m.algorithm + ": avgWait=" + String.format("%.2f", m.avgWaiting)
                        + ", avgTAT=" + String.format("%.2f", m.avgTurnaround)
                        + ", throughput=" + String.format("%.3f", m.throughput))
                .collect(Collectors.joining(" | "));

        String prompt = "You are an OS research assistant. Analyze CPU scheduling experiment. "
                + "Quantum=" + quantum + ". Processes=\n" + processInput + "\n"
                + "Metrics=" + metricText + ". "
                + modeInstruction(mode)
                + " Recommend best algorithm and explain workload sensitivity.";
        return client.generateText(prompt);
    }

    public String detectAnomalies(String contextText, OutputMode mode) throws IOException, InterruptedException {
        String prompt = "Detect OS simulation anomalies from this data:\n"
                + contextText + "\n"
                + "Find risks for thrashing, starvation, deadlock, and unfair scheduling. "
                + modeInstruction(mode)
                + " Include confidence levels and mitigation suggestions.";
        return client.generateText(prompt);
    }

    private String modeInstruction(OutputMode mode) {
        if (mode == OutputMode.RESEARCH) {
            return "Output in IEEE-friendly structure: Objective, Findings, Evidence, Threats-to-validity, Recommendations.";
        }
        return "Output concise bullet points with direct actionable insights.";
    }
}
