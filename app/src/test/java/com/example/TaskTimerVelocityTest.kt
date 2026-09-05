package com.example

import com.example.data.entity.ArtifactEntity
import com.example.ui.components.TaskVelocityRating
import com.example.ui.components.computeTaskVelocity
import com.example.ui.components.formatTimerDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTimerVelocityTest {

    @Test
    fun formatTimerDuration_formatsSecondsProperly() {
        assertEquals("00:00", formatTimerDuration(0L))
        assertEquals("00:45", formatTimerDuration(45L))
        assertEquals("05:30", formatTimerDuration(330L))
        assertEquals("59:59", formatTimerDuration(3599L))
        assertEquals("01:00:00", formatTimerDuration(3600L))
        assertEquals("02:15:30", formatTimerDuration(8130L))
    }

    @Test
    fun computeTaskVelocity_highPriorityBenchmarks() {
        // High priority benchmark is 3600s (60 mins)
        val highPriorityArtifact = ArtifactEntity(
            title = "Algorytm kwantowy",
            category = "Kod OtakOS",
            proposingAgent = "Otak-Alpha",
            status = "W_OPRACOWANIU",
            priority = "Wysoki"
        )

        // 10 mins (600s) -> HIGH velocity
        val rapid = computeTaskVelocity(highPriorityArtifact, 600L)
        assertEquals(TaskVelocityRating.HIGH, rapid.velocityRating)

        // 40 mins (2400s) -> OPTIMAL
        val optimal = computeTaskVelocity(highPriorityArtifact, 2400L)
        assertEquals(TaskVelocityRating.OPTIMAL, optimal.velocityRating)

        // 80 mins (4800s) -> EXTENDED
        val extended = computeTaskVelocity(highPriorityArtifact, 4800L)
        assertEquals(TaskVelocityRating.EXTENDED, extended.velocityRating)
    }

    @Test
    fun computeTaskVelocity_lowPriorityBenchmarks() {
        // Low priority benchmark is 1200s (20 mins)
        val lowPriorityArtifact = ArtifactEntity(
            title = "Drobne poprawki dokumentacji",
            category = "Teoria Katedry",
            proposingAgent = "Otak-Beta",
            status = "W_OPRACOWANIU",
            priority = "Niski"
        )

        val fast = computeTaskVelocity(lowPriorityArtifact, 300L)
        assertEquals(TaskVelocityRating.HIGH, fast.velocityRating)

        val balanced = computeTaskVelocity(lowPriorityArtifact, 900L)
        assertEquals(TaskVelocityRating.OPTIMAL, balanced.velocityRating)

        val slow = computeTaskVelocity(lowPriorityArtifact, 1800L)
        assertEquals(TaskVelocityRating.EXTENDED, slow.velocityRating)
    }

    @Test
    fun artifactEntity_currentEffectiveSeconds_reflectsRunningTimer() {
        val baseTime = 120L
        val startedAt = System.currentTimeMillis() - 30_000L // 30 seconds ago
        val runningArtifact = ArtifactEntity(
            title = "Algorytm kwantowy",
            category = "Kod OtakOS",
            proposingAgent = "Otak-Alpha",
            status = "W_OPRACOWANIU",
            priority = "Wysoki",
            description = "Test",
            digitalPayload = "val x = 1",
            timeSpentSeconds = baseTime,
            isTimerRunning = true,
            timerStartedTimestamp = startedAt
        )

        val effective = runningArtifact.currentEffectiveSeconds
        // Should be approximately 120 + 30 = 150 seconds (allow 2s tolerance)
        assertTrue("Expected effective around 150, got $effective", effective in 149L..153L)
    }

    @Test
    fun artifactEntity_currentEffectiveSeconds_returnsBaseTimeWhenStopped() {
        val stoppedArtifact = ArtifactEntity(
            title = "Analiza epistemologiczna",
            category = "Teoria Katedry",
            proposingAgent = "Otak-Beta",
            status = "W_OPRACOWANIU",
            priority = "Średni",
            description = "Test",
            digitalPayload = "teoria",
            timeSpentSeconds = 300L,
            isTimerRunning = false,
            timerStartedTimestamp = null
        )

        assertEquals(300L, stoppedArtifact.currentEffectiveSeconds)
    }

    @Test
    fun artifactEntity_activeTrackingAgent_fallsBackProperly() {
        val artifactWithTimerAgent = ArtifactEntity(
            title = "Zadanie A",
            category = "Research",
            proposingAgent = "Otak-Alpha",
            assignedAgent = "Otak-Beta",
            reviewingAgent = "Otak-Gamma",
            activeTimerAgent = "Otak-Delta",
            status = "W_OPRACOWANIU",
            priority = "Wysoki",
            description = "Test",
            digitalPayload = "data"
        )
        assertEquals("Otak-Delta", artifactWithTimerAgent.activeTrackingAgent)

        val artifactWithAssigned = artifactWithTimerAgent.copy(activeTimerAgent = null)
        assertEquals("Otak-Beta", artifactWithAssigned.activeTrackingAgent)

        val artifactWithProposer = artifactWithAssigned.copy(assignedAgent = "")
        assertEquals("Otak-Alpha", artifactWithProposer.activeTrackingAgent)
    }
}
