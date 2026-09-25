package com.example.most

/**
 * Wspólny projekt stada TeOgochi, tak jak oddaje go most (`GET /api/stado/projekty`,
 * services/ProjektStada.js → skrot). Telefon może projekt ZAŁOŻYĆ (`POST /api/stado/projekt/nowy`,
 * token sparowanego urządzenia) — dalszą pracę robi Katedra: stado pisze wkłady, a te same
 * zlecają moduły (Marketplace, muzyka, 3D, wideo).
 */
data class KrokProjektu(val agent: String, val imie: String, val zadanie: String, val model: String?, val stan: String, val fala: Int)

data class ZlecenieProjektu(val id: String, val modul: String, val imie: String, val opis: String, val stan: String)

data class ProjektStada(
    val id: String,
    val nazwa: String,
    val wizja: String,
    /** trwa | gotowe | czesciowo | blad | przerwany */
    val stan: String,
    val od: String?,
    /** Nazwa urządzenia, z którego założono projekt; null = przy Katedrze. */
    val zalozyl: String?,
    val gotowe: Int,
    val razem: Int,
    val kroki: List<KrokProjektu>,
    val zlecenia: List<ZlecenieProjektu>,
) {
    val zleceniaGotowe: Int get() = zlecenia.count { it.stan == "gotowe" }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>) = ProjektStada(
            id = m.napis("id").orEmpty(),
            nazwa = m.napis("nazwa").orEmpty(),
            wizja = m.napis("wizja").orEmpty(),
            stan = m.napis("stan") ?: "trwa",
            od = m.napis("od"),
            zalozyl = m.napis("zalozyl")?.ifBlank { null },
            gotowe = m.liczba("gotowe")?.toInt() ?: 0,
            razem = m.liczba("razem")?.toInt() ?: 0,
            kroki = m.lista("kroki").mapNotNull { k ->
                (k as? Map<String, Any?>)?.let {
                    KrokProjektu(
                        agent = it.napis("agent").orEmpty(), imie = it.napis("imie").orEmpty(),
                        zadanie = it.napis("zadanie").orEmpty(), model = it.napis("model"),
                        stan = it.napis("stan") ?: "czeka", fala = it.liczba("fala")?.toInt() ?: 2,
                    )
                }
            },
            zlecenia = m.lista("zlecenia").mapNotNull { z ->
                (z as? Map<String, Any?>)?.let {
                    ZlecenieProjektu(
                        id = it.napis("id").orEmpty(), modul = it.napis("modul").orEmpty(), imie = it.napis("imie").orEmpty(),
                        opis = it.napis("opis").orEmpty(), stan = it.napis("stan") ?: "czeka",
                    )
                }
            },
        )
    }
}

/**
 * Szkic nowego projektu z formularza. Te same zasady co w moście (ProjektStada.zaloz),
 * żeby telefon powiedział „czego brakuje" od razu, bez rundy przez tunel.
 */
data class NowyProjekt(
    val nazwa: String,
    val wizja: String,
    val uczestnicy: List<String>,
    val samoZlecanie: Boolean = true,
) {
    /** null = można wysyłać; inaczej zdanie dla Suwerena. */
    fun brak(): String? = when {
        nazwa.isBlank() -> "Nadaj projektowi nazwę."
        wizja.trim().length < MIN_WIZJI -> "Opisz wizję choć jednym zdaniem."
        uczestnicy.distinct().size < 2 -> "Wspólny projekt potrzebuje co najmniej dwóch TeOgochi."
        else -> null
    }

    fun doJson(): String = buildString {
        append("{\"nazwa\":").append(jsonNapis(nazwa.trim().take(MAX_NAZWY)))
        append(",\"wizja\":").append(jsonNapis(wizja.trim().take(MAX_WIZJI)))
        append(",\"uczestnicy\":[").append(uczestnicy.distinct().joinToString(",") { jsonNapis(it) }).append(']')
        append(",\"samoZlecanie\":").append(samoZlecanie)
        append('}')
    }

    companion object {
        const val MIN_WIZJI = 10
        const val MAX_NAZWY = 80
        const val MAX_WIZJI = 3000
    }
}
