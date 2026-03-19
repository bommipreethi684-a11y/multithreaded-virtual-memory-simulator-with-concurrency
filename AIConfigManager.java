import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AIConfigManager {
    private static final Path CONFIG_PATH = Paths.get("./ai_config.properties");
    private static final String API_KEY_FIELD = "GEMINI_API_KEY";

    public static String loadGeminiApiKey() throws IOException {
        if (!Files.exists(CONFIG_PATH)) {
            throw new IOException("Missing ai_config.properties");
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
            properties.load(inputStream);
        }

        String apiKey = properties.getProperty(API_KEY_FIELD, "").trim();
        if (apiKey.isEmpty()) {
            throw new IOException("GEMINI_API_KEY is empty in ai_config.properties");
        }
        return apiKey;
    }

    public static void saveGeminiApiKey(String apiKey) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("API key cannot be empty");
        }

        Properties properties = new Properties();
        properties.setProperty(API_KEY_FIELD, apiKey.trim());
        try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(outputStream, "Gemini API configuration for Swing AI assistant");
        }
    }
}
