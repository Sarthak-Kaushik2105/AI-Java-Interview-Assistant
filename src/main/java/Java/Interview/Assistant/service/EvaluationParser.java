package Java.Interview.Assistant.service;

import Java.Interview.Assistant.model.EvaluationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Parses and validates the model's JSON before it reaches the API client. */
@Component
public class EvaluationParser {
    private final ObjectMapper objectMapper;

    public EvaluationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluationResult parse(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Evaluation response is empty.");
        }

        String json = removeCodeFence(response.trim());
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Evaluation must be a JSON object.");
            }

            JsonNode scoreNode = root.get("overallScore");
            if (scoreNode == null || !scoreNode.isNumber()) {
                throw new IllegalArgumentException("overallScore must be a number.");
            }
            double score = scoreNode.asDouble();
            if (!Double.isFinite(score) || score < 0 || score > 10) {
                throw new IllegalArgumentException("overallScore must be between 0 and 10.");
            }

            return new EvaluationResult(
                    score,
                    requiredText(root, "technicalCorrectness"),
                    requiredText(root, "conceptualUnderstanding"),
                    stringList(root, "missingOrIncorrectConcepts"),
                    stringList(root, "strengths"),
                    stringList(root, "areasForImprovement"),
                    requiredText(root, "referenceAnswer"),
                    requiredText(root, "interviewVerdict"));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Evaluation response is not valid JSON.", e);
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " must be a non-empty string.");
        }
        return value.asText().trim();
    }

    private List<String> stringList(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException(field + " must be an array of strings.");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw new IllegalArgumentException(field + " must contain non-empty strings.");
            }
            result.add(item.asText().trim());
        }
        return List.copyOf(result);
    }

    private String removeCodeFence(String response) {
        if (!response.startsWith("```") || !response.endsWith("```")) {
            return response;
        }
        String body = response.substring(3, response.length() - 3).trim();
        if (body.regionMatches(true, 0, "json", 0, 4)) {
            body = body.substring(4).trim();
        }
        return body;
    }
}
