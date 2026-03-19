import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeminiAIClient {
    private static final String MODEL = "gemini-3-flash-preview";

    private final String apiKey;
    private final HttpClient httpClient;

    public GeminiAIClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String generateText(String prompt) throws IOException, InterruptedException {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent?key=" + apiKey;

        String payload = "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.3,\"maxOutputTokens\":2048}"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            String message = extractError(response.body());
            throw new IOException("Gemini API error " + response.statusCode() + ": " + message);
        }

        String text = extractText(response.body());
        if (text.isEmpty()) {
            throw new IOException("Gemini response did not contain text output.");
        }
        return text;
    }

    private String extractText(String responseBody) {
        String key = "\"text\"";
        int cursor = 0;
        StringBuilder output = new StringBuilder();
        while (true) {
            int keyIndex = responseBody.indexOf(key, cursor);
            if (keyIndex < 0) {
                break;
            }
            int colonIndex = responseBody.indexOf(':', keyIndex + key.length());
            if (colonIndex < 0) {
                break;
            }
            int startQuote = responseBody.indexOf('"', colonIndex + 1);
            if (startQuote < 0) {
                break;
            }
            String parsed = parseJsonStringValue(responseBody, startQuote);
            cursor = Math.max(startQuote + 1, nextCursor(responseBody, startQuote));
            if (parsed.isEmpty()) {
                continue;
            }
            if (output.length() > 0) {
                output.append("\n");
            }
            output.append(unescapeJson(parsed));
        }
        return output.toString().trim();
    }

    private String extractError(String responseBody) {
        String key = "\"message\"";
        int keyIndex = responseBody.indexOf(key);
        if (keyIndex >= 0) {
            int colonIndex = responseBody.indexOf(':', keyIndex + key.length());
            int startQuote = colonIndex < 0 ? -1 : responseBody.indexOf('"', colonIndex + 1);
            if (startQuote >= 0) {
                return unescapeJson(parseJsonStringValue(responseBody, startQuote));
            }
        }
        return "Unknown API failure";
    }

    private int nextCursor(String source, int startQuote) {
        boolean escaped = false;
        for (int i = startQuote + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if (c == '"' && !escaped) {
                return i + 1;
            }
            escaped = false;
        }
        return source.length();
    }

    private String parseJsonStringValue(String source, int startQuote) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                builder.append('\\').append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", " ");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
