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

/** „przed chwilą", „12 min temu", „3 h temu" — do napisu „migawka sprzed…". */
fun ileTemu(sekundy: Long): String = when {
    sekundy < 60 -> "przed chwilą"
    sekundy < 3600 -> "${sekundy / 60} min temu"
    sekundy < 86400 -> "${sekundy / 3600} h temu"
    else -> "${sekundy / 86400} dni temu"
}
