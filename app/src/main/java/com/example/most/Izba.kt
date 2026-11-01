package com.example.most

/**
 * 📥 Izba zgód (Suweren 2026-11-01: „te zatwierdzenia niech idą na StoL… bo się nam to rozrasta, nie ogarniam tej Kuwety”).
 * Jedna lista wszystkich ✓ z Katedry — `GET /api/izba` (services/IzbaZgod.js w teo-app-hub). Moduły Katedry rejestrują
 * w moście swoje źródła (publikacje YouTube, nowe Katedry w rejestrze, Warsztat Stada…), telefon pokazuje je jednym
 * wzorem karty i decyduje `POST /api/izba/:zrodlo/:id/:akcja {powod}`. Akcja bez `zdalnie` = tylko przy Katedrze
 * (koszt, GPU) — telefon pokazuje ją wyszarzoną z wyjaśnieniem.
 */
data class AkcjaIzby(val id: String, val etykieta: String, val zdalnie: Boolean, val potwierdz: String?)

data class PozycjaIzby(
    val zrodlo: String,
    val emoji: String,
    val nazwaZrodla: String,
    val id: String,
    val tytul: String,
    val opis: String,
    val kiedy: String?,
    val pilne: Boolean,
    /** Ile podglądów ma pozycja — obraz `nr` pod `GET /api/izba/obraz/:zrodlo/:id/:nr`. */
    val obrazy: Int,
    val akcje: List<AkcjaIzby>,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>): PozycjaIzby? {
            val zrodlo = m.napis("zrodlo")?.takeIf { it.isNotBlank() } ?: return null
            val id = m.napis("id")?.takeIf { it.isNotBlank() } ?: return null
            return PozycjaIzby(
                zrodlo = zrodlo, id = id,
                emoji = m.napis("emoji") ?: "📌",
                nazwaZrodla = m.napis("nazwaZrodla") ?: zrodlo,
                tytul = m.napis("tytul").orEmpty(),
                opis = m.napis("opis").orEmpty(),
                kiedy = m.napis("kiedy"),
                pilne = m.logika("pilne") == true,
                obrazy = m.liczba("obrazy")?.toInt() ?: 0,
                akcje = m.lista("akcje").mapNotNull { a ->
                    (a as? Map<String, Any?>)?.let { x ->
                        val aid = x.napis("id")?.takeIf { it.isNotBlank() } ?: return@let null
                        AkcjaIzby(aid, x.napis("etykieta") ?: aid, x.logika("zdalnie") == true, x.napis("potwierdz"))
                    }
                },
            )
        }
    }
}

/** Źródło, którego most nie odczytał (padło albo nie zdążyło) — telefon mówi to wprost, zamiast udawać pustą Izbę. */
data class NieodczytaneZrodlo(val zrodlo: String, val emoji: String, val nazwa: String, val powod: String)

data class StanIzby(val pozycje: List<PozycjaIzby>, val nieOdczytane: List<NieodczytaneZrodlo>, val zrodla: List<String>) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>): StanIzby = StanIzby(
            pozycje = m.lista("pozycje").mapNotNull { (it as? Map<String, Any?>)?.let(PozycjaIzby::zJson) },
            nieOdczytane = m.lista("nieOdczytane").mapNotNull { x ->
                (x as? Map<String, Any?>)?.let { NieodczytaneZrodlo(it.napis("zrodlo").orEmpty(), it.napis("emoji") ?: "🔇", it.napis("nazwa").orEmpty(), it.napis("powod").orEmpty()) }
            },
            zrodla = m.lista("zrodla").mapNotNull { (it as? Map<String, Any?>)?.napis("id") },
        )
    }
}
