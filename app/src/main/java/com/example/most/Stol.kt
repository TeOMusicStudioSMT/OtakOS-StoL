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

/** Rundy tego projektu zaplanowane na Nocną Zmianę (Katedra robi je, gdy Suweren śpi). */
data class NocneZadanie(val id: String, val stan: String, val wykonane: Int, val powtorzenia: Int, val rundy: Int, val blad: String?)

/** Nocna Zmiana z punktu widzenia karty: czy w ogóle jest włączona i co czeka dla tego projektu. */
data class NocnaKarty(val wlaczona: Boolean, val zadania: List<NocneZadanie>)

/** Ocena Sędziego po rundzie: zgodność Biblii z wizją 0–10 (null = model nie trzymał formatu). */
data class OcenaRundy(val runda: Int, val ocena: Int?)

data class ProjektKarty(
    val id: String,
    val stan: String,
    val gotowe: Int,
    val razem: Int,
    /** Biblia projektu (scalenie wkładów) — pusta, dopóki stado nie skończy. */
    val biblia: String,
    val zlecenia: List<ZlecenieKarty>,
    /** Rundy doskonalenia: która teraz i ile w planie; pętla kreatywna na punkt planu. */
    val runda: Int = 1,
    val rundy: Int = 1,
    val petla: Int = 0,
    val oceny: List<OcenaRundy> = emptyList(),
    /** Braki wskazane przez Sędziego po ostatniej rundzie — na nich stado buduje dalej. */
    val braki: List<String> = emptyList(),
) {
    val ostatniaOcena: Int? get() = oceny.lastOrNull()?.ocena
}

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
    /** null = stary most albo karta bez projektu. */
    val nocna: NocnaKarty? = null,
) {
    val moznaPrzyjac: Boolean get() = etap == "na_stole" || etap == "utknela"
    val moznaOdrzucic: Boolean get() = etap == "na_stole" || etap == "utknela" || etap == "do_akceptacji"
    val moznaRatyfikowac: Boolean get() = etap == "do_akceptacji"
    /** Kolejne rundy doskonalenia na brakach Sędziego — przed ratyfikacją albo po niej (wtedy znów do akceptacji). */
    val moznaDoskonalic: Boolean get() = etap == "do_akceptacji" || etap == "zratyfikowane"
    /** Rundy na Nocną Zmianę: projekt już był w pracy stada. */
    val moznaNaNoc: Boolean get() = projekt != null && (moznaDoskonalic || etap == "opracowuje")
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
                        runda = it.liczba("runda")?.toInt() ?: 1,
                        rundy = it.liczba("rundy")?.toInt() ?: 1,
                        petla = it.liczba("petla")?.toInt() ?: 0,
                        oceny = it.lista("oceny").mapNotNull { o ->
                            (o as? Map<String, Any?>)?.let { x -> OcenaRundy(x.liczba("runda")?.toInt() ?: 1, x.liczba("ocena")?.toInt()) }
                        },
                        braki = it.lista("braki").mapNotNull { b -> b as? String },
                    )
                },
                nocna = m.obiekt("nocna")?.let { n ->
                    NocnaKarty(
                        wlaczona = n.logika("wlaczona") ?: false,
                        zadania = n.lista("zadania").mapNotNull { z ->
                            (z as? Map<String, Any?>)?.let {
                                NocneZadanie(
                                    id = it.napis("id").orEmpty(), stan = it.napis("stan") ?: "czeka",
                                    wykonane = it.liczba("wykonane")?.toInt() ?: 0, powtorzenia = it.liczba("powtorzenia")?.toInt() ?: 1,
                                    rundy = it.liczba("rundy")?.toInt() ?: 1, blad = it.napis("blad"),
                                )
                            }
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
enum class AkcjaStolu(val sciezka: String) { PRZYJMIJ("przyjmij"), ODRZUC("odrzuc"), RATYFIKUJ("ratyfikuj"), DOSKONAL("doskonal"), NOCNA("nocna") }

/**
 * Jak stado ma pracować nad kartą: rundy doskonalenia (1–5, Sędzia ocenia po każdej, ≥ 9/10 kończy
 * wcześniej) i pętla kreatywna na każdym punkcie planu (0–3). Te same granice co w moście.
 */
data class Warsztat(val rundy: Int = 1, val petla: Int = 0, val powtorzenia: Int = 1) {
    fun wGranicach() = Warsztat(rundy.coerceIn(1, MAX_RUND), petla.coerceIn(0, MAX_PETLI), powtorzenia.coerceIn(1, MAX_POWTORZEN))

    companion object {
        const val MAX_RUND = 5
        const val MAX_PETLI = 3
        const val CEL_OCENY = 9
        /** Nocna Zmiana: ile razy powtórzyć rundy (services/NocnaZmiana.js → MAX_POWTORZEN). */
        const val MAX_POWTORZEN = 20
    }
}

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

/**
 * 🔊 Co Stół ma powiedzieć na głos po odświeżeniu (Suweren: „niech stół ma powiadomienie głosowe,
 * jak wykona projekt"). Tylko PRZEJŚCIA między odczytami — pierwsze wczytanie (`przed` = null) milczy,
 * żeby otwarcie apki nie odczytywało całego stołu.
 */
fun zapowiedziStolu(przed: List<KartaStolu>?, po: List<KartaStolu>): List<String> {
    if (przed == null) return emptyList()
    val stare = przed.associateBy { it.id }
    return po.mapNotNull { k ->
        val s = stare[k.id] ?: return@mapNotNull null
        val p = k.projekt
        when {
            k.etap == "do_akceptacji" && s.etap != "do_akceptacji" -> buildString {
                append("Stół: projekt ").append(k.tytul).append(" gotowy do ratyfikacji")
                if (p != null && p.rundy > 1) append(" po ").append(p.runda).append(if (p.runda == 1) " rundzie" else " rundach")
                p?.ostatniaOcena?.let { append(", zgodność z wizją ").append(it).append(" na 10") }
                append('.')
            }
            k.etap == "utknela" && s.etap != "utknela" -> "Stół: projekt ${k.tytul} utknął. Możesz przyjąć go od nowa."
            k.etap == "zratyfikowane" && p != null && p.zlecenia.isNotEmpty() &&
                p.zlecenia.none { it.stan == "czeka" || it.stan == "trwa" } &&
                (s.projekt?.zlecenia.orEmpty().let { z -> z.isEmpty() || z.any { it.stan == "czeka" || it.stan == "trwa" } }) ->
                "Moduły Katedry oddały zlecenia projektu ${k.tytul}: ${p.zlecenia.count { it.stan == "gotowe" }} z ${p.zlecenia.size} gotowe."
            else -> null
        }
    }
}
