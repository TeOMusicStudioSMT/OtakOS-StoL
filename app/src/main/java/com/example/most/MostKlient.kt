package com.example.most

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klient mostu Katedry (Wiesio-Bridge) dla StoL-a:
 *  · paruj    — kod z Katedry → token urządzenia (`POST /api/stado/paruj`),
 *  · stan     — co robi stado, jednorazowo (`GET /api/stado/stan`, token w `X-Stado-Token`),
 *  · strumien — to samo na żywo (SSE `GET /api/stado/strumien`): stan + każde zdarzenie szyny,
 *  · projekty / zalozProjekt — wspólne projekty stada (`GET /api/stado/projekty`, `POST /api/stado/projekt/nowy`),
 *  · rozmowcy / rozmawiaj — Delegat: rozmowa z dowolnym TeOgochi (`/api/delegat/…`),
 *  · pamiec / zwolnij — RAM Katedry i zamykanie procesów po PID (`/api/system/memory`, `/api/system/free`).
 *  · tostKontakty / tostRozmowa / tostWyslij — TOST między Katedrami (`/api/tost/siec/…`).
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

    /** Karty Stołu ratyfikacji, najnowsze pierwsze (`GET /api/stol`). */
    @Suppress("UNCHECKED_CAST")
    fun stol(token: String): Wynik<List<KartaStolu>> =
        zapytaj("GET", "/api/stol", null, mapOf("X-Stado-Token" to token)) { m ->
            m.lista("karty").mapNotNull { (it as? Map<String, Any?>)?.let(KartaStolu::zJson) }
        }

    /** Jedna karta z pełną treścią i Biblią projektu (`GET /api/stol/:id`). */
    fun karta(token: String, id: String): Wynik<KartaStolu> =
        zapytaj("GET", "/api/stol/${sciezkaId(id)}", null, mapOf("X-Stado-Token" to token)) { m ->
            m.obiekt("karta")?.let(KartaStolu::zJson) ?: throw IOException("Most nie oddał karty.")
        }

    /** Połóż propozycję na stół z telefonu (`POST /api/stol`; most zapisze źródło „telefon"). */
    fun polozNaStol(token: String, karta: NowaKarta): Wynik<KartaStolu> {
        karta.brak()?.let { return Wynik.Blad(it) }
        return zapytaj("POST", "/api/stol", karta.doJson(), mapOf("X-Stado-Token" to token)) { m ->
            m.obiekt("karta")?.let(KartaStolu::zJson) ?: throw IOException("Most nie oddał karty.")
        }
    }

    /**
     * Decyzja Suwerena nad kartą: przyjmij (→ Projekt Stada; `uczestnicy` = id TeOgochi, pusta lista =
     * sugerowani z karty albo całe stado), odrzuć, ratyfikuj (→ zlecenia modułów), doskonal (→ kolejne rundy).
     * `warsztat` (przyjmij, doskonal, nocna): rundy doskonalenia, pętla kreatywna, powtórzenia na noc. Odmowę mostu
     * (np. „stado pracuje już nad innym projektem") oddaje jego słowami; świeży etap daje kolejne `stol()`.
     * Ok niesie liczbę zleceń modułów (tylko ratyfikacja ją ma; reszta = 0).
     */
    fun decyzja(
        token: String, id: String, akcja: AkcjaStolu, uczestnicy: List<String> = emptyList(), warsztat: Warsztat? = null,
    ): Wynik<Int> {
        val w = warsztat?.wGranicach()
        val cialo = buildString {
            append("{\"uczestnicy\":[").append(uczestnicy.distinct().joinToString(",") { jsonNapis(it) }).append(']')
            if (w != null) append(",\"rundy\":").append(w.rundy).append(",\"petla\":").append(w.petla).append(",\"powtorzenia\":").append(w.powtorzenia)
            append('}')
        }
        return zapytaj("POST", "/api/stol/${sciezkaId(id)}/${akcja.sciezka}", cialo, mapOf("X-Stado-Token" to token)) { m ->
            m.liczba("zlecenia")?.toInt() ?: 0
        }
    }

    /** Ogon szyny Katedry (`GET /api/szyna/zdarzenia`) — Historia: kto co zrobił, najnowsze pierwsze. */
    @Suppress("UNCHECKED_CAST")
    fun zdarzenia(token: String, ile: Int = 80): Wynik<List<ZdarzenieSzyny>> =
        zapytaj("GET", "/api/szyna/zdarzenia?ile=$ile", null, mapOf("X-Stado-Token" to token)) { m ->
            m.lista("zdarzenia").mapNotNull { (it as? Map<String, Any?>)?.let(ZdarzenieSzyny::zJson) }
                .sortedByDescending { it.kiedy.orEmpty() }
        }

    // ── Delegat: rozmowa z dowolnym TeOgochi i pamięć Katedry (most/Delegat.kt) ──

    /** Z kim można rozmawiać (`GET /api/delegat/wszyscy`): pełne profile i gatunki z kartą roli. */
    @Suppress("UNCHECKED_CAST")
    fun rozmowcy(token: String): Wynik<List<Rozmowca>> =
        zapytaj("GET", "/api/delegat/wszyscy", null, mapOf("X-Stado-Token" to token)) { m ->
            m.lista("delegaci").mapNotNull { (it as? Map<String, Any?>)?.let(Rozmowca::zJson) }
        }

    /**
     * Jedna tura rozmowy (`POST /api/delegat/rozmowa`, bez strumienia). Model lokalny z pętlą narzędzi potrafi
     * myśleć minutami (zwłaszcza gdy ComfyUI liczy wideo) — dlatego osobny, długi limit odczytu.
     */
    fun rozmawiaj(token: String, delegat: String, tekst: String, rozmowaId: String?, limitOdpowiedziMs: Int = 300_000): Wynik<OdpowiedzDelegata> {
        if (tekst.isBlank()) return Wynik.Blad("Pusta wypowiedź.")
        val cialo = buildString {
            append("{\"delegat\":").append(jsonNapis(delegat)).append(",\"tekst\":").append(jsonNapis(tekst.trim()))
            if (!rozmowaId.isNullOrBlank()) append(",\"rozmowaId\":").append(jsonNapis(rozmowaId))
            append('}')
        }
        return zapytaj("POST", "/api/delegat/rozmowa", cialo, mapOf("X-Stado-Token" to token), limitOdpowiedziMs) { m ->
            OdpowiedzDelegata.zJson(m) ?: throw IOException("Most nie oddał odpowiedzi delegata.")
        }
    }

    /** RAM Katedry i procesy, które go zjadają (`GET /api/system/memory`). */
    fun pamiec(token: String): Wynik<PamiecKatedry> =
        zapytaj("GET", "/api/system/memory", null, mapOf("X-Stado-Token" to token), 20_000) { PamiecKatedry.zJson(it) }

    /** Zamknij procesy po PID (`POST /api/system/free`) — most wpuszcza tylko sparowany telefon i niechronione. */
    fun zwolnij(token: String, pidy: List<Int>): Wynik<WynikZwolnienia> {
        if (pidy.isEmpty()) return Wynik.Blad("Nie wskazano procesu.")
        val cialo = "{\"pidy\":[${pidy.distinct().joinToString(",")}]}"
        return zapytaj("POST", "/api/system/free", cialo, mapOf("X-Stado-Token" to token), 20_000) { WynikZwolnienia.zJson(it) }
    }

    // ── 💬 TOST między Katedrami (most/Tost.kt) — telefon pisze przez SWOJĄ Katedrę ──

    /** Kontakty TOST-a: Katedry online z rejestru otakos.wtf i dotychczasowe rozmowy (`GET /api/tost/siec/kontakty`). */
    fun tostKontakty(token: String): Wynik<KontaktyTost> =
        zapytaj("GET", "/api/tost/siec/kontakty", null, mapOf("X-Stado-Token" to token)) { KontaktyTost.zJson(it) }

    /** Wątek z jedną Katedrą (`GET /api/tost/siec/rozmowa/:nick`) — most oznacza przychodzące jako przeczytane. */
    @Suppress("UNCHECKED_CAST")
    fun tostRozmowa(token: String, nick: String): Wynik<List<WiadomoscTost>> =
        zapytaj("GET", "/api/tost/siec/rozmowa/${sciezkaId(nick)}", null, mapOf("X-Stado-Token" to token)) { m ->
            m.lista("wiadomosci").mapNotNull { (it as? Map<String, Any?>)?.let(WiadomoscTost::zJson) }
        }

    /** Wyślij (`POST /api/tost/siec/wyslij`). Gdy odbiorca offline — Ok ze stanem „czeka”, Katedra wyśle sama. */
    fun tostWyslij(token: String, nick: String, tekst: String): Wynik<WiadomoscTost> {
        if (tekst.isBlank()) return Wynik.Blad("Pusta wiadomość.")
        if (tekst.length > 4000) return Wynik.Blad("Za długa wiadomość (max 4000 znaków).")
        val cialo = "{\"do\":${jsonNapis(nick)},\"tekst\":${jsonNapis(tekst.trim())}}"
        return zapytaj("POST", "/api/tost/siec/wyslij", cialo, mapOf("X-Stado-Token" to token), 30_000) { m ->
            m.obiekt("wiadomosc")?.let(WiadomoscTost::zJson) ?: throw IOException("Most nie oddał wiadomości.")
        }
    }

    private fun sciezkaId(id: String) = java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")

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
        limitOdczytuMs: Int = limitMs,
        czytaj: (Map<String, Any?>) -> T,
    ): Wynik<T> {
        val c = (URL(adres + sciezka).openConnection() as HttpURLConnection)
        return try {
            c.requestMethod = metoda
            c.connectTimeout = limitMs
            c.readTimeout = limitOdczytuMs
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
                Wynik.Blad("Ten most nie przyjmuje tego z telefonu — zaktualizuj Katedrę albo zrób to przy maszynie.")
            kod == 403 -> Wynik.Blad(msg ?: "Most odmówił (403).")
            kod == 404 && tekst.contains("Cannot") -> Wynik.Blad("Ten most nie zna trasy StoL-a — zaktualizuj Katedrę.")
            else -> Wynik.Blad(msg ?: "Most odpowiedział HTTP $kod.")
        }
    }
}
