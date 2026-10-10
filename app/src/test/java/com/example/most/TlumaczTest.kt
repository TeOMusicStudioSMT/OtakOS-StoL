package com.example.most

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

/** 🌍 Tłumacz StoL-a: ten sam filtr co most i Hub (TeO_Genesis lib/tlumaczRdzen.js), słownik z kolejką, trasy mostu. */
class TlumaczTest {
    @Test fun filtrJakWMoscie() {
        assertTrue(FiltrTlumacza.doTlumaczenia("Połóż na stół"))
        assertTrue(FiltrTlumacza.doTlumaczenia("  Izba  "))
        assertTrue(FiltrTlumacza.doTlumaczenia("MENU"))
        for (t in listOf("42", "https://otakos.wtf", "/api/tlumacz", "model.glb.json", "a1b2c3d4e5", "src/gra/postac.ts", "x", "GDD", "−", "🟢", "⟳ …")) {
            assertFalse(t, FiltrTlumacza.doTlumaczenia(t))
        }
    }

    @Test fun kodJezyka() {
        assertEquals("en", FiltrTlumacza.kodJezyka(" en "))
        assertEquals("pt-BR", FiltrTlumacza.kodJezyka("pt-BR"))
        for (k in listOf("", "english", "<x>", null)) assertNull(FiltrTlumacza.kodJezyka(k))
        assertTrue(JEZYKI_KATEDRY.any { it.first == "pl" } && JEZYKI_KATEDRY.any { it.first == "en" })
    }

    @Test fun slownikKolejkaIPowrot() {
        val s = SlownikTlumacza("en")
        assertNull(s.przetlumacz("Stół"))
        assertNull(s.przetlumacz(" Izba "))
        assertEquals("42", s.przetlumacz("42"))            // poza filtrem — bez zmian, bez kolejki
        assertEquals(2, s.czeka())
        val paczka = s.paczka()
        assertEquals(listOf("Stół", "Izba"), paczka)
        assertNull(s.przetlumacz("Stół"))                 // w drodze — nie trafia drugi raz do kolejki
        assertEquals(emptyList<String>(), s.paczka())
        s.przyjmij(paczka, OdpowiedzTlumacza(mapOf("Stół" to "Table"), listOf("Izba"), null))
        assertEquals("Table", s.przetlumacz("Stół"))
        assertEquals(" Table ", s.przetlumacz(" Stół "))  // spacje wokół zostają
        assertNull(s.przetlumacz("Izba"))                 // pominięta przez model: polski, ale bez ponownej kolejki
        assertEquals(0, s.czeka())
    }

    @Test fun nieDoszlaChwilowoWracaDoKolejki() {
        val s = SlownikTlumacza("de")
        s.przetlumacz("Historia")
        val p = s.paczka()
        s.nieDoszla(p, naStale = false)
        assertEquals(listOf("Historia"), s.paczka())
        s.nieDoszla(listOf("Historia"), naStale = true)
        s.przetlumacz("Historia")
        assertEquals(0, s.czeka())
    }

    @Test fun polskiBezZmian() {
        val s = SlownikTlumacza("pl")
        assertEquals("Stół", s.przetlumacz("Stół"))
        assertEquals(0, s.czeka())
    }

    @Test fun pamiecTelefonuTamIZPowrotem() {
        val s = SlownikTlumacza("en")
        s.dolacz(mapOf("Wyślij" to "Send", "„Cytat”\n" to "\"Quote\""))
        assertEquals(s.kopia(), SlownikTlumacza.zJson(s.doJson()))
        assertEquals(emptyMap<String, String>(), SlownikTlumacza.zJson("{zepsute"))
        assertEquals(emptyMap<String, String>(), SlownikTlumacza.zJson(null))
    }

    @Test fun adresSwiataNiesieJezyk() {
        assertEquals("https://x.pl/swiat/#k=k&t=t&jezyk=en", adresSwiata("https://x.pl", "k", "t", "en"))
        assertEquals("https://x.pl/swiat/#k=k&t=t&jezyk=pl", adresSwiata("https://x.pl", "k", "t", "pl"))
        assertEquals("https://x.pl/swiat/#k=k&t=t", adresSwiata("https://x.pl", "k", "t", "zły kod"))
    }

    /** Prawdziwe HTTP: klucz Straży w nagłówku, ciało jak w Hubie, odpowiedź mostu → słownik. */
    @Test fun trasyMostu() {
        val serwer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var klucz: String? = null
        var cialo: String? = null
        serwer.createContext("/api/tlumacz") { ex ->
            klucz = ex.requestHeaders.getFirst("x-teo-klucz")
            val odp = if (ex.requestMethod == "POST") {
                cialo = ex.requestBody.readBytes().toString(Charsets.UTF_8)
                """{"success":true,"mapa":{"Stół":"Table"},"nowych":1,"pominiete":["Izba"]}"""
            } else {
                """{"success":true,"jezyk":"en","slownik":{"Wyślij":"Send","x":1}}"""
            }
            val b = odp.toByteArray(Charsets.UTF_8)
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, b.size.toLong())
            ex.responseBody.use { it.write(b) }
        }
        serwer.start()
        try {
            val k = MostKlient("http://127.0.0.1:${serwer.address.port}", "klucz-strazy")
            val w = k.tlumacz("en", listOf("Stół", "Izba"))
            assertTrue(w is MostKlient.Wynik.Ok)
            assertEquals(OdpowiedzTlumacza(mapOf("Stół" to "Table"), listOf("Izba"), null), (w as MostKlient.Wynik.Ok).wartosc)
            assertEquals("klucz-strazy", klucz)
            @Suppress("UNCHECKED_CAST")
            val wyslane = Json.parsuj(cialo!!) as Map<String, Any?>
            assertEquals("en", wyslane["jezyk"])
            assertEquals(listOf("Stół", "Izba"), wyslane["teksty"])
            val s = k.slownikTlumacza("en")
            assertEquals(mapOf("Wyślij" to "Send"), (s as MostKlient.Wynik.Ok).wartosc)
        } finally {
            serwer.stop(0)
        }
    }

    @Test fun pustaPaczkaNiePytaMostu() {
        val w = MostKlient("http://127.0.0.1:9", "k", limitMs = 500).tlumacz("en", emptyList())
        assertTrue(w is MostKlient.Wynik.Ok)
    }
}
