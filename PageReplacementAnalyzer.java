import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PageReplacementAnalyzer {

    public static final List<String> ALGORITHMS = Arrays.asList("FIFO", "LRU", "MRU", "OPT");

    public static class PageStep {
        public final int step;
        public final int page;
        public final String frames;
        public final String event;
        public final String evicted;

        public PageStep(int step, int page, String frames, String event, String evicted) {
            this.step = step;
            this.page = page;
            this.frames = frames;
            this.event = event;
            this.evicted = evicted;
        }
    }

    public static class PageMetrics {
        public final String algorithm;
        public final int hits;
        public final int faults;
        public final double hitRatio;

        public PageMetrics(String algorithm, int hits, int faults, double hitRatio) {
            this.algorithm = algorithm;
            this.hits = hits;
            this.faults = faults;
            this.hitRatio = hitRatio;
        }
    }

    public static class DetailedPageRun {
        public final PageMetrics metrics;
        public final List<PageStep> steps;

        public DetailedPageRun(PageMetrics metrics, List<PageStep> steps) {
            this.metrics = metrics;
            this.steps = steps;
        }
    }

    public static List<PageMetrics> compareAll(int[] reference, int frameCount) {
        List<PageMetrics> results = new ArrayList<>();
        for (String algorithm : ALGORITHMS) {
            results.add(simulateDetailed(algorithm, reference, frameCount).metrics);
        }
        return results;
    }

    public static DetailedPageRun simulateDetailed(String algorithm, int[] reference, int frameCount) {
        List<Integer> frames = new ArrayList<>();
        LinkedList<Integer> fifoQueue = new LinkedList<>();
        LinkedList<Integer> recency = new LinkedList<>();
        List<PageStep> steps = new ArrayList<>();

        int hits = 0;
        int faults = 0;

        for (int i = 0; i < reference.length; i++) {
            int page = reference[i];
            boolean hit = frames.contains(page);
            String evicted = "-";

            if (hit) {
                hits++;
                if ("LRU".equals(algorithm) || "MRU".equals(algorithm)) {
                    recency.remove(Integer.valueOf(page));
                    recency.addLast(page);
                }
            } else {
                faults++;
                if (frames.size() == frameCount) {
                    int pageToRemove = chooseEviction(algorithm, frames, fifoQueue, recency, reference, i);
                    evicted = String.valueOf(pageToRemove);
                    frames.remove(Integer.valueOf(pageToRemove));
                    fifoQueue.remove(Integer.valueOf(pageToRemove));
                    recency.remove(Integer.valueOf(pageToRemove));
                }
                frames.add(page);
                fifoQueue.addLast(page);
                recency.remove(Integer.valueOf(page));
                recency.addLast(page);
            }

            steps.add(new PageStep(
                    i + 1,
                    page,
                    frameSnapshot(frames, frameCount),
                    hit ? "HIT" : "FAULT",
                    evicted
            ));
        }

        double hitRatio = reference.length == 0 ? 0.0 : (hits * 100.0) / reference.length;
        PageMetrics metrics = new PageMetrics(algorithm, hits, faults, hitRatio);
        return new DetailedPageRun(metrics, steps);
    }

    public static Map<String, Double> faultsForChart(List<PageMetrics> metrics) {
        Map<String, Double> data = new LinkedHashMap<>();
        for (PageMetrics metric : metrics) {
            data.put(metric.algorithm, (double) metric.faults);
        }
        return data;
    }

    private static int chooseEviction(
            String algorithm,
            List<Integer> frames,
            LinkedList<Integer> fifoQueue,
            LinkedList<Integer> recency,
            int[] reference,
            int currentIndex
    ) {
        return switch (algorithm) {
            case "FIFO" -> fifoQueue.peekFirst();
            case "LRU" -> recency.peekFirst();
            case "MRU" -> recency.peekLast();
            case "OPT" -> chooseOptimal(frames, reference, currentIndex);
            default -> fifoQueue.peekFirst();
        };
    }

    private static int chooseOptimal(List<Integer> frames, int[] reference, int currentIndex) {
        int farthestUse = -1;
        int candidate = frames.get(0);

        for (int page : frames) {
            int nextUse = Integer.MAX_VALUE;
            for (int i = currentIndex + 1; i < reference.length; i++) {
                if (reference[i] == page) {
                    nextUse = i;
                    break;
                }
            }
            if (nextUse > farthestUse) {
                farthestUse = nextUse;
                candidate = page;
            }
        }
        return candidate;
    }

    private static String frameSnapshot(List<Integer> frames, int frameCount) {
        List<String> snapshot = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            snapshot.add(i < frames.size() ? String.valueOf(frames.get(i)) : "-");
        }
        return "[" + String.join(", ", snapshot) + "]";
    }
}
