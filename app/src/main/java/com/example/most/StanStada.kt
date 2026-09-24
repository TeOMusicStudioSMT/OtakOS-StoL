package com.example.most

/**
 * Stan stada TeOgochi tak, jak oddaje go most (`GET /api/stado/stan` z tokenem urządzenia,
 * services/MostStada.js → stanDlaApki). Nic tu nie jest wymyślone: gdy most czegoś nie wie,
 * pole jest null i ekran mówi „cisza", zamiast wstawiać „analizuje spójność macierzy".
 */
data class Gatunek(
    val id: String,
    val imie: String,
    val dziedzina: String,
    /** Kolor akcentu z Katedry, np. "#a855f7". */
    val kolor: String,
    /** Wygląd na obecnym etapie (emoji z Katedry). */
    val forma: String,
    val etap: String,
    val xp: Int,
    val wyklute: Boolean,
    /** Ostatni fakt z szyny zdarzeń mostu — albo null, gdy agent milczy. */
    val robi: String?,
    /** Kiedy (ms epoki) padł ten fakt. */
    val robiOd: Long?,
)

data class Aktywnosc(val kto: String, val rodzaj: String?, val tresc: String?, val ts: Long)

data class StanStada(
    /** Nazwa, pod którą most zna ten telefon. */
    val urzadzenie: String?,
    /** Ile sekund ma migawka z Katedry; null = Katedra jeszcze nic nie opublikowała. */
    val wiekMigawkiSekund: Long?,
    val aktywny: String?,
    val gatunki: List<Gatunek>,
    val aktywnosc: List<Aktywnosc>,
    /** Wyjaśnienie od mostu, gdy migawki brak. */
    val powod: String?,
) {
    val wyklute: List<Gatunek> get() = gatunki.filter { it.wyklute }

    /**
     * Nałóż zdarzenie ze strumienia na stan — ta sama zasada co w moście (stanDlaApki):
     * agent pasuje do gatunku po imieniu albo id (bez wielkości liter); aktywność trzyma
     * ostatni ślad każdego agenta, najnowsze pierwsze.
     */
    fun zZdarzeniem(z: ZdarzenieSzyny, teraz: Long = System.currentTimeMillis()): StanStada {
        val kto = z.agent.lowercase()
        if (kto.isBlank()) return this
        val kiedy = z.kiedy?.let(::czasIso) ?: teraz
        val tresc = z.tresc.ifBlank { z.rodzaj }.take(160)
        return copy(
            gatunki = gatunki.map { g ->
                if (g.imie.lowercase() == kto || g.id.lowercase() == kto) g.copy(robi = tresc, robiOd = kiedy) else g
            },
            aktywnosc = listOf(Aktywnosc(kto, z.rodzaj, z.tresc, kiedy)) + aktywnosc.filter { it.kto != kto },
        )
    }

    companion object {
        fun zJson(m: Map<String, Any?>): StanStada {
            val migawka = m.obiekt("migawka")
            return StanStada(
                urzadzenie = m.napis("urzadzenie"),
                wiekMigawkiSekund = migawka?.liczba("wiekSekund")?.toLong(),
                aktywny = migawka?.napis("aktywny")?.ifBlank { null },
                gatunki = m.lista("gatunki").mapNotNull { g ->
                    @Suppress("UNCHECKED_CAST")
                    (g as? Map<String, Any?>)?.let {
                        Gatunek(
                            id = it.napis("id").orEmpty(),
                            imie = it.napis("imie").orEmpty(),
                            dziedzina = it.napis("dziedzina").orEmpty(),
                            kolor = it.napis("kolor") ?: "#94a3b8",
                            forma = it.napis("forma") ?: "🥚",
                            etap = it.napis("etap") ?: "jajko",
                            xp = it.liczba("xp")?.toInt() ?: 0,
                            wyklute = it.logika("wyklute") ?: false,
                            robi = it.napis("robi")?.ifBlank { null },
                            robiOd = it.liczba("robiOd")?.toLong()?.takeIf { t -> t > 0 },
                        )
                    }
                },
                aktywnosc = m.lista("aktywnosc").mapNotNull { a ->
                    @Suppress("UNCHECKED_CAST")
                    (a as? Map<String, Any?>)?.let {
                        Aktywnosc(it.napis("kto").orEmpty(), it.napis("rodzaj"), it.napis("tresc"), it.liczba("ts")?.toLong() ?: 0L)
                    }
                }.sortedByDescending { it.ts },
                powod = m.napis("powod"),
            )
        }
    }
}

/**
 * Czas ISO z mostu (`2026-09-24T21:38:04.234Z`, zawsze UTC) → ms epoki.
 * SimpleDateFormat, nie java.time: StoL ma minSdk 24, a java.time jest od API 26.
 */
fun czasIso(iso: String): Long? = runCatching {
    val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ROOT)
    f.timeZone = java.util.TimeZone.getTimeZone("UTC")
    f.parse(iso)?.time
}.getOrNull()

/** „przed chwilą", „12 min temu", „3 h temu" — do napisu „migawka sprzed…". */
fun ileTemu(sekundy: Long): String = when {
    sekundy < 60 -> "przed chwilą"
    sekundy < 3600 -> "${sekundy / 60} min temu"
    sekundy < 86400 -> "${sekundy / 3600} h temu"
    else -> "${sekundy / 86400} dni temu"
}
