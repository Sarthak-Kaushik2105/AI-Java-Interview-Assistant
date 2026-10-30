package Java.Interview.Assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class ClaudeService {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeService.class);
    private static final String MODEL = "gemini-3.6-flash";
    private static final URI INTERACTIONS_URI =
            URI.create("https://generativelanguage.googleapis.com/v1/interactions");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    private static final int MAX_ATTEMPTS = 2;

    private final HttpClient httpClient;
    private final Supplier<String> apiKeySupplier;
    private final URI interactionsUri;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;

    @Autowired
    public ClaudeService() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
                () -> System.getenv("GEMINI_API_KEY"), INTERACTIONS_URI,
                REQUEST_TIMEOUT, new ObjectMapper());
    }

    ClaudeService(HttpClient httpClient, String apiKey, URI interactionsUri,
                  Duration requestTimeout, ObjectMapper objectMapper) {
        this(httpClient, () -> apiKey, interactionsUri, requestTimeout, objectMapper);
    }

    private ClaudeService(HttpClient httpClient, Supplier<String> apiKeySupplier,
                          URI interactionsUri, Duration requestTimeout, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.apiKeySupplier = apiKeySupplier;
        this.interactionsUri = interactionsUri;
        this.requestTimeout = requestTimeout;
        this.objectMapper = objectMapper;
    }

    public String askClaude(String question) {
        String prompt = """
                You are a professional Java Interview Assistant.

                Answer the following Java interview question in a clear,
                structured and interview-friendly format.

                Follow this format whenever applicable:

                ## 1. Definition
                Give a simple and accurate definition.

                ## 2. Key Points
                Explain the important points in bullet points.

                ## 3. How It Works
                Explain the internal working step-by-step.

                ## 4. Example
                Give a simple real-world or programming example.

                ## 5. Java Code Example
                If applicable, provide clean Java code inside a Java code block.

                ## 6. Time Complexity
                Mention time and space complexity when applicable.

                ## 7. Interview Tip
                Give an important point that an interviewer may ask.

                ## 8. Short Interview Answer
                Give a concise answer that the candidate can speak directly
                in an interview.

                Important instructions:
                - Use Markdown headings and bullet points.
                - Keep explanations easy to understand.
                - Do not return HTML.
                - Do not wrap the entire answer inside a code block.
                - Use ```java for Java code.
                - Keep the answer technically accurate.
                - Avoid unnecessary repetition.

                Question:
                %s
                """.formatted(question);
        return sendToGemini(prompt);
    }

    public String askClaudeRaw(String prompt) {
        return sendToGemini(prompt);
    }

    /** Strict variant for endpoints that must never mistake service errors for generated text. */
    public String askClaudeRawOrThrow(String prompt) {
        String apiKey = apiKeySupplier.get();
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiApiException(0, "GEMINI_API_KEY is not configured.");
        }
        return sendToGeminiOrThrow(prompt, apiKey);
    }

    private String sendToGemini(String prompt) {
        String apiKey = apiKeySupplier.get();
        if (apiKey == null || apiKey.isBlank()) {
            return "Error: GEMINI_API_KEY environment variable is not set.";
        }
        try {
            return sendToGeminiOrThrow(prompt, apiKey);
        } catch (GeminiApiException e) {
            if (e.getUpstreamStatus() == 0) {
                return "Error connecting to Gemini: " + e.getSafeDetail();
            }
            if (e.getUpstreamStatus() == 200) {
                return "Error parsing Gemini response: " + e.getSafeDetail();
            }
            return "Gemini API Error: HTTP " + e.getUpstreamStatus() + ": " + e.getSafeDetail();
        }
    }

    private String sendToGeminiOrThrow(String prompt, String apiKey) {
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(Map.of("model", MODEL, "input", prompt));
        } catch (JsonProcessingException e) {
            logger.error("Could not encode Gemini request as JSON.");
            throw new GeminiApiException(0, "Could not encode the Gemini request.", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(interactionsUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException e) {
                logger.warn("Gemini Interactions API request timed out after {} ms.",
                        requestTimeout.toMillis());
                throw new GeminiApiException(0,
                        "Gemini request timed out after " + requestTimeout.toMillis() + " ms.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Gemini Interactions API request was interrupted.");
                throw new GeminiApiException(0, "Gemini request was interrupted.", e);
            } catch (IOException e) {
                logger.warn("Gemini Interactions API connection failed ({}).", e.getClass().getSimpleName());
                throw new GeminiApiException(0, "Could not connect to the Gemini API.", e);
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return extractText(response.body(), status, apiKey);
            }

            String detail = safeProviderError(response.body(), apiKey);
            logger.warn("Gemini Interactions API returned HTTP {}: {}", status, detail);
            if (attempt < MAX_ATTEMPTS && isTransientStatus(status)) {
                pauseBeforeRetry(response);
                continue;
            }
            throw new GeminiApiException(status, detail);
        }
        throw new GeminiApiException(0, "Gemini request failed after retries.");
    }

    private String extractText(String body, int httpStatus, String apiKey) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            logger.warn("Gemini Interactions API returned HTTP {} with malformed JSON.", httpStatus);
            throw new GeminiApiException(httpStatus, "Gemini returned malformed JSON.", e);
        }

        String interactionStatus = root.path("status").asText("");
        if (!interactionStatus.isBlank() && !interactionStatus.equalsIgnoreCase("completed")) {
            String detail = safeProviderError(body, apiKey);
            logger.warn("Gemini interaction completed with status '{}' (HTTP {}): {}",
                    interactionStatus, httpStatus, detail);
            throw new GeminiApiException(httpStatus,
                    "Gemini interaction status " + interactionStatus + ": " + detail);
        }

        String result = findText(root);
        if (result == null || result.isBlank()) {
            logger.warn("Gemini Interactions API returned HTTP {} without text output.", httpStatus);
            throw new GeminiApiException(httpStatus, "Gemini response contained no text output.");
        }
        return result.trim();
    }

    private String safeProviderError(String body, String apiKey) {
        String detail = "Upstream returned an error without a readable detail.";
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            String message = error.path("message").asText("");
            String code = error.path("status").asText("");
            if (message.isBlank()) message = error.path("code").asText("");
            if (!message.isBlank()) detail = code.isBlank() ? message : code + ": " + message;
        } catch (Exception ignored) {
            // Do not log arbitrary response bodies; they are not guaranteed to be safe.
        }
        detail = detail.replace(apiKey, "[REDACTED]")
                .replaceAll("AIza[0-9A-Za-z_-]{20,}", "[REDACTED]")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return detail.length() > 500 ? detail.substring(0, 500) : detail;
    }

    private boolean isTransientStatus(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private void pauseBeforeRetry(HttpResponse<?> response) {
        long delayMillis = response.headers().firstValue("Retry-After")
                .flatMap(value -> {
                    try {
                        return java.util.Optional.of(Math.max(0, Math.min(2, Long.parseLong(value)) * 1_000));
                    } catch (NumberFormatException e) {
                        return java.util.Optional.empty();
                    }
                })
                .orElse(250L);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiApiException(response.statusCode(), "Gemini retry was interrupted.", e);
        }
    }

    private String findText(JsonNode node) {
        if (node.isObject() && node.has("text") && node.get("text").isTextual()) {
            return node.get("text").asText();
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                String result = findText(fields.next().getValue());
                if (result != null && !result.isBlank()) return result;
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String result = findText(child);
                if (result != null && !result.isBlank()) return result;
            }
        }
        return null;
    }
}
