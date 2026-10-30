package com.example.most

/**
 * 📯 Wieści kanału Mistrza (`GET /api/mistrz/wiesci?od=N`) — to, co Katedra mówi sama:
 * 🌍 ktoś wszedł na teo.center, ☀️ poranny brief, wyniki Kodeksa, przekazy z pola.
 * StoL sprawdza je w tle i pokazuje jako powiadomienia Androida (ui/powiadomienia).
 * Most numeruje wieści rosnąco (`nr`), więc „nowe” = nr większy niż ostatnio widziany.
 */
data class Wiesc(val nr: Int, val kiedy: String, val rodzaj: String, val skad: String, val tresc: String) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun lista(m: Map<String, Any?>): List<Wiesc> = m.lista("wiesci").mapNotNull { x ->
            val w = x as? Map<String, Any?> ?: return@mapNotNull null
            val nr = w.liczba("nr")?.toInt() ?: return@mapNotNull null
            val tresc = w.napis("tresc")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Wiesc(nr, w.napis("kiedy").orEmpty(), w.napis("rodzaj") ?: "pole", w.napis("skad") ?: "Katedra", tresc)
        }

        fun ostatni(m: Map<String, Any?>): Int = m.liczba("ostatni")?.toInt() ?: 0

        /** Rodzaje = osobne kanały powiadomień Androida: Suweren wyłącza każdy w ustawieniach powiadomień StoL-a. */
        val RODZAJE = linkedMapOf(
            "wejscie" to "🌍 Wejścia na strony",
            "brief" to "☀️ Poranny brief",
            "pole" to "📯 Przekazy i wieści Mistrza",
        )

        /** Rodzaj, który nie ma własnego przełącznika, idzie razem z „pole”. */
        fun grupa(rodzaj: String) = if (rodzaj in RODZAJE) rodzaj else "pole"
    }
}

/**
 * Co pokazać po jednym sprawdzeniu. Czysta funkcja — testowana na JVM.
 *  · `widziane` < 0 = pierwsze sprawdzenie po instalacji: nic nie pokazujemy (bez zalewu historii),
 *    tylko zapamiętujemy, dokąd doszedł kanał.
 *  · Najwyżej `max` pojedynczych powiadomień; reszta = `pominiete` (do jednego zbiorczego).
 */
data class DoPowiadomienia(val pokaz: List<Wiesc>, val pominiete: Int, val noweWidziane: Int)

fun doPowiadomienia(wiesci: List<Wiesc>, ostatni: Int, widziane: Int, wylaczone: Set<String>, max: Int = 4): DoPowiadomienia {
    val dokad = maxOf(ostatni, wiesci.maxOfOrNull { it.nr } ?: 0, widziane)
    if (widziane < 0) return DoPowiadomienia(emptyList(), 0, dokad)
    val nowe = wiesci.filter { it.nr > widziane && Wiesc.grupa(it.rodzaj) !in wylaczone }.sortedBy { it.nr }
    return DoPowiadomienia(nowe.takeLast(max), maxOf(0, nowe.size - max), dokad)
}
