package com.example

import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakDepartment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentStatusIndicatorTest {

    @Test
    fun `effectiveAssignedAgent defaults to proposingAgent if not explicitly assigned`() {
        val artifact = ArtifactEntity(
            id = 10,
            title = "Protokół Mostu Kwantowego",
            category = "Architektura Neuronowa",
            proposingAgent = "Otak-Alpha",
            status = "NA_STOLE",
            priority = "Wysoki",
            description = "Test przydziału",
            digitalPayload = "val bridge = true",
            assignedAgent = ""
        )

        assertEquals("Otak-Alpha", artifact.effectiveAssignedAgent)
        val agent = OtakDepartment.getAgentByName(artifact.effectiveAssignedAgent)
        assertEquals("Otak-Alpha", agent.name)
        assertEquals("Ω", agent.avatarSymbol)
    }

    @Test
    fun `effectiveAssignedAgent uses explicitly assigned agent when present`() {
        val artifact = ArtifactEntity(
            id = 11,
            title = "Optymalizacja Tensorów Pamięci",
            category = "Kod OtakOS",
            proposingAgent = "Otak-Alpha",
            assignedAgent = "Vektor-9",
            status = "W_OPRACOWANIU",
            priority = "Krytyczny",
            description = "Zadanie z dedykowanym opiekunem",
            digitalPayload = "fun optimize() {}"
        )

        assertEquals("Vektor-9", artifact.effectiveAssignedAgent)
        val agent = OtakDepartment.getAgentByName(artifact.effectiveAssignedAgent)
        assertEquals("Vektor-9", agent.name)
        assertEquals("∇", agent.avatarSymbol)
    }

    @Test
    fun `active review status is correctly reflected`() {
        val taskUnderReview = ArtifactEntity(
            id = 12,
            title = "Synteza Semantyczna Rdzenia",
            category = "Teoria Katedry",
            proposingAgent = "Otak-Alpha",
            assignedAgent = "Kaliope-AI",
            reviewingAgent = "Neuro-Marmur",
            reviewStatus = "Audyt Architektury",
            status = "W_OPRACOWANIU",
            priority = "Wysoki",
            description = "Zadanie w trakcie weryfikacji przez recenzenta",
            digitalPayload = "fun core() {}"
        )

        assertTrue(taskUnderReview.isUnderActiveReview)
        assertEquals("Neuro-Marmur", taskUnderReview.reviewingAgent)
        assertEquals("Audyt Architektury", taskUnderReview.reviewStatus)

        val reviewer = OtakDepartment.getAgentByName(taskUnderReview.reviewingAgent!!)
        assertEquals("Neuro-Marmur", reviewer.name)
        assertEquals("❖", reviewer.avatarSymbol)
    }

    @Test
    fun `ratified task is no longer considered under active review`() {
        val ratifiedTask = ArtifactEntity(
            id = 13,
            title = "Kanon Jądra OtakOS v1.0",
            category = "Kod OtakOS",
            proposingAgent = "Otak-Alpha",
            assignedAgent = "Otak-Alpha",
            reviewingAgent = "Vektor-9",
            reviewStatus = "Rada Katedry zatwierdziła",
            status = "ZRATYFIKOWANE",
            priority = "Krytyczny",
            description = "Dzieło ratyfikowane",
            digitalPayload = "// Canon code"
        )

        assertFalse(ratifiedTask.isUnderActiveReview)
    }
}
