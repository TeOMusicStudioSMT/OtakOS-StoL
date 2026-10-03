package com.example.most

/**
 * 💬 TOST między Katedrami w StoL-u (Suweren 2026-10-02: „dodajmy do Stołu moduł TOSTa, by można gadać
 * z innymi katedrami"). Telefon rozmawia tylko ze SWOJĄ Katedrą (tunel + klucz); to Katedra szyfruje
 * kopertę end-to-end i niesie ją do tunelu drugiej Katedry (services/TostSiec.js w teo-app-hub).
 *  · kontakty — `GET /api/tost/siec/kontakty`: Katedry online z rejestru otakos.wtf + ci, z którymi już pisałeś,
 *  · rozmowa  — `GET /api/tost/siec/rozmowa/:nick` (otwarcie = przeczytane),
 *  · wyślij   — `POST /api/tost/siec/wyslij {do, tekst}`: gdy odbiorca offline, wiadomość CZEKA w Katedrze.
 */
data class KontaktTost(
    val nick: String,
    val motto: String,
    val online: Boolean,
    val nieprzeczytane: Int,
    val czeka: Int,
    val ostatnia: String?,
) {
    companion object {
        fun zJson(m: Map<String, Any?>): KontaktTost? {
            val nick = m.napis("nick")?.takeIf { it.isNotBlank() } ?: return null
            return KontaktTost(
                nick = nick,
                motto = m.napis("motto").orEmpty(),
                online = m.logika("online") == true,
                nieprzeczytane = m.liczba("nieprzeczytane")?.toInt() ?: 0,
                czeka = m.liczba("czeka")?.toInt() ?: 0,
                ostatnia = m.obiekt("ostatnia")?.napis("tekst"),
            )
        }
    }
}

/** Kim jest ta Katedra w sieci (`ja` = nick; pusty = brak wizytówki) i z kim można pisać. */
data class KontaktyTost(val ja: String?, val kontakty: List<KontaktTost>, val bladRejestru: String?) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>) = KontaktyTost(
            ja = m.napis("ja")?.takeIf { it.isNotBlank() },
            kontakty = m.lista("kontakty").mapNotNull { (it as? Map<String, Any?>)?.let(KontaktTost::zJson) },
            bladRejestru = m.obiekt("rejestr")?.napis("blad"),
        )
    }
}

/** Jedna wiadomość. `stan` tylko dla wychodzących: czeka / dostarczona / niedostarczona (+ `blad` = powód). */
data class WiadomoscTost(
    val id: String,
    val przychodzaca: Boolean,
    val tekst: String,
    val czas: String,
    val stan: String?,
    val blad: String?,
) {
    val opisStanu: String
        get() = when {
            przychodzaca -> ""
            stan == "dostarczona" -> "✓ dostarczona"
            stan == "niedostarczona" -> "✕ niedostarczona" + (blad?.let { " ($it)" } ?: "")
            else -> "⏳ czeka" + (blad?.let { " ($it)" } ?: "")
        }

    companion object {
        fun zJson(m: Map<String, Any?>): WiadomoscTost? {
            val id = m.napis("id") ?: return null
            return WiadomoscTost(
                id = id,
                przychodzaca = m.napis("kierunek") == "przychodzaca",
                tekst = m.napis("tekst").orEmpty(),
                czas = m.napis("czas").orEmpty(),
                stan = m.napis("stan"),
                blad = m.napis("blad"),
            )
        }
    }
}

/** Link do wizytówek na otakos.wtf, który zapamięta w przeglądarce telefonu „moją Katedrę". */
fun linkWizytowek(ja: String?): String =
    if (ja.isNullOrBlank()) "https://otakos.wtf/#katedry" else "https://otakos.wtf/#katedry?moja=$ja"

/**
 * 🏛️ Zatwierdzanie Katedr przez Stół (Suweren 2026-10-03): Katedra zarządcy rejestru otakos.wtf pokazuje
 * Katedry, które się meldują bez zatwierdzenia (`GET /api/rejestr/stan`), a Suweren decyduje w Izbie Akceptacji
 * (`POST /api/rejestr/zatwierdz|odrzuc`). Inna Katedra widzi tylko, kto jest zarządcą.
 */
data class OczekujacaKatedra(val nick: String, val klucz: String, val kiedy: String, val powod: String) {
    companion object {
        fun zJson(m: Map<String, Any?>): OczekujacaKatedra? {
            val nick = m.napis("nick")?.takeIf { it.isNotBlank() } ?: return null
            val klucz = m.napis("klucz")?.takeIf { it.isNotBlank() } ?: return null
            return OczekujacaKatedra(nick, klucz, m.napis("kiedy").orEmpty(), m.napis("powod") ?: "nowa Katedra")
        }
    }
}

data class StanRejestru(
    val jestZarzadca: Boolean,
    val zarzadca: String?,
    val oczekujace: List<OczekujacaKatedra>,
    val zatwierdzone: List<String>,
    val blad: String?,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>) = StanRejestru(
            jestZarzadca = m.logika("jestZarzadca") == true,
            zarzadca = m.napis("zarzadca"),
            oczekujace = m.lista("oczekujace").mapNotNull { (it as? Map<String, Any?>)?.let(OczekujacaKatedra::zJson) },
            zatwierdzone = m.lista("zatwierdzone").mapNotNull { (it as? Map<String, Any?>)?.napis("nick") },
            blad = m.napis("blad"),
        )
    }
}
