package Java.Interview.Assistant.model;

import java.util.List;

/** A validated, structured assessment returned by the interview evaluator. */
public record EvaluationResult(
        double overallScore,
        String technicalCorrectness,
        String conceptualUnderstanding,
        List<String> missingOrIncorrectConcepts,
        List<String> strengths,
        List<String> areasForImprovement,
        String referenceAnswer,
        String interviewVerdict) {
}
