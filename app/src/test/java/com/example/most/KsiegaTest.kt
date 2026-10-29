package com.example.most

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 📖 Żywa Księga: dokładnie taki JSON oddaje most (services/ZywaKsiega.js → GET /api/zywa-ksiega). */
class KsiegaTest {
    private val odpowiedz = """
        {"success":true,"wpisy":[
          {"id":"k-20261029-087b8c","kiedy":"2026-10-29T14:35:44.482Z","tekst":"Dziś zbudowaliśmy SPA 💛","publiczny":true,
           "zdjecie":{"plik":"zdjecie.jpg","kiedy":"2026-10-29T15:24:43.590Z"},"nagranie":null,"rysunki":[],
           "stado":[{"rodzaj":"slowo","kto":"kronikarz","stan":"gotowe","tekst":"Twoja łaska rozświetla drogę.","powod":null},
                    {"rodzaj":"ilustracja","kto":"paleta","stan":"gotowe","plik":"ilustracja.png","powod":null}]},
          {"id":"k-20261030-aabbcc","kiedy":"2026-10-30T07:00:00Z","tekst":"Ranek","publiczny":false,
           "zdjecie":{"plik":"../wpis.json"},"nagranie":{"plik":"nagranie.mp4","sekundy":12.5},"rysunki":[{},{}],
           "stado":[{"rodzaj":"slowo","stan":"robi"},{"rodzaj":"ilustracja","stan":"blad","powod":"FLUX nie oddał pliku"}]},
          {"id":"zly-id","tekst":"x"}
        ]}
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    private fun wpisy() = WpisKsiegi.lista(Json.parsuj(odpowiedz) as Map<String, Any?>)

    @Test fun czytaWpisyZMostu() {
        val w = wpisy()
        assertEquals(2, w.size)   // zły id odpada
        val pierwszy = w[0]
        assertTrue(pierwszy.publiczny)
        assertEquals("zdjecie.jpg", pierwszy.zdjecie)
        assertEquals("Twoja łaska rozświetla drogę.", pierwszy.slowo?.tekst)
        assertEquals("ilustracja.png", pierwszy.ilustracja?.plik)
        assertFalse(pierwszy.stadoPracuje)
        assertFalse(pierwszy.maNagranie)
    }

    @Test fun obcaNazwaPlikuOdpadaAStanStadaWidac() {
        val drugi = wpisy()[1]
        assertNull(drugi.zdjecie)   // tylko nazwy, które most naprawdę podaje
        assertTrue(drugi.maNagranie)
        assertEquals(12.5, drugi.nagranieSekundy!!, 0.0)
        assertEquals(2, drugi.rysunkow)
        assertTrue(drugi.stadoPracuje)
        assertTrue(drugi.stadoBlad)
        assertEquals("⚠ FLUX nie oddał pliku", drugi.ilustracja?.opisStanu)
    }

    @Test fun cialoNowegoWpisu() {
        assertEquals("{\"tekst\":\"Iskra \\\"z\\\" telefonu\",\"publiczny\":false}", WpisKsiegi.cialoNowego("  Iskra \"z\" telefonu ", false))
        assertNull(WpisKsiegi.cialoNowego("   ", true))
        assertNull(WpisKsiegi.cialoNowego("x".repeat(WpisKsiegi.MAX_TEKSTU + 1), true))
    }

    @Test fun zaDuzyFilmNieIdzieDoMostu() {
        val k = MostKlient("http://127.0.0.1:9", "a".repeat(48))
        val w = k.wyslijDoWpisu("t", "k-20261029-087b8c", "nagranie", "video/mp4", WpisKsiegi.MAX_NAGRANIA_BAJTOW + 1, java.io.ByteArrayInputStream(ByteArray(0)))
        assertTrue(w is MostKlient.Wynik.Blad && w.opis.contains("tunel przyjmuje"))
        val p = k.plikWpisu("t", "k-20261029-087b8c", "wpis.json")
        assertTrue(p is MostKlient.Wynik.Blad)
    }
}
