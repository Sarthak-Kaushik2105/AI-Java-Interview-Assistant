package Java.Interview.Assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ClaudeService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String askClaude(String question) {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            return "Error: GEMINI_API_KEY environment variable is not set.";
        }

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

        return sendToGemini(prompt, apiKey);
    }
    public String askClaudeRaw(String prompt) {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            return "Error: GEMINI_API_KEY environment variable is not set.";
        }

        return sendToGemini(prompt, apiKey);
    }

    private String sendToGemini(String prompt, String apiKey) {

        String jsonBody = """
                {
                  "model": "gemini-3.6-flash",
                  "input": "%s"
                }
                """.formatted(escapeJson(prompt));

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "https://generativelanguage.googleapis.com/v1/interactions"
                            ))
                            .header("Content-Type", "application/json")
                            .header("x-goog-api-key", apiKey)
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(jsonBody)
                            )
                            .build();


            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );


            if (response.statusCode() >= 200 &&
                    response.statusCode() < 300) {

                return extractText(response.body());

            } else {

                return "Gemini API Error: " + response.body();

            }

        } catch (Exception e) {

            return "Error connecting to Gemini: " + e.getMessage();

        }
    }
    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    // =========================================================
    // EXTRACT TEXT FROM RESPONSE
    // =========================================================

    private String extractText(String json) {

        try {

            JsonNode root =
                    objectMapper.readTree(json);

            String result =
                    findText(root);

            if (result == null ||
                    result.isBlank()) {

                return "No text response received from Gemini.";

            }

            return result.trim();

        } catch (Exception e) {

            return "Error parsing Gemini response: "
                    + e.getMessage();
        }
    }

    private String findText(JsonNode node) {

        if (node.isObject() &&
                node.has("text")) {

            JsonNode textNode =
                    node.get("text");

            if (textNode.isTextual()) {

                return textNode.asText();

            }
        }


        if (node.isObject()) {

            var fields =
                    node.fields();

            while (fields.hasNext()) {

                var field =
                        fields.next();

                String result =
                        findText(field.getValue());

                if (result != null &&
                        !result.isBlank()) {

                    return result;

                }
            }
        }


        if (node.isArray()) {

            for (JsonNode child : node) {

                String result =
                        findText(child);

                if (result != null &&
                        !result.isBlank()) {

                    return result;

                }
            }
        }


        return null;
    }
}