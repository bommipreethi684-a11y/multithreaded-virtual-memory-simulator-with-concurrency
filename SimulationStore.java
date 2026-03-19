import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimulationStore {

    public static class SimulationRecord {
        public final String name;
        public final String category;
        public final String algorithm;
        public final String summary;
        public final String timestamp;

        public SimulationRecord(String name, String category, String algorithm, String summary) {
            this.name = name;
            this.category = category;
            this.algorithm = algorithm;
            this.summary = summary;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    private final List<SimulationRecord> records = new ArrayList<>();

    public void addRecord(String name, String category, String algorithm, String summary) {
        records.add(new SimulationRecord(name, category, algorithm, summary));
    }

    public List<SimulationRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public void clear() {
        records.clear();
    }
}
