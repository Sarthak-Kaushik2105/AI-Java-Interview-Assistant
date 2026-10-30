package Java.Interview.Assistant.service;

import org.springframework.stereotype.Component;

import java.util.List;

/** Pure difficulty and focus selection policy, independent of the AI and database. */
@Component
public class AdaptiveDifficultyService {
    public AdaptiveDecision decide(String currentDifficulty, Double previousScore,
                                   List<String> weakConcepts, String category) {
        String current = normalizeDifficulty(currentDifficulty);
        var weakConcept = firstConcept(weakConcepts);
        String topic = weakConcept.orElse(category == null || category.isBlank()
                ? "Java fundamentals" : category.trim());

        if (previousScore == null) {
            return new AdaptiveDecision(current, topic,
                    "Starting at your selected " + current + " difficulty.");
        }
        if (!Double.isFinite(previousScore) || previousScore < 0 || previousScore > 10) {
            throw new IllegalArgumentException("Previous score must be between 0 and 10.");
        }

        String selected = current;
        String reason;
        if (previousScore < 4) {
            selected = lower(current);
            reason = "Your previous score was below 4/10, so difficulty was reduced";
        } else if (previousScore < 7) {
            reason = "Your previous score was 4–6.9/10, so difficulty was maintained";
        } else if (previousScore < 9) {
            selected = higher(current);
            reason = "Your previous score was 7–8.9/10, so difficulty was increased";
        } else {
            selected = higher(current);
            reason = "Your previous score was at least 9/10, so difficulty was increased";
            if (weakConcept.isPresent()) {
                topic = "an advanced application related to " + weakConcept.get();
                reason += " and an advanced application of that weak concept was selected";
            } else {
                topic = "an advanced concept related to " + topic;
                reason += " and an advanced related concept was selected";
            }
        }

        if (weakConcept.isPresent()) {
            reason += "; the focus targets a concept to improve";
        }
        return new AdaptiveDecision(selected, topic, reason + ".");
    }

    private java.util.Optional<String> firstConcept(List<String> concepts) {
        if (concepts == null) return java.util.Optional.empty();
        return concepts.stream().filter(c -> c != null && !c.isBlank()).map(String::trim).findFirst();
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "Medium";
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> "Easy";
            case "hard" -> "Hard";
            default -> "Medium";
        };
    }

    private String lower(String difficulty) {
        return difficulty.equals("Hard") ? "Medium" : "Easy";
    }

    private String higher(String difficulty) {
        return difficulty.equals("Easy") ? "Medium" : "Hard";
    }

    public record AdaptiveDecision(String difficulty, String topic, String reason) { }
}
