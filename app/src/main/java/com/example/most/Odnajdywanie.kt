package com.example.most

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * 🔄 Odnajdywanie Katedry po zmianie adresu tunelu.
 *
 * Każdy Suweren ma WŁASNĄ Katedrę i własny tunel Cloudflare: quick tunnel (*.trycloudflare.com — nowy adres
 * przy każdym starcie) albo nazwany tunel na swojej domenie. Telefon jest sparowany z JEDNĄ Katedrą (klucz Straży
 * + token urządzenia). Gdy zapamiętany adres milknie, StoL pyta rejestr otakos.wtf (`GET /api/katedry` →
 * `{ katedry: [{ nick, adres, klucz, … }] }`; Katedra melduje się tam co 60 s podpisem ed25519, rejestr sam
 * sprawdza wizytówkę pod adresem) o aktualny adres TEJ Katedry — po nicku z parowania.
 *
 * Nowy adres przyjmujemy dopiero, gdy jego wizytówka (`GET <adres>/api/wizytowka`, publiczna) ma TEN SAM nick
 * i TEN SAM klucz wizytówki, co przy parowaniu — inaczej to podszycie i adres odpada. Klucz Straży i token
 * jadą pod nowy adres dopiero po tej weryfikacji; do rejestru i do wizytówki kandydata nie jadą nigdy.
 * Ruch telefonu dalej idzie prosto do Katedry — otakos.wtf tylko mówi, gdzie ona jest.
 */
const val REJESTR_KATEDR = "https://otakos.wtf"

/** Tożsamość Katedry z jej publicznej wizytówki: nick (jak w rejestrze) i klucz ed25519 wizytówki. */
data class TozsamoscKatedry(val nick: String, val klucz: String) {
    companion object {
        /** Ten sam wzór co services/Wizytowka.js i server/rejestr.mjs. */
        val NICK = Regex("^[a-z0-9][a-z0-9-]{2,31}$")

        /** Z odpowiedzi `/api/wizytowka`; null, gdy brak nicka albo klucza (Katedra bez wizytówki). */
        fun zJson(m: Map<String, Any?>?): TozsamoscKatedry? {
            val nick = m?.napis("nick")?.trim().orEmpty()
            val klucz = m?.napis("klucz")?.trim().orEmpty()
            if (!NICK.matches(nick) || klucz.length < 32) return null
            return TozsamoscKatedry(nick, klucz)
        }
    }
}

/** Skąd jest adres w użyciu — ekran połączenia mówi to wprost. */
enum class ZrodloAdresu(val opis: String) {
    PAROWANIE("z parowania"),
    REJESTR("z rejestru otakos.wtf");

    companion object {
        fun zNazwy(n: String?) = entries.firstOrNull { it.name == n } ?: PAROWANIE
    }
}

object Odnajdywanie {

    const val SPARUJ_PONOWNIE =
        "Katedra zmieniła adres, a to parowanie nie zna jej nicka — sparuj ponownie (QR w Katedrze: Dashboard → karta „StoL”)."

    /**
     * Czy błąd mostu znaczy „pod tym adresem nikogo nie ma” (sieć, DNS, timeout, Cloudflare 52x/530 = tunel zgasł)
     * — tylko wtedy warto pytać rejestr. 502–504 z JSON-em to odpowiedź samego mostu, nie milczenie. Odmowa Straży, 401 czy błąd trasy to sprawa samej Katedry.
     */
    fun adresMilczy(kod: Int?, odpowiedzJson: Boolean = false): Boolean = when {
        kod == null || kod in 520..530 -> true
        kod in 502..504 -> !odpowiedzJson   // strona błędu Cloudflare, nie odpowiedź mostu
        else -> false
    }

    /**
     * Adres tej Katedry z odpowiedzi rejestru (`{ katedry: [...] }`) — tylko https bez ścieżki (jak `hostAdresu`
     * w rejestrze). Gdy rejestr zna też klucz wizytówki, musi się zgadzać z zapamiętanym.
     */
    fun adresZRejestru(json: Map<String, Any?>?, ja: TozsamoscKatedry): String? {
        @Suppress("UNCHECKED_CAST")
        val wpis = json?.lista("katedry")?.mapNotNull { it as? Map<String, Any?> }?.firstOrNull { it.napis("nick") == ja.nick }
            ?: return null
        val klucz = wpis.napis("klucz")
        if (klucz != null && klucz != ja.klucz) return null
        return bezpiecznyAdres(wpis.napis("adres"))
    }

    /** https://host (bez portu, ścieżki, zapytania, loginu); host z kropką. Inaczej null. */
    fun bezpiecznyAdres(surowy: String?): String? {
        val a = surowy?.trim()?.trimEnd('/') ?: return null
        val u = try { URI(a) } catch (_: Exception) { return null }
        val host = u.host?.lowercase() ?: return null
        if (u.scheme != "https" || u.port != -1 || u.userInfo != null || !u.rawPath.isNullOrEmpty() || u.rawQuery != null || u.rawFragment != null) return null
        if (!Regex("^([a-z0-9-]+\\.)+[a-z]{2,}$").matches(host)) return null
        return "https://$host"
    }

    /** Czy wizytówka spod nowego adresu to NASZA Katedra. Zwraca powód odrzucenia albo null, gdy zgodna. */
    fun sprawdzWizytowke(wizytowka: Map<String, Any?>?, ja: TozsamoscKatedry): String? {
        val w = TozsamoscKatedry.zJson(wizytowka) ?: return "pod nowym adresem nie ma wizytówki Katedry"
        if (w.nick != ja.nick) return "pod nowym adresem jest inna Katedra („${w.nick}”)"
        if (w.klucz != ja.klucz) return "wizytówka „${w.nick}” ma inny klucz niż przy parowaniu — możliwe podszycie, adres odrzucony"
        return null
    }

    /** Odpowiedź HTTP: kod i treść. */
    data class Odpowiedz(val kod: Int, val tekst: String)

    sealed class Wynik {
        /** Nowy, sprawdzony adres. `zmieniony` = false, gdy rejestr podaje ten sam (Katedra tam jest, ale nie odpowiada). */
        data class Znaleziony(val adres: String, val zmieniony: Boolean) : Wynik()
        data class Nie(val opis: String) : Wynik()
    }

    /**
     * Cała droga: rejestr → wpis z nickiem → wizytówka kandydata → zgodność nicku i klucza.
     * `pobierz` = GET bez żadnych nagłówków Katedry (podmieniany w testach); rzuca IOException przy braku sieci.
     */
    fun znajdz(
        ja: TozsamoscKatedry?,
        obecnyAdres: String,
        rejestr: String = REJESTR_KATEDR,
        pobierz: (String) -> Odpowiedz = ::pobierzBezKlucza,
    ): Wynik {
        if (ja == null) return Wynik.Nie(SPARUJ_PONOWNIE)
        val baza = rejestr.trimEnd('/')
        val lista = try { pobierz("$baza/api/katedry") } catch (e: IOException) {
            return Wynik.Nie("Rejestr otakos.wtf milczy (${e.message ?: e.javaClass.simpleName}) — sprawdź internet telefonu.")
        }
        if (lista.kod !in 200..299) return Wynik.Nie("Rejestr otakos.wtf odpowiedział HTTP ${lista.kod}.")
        val adres = adresZRejestru(json(lista.tekst), ja)
            ?: return Wynik.Nie("Katedra „${ja.nick}” nie melduje się teraz w rejestrze otakos.wtf" + powodOffline(baza, ja.nick, pobierz) +
                ". Włącz Katedrę z tunelem i meldunkiem (karta Wystawy → 🪪).")
        val wiz = try { pobierz("$adres/api/wizytowka") } catch (e: IOException) {
            return Wynik.Nie("Rejestr podaje $adres, ale ten adres milczy (${e.message ?: e.javaClass.simpleName}).")
        }
        if (wiz.kod !in 200..299) return Wynik.Nie("Rejestr podaje $adres, ale wizytówka odpowiada HTTP ${wiz.kod}.")
        sprawdzWizytowke(json(wiz.tekst), ja)?.let { return Wynik.Nie("Adres z rejestru odrzucony: $it.") }
        return Wynik.Znaleziony(adres, zmieniony = adres != obecnyAdres.trimEnd('/'))
    }

    /** Co rejestr odpowiedział na ostatni meldunek (`/api/katedry/stan/:nick`) — „ ( … )” albo pusto. */
    private fun powodOffline(baza: String, nick: String, pobierz: (String) -> Odpowiedz): String = try {
        val o = pobierz("$baza/api/katedry/stan/$nick")
        json(o.tekst)?.obiekt("meldunek")?.napis("wiadomosc")?.takeIf { it.isNotBlank() }?.let { " (ostatni meldunek: $it)" } ?: ""
    } catch (_: IOException) { "" }

    @Suppress("UNCHECKED_CAST")
    private fun json(tekst: String): Map<String, Any?>? = runCatching { Json.parsuj(tekst) as? Map<String, Any?> }.getOrNull()

    /** GET bez klucza Straży i tokenu, bez przekierowań (jak rejestr: `redirect: 'error'`). */
    fun pobierzBezKlucza(adres: String): Odpowiedz {
        val c = URL(adres).openConnection() as HttpURLConnection
        return try {
            c.instanceFollowRedirects = false
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("Accept", "application/json")
            val kod = c.responseCode
            val tekst = (if (kod in 200..299) c.inputStream else c.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Odpowiedz(kod, tekst.take(2_000_000))
        } finally {
            c.disconnect()
        }
    }
}
