package Java.Interview.Assistant;

import Java.Interview.Assistant.service.ClaudeService;
import Java.Interview.Assistant.model.AdaptiveQuestion;
import Java.Interview.Assistant.model.EvaluationResult;
import Java.Interview.Assistant.service.AdaptiveDifficultyService;
import Java.Interview.Assistant.service.EvaluationParser;
import Java.Interview.Assistant.service.GeminiApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final ClaudeService claudeService;
    private final EvaluationParser evaluationParser;
    private final AdaptiveDifficultyService adaptiveDifficultyService;

    public InterviewController(ClaudeService claudeService,
                               EvaluationParser evaluationParser,
                               AdaptiveDifficultyService adaptiveDifficultyService) {
        this.claudeService = claudeService;
        this.evaluationParser = evaluationParser;
        this.adaptiveDifficultyService = adaptiveDifficultyService;
    }

    @GetMapping("/ask")
    public String ask(
            @RequestParam String question) {

        return claudeService.askClaude(question);
    }
    @GetMapping("/mock-question")
    public String generateMockQuestion(
            @RequestParam String category,
            @RequestParam String difficulty) {

        return claudeService.askClaudeRaw(mockQuestionPrompt(category, difficulty, null));
    }

    /** Additive endpoint: the original /mock-question contract remains available. */
    @GetMapping("/adaptive-question")
    public AdaptiveQuestion generateAdaptiveQuestion(
            @RequestParam String category,
            @RequestParam String difficulty,
            @RequestParam(required = false) Double previousScore,
            @RequestParam(required = false) List<String> weakConcepts) {

        var decision = adaptiveDifficultyService.decide(
                difficulty, previousScore, weakConcepts, category);
        String generated;
        try {
            generated = claudeService.askClaudeRawOrThrow(
                    mockQuestionPrompt(category, decision.difficulty(), decision.topic()));
        } catch (GeminiApiException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini could not generate the next question. Please retry.");
        }
        if (isAiError(generated)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI question service is unavailable. Please try again.");
        }
        return new AdaptiveQuestion(generated.trim(), decision.difficulty(),
                decision.topic(), decision.reason());
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResult> evaluateAnswer(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");
        String answer = request.get("answer");

        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is missing.");
        }

        if (answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer is missing.");
        }

        String prompt = """
                You are a professional Java technical interviewer.

                Evaluate the candidate's answer fairly and return ONLY one valid JSON object.
                Do not use Markdown or wrap the JSON in a code fence. Do not add text before or after it.

                INTERVIEW QUESTION:
                %s

                CANDIDATE ANSWER:
                %s

                Return exactly these JSON fields and types:
                {
                  "overallScore": number from 0 to 10,
                  "technicalCorrectness": "brief assessment of correctness",
                  "conceptualUnderstanding": "brief assessment of understanding",
                  "missingOrIncorrectConcepts": ["short topic or concept label, not an improvement suggestion"],
                  "strengths": ["strength"],
                  "areasForImprovement": ["actionable improvement"],
                  "referenceAnswer": "technically accurate interview-ready answer",
                  "interviewVerdict": "Excellent, Good, Average, or Needs Improvement"
                }
                Use empty arrays when a list has no entries. Keep all strings constructive and Java-interview focused.
                """.formatted(question, answer);

        String rawEvaluation = claudeService.askClaudeRaw(prompt);
        if (isAiError(rawEvaluation)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI evaluation service is unavailable. Please try again.");
        }
        try {
            return ResponseEntity.ok(evaluationParser.parse(rawEvaluation));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI returned an invalid evaluation. Please retry.");
        }
    }

    private String mockQuestionPrompt(String category, String difficulty, String focusTopic) {
        String focusInstruction = focusTopic == null || focusTopic.isBlank()
                ? "" : "\nFocus concept (relate it to the category):\n" + focusTopic;
        return """
                You are a professional technical interviewer.
                Generate ONE Java interview question.
                Category: %s
                Difficulty: %s%s
                Rules: output only the question text, with no answer, explanation, Markdown, numbering, or label.
                """.formatted(category, difficulty, focusInstruction);
    }

    static boolean isAiError(String response) {
        if (response == null || response.isBlank()) return true;
        String value = response.trim();
        return value.startsWith("Error:")
                || value.startsWith("Error connecting to Gemini:")
                || value.startsWith("Error parsing Gemini response:")
                || value.startsWith("Gemini API Error:")
                || value.startsWith("No text response received from Gemini.");
    }
}
