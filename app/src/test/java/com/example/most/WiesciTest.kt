package com.example.most

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 📯 Wieści kanału Mistrza: dokładnie taki JSON oddaje most (GET /api/mistrz/wiesci). */
class WiesciTest {
    private val odpowiedz = """
        {"success":true,"wiesci":[
          {"nr":86,"id":"cfda005f","kiedy":"2026-10-30T06:00:15.062Z","rodzaj":"brief","skad":"Poranny brief Katedry","tresc":"☀️ Dzień dobry!","glos":true},
          {"nr":87,"id":"585a781f","kiedy":"2026-10-30T09:12:18.451Z","rodzaj":"wejscie","skad":"Licznik wejść","tresc":"🌍 Niemcy — ktoś wszedł na teo.center","glos":false},
          {"nr":88,"rodzaj":"teterhia","skad":"Mistrz Gry","tresc":"🏆 Turniej"},
          {"nr":89,"rodzaj":"pole","tresc":"  "},
          {"rodzaj":"pole","tresc":"bez numeru"}
        ],"ostatni":89}
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    private fun json() = Json.parsuj(odpowiedz) as Map<String, Any?>

    @Test fun czytaWiesciZMostu() {
        val w = Wiesc.lista(json())
        assertEquals(listOf(86, 87, 88), w.map { it.nr })   // pusta treść i brak numeru odpadają
        assertEquals("Licznik wejść", w[1].skad)
        assertEquals(89, Wiesc.ostatni(json()))
        assertEquals("pole", Wiesc.grupa("teterhia"))
        assertEquals("wejscie", Wiesc.grupa("wejscie"))
    }

    @Test fun pierwszeSprawdzenieNicNiePokazujeTylkoZapamietuje() {
        val d = doPowiadomienia(Wiesc.lista(json()), 89, widziane = -1, wylaczone = emptySet())
        assertTrue(d.pokaz.isEmpty())
        assertEquals(89, d.noweWidziane)
    }

    @Test fun pokazujeTylkoNowe() {
        val d = doPowiadomienia(Wiesc.lista(json()), 89, widziane = 86, wylaczone = emptySet())
        assertEquals(listOf(87, 88), d.pokaz.map { it.nr })
        assertEquals(0, d.pominiete)
        assertEquals(89, d.noweWidziane)
    }

    @Test fun zalewZbieraWJedno() {
        val duzo = (1..10).map { Wiesc(it, "", "wejscie", "Licznik wejść", "🌍 $it") }
        val d = doPowiadomienia(duzo, 10, widziane = 0, wylaczone = emptySet(), max = 4)
        assertEquals(listOf(7, 8, 9, 10), d.pokaz.map { it.nr })
        assertEquals(6, d.pominiete)
    }

    @Test fun wylaczonaGrupaMilczy() {
        val d = doPowiadomienia(Wiesc.lista(json()), 89, widziane = 85, wylaczone = setOf("pole"))
        assertEquals(listOf(86, 87), d.pokaz.map { it.nr })
    }
}
