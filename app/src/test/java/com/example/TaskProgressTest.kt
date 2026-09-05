package com.example

import com.example.data.entity.ArtifactEntity
import com.example.ui.components.calculateTaskProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskProgressTest {

    @Test
    fun `initial task on table reflects initialization progress`() {
        val artifact = ArtifactEntity(
            id = 1,
            title = "Nowy szkic architektury",
            category = "Architektura Neuronowa",
            proposingAgent = "Otak-Alpha",
            status = "NA_STOLE",
            priority = "Standardowy",
            description = "Wstępna propozycja",
            digitalPayload = "// Code",
            consensusScore = 50,
            iterationsCount = 1,
            collaboratingAgents = ""
        )

        val progress = calculateTaskProgress(artifact)
        assertEquals(0, progress.stageIndex)
        assertEquals("Inicjacja zadania", progress.stageTitle)
        assertFalse(progress.isCompleted)
        assertTrue(progress.percentage in 10..26)
    }

    @Test
    fun `agent interactions dynamically increase task progress`() {
        val baseArtifact = ArtifactEntity(
            id = 2,
            title = "Kwantowy Algorytm Spójności",
            category = "Algorytmy Kwantowe",
            proposingAgent = "Vektor-9",
            status = "W_OPRACOWANIU",
            priority = "Wysoki",
            description = "Algorytm w trakcie rozwoju",
            digitalPayload = "// Algorithm",
            consensusScore = 60,
            iterationsCount = 2,
            collaboratingAgents = "Kaliope-AI"
        )

        val initialProgress = calculateTaskProgress(baseArtifact)
        assertEquals(1, initialProgress.stageIndex)
        assertEquals("Kooperacja agentów", initialProgress.stageTitle)

        // Agent Vektor-9 and Neuro-Marmur collaborate, increasing iterations and consensus
        val collaboratedArtifact = baseArtifact.copy(
            iterationsCount = 5,
            consensusScore = 88,
            collaboratingAgents = "Kaliope-AI, Neuro-Marmur, Otak-Alpha"
        )
        val advancedProgress = calculateTaskProgress(collaboratedArtifact)

        assertTrue(advancedProgress.percentage > initialProgress.percentage)
        assertEquals(1, advancedProgress.stageIndex)
        assertTrue(advancedProgress.percentage in 32..84)
    }

    @Test
    fun `artifact submitted for ratification reflects near completion status`() {
        val artifact = ArtifactEntity(
            id = 3,
            title = "Traktat o Perłowych Strukturach",
            category = "Teoria Katedry",
            proposingAgent = "Kaliope-AI",
            status = "DO_AKCEPTACJI",
            priority = "Kluczowy",
            description = "Oczekiwanie na ratyfikację",
            digitalPayload = "// Text",
            consensusScore = 96,
            iterationsCount = 6,
            collaboratingAgents = "Otak-Alpha, Vektor-9"
        )

        val progress = calculateTaskProgress(artifact)
        assertEquals(2, progress.stageIndex)
        assertEquals("Gotowe do akceptacji", progress.stageTitle)
        assertFalse(progress.isCompleted)
        assertTrue(progress.percentage in 88..98)
    }

    @Test
    fun `ratified artifact reaches 100 percent completion`() {
        val artifact = ArtifactEntity(
            id = 4,
            title = "Sterownik Akceleracji Tensorowej",
            category = "Kod OtakOS",
            proposingAgent = "Neuro-Marmur",
            status = "ZRATYFIKOWANE",
            priority = "Kluczowy",
            description = "Zratyfikowane dzieło",
            digitalPayload = "// Native",
            consensusScore = 100,
            iterationsCount = 8,
            collaboratingAgents = "Neuro-Marmur, Otak-Alpha, Vektor-9"
        )

        val progress = calculateTaskProgress(artifact)
        assertEquals(100, progress.percentage)
        assertEquals(1.0f, progress.fraction, 0.001f)
        assertEquals(3, progress.stageIndex)
        assertEquals("Zratyfikowane", progress.stageTitle)
        assertTrue(progress.isCompleted)
    }
}
