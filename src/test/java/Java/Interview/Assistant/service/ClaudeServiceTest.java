package Java.Interview.Assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeServiceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void retriesTransientUpstreamFailureAndParsesInteractionsOutput() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> sentApiKey = new AtomicReference<>();
        AtomicReference<String> sentBody = new AtomicReference<>();
        startServer(exchange -> {
            sentApiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            sentBody.set(new String(exchange.getRequestBody().readAllBytes()));
            if (attempts.incrementAndGet() == 1) {
                exchange.getResponseHeaders().add("Retry-After", "0");
                respond(exchange, 503, "{\"error\":{\"message\":\"temporarily overloaded\"}}");
            } else {
                respond(exchange, 200, interactionsResponse("Generated question"));
            }
        });

        String prompt = "Question with \"quotes\" and\nnewline";
        String result = service("secret-test-key", Duration.ofSeconds(2)).askClaudeRawOrThrow(prompt);

        assertEquals("Generated question", result);
        assertEquals(2, attempts.get());
        assertEquals("secret-test-key", sentApiKey.get());
        JsonNode request = MAPPER.readTree(sentBody.get());
        assertEquals("gemini-3.6-flash", request.path("model").asText());
        assertEquals(prompt, request.path("input").asText());
    }

    @Test
    void reportsUpstreamStatusAndRedactsKeyAfterRetryExhaustion() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        startServer(exchange -> {
            attempts.incrementAndGet();
            exchange.getResponseHeaders().add("Retry-After", "0");
            respond(exchange, 429,
                    "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"key secret-test-key is rate limited\"}}");
        });

        GeminiApiException error = assertThrows(GeminiApiException.class,
                () -> service("secret-test-key", Duration.ofSeconds(2)).askClaudeRawOrThrow("prompt"));

        assertEquals(429, error.getUpstreamStatus());
        assertTrue(error.getSafeDetail().contains("RESOURCE_EXHAUSTED"));
        assertFalse(error.getSafeDetail().contains("secret-test-key"));
        assertEquals(2, attempts.get());
    }

    @Test
    void rejectsMalformedAndNoTextSuccessfulResponses() throws Exception {
        startServer(exchange -> respond(exchange, 200, "not-json"));
        GeminiApiException malformed = assertThrows(GeminiApiException.class,
                () -> service("secret-test-key", Duration.ofSeconds(2)).askClaudeRawOrThrow("prompt"));
        assertEquals(200, malformed.getUpstreamStatus());
        assertTrue(malformed.getSafeDetail().contains("malformed JSON"));

        stopServer();
        startServer(exchange -> respond(exchange, 200, "{\"status\":\"completed\",\"steps\":[]}"));
        GeminiApiException empty = assertThrows(GeminiApiException.class,
                () -> service("secret-test-key", Duration.ofSeconds(2)).askClaudeRawOrThrow("prompt"));
        assertEquals(200, empty.getUpstreamStatus());
        assertTrue(empty.getSafeDetail().contains("no text output"));
    }

    @Test
    void convertsRequestTimeoutToSafeFailure() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                respond(exchange, 200, interactionsResponse("late response"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        GeminiApiException error = assertThrows(GeminiApiException.class,
                () -> service("secret-test-key", Duration.ofMillis(75)).askClaudeRawOrThrow("prompt"));
        assertEquals(0, error.getUpstreamStatus());
        assertTrue(error.getSafeDetail().contains("timed out"));
    }

    private ClaudeService service(String apiKey, Duration timeout) {
        return new ClaudeService(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                apiKey, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/interactions"),
                timeout, MAPPER);
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/interactions", handler);
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String interactionsResponse(String text) {
        try {
            return MAPPER.writeValueAsString(java.util.Map.of(
                    "status", "completed",
                    "steps", java.util.List.of(java.util.Map.of(
                            "type", "model_output",
                            "content", java.util.List.of(java.util.Map.of("type", "text", "text", text))))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
