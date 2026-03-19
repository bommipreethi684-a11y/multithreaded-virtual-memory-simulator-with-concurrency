import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class CpuSchedulingAnalyzer {

    public static class ProcessSpec {
        public final String id;
        public final int arrival;
        public final int burst;
        public final int priority;

        public ProcessSpec(String id, int arrival, int burst, int priority) {
            this.id = id;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
        }
    }

    public static class GanttSlot {
        public final String processId;
        public final int start;
        public final int end;

        public GanttSlot(String processId, int start, int end) {
            this.processId = processId;
            this.start = start;
            this.end = end;
        }
    }

    public static class CpuMetrics {
        public final String algorithm;
        public final double avgWaiting;
        public final double avgTurnaround;
        public final double throughput;
        public final List<GanttSlot> slots;

        public CpuMetrics(String algorithm, double avgWaiting, double avgTurnaround, double throughput, List<GanttSlot> slots) {
            this.algorithm = algorithm;
            this.avgWaiting = avgWaiting;
            this.avgTurnaround = avgTurnaround;
            this.throughput = throughput;
            this.slots = slots;
        }
    }

    private static class MutableProcess {
        String id;
        int arrival;
        int burst;
        int priority;
        int remaining;
        int completion;

        MutableProcess(ProcessSpec process) {
            this.id = process.id;
            this.arrival = process.arrival;
            this.burst = process.burst;
            this.priority = process.priority;
            this.remaining = process.burst;
            this.completion = -1;
        }
    }

    public static List<ProcessSpec> parseProcesses(String rawInput) {
        List<ProcessSpec> processes = new ArrayList<>();
        String[] lines = rawInput.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("\\s*,\\s*");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Each line must be: ProcessId,Arrival,Burst,Priority");
            }
            processes.add(new ProcessSpec(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            ));
        }

        if (processes.isEmpty()) {
            throw new IllegalArgumentException("Add at least one process.");
        }

        for (ProcessSpec process : processes) {
            if (process.id == null || process.id.trim().isEmpty()) {
                throw new IllegalArgumentException("Process ID cannot be empty.");
            }
            if (process.arrival < 0) {
                throw new IllegalArgumentException("Arrival time cannot be negative for process " + process.id);
            }
            if (process.burst <= 0) {
                throw new IllegalArgumentException("Burst time must be > 0 for process " + process.id);
            }
            if (process.priority < 0) {
                throw new IllegalArgumentException("Priority cannot be negative for process " + process.id);
            }
        }

        return processes;
    }

    public static List<CpuMetrics> compareAll(List<ProcessSpec> processes, int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Round Robin quantum must be greater than 0.");
        }
        List<CpuMetrics> results = new ArrayList<>();
        results.add(simulateFcfs(processes));
        results.add(simulatePriority(processes));
        results.add(simulateRoundRobin(processes, quantum));
        return results;
    }

    public static Map<String, Double> waitingForChart(List<CpuMetrics> metrics) {
        Map<String, Double> data = new LinkedHashMap<>();
        for (CpuMetrics metric : metrics) {
            data.put(metric.algorithm, metric.avgWaiting);
        }
        return data;
    }

    private static CpuMetrics simulateFcfs(List<ProcessSpec> processSpecs) {
        List<MutableProcess> list = toMutable(processSpecs);
        list.sort(Comparator.comparingInt((MutableProcess p) -> p.arrival).thenComparing(p -> p.id));

        int time = 0;
        List<GanttSlot> slots = new ArrayList<>();
        for (MutableProcess process : list) {
            if (time < process.arrival) {
                time = process.arrival;
            }
            int start = time;
            time += process.burst;
            process.completion = time;
            slots.add(new GanttSlot(process.id, start, time));
        }

        return toMetrics("FCFS", list, slots);
    }

    private static CpuMetrics simulatePriority(List<ProcessSpec> processSpecs) {
        List<MutableProcess> pending = toMutable(processSpecs);
        pending.sort(Comparator.comparingInt((MutableProcess p) -> p.arrival).thenComparing(p -> p.id));

        List<MutableProcess> ready = new ArrayList<>();
        List<MutableProcess> completed = new ArrayList<>();
        List<GanttSlot> slots = new ArrayList<>();

        int index = 0;
        int time = pending.get(0).arrival;

        while (completed.size() < pending.size()) {
            while (index < pending.size() && pending.get(index).arrival <= time) {
                ready.add(pending.get(index));
                index++;
            }

            if (ready.isEmpty()) {
                time = pending.get(index).arrival;
                continue;
            }

            ready.sort(Comparator
                    .comparingInt((MutableProcess p) -> p.priority).reversed()
                    .thenComparingInt(p -> p.arrival)
                    .thenComparing(p -> p.id));

            MutableProcess current = ready.remove(0);
            int start = time;
            time += current.burst;
            current.completion = time;
            slots.add(new GanttSlot(current.id, start, time));
            completed.add(current);
        }

        return toMetrics("Priority", completed, slots);
    }

    private static CpuMetrics simulateRoundRobin(List<ProcessSpec> processSpecs, int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Round Robin quantum must be greater than 0.");
        }
        List<MutableProcess> all = toMutable(processSpecs);
        all.sort(Comparator.comparingInt((MutableProcess p) -> p.arrival).thenComparing(p -> p.id));

        Queue<MutableProcess> readyQueue = new ArrayDeque<>();
        List<GanttSlot> slots = new ArrayList<>();
        int index = 0;
        int completed = 0;
        int time = all.get(0).arrival;

        while (completed < all.size()) {
            while (index < all.size() && all.get(index).arrival <= time) {
                readyQueue.offer(all.get(index));
                index++;
            }

            if (readyQueue.isEmpty()) {
                time = all.get(index).arrival;
                continue;
            }

            MutableProcess current = readyQueue.poll();
            int run = Math.min(quantum, current.remaining);
            int start = time;
            time += run;
            current.remaining -= run;
            slots.add(new GanttSlot(current.id, start, time));

            while (index < all.size() && all.get(index).arrival <= time) {
                readyQueue.offer(all.get(index));
                index++;
            }

            if (current.remaining > 0) {
                readyQueue.offer(current);
            } else {
                current.completion = time;
                completed++;
            }
        }

        return toMetrics("Round Robin", all, slots);
    }

    private static List<MutableProcess> toMutable(List<ProcessSpec> processSpecs) {
        List<MutableProcess> list = new ArrayList<>();
        for (ProcessSpec process : processSpecs) {
            list.add(new MutableProcess(process));
        }
        return list;
    }

    private static CpuMetrics toMetrics(String algorithm, List<MutableProcess> completed, List<GanttSlot> slots) {
        double totalWaiting = 0;
        double totalTurnaround = 0;
        int minArrival = Integer.MAX_VALUE;
        int maxCompletion = Integer.MIN_VALUE;

        for (MutableProcess process : completed) {
            int turnaround = process.completion - process.arrival;
            int waiting = turnaround - process.burst;
            totalWaiting += waiting;
            totalTurnaround += turnaround;
            minArrival = Math.min(minArrival, process.arrival);
            maxCompletion = Math.max(maxCompletion, process.completion);
        }

        double avgWaiting = totalWaiting / completed.size();
        double avgTurnaround = totalTurnaround / completed.size();
        double elapsed = Math.max(1, maxCompletion - minArrival);
        double throughput = completed.size() / elapsed;

        return new CpuMetrics(algorithm, avgWaiting, avgTurnaround, throughput, slots);
    }
}
