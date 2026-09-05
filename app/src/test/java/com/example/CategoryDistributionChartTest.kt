package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.entity.ArtifactEntity
import com.example.ui.components.getCategoryChartColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDistributionChartTest {

    @Test
    fun getCategoryChartColor_returnsDistinctColorsForStandardCategories() {
        val researchColor = getCategoryChartColor("Research")
        val codeColor = getCategoryChartColor("Kod OtakOS")
        val designColor = getCategoryChartColor("Design & UX")
        val archColor = getCategoryChartColor("Architektura Neuronowa")
        val quantumColor = getCategoryChartColor("Algorytmy Kwantowe")
        val theoryColor = getCategoryChartColor("Teoria Katedry")

        // Check expected colors
        assertEquals(Color(0xFF6366F1), researchColor) // Indigo
        assertEquals(Color(0xFF10B981), codeColor)     // Emerald
        assertEquals(Color(0xFFEC4899), designColor)   // Pink
        assertEquals(Color(0xFF0284C7), archColor)     // Sky Blue
        assertEquals(Color(0xFF0D9488), quantumColor)  // Teal
        assertEquals(Color(0xFF8B5CF6), theoryColor)   // Violet

        // Ensure distinct colors
        assertNotEquals(researchColor, codeColor)
        assertNotEquals(codeColor, designColor)
        assertNotEquals(designColor, archColor)
    }

    @Test
    fun categoryDistributionCalculation_computesAccuratePercentagesAndAngles() {
        val tasks = listOf(
            ArtifactEntity(
                id = 1,
                title = "Zadanie 1",
                category = "Research",
                proposingAgent = "Kaliope-AI",
                assignedAgent = "Kaliope-AI",
                status = "NA_STOLE",
                priority = "Wysoki"
            ),
            ArtifactEntity(
                id = 2,
                title = "Zadanie 2",
                category = "Research",
                proposingAgent = "Kaliope-AI",
                assignedAgent = "Kaliope-AI",
                status = "NA_STOLE",
                priority = "Wysoki"
            ),
            ArtifactEntity(
                id = 3,
                title = "Zadanie 3",
                category = "Kod OtakOS",
                proposingAgent = "Neuro-Marmur",
                assignedAgent = "Neuro-Marmur",
                status = "NA_STOLE",
                priority = "Wysoki"
            ),
            ArtifactEntity(
                id = 4,
                title = "Zadanie 4",
                category = "Design & UX",
                proposingAgent = "Otak-Alpha",
                assignedAgent = "Otak-Alpha",
                status = "NA_STOLE",
                priority = "Standardowy"
            )
        )

        val total = tasks.size
        assertEquals(4, total)

        val grouped = tasks.groupBy { it.category }
        val researchCount = grouped["Research"]?.size ?: 0
        val codeCount = grouped["Kod OtakOS"]?.size ?: 0
        val designCount = grouped["Design & UX"]?.size ?: 0

        assertEquals(2, researchCount)
        assertEquals(1, codeCount)
        assertEquals(1, designCount)

        val researchPct = (researchCount.toFloat() / total) * 100f
        val codePct = (codeCount.toFloat() / total) * 100f
        val designPct = (designCount.toFloat() / total) * 100f

        assertEquals(50f, researchPct, 0.01f)
        assertEquals(25f, codePct, 0.01f)
        assertEquals(25f, designPct, 0.01f)

        val totalPercentage = researchPct + codePct + designPct
        assertEquals(100f, totalPercentage, 0.01f)

        val researchSweep = (researchCount.toFloat() / total) * 360f
        val codeSweep = (codeCount.toFloat() / total) * 360f
        val designSweep = (designCount.toFloat() / total) * 360f

        assertEquals(180f, researchSweep, 0.01f)
        assertEquals(90f, codeSweep, 0.01f)
        assertEquals(90f, designSweep, 0.01f)

        val totalSweep = researchSweep + codeSweep + designSweep
        assertEquals(360f, totalSweep, 0.01f)
    }

    @Test
    fun agentFiltering_filtersTasksCorrectlyForAgentDistribution() {
        val tasks = listOf(
            ArtifactEntity(
                id = 1,
                title = "Zadanie 1",
                category = "Research",
                proposingAgent = "Kaliope-AI",
                assignedAgent = "Kaliope-AI",
                status = "NA_STOLE",
                priority = "Wysoki"
            ),
            ArtifactEntity(
                id = 2,
                title = "Zadanie 2",
                category = "Kod OtakOS",
                proposingAgent = "Neuro-Marmur",
                assignedAgent = "Neuro-Marmur",
                status = "NA_STOLE",
                priority = "Wysoki"
            )
        )

        val kaliopeTasks = tasks.filter { it.assignedAgent == "Kaliope-AI" || it.proposingAgent == "Kaliope-AI" }
        assertEquals(1, kaliopeTasks.size)
        assertEquals("Research", kaliopeTasks.first().category)

        val marmurTasks = tasks.filter { it.assignedAgent == "Neuro-Marmur" || it.proposingAgent == "Neuro-Marmur" }
        assertEquals(1, marmurTasks.size)
        assertEquals("Kod OtakOS", marmurTasks.first().category)
    }
}
