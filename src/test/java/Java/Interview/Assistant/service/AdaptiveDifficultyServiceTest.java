package Java.Interview.Assistant.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveDifficultyServiceTest {
    private final AdaptiveDifficultyService service = new AdaptiveDifficultyService();

    @Test
    void appliesScoreBandsAndDifficultyCaps() {
        assertEquals("Easy", service.decide("Medium", 3.9, List.of(), "Core Java").difficulty());
        assertEquals("Medium", service.decide("Medium", 4.0, List.of(), "Core Java").difficulty());
        assertEquals("Hard", service.decide("Medium", 7.0, List.of(), "Core Java").difficulty());
        assertEquals("Hard", service.decide("Medium", 9.0, List.of(), "Core Java").difficulty());
        assertEquals("Easy", service.decide("Easy", 2.0, List.of(), "Core Java").difficulty());
        assertEquals("Hard", service.decide("Hard", 9.5, List.of(), "Core Java").difficulty());
    }

    @Test
    void selectsWeakConceptAndExplainsTheChoice() {
        var decision = service.decide("Medium", 5.5,
                List.of("Hash collisions", "Resizing"), "Core Java");

        assertEquals("Hash collisions", decision.topic());
        assertTrue(decision.reason().contains("maintained"));
        assertTrue(decision.reason().contains("targets a concept"));
    }

    @Test
    void highScoreWithoutWeakConceptChoosesRelatedAdvancedTopic() {
        var decision = service.decide("Medium", 9.5, List.of(), "Collections");

        assertEquals("Hard", decision.difficulty());
        assertTrue(decision.topic().contains("advanced concept"));
        assertTrue(decision.reason().contains("advanced related concept"));
    }

    @Test
    void highScoreCanTargetAnAdvancedVersionOfAWeakConcept() {
        var decision = service.decide("Medium", 9.2, List.of("Concurrency"), "Core Java");

        assertEquals("Hard", decision.difficulty());
        assertTrue(decision.topic().contains("advanced application"));
        assertTrue(decision.topic().contains("Concurrency"));
        assertTrue(decision.reason().contains("weak concept"));
    }

    @Test
    void rejectsInvalidPreviousScore() {
        assertThrows(IllegalArgumentException.class,
                () -> service.decide("Easy", 10.1, List.of(), "Core Java"));
    }
}
