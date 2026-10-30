package Java.Interview.Assistant.service;

import Java.Interview.Assistant.model.EvaluationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationParserTest {
    private final EvaluationParser parser = new EvaluationParser(new ObjectMapper());

    @Test
    void parsesValidStructuredEvaluationAndCodeFence() {
        EvaluationResult result = parser.parse("""
                ```json
                {"overallScore":8.5,"technicalCorrectness":"Correct","conceptualUnderstanding":"Clear",
                 "missingOrIncorrectConcepts":["Hash collisions"],"strengths":["Good example"],
                 "areasForImprovement":["Explain resizing"],"referenceAnswer":"A hash map stores key-value pairs.",
                 "interviewVerdict":"Good"}
                ```
                """);

        assertEquals(8.5, result.overallScore());
        assertEquals("Correct", result.technicalCorrectness());
        assertEquals("Hash collisions", result.missingOrIncorrectConcepts().getFirst());
        assertEquals("Good", result.interviewVerdict());
    }

    @Test
    void rejectsMalformedJsonAndOutOfRangeScore() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("not json"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(validJson(11)));
    }

    @Test
    void rejectsMissingOrWronglyTypedRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{\"overallScore\":5}"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(validJsonWithList("strengths", "\"not-an-array\"")));
    }

    private String validJson(double score) {
        return """
                {"overallScore":%s,"technicalCorrectness":"Correct","conceptualUnderstanding":"Clear",
                 "missingOrIncorrectConcepts":[],"strengths":[],"areasForImprovement":[],
                 "referenceAnswer":"Reference","interviewVerdict":"Average"}
                """.formatted(score);
    }

    private String validJsonWithList(String field, String value) {
        return """
                {"overallScore":5,"technicalCorrectness":"Correct","conceptualUnderstanding":"Clear",
                 "missingOrIncorrectConcepts":[],"strengths":%s,"areasForImprovement":[],
                 "referenceAnswer":"Reference","interviewVerdict":"Average"}
                """.formatted(value);
    }
}
