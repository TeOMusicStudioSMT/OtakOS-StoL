package com.example.most

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klient mostu Katedry (Wiesio-Bridge) dla StoL-a:
 *  · paruj    — kod z Katedry → token urządzenia (`POST /api/stado/paruj`),
 *  · stan     — co robi stado, jednorazowo (`GET /api/stado/stan`, token w `X-Stado-Token`),
 *  · strumien — to samo na żywo (SSE `GET /api/stado/strumien`): stan + każde zdarzenie szyny,
 *  · projekty / zalozProjekt — wspólne projekty stada (`GET /api/stado/projekty`,
 *    `POST /api/stado/projekt/nowy` — jedyna zmiana, którą most przyjmuje od telefonu).
 *
 * Każde żądanie niesie klucz Straży Mostu (`x-teo-klucz`) — telefon łączy się przez
 * Kwantowy Tunel, a most bez klucza odrzuca wszystko spoza maszyny Suwerena.
 * Tylko java.net: działa na Androidzie i w testach JVM. Wołać z wątku w tle.
 */
class MostKlient(
    private val adres: String,
    private val klucz: String,
    private val limitMs: Int = 12_000,
) {
    sealed class Wynik<out T> {
        data class Ok<T>(val wartosc: T) : Wynik<T>()
        /** Opis po polsku, gotowy do pokazania Suwerenowi. `rozparowany` = token już nie działa. */
        data class Blad(val opis: String, val rozparowany: Boolean = false) : Wynik<Nothing>()
    }

    fun paruj(kod: String, nazwaUrzadzenia: String): Wynik<String> {
        val cialo = """{"kod":${jsonNapis(kod)},"urzadzenie":${jsonNapis(nazwaUrzadzenia)}}"""
        return zapytaj("POST", "/api/stado/paruj", cialo, emptyMap()) { m ->
            m.napis("token") ?: throw IOException("Most nie oddał tokenu.")
        }
    }

    fun stan(token: String): Wynik<StanStada> =
        zapytaj("GET", "/api/stado/stan", null, mapOf("X-Stado-Token" to token)) { StanStada.zJson(it) }

    @Suppress("UNCHECKED_CAST")
    fun projekty(token: String): Wynik<List<ProjektStada>> =
        zapytaj("GET", "/api/stado/projekty", null, mapOf("X-Stado-Token" to token)) { m ->
            m.lista("projekty").mapNotNull { (it as? Map<String, Any?>)?.let(ProjektStada::zJson) }
        }

    /** Załóż projekt. Braki formularza wracają jako Blad bez pytania mostu. */
    fun zalozProjekt(token: String, projekt: NowyProjekt): Wynik<ProjektStada> {
        projekt.brak()?.let { return Wynik.Blad(it) }
        return zapytaj("POST", "/api/stado/projekt/nowy", projekt.doJson(), mapOf("X-Stado-Token" to token)) { m ->
            m.obiekt("projekt")?.let(ProjektStada::zJson) ?: throw IOException("Most nie oddał projektu.")
        }
    }

    /**
     * Strumień stada (SSE, `GET /api/stado/strumien`). Blokuje wątek, dopóki połączenie żyje:
     * woła `naStan` z pełnym stanem (na start i po każdej migawce z Katedry) i `naZdarzenie`
     * z każdym zdarzeniem szyny. Kończy się wynikiem, gdy połączenie padnie albo
     * `czyDalej()` zwróci false — wtedy wołający decyduje, czy wznowić.
     */
    fun strumien(
        token: String,
        naStan: (StanStada) -> Unit,
        naZdarzenie: (ZdarzenieSzyny) -> Unit,
        czyDalej: () -> Boolean = { true },
        limitCiszyMs: Int = 70_000,   // most pulsuje co 25 s — dłuższa cisza znaczy zerwane połączenie
        /** Dostaje połączenie, żeby wołający mógł je zamknąć od razu (disconnect przerywa readLine). */
        naPolaczenie: (HttpURLConnection) -> Unit = {},
    ): Wynik<Unit> {
        val c = (URL("$adres/api/stado/strumien").openConnection() as HttpURLConnection)
        naPolaczenie(c)
        return try {
            c.connectTimeout = limitMs
            c.readTimeout = limitCiszyMs
            c.setRequestProperty("Accept", "text/event-stream")
            c.setRequestProperty("x-teo-klucz", klucz)
            c.setRequestProperty("X-Stado-Token", token)
            val kod = c.responseCode
            if (kod !in 200..299) {
                val tekst = c.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                @Suppress("UNCHECKED_CAST")
                return opiszBlad(kod, runCatching { Json.parsuj(tekst) as? Map<String, Any?> }.getOrNull(), tekst)
            }
            var rozparowany: Wynik.Blad? = null
            val parser = SseParser { zdarzenie, dane ->
                @Suppress("UNCHECKED_CAST")
                val m = runCatching { Json.parsuj(dane) as? Map<String, Any?> }.getOrNull() ?: return@SseParser
                when (zdarzenie) {
                    "stan" -> naStan(StanStada.zJson(m))
                    "szyna" -> naZdarzenie(ZdarzenieSzyny.zJson(m))
                    "rozparowany" -> rozparowany = Wynik.Blad(m.napis("message") ?: "Telefon odłączony w Katedrze.", rozparowany = true)
                }
            }
            c.inputStream.bufferedReader(Charsets.UTF_8).use { r ->
                while (czyDalej() && rozparowany == null) {
                    val l = r.readLine() ?: break
                    parser.linia(l)
                }
            }
            rozparowany ?: Wynik.Ok(Unit)
        } catch (e: IOException) {
            Wynik.Blad("Strumień zerwany (${e.message ?: e.javaClass.simpleName}).")
        } finally {
            c.disconnect()
        }
    }

    private fun <T> zapytaj(
        metoda: String, sciezka: String, cialo: String?, naglowki: Map<String, String>,
        czytaj: (Map<String, Any?>) -> T,
    ): Wynik<T> {
        val c = (URL(adres + sciezka).openConnection() as HttpURLConnection)
        return try {
            c.requestMethod = metoda
            c.connectTimeout = limitMs
            c.readTimeout = limitMs
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("x-teo-klucz", klucz)
            naglowki.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (cialo != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(cialo.toByteArray(Charsets.UTF_8)) }
            }
            val kod = c.responseCode
            val tekst = (if (kod in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { Json.parsuj(tekst) as? Map<String, Any?> }.getOrNull()
            if (kod in 200..299 && json != null && json.logika("success") != false) {
                Wynik.Ok(czytaj(json))
            } else {
                opiszBlad(kod, json, tekst)
            }
        } catch (e: IOException) {
            Wynik.Blad("Most milczy (${e.message ?: e.javaClass.simpleName}). Czy tunel w Katedrze działa?")
        } catch (e: Json.BladJson) {
            Wynik.Blad("Most odpowiedział czymś, co nie jest JSON-em: ${e.message}")
        } finally {
            c.disconnect()
        }
    }

    private fun opiszBlad(kod: Int, json: Map<String, Any?>?, tekst: String): Wynik.Blad {
        val msg = json?.napis("message")
        return when {
            json?.napis("blad") == "BRAK_KLUCZA" ->
                Wynik.Blad("Most nie przyjął klucza Straży. Klucz mógł zostać przekuty — sparuj telefon od nowa.", rozparowany = true)
            kod == 401 -> Wynik.Blad(msg ?: "Telefon nie jest sparowany z tą Katedrą.", rozparowany = true)
            json?.napis("blad") == "TYLKO_LOKALNIE" ->
                Wynik.Blad("Ten most nie przyjmuje jeszcze projektów z telefonu — zaktualizuj Katedrę.")
            kod == 403 -> Wynik.Blad(msg ?: "Most odmówił (403).")
            kod == 404 && tekst.contains("Cannot") -> Wynik.Blad("Ten most nie zna trasy StoL-a — zaktualizuj Katedrę.")
            else -> Wynik.Blad(msg ?: "Most odpowiedział HTTP $kod.")
        }
    }
}
