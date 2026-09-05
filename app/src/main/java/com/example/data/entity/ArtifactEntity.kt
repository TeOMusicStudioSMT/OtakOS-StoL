package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Reprezentuje cyfrowy artefakt / zawartość położoną na stole Katedry OtakOS.
 */
@Entity(tableName = "stol_artifacts")
data class ArtifactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // np. "Architektura Neuronowa", "Algorytmy Kwantowe", "Kod OtakOS", "Teoria Katedry", "Synteza Danych"
    val proposingAgent: String, // np. "Otak-Alpha", "Vektor-9", "Kaliope-AI", "Neuro-Marmur", "Katedra OtakOS"
    val assignedAgent: String = "", // Przypisany główny opiekun zadania (jeśli puste, domyślnie proposingAgent)
    val reviewingAgent: String? = null, // Agent aktualnie recenzujący zadanie (np. "Vektor-9", "Kaliope-AI")
    val reviewStatus: String? = null, // np. "W TRAKCIE RECENZJI", "AUDYT KODU", "WERYFIKACJA KONSENSUSU"
    val status: String, // "NA_STOLE", "W_OPRACOWANIU", "DO_AKCEPTACJI", "ZRATYFIKOWANE"
    val priority: String, // "Kluczowy", "Wysoki", "Eksperymentalny", "Standardowy"
    val description: String = "",
    val digitalPayload: String = "", // Kod źródłowy, matematyczna formuła, struktura wiedzy
    val consensusScore: Int = 50, // 0-100%
    val iterationsCount: Int = 1,
    val collaboratingAgents: String = "", // np. "Vektor-9, Kaliope-AI"
    val timeSpentSeconds: Long = 0L, // Łączny czas spędzony przez agentów AI nad zadaniem w sekundach
    val isTimerRunning: Boolean = false, // Czy stoper zadania jest aktualnie aktywny
    val timerStartedTimestamp: Long? = null, // Czas rozpoczęcia bieżącej sesji stopera
    val activeTimerAgent: String? = null, // Agent aktualnie rejestrujący czas (lub przypisany opiekun)
    val ratificationNotes: String? = null,
    val ratifiedTimestamp: Long? = null,
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    val effectiveAssignedAgent: String
        get() = if (assignedAgent.isNotBlank()) assignedAgent else proposingAgent

    val isUnderActiveReview: Boolean
        get() = !reviewingAgent.isNullOrBlank() && status != "ZRATYFIKOWANE"

    val currentEffectiveSeconds: Long
        get() {
            if (!isTimerRunning || timerStartedTimestamp == null) {
                return timeSpentSeconds
            }
            val elapsed = ((System.currentTimeMillis() - timerStartedTimestamp) / 1000).coerceAtLeast(0)
            return timeSpentSeconds + elapsed
        }

    val activeTrackingAgent: String
        get() = activeTimerAgent ?: effectiveAssignedAgent
}

