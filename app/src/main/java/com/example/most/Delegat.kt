package com.example.most

/**
 * Delegat w StoL-u: rozmowa z dowolnym TeOgochi i pamięć Katedry — dane z mostu, nic zmyślonego.
 *
 * Suweren (2026-09-29): „możemy połączyć StoL z Delegatem… na stole można wybrać każdego z dostępnych",
 * oraz pamięć: „python coś trzyma… by można było to zrobić z poziomu Katedry i Stołu".
 *  · rozmówcy  — `GET /api/delegat/wszyscy` (trzy pełne profile + każdy gatunek z kartą roli),
 *  · rozmowa   — `POST /api/delegat/rozmowa` (odpowiedź po całej pętli narzędzi; model lokalny bywa wolny),
 *  · pamięć    — `GET /api/system/memory` (PID, opis procesu, „chroniony"),
 *  · zwolnij   — `POST /api/system/free {pidy}` (sparowany telefon, tylko po PID).
 */
data class Rozmowca(
    val id: String,
    val imie: String,
    val emoji: String,
    val dziedzina: String,
    /** Pełny profil (muzyka, kod, warsztat) — reszta rozmawia i czyta stan Katedry. */
    val pelny: Boolean,
    /** Narzędzia, które zadziałają z telefonu. */
    val narzedzia: List<String>,
) {
    companion object {
        fun zJson(m: Map<String, Any?>): Rozmowca? {
            val id = m.napis("id")?.takeIf { it.isNotBlank() } ?: return null
            return Rozmowca(
                id = id,
                imie = m.napis("imie") ?: id,
                emoji = m.napis("emoji") ?: "🥚",
                dziedzina = m.napis("dziedzina").orEmpty(),
                pelny = m.logika("pelny") == true,
                narzedzia = m.lista("narzedzia").filterIsInstance<String>(),
            )
        }
    }
}

/** Jedna wypowiedź w rozmowie na telefonie. `kto`: "suweren" albo "delegat". */
data class Wypowiedz(val kto: String, val tekst: String)

/** Rozmowa z TeOgochi przez Delegata: wypowiedzi na telefonie, id rozmowy z mostu, „myśli" = czeka na model. */
data class RozmowaUi(
    val z: Rozmowca,
    val wypowiedzi: List<Wypowiedz> = emptyList(),
    val rozmowaId: String? = null,
    val mysli: Boolean = false,
    val blad: String? = null,
)

data class OdpowiedzDelegata(val rozmowaId: String, val odpowiedz: String, val model: String?) {
    companion object {
        fun zJson(m: Map<String, Any?>): OdpowiedzDelegata? {
            val id = m.napis("rozmowaId") ?: return null
            return OdpowiedzDelegata(id, m.napis("odpowiedz").orEmpty(), m.napis("model"))
        }
    }
}

data class ProcesKatedry(
    val pid: Int,
    val nazwa: String,
    val mb: Int,
    /** Czym jest proces wg mostu, np. „ComfyUI (obrazy, wideo, muzyka)"; pusty, gdy nie wiadomo. */
    val opis: String,
    val skrypt: String?,
    /** Systemowy albo sam most — tego telefon nie zamyka. */
    val chroniony: Boolean,
    val uwaga: String?,
)

data class PamiecKatedry(val wolneGB: Double, val razemGB: Double, val procesy: List<ProcesKatedry>, val blad: String?) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>): PamiecKatedry = PamiecKatedry(
            wolneGB = m.liczba("freeGB") ?: 0.0,
            razemGB = m.liczba("totalGB") ?: 0.0,
            procesy = m.lista("procesy").mapNotNull { p ->
                val x = p as? Map<String, Any?> ?: return@mapNotNull null
                val pid = x.liczba("pid")?.toInt() ?: return@mapNotNull null
                ProcesKatedry(
                    pid = pid,
                    nazwa = x.napis("name").orEmpty(),
                    mb = x.liczba("mb")?.toInt() ?: 0,
                    opis = x.napis("opis").orEmpty(),
                    skrypt = x.napis("skrypt"),
                    chroniony = x.logika("chroniony") == true,
                    uwaga = x.napis("uwaga"),
                )
            },
            blad = m.napis("blad"),
        )
    }
}

/** Wynik zamykania: co zamknięto i czego nie — z powodem od mostu. */
data class WynikZwolnienia(val zamkniete: List<String>, val odmowy: List<String>) {
    /** Jedno zdanie do paska na dole ekranu. */
    val zdanie: String get() = buildList {
        if (zamkniete.isNotEmpty()) add("Zamknięto: ${zamkniete.joinToString(", ")}.")
        addAll(odmowy)
    }.joinToString(" ").ifBlank { "Nic nie zamknięto." }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>) = WynikZwolnienia(
            zamkniete = m.lista("zamkniete").mapNotNull { p -> (p as? Map<String, Any?>)?.let { "${it.napis("name").orEmpty()} #${it.liczba("pid")?.toInt() ?: "?"}" } },
            odmowy = m.lista("odmowy").mapNotNull { p -> (p as? Map<String, Any?>)?.let { "#${it.liczba("pid")?.toInt() ?: "?"}: ${it.napis("powod").orEmpty()}" } },
        )
    }
}
