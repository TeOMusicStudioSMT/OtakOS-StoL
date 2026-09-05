package com.example.ui.model

import androidx.compose.ui.graphics.Color

data class OtakAgent(
    val id: String,
    val name: String,
    val role: String,
    val specialty: String,
    val primaryColor: Color,
    val accentColor: Color,
    val avatarSymbol: String,
    val statusText: String
)

object OtakDepartment {
    val agents = listOf(
        OtakAgent(
            id = "otak_alpha",
            name = "Otak-Alpha",
            role = "Arcymistrz Katedry",
            specialty = "Architektura Systemowa i Nadzór Konsensusu",
            primaryColor = Color(0xFF0284C7), // Sky blue
            accentColor = Color(0xFF38BDF8),
            avatarSymbol = "Ω",
            statusText = "Koordynuje stół myśli"
        ),
        OtakAgent(
            id = "vektor_9",
            name = "Vektor-9",
            role = "Architekt Algorytmów",
            specialty = "Optymalizacja Tensorowa i Stany Kwantowe",
            primaryColor = Color(0xFF0D9488), // Teal
            accentColor = Color(0xFF2DD4BF),
            avatarSymbol = "∇",
            statusText = "Analizuje spójność macierzy"
        ),
        OtakAgent(
            id = "kaliope_ai",
            name = "Kaliope-AI",
            role = "Syntezator Konceptualny",
            specialty = "Manifesty, Teoria i Redagowanie Dzieł",
            primaryColor = Color(0xFF9333EA), // Purple
            accentColor = Color(0xFFC084FC),
            avatarSymbol = "✦",
            statusText = "Formułuje syntezy"
        ),
        OtakAgent(
            id = "neuro_marmur",
            name = "Neuro-Marmur",
            role = "Strażnik Integralności",
            specialty = "Jądro Katedry, Pamięć Lokalna i Wydajność",
            primaryColor = Color(0xFFD97706), // Amber Gold
            accentColor = Color(0xFFFBBF24),
            avatarSymbol = "❖",
            statusText = "Pilnuje stabilności stołu"
        ),
        OtakAgent(
            id = "chronos_log",
            name = "Chronos-Log",
            role = "Archiwista Katedry",
            specialty = "Rejestr Czasowy i Dowody Konsensusu",
            primaryColor = Color(0xFF475569), // Slate
            accentColor = Color(0xFF94A3B8),
            avatarSymbol = "⏱",
            statusText = "Zapisuje historię"
        ),
        OtakAgent(
            id = "przewodniczacy",
            name = "Przewodniczący Katedry",
            role = "Głos Ludzki / OtakOS",
            specialty = "Inicjacja Zadań i Ostateczna Ratyfikacja",
            primaryColor = Color(0xFF059669), // Emerald
            accentColor = Color(0xFF34D399),
            avatarSymbol = "👑",
            statusText = "Obecny przy stole"
        )
    )

    fun getAgentByName(name: String): OtakAgent {
        return agents.find { it.name.equals(name, ignoreCase = true) }
            ?: agents.first()
    }
}
