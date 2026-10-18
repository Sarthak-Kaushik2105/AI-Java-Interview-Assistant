package Java.Interview.Assistant;

import Java.Interview.Assistant.service.ClaudeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final ClaudeService claudeService;

    public InterviewController(ClaudeService claudeService) {
        this.claudeService = claudeService;
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

        String prompt = """
                You are a professional technical interviewer.

                Generate ONE Java interview question.

                Category:
                %s

                Difficulty:
                %s

                RULES:
                - Generate ONLY ONE interview question.
                - Do NOT provide the answer.
                - Do NOT provide explanation.
                - Do NOT use Markdown.
                - Do NOT use numbering.
                - Do NOT write "Question:".
                - Return ONLY the question text.
                """.formatted(category, difficulty);

        return claudeService.askClaudeRaw(prompt);
    }
    @PostMapping("/evaluate")
    public String evaluateAnswer(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");
        String answer = request.get("answer");

        if (question == null || question.isBlank()) {
            return "Error: Question is missing.";
        }

        if (answer == null || answer.isBlank()) {
            return "Error: Answer is missing.";
        }

        String prompt = """
                You are a professional Java technical interviewer.

                Evaluate the candidate's answer.

                INTERVIEW QUESTION:
                %s

                CANDIDATE ANSWER:
                %s

                Give the evaluation in exactly this structure:

                ## Score
                Give a score out of 10.

                ## What Was Correct
                Explain what the candidate answered correctly.

                ## What Was Missing or Incorrect
                Explain missing concepts or mistakes.

                ## Suggestions for Improvement
                Give practical suggestions.

                ## Correct / Reference Answer
                Give a technically accurate interview-ready answer.

                ## Interview Verdict
                Briefly say whether the answer is Excellent, Good,
                Average, or Needs Improvement.

                IMPORTANT:
                - Evaluate technical correctness.
                - Be fair and constructive.
                - Keep it Java-interview focused.
                - Use Markdown headings and bullet points.
                - Do NOT return JSON.
                - Do NOT wrap the entire response in a code block.
                """.formatted(question, answer);

        return claudeService.askClaudeRaw(prompt);
    }
}