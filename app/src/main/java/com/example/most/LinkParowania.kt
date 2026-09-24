package com.example.most

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Link parowania, który pokazuje Katedra (karta „StoL" na Dashboardzie Huba):
 *
 *   otakos-stol://paruj?adres=https%3A%2F%2Fx.trycloudflare.com&k=<klucz Straży>&kod=123456
 *
 *  · adres — Kwantowy Tunel mostu (telefon nie widzi 127.0.0.1 komputera),
 *  · k     — klucz Straży Mostu: bez niego most odrzuca każde żądanie spoza maszyny,
 *  · kod   — jednorazowy kod parowania (6 cyfr, żyje 5 minut).
 *
 * Kod wymienia się na TOKEN urządzenia; od tej chwili apka niesie klucz + token.
 */
/**
 * Adres świata klocków (strona mostu `/swiat/`, pokazywana w WebView).
 * Klucz i token jadą we FRAGMENCIE (#…) — fragment nie wychodzi w żądaniu HTTP,
 * więc nie ląduje w logach tunelu ani mostu; strona sama dokłada je do zapytań.
 */
fun adresSwiata(adres: String, klucz: String, token: String): String =
    "${adres.trimEnd('/')}/swiat/#k=${URLEncoder.encode(klucz, "UTF-8")}&t=${URLEncoder.encode(token, "UTF-8")}"

data class LinkParowania(val adres: String, val klucz: String, val kod: String) {

    fun doTekstu(): String =
        "otakos-stol://paruj?adres=${enc(adres)}&k=${enc(klucz)}&kod=${enc(kod)}"

    companion object {
        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

        /** Adres mostu: http(s), bez ścieżki i końcowego ukośnika. */
        fun normalizujAdres(surowy: String): String? {
            var a = surowy.trim().trimEnd('/')
            if (a.isEmpty()) return null
            if (!a.startsWith("http://") && !a.startsWith("https://")) a = "https://$a"
            return try {
                val u = URI(a)
                if (u.host.isNullOrBlank()) null
                else "${u.scheme}://${u.host}${if (u.port > 0) ":${u.port}" else ""}"
            } catch (_: Exception) { null }
        }

        /**
         * Czyta link z QR albo wklejony tekst. Zwraca null, gdy to nie jest link StoL-a
         * albo brakuje którejś z trzech części — wtedy ekran mówi, czego brakuje.
         */
        fun zTekstu(tekst: String): LinkParowania? {
            val t = tekst.trim()
            val zapytanie = when {
                t.startsWith("otakos-stol://paruj?") -> t.substringAfter('?')
                else -> return null
            }
            val pola = zapytanie.split('&').mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i) to dec(it.substring(i + 1))
            }.toMap()
            val adres = normalizujAdres(pola["adres"] ?: return null) ?: return null
            val klucz = pola["k"]?.trim().orEmpty()
            val kod = pola["kod"]?.trim().orEmpty()
            if (klucz.length < 32 || !Regex("^\\d{6}$").matches(kod)) return null
            return LinkParowania(adres, klucz, kod)
        }
    }
}
