package Java.Interview.Assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewControllerTest {
    @Test
    void recognizesAllGeminiServiceErrorResponses() {
        assertTrue(InterviewController.isAiError("Error: GEMINI_API_KEY environment variable is not set."));
        assertTrue(InterviewController.isAiError("Gemini API Error: 429"));
        assertTrue(InterviewController.isAiError("Error connecting to Gemini: timed out"));
        assertTrue(InterviewController.isAiError("Error parsing Gemini response: invalid JSON"));
        assertTrue(InterviewController.isAiError("No text response received from Gemini."));
        assertTrue(InterviewController.isAiError("  "));
    }

    @Test
    void doesNotClassifyOrdinaryQuestionTextAsAnError() {
        assertFalse(InterviewController.isAiError("What is the purpose of the Java garbage collector?"));
    }
}
