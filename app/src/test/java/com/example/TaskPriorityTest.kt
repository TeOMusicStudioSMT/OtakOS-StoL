package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.components.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPriorityTest {

    @Test
    fun fromString_parsesHighPriorityVariants() {
        val variants = listOf("High", "high", "WYSOKI", "Wysoki", "Kluczowy", "kluczowy", "Pilny", "Critical", "Urgent")
        for (variant in variants) {
            val priority = TaskPriority.fromString(variant)
            assertEquals("Expected HIGH for variant '$variant'", TaskPriority.HIGH, priority)
            assertTrue(priority.isUrgent)
            assertEquals("High", priority.label)
            assertEquals("Wysoki", priority.localizedLabel)
            assertEquals(Color(0xFFEF4444), priority.color)
        }
    }

    @Test
    fun fromString_parsesMediumPriorityVariants() {
        val variants = listOf("Medium", "medium", "Średni", "sredni", "Standardowy", "Normalny", "Eksperymentalny")
        for (variant in variants) {
            val priority = TaskPriority.fromString(variant)
            assertEquals("Expected MEDIUM for variant '$variant'", TaskPriority.MEDIUM, priority)
            assertFalse(priority.isUrgent)
            assertEquals("Medium", priority.label)
            assertEquals("Średni", priority.localizedLabel)
            assertEquals(Color(0xFFF59E0B), priority.color)
        }
    }

    @Test
    fun fromString_parsesLowPriorityVariants() {
        val variants = listOf("Low", "low", "Niski", "niski", "Drobny", "Drugorzędny")
        for (variant in variants) {
            val priority = TaskPriority.fromString(variant)
            assertEquals("Expected LOW for variant '$variant'", TaskPriority.LOW, priority)
            assertFalse(priority.isUrgent)
            assertEquals("Low", priority.label)
            assertEquals("Niski", priority.localizedLabel)
            assertEquals(Color(0xFF64748B), priority.color)
        }
    }

    @Test
    fun fromString_fallbackToMediumForUnknownOrNull() {
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString(null))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString(""))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString("UnknownPriority"))
    }

    @Test
    fun priorityLevels_allowDescendingSortingForUrgentFirst() {
        val priorities = listOf(TaskPriority.LOW, TaskPriority.HIGH, TaskPriority.MEDIUM)
        val sortedDesc = priorities.sortedByDescending { it.level }

        assertEquals(listOf(TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW), sortedDesc)
    }
}
