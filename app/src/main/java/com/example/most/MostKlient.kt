package com.example.most

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klient mostu Katedry (Wiesio-Bridge) dla StoL-a. Tylko dwie rozmowy:
 *  · paruj — kod z Katedry → token urządzenia (`POST /api/stado/paruj`),
 *  · stan  — co robi stado (`GET /api/stado/stan`, token w `X-Stado-Token`).
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
        val cialo = """{"kod":"${esc(kod)}","urzadzenie":"${esc(nazwaUrzadzenia)}"}"""
        return zapytaj("POST", "/api/stado/paruj", cialo, emptyMap()) { m ->
            m.napis("token") ?: throw IOException("Most nie oddał tokenu.")
        }
    }

    fun stan(token: String): Wynik<StanStada> =
        zapytaj("GET", "/api/stado/stan", null, mapOf("X-Stado-Token" to token)) { StanStada.zJson(it) }

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
            kod == 403 -> Wynik.Blad(msg ?: "Most odmówił (403).")
            kod == 404 && tekst.contains("Cannot") -> Wynik.Blad("Ten most nie zna trasy StoL-a — zaktualizuj Katedrę.")
            else -> Wynik.Blad(msg ?: "Most odpowiedział HTTP $kod.")
        }
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
