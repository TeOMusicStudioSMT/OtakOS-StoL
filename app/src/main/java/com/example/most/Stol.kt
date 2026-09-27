package com.example.most

/**
 * 🪑 Stół ratyfikacji — karty propozycji tak, jak oddaje je most (`GET /api/stol`, services/Stol.js).
 *
 * Droga karty (etap liczy most z faktów — stanu Projektu Stada):
 *   na_stole → (Przyjmij) → opracowuje → do_akceptacji → (Ratyfikuj) → zratyfikowane
 *   + utknela (projekt padł — można przyjąć od nowa) i odrzucona.
 * Moduły Katedry (Marketplace, muzyka, 3D, wideo) ruszają dopiero po ratyfikacji Suwerena.
 */
data class Decyzja(val co: String, val kto: String, val kiedy: String?)

data class ZlecenieKarty(val modul: String, val opis: String, val stan: String)

data class ProjektKarty(
    val id: String,
    val stan: String,
    val gotowe: Int,
    val razem: Int,
    /** Biblia projektu (scalenie wkładów) — pusta, dopóki stado nie skończy. */
    val biblia: String,
    val zlecenia: List<ZlecenieKarty>,
)

data class KartaStolu(
    val id: String,
    val tytul: String,
    val tresc: String,
    val wizja: String,
    /** Uczestnicy zaproponowani w karcie („Uczestnicy…:") — imiona TeOgochi. */
    val sugerowani: List<String>,
    val zrodlo: String,
    val zalozyl: String?,
    val od: String?,
    val etap: String,
    val decyzje: List<Decyzja>,
    val projekt: ProjektKarty?,
) {
    val moznaPrzyjac: Boolean get() = etap == "na_stole" || etap == "utknela"
    val moznaOdrzucic: Boolean get() = etap == "na_stole" || etap == "utknela" || etap == "do_akceptacji"
    val moznaRatyfikowac: Boolean get() = etap == "do_akceptacji"
    /** Czeka na decyzję Suwerena (Izba Akceptacji). */
    val czekaNaSuwerena: Boolean get() = moznaPrzyjac || moznaRatyfikowac

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>): KartaStolu {
            val p = m.obiekt("projektSkrot")
            return KartaStolu(
                id = m.napis("id").orEmpty(),
                tytul = m.napis("tytul").orEmpty(),
                tresc = m.napis("tresc").orEmpty(),
                wizja = m.napis("wizja").orEmpty(),
                sugerowani = m.lista("sugerowani").mapNotNull { it as? String },
                zrodlo = m.napis("zrodlo") ?: "hub",
                zalozyl = m.napis("zalozyl")?.ifBlank { null },
                od = m.napis("od"),
                etap = m.napis("etap") ?: "na_stole",
                decyzje = m.lista("decyzje").mapNotNull { d ->
                    (d as? Map<String, Any?>)?.let { Decyzja(it.napis("co").orEmpty(), it.napis("kto") ?: "Katedra", it.napis("kiedy")) }
                },
                projekt = p?.let {
                    ProjektKarty(
                        id = it.napis("id").orEmpty(), stan = it.napis("stan") ?: "trwa",
                        gotowe = it.liczba("gotowe")?.toInt() ?: 0, razem = it.liczba("razem")?.toInt() ?: 0,
                        biblia = it.napis("biblia").orEmpty(),
                        zlecenia = it.lista("zlecenia").mapNotNull { z ->
                            (z as? Map<String, Any?>)?.let { x -> ZlecenieKarty(x.napis("modul").orEmpty(), x.napis("opis").orEmpty(), x.napis("stan") ?: "czeka") }
                        },
                    )
                },
            )
        }

        /** Kolejność kolumn na stole i ich nazwy dla Suwerena. */
        val ETAPY = listOf(
            "na_stole" to "Na stole",
            "opracowuje" to "Opracowuje stado",
            "do_akceptacji" to "Do akceptacji",
            "utknela" to "Utknęła",
            "zratyfikowane" to "Zratyfikowane",
            "odrzucona" to "Odłożone",
        )
        val NAZWA_ETAPU: Map<String, String> = ETAPY.toMap()

        val ZRODLA = mapOf(
            "podcast-twin" to "🎙️ Podcast Twin", "koom" to "📖 Księga KOOM", "plik" to "📄 Plik",
            "telefon" to "📱 Telefon", "hub" to "🏛️ Katedra",
        )
    }
}

/** Decyzja Suwerena nad kartą — trasa `POST /api/stol/:id/<akcja>`. */
enum class AkcjaStolu(val sciezka: String) { PRZYJMIJ("przyjmij"), ODRZUC("odrzuc"), RATYFIKUJ("ratyfikuj") }

/** Nowa karta z telefonu. Te same zasady co w moście (Stol.dodaj), żeby braki widać było od razu. */
data class NowaKarta(val tytul: String, val tresc: String) {
    fun brak(): String? = when {
        tytul.isBlank() -> "Nadaj propozycji tytuł."
        tresc.trim().length < MIN_TRESCI -> "Opisz propozycję choć jednym zdaniem."
        else -> null
    }

    fun doJson(): String =
        "{\"tytul\":${jsonNapis(tytul.trim().take(MAX_TYTULU))},\"tresc\":${jsonNapis(tresc.trim().take(MAX_TRESCI))}}"

    companion object {
        const val MIN_TRESCI = 10
        const val MAX_TYTULU = 80
        const val MAX_TRESCI = 20_000
    }
}
