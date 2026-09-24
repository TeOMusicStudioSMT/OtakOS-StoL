package com.example.most

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTest {
    @Test fun parsujeObiektyTabliceINapisy() {
        @Suppress("UNCHECKED_CAST")
        val m = Json.parsuj("""{"a":1.5,"b":[true,null,"x"],"c":{"d":"żą\n\"q\""}}""") as Map<String, Any?>
        assertEquals(1.5, m["a"])
        assertEquals(listOf(true, null, "x"), m["b"])
        assertEquals("żą\n\"q\"", (m["c"] as Map<*, *>)["d"])
    }

    @Test(expected = Json.BladJson::class) fun odrzucaUrwanyJson() { Json.parsuj("""{"a":""") }
}

class LinkParowaniaTest {
    private val klucz = "a".repeat(48)

    @Test fun tamIZPowrotem() {
        val l = LinkParowania("https://x.trycloudflare.com", klucz, "012345")
        assertEquals(l, LinkParowania.zTekstu(l.doTekstu()))
    }

    /** Dokładnie taki link buduje Hub (components/dashboard/StolCard.tsx → linkParowaniaStol, encodeURIComponent). */
    @Test fun czytaLinkZHuba() {
        val l = LinkParowania.zTekstu("otakos-stol://paruj?adres=https%3A%2F%2Fciche-lodzie-gra.trycloudflare.com&k=065c105e6eab46998c0e651dbc85df66a82f1b2c3d4e5f60&kod=012345")
        assertEquals(LinkParowania("https://ciche-lodzie-gra.trycloudflare.com", "065c105e6eab46998c0e651dbc85df66a82f1b2c3d4e5f60", "012345"), l)
    }

    @Test fun adresSwiataNiesieKluczITokenWeFragmencie() {
        assertEquals(
            "https://x.trycloudflare.com/swiat/#k=abc%2B%2F&t=E_GO-ut2",
            adresSwiata("https://x.trycloudflare.com/", "abc+/", "E_GO-ut2"),
        )
    }

    @Test fun normalizujeAdres() {
        assertEquals("https://x.trycloudflare.com", LinkParowania.normalizujAdres(" x.trycloudflare.com/ "))
        assertEquals("http://192.168.1.5:3001", LinkParowania.normalizujAdres("http://192.168.1.5:3001/api/bridge/execute"))
        assertNull(LinkParowania.normalizujAdres(""))
    }

    @Test fun odrzucaNiepelneLinki() {
        assertNull(LinkParowania.zTekstu("https://x.trycloudflare.com"))
        assertNull(LinkParowania.zTekstu("otakos-stol://paruj?adres=x.pl&k=$klucz"))            // bez kodu
        assertNull(LinkParowania.zTekstu("otakos-stol://paruj?adres=x.pl&k=krotki&kod=123456")) // za krótki klucz
        assertNull(LinkParowania.zTekstu("otakos-stol://paruj?adres=x.pl&k=$klucz&kod=12a456")) // kod nie z cyfr
    }
}

class StanStadaTest {
    @Test fun czytaOdpowiedzMostu() {
        // Kształt jak w services/MostStada.js → stanDlaApki (+ urzadzenie z trasy).
        val json = """{"success":true,"urzadzenie":"Pixel","migawka":{"czas":1,"wiekSekund":2400,"aktywny":"joanna"},
          "gatunki":[
            {"id":"joanna","imie":"Joanna","dziedzina":"Muzyka","kolor":"#a855f7","forma":"👑","etap":"legenda","xp":11923,"wyklute":true,"robi":"skomponowała szkic","robiOd":1790000000000},
            {"id":"kodeks","imie":"Kodeks","dziedzina":"Kod","kolor":"#22c55e","forma":"🥚","etap":"jajko","xp":0,"wyklute":false,"robi":null,"robiOd":null}],
          "aktywnosc":[{"kto":"kodeks","ts":5,"rodzaj":"praca","tresc":"a"},{"kto":"joanna","ts":9,"rodzaj":"praca","tresc":"b"}]}"""
        @Suppress("UNCHECKED_CAST")
        val s = StanStada.zJson(Json.parsuj(json) as Map<String, Any?>)
        assertEquals("Pixel", s.urzadzenie)
        assertEquals(2400L, s.wiekMigawkiSekund)
        assertEquals(listOf("joanna"), s.wyklute.map { it.id })
        assertEquals(11923, s.gatunki[0].xp)
        assertNull(s.gatunki[1].robi)
        assertEquals(listOf("joanna", "kodeks"), s.aktywnosc.map { it.kto })   // najnowsze pierwsze
        assertEquals("40 min temu", ileTemu(2400))
    }

    @Test fun brakMigawkiToPowodNieBlad() {
        @Suppress("UNCHECKED_CAST")
        val s = StanStada.zJson(Json.parsuj("""{"success":true,"migawka":null,"powod":"Katedra jeszcze nic nie opublikowała.","aktywnosc":[]}""") as Map<String, Any?>)
        assertNull(s.wiekMigawkiSekund)
        assertTrue(s.gatunki.isEmpty())
        assertNotNull(s.powod)
    }
}

class SseTest {
    @Test fun skladaRamkiIPomijaPuls() {
        val ramki = mutableListOf<Pair<String, String>>()
        val p = SseParser { z, d -> ramki += z to d }
        listOf(": strumien otwarty", "", "event: stan", "data: {\"a\":1}", "", ": puls", "", "event: szyna", "data: x", "data: y", "").forEach(p::linia)
        assertEquals(listOf("stan" to "{\"a\":1}", "szyna" to "x\ny"), ramki)
    }

    @Test fun zdarzenieUstawiaRobiINajnowszySlad() {
        @Suppress("UNCHECKED_CAST")
        val s = StanStada.zJson(Json.parsuj("""{"gatunki":[{"id":"joanna","imie":"Joanna","wyklute":true},{"id":"kodeks","imie":"Kodeks","wyklute":true}],
            "aktywnosc":[{"kto":"joanna","ts":1,"tresc":"stare"},{"kto":"kodeks","ts":2,"tresc":"k"}]}""") as Map<String, Any?>)
        val po = s.zZdarzeniem(ZdarzenieSzyny(5, "2026-09-24T21:38:04.234Z", "Joanna", "praca", "ballada"))
        assertEquals("ballada", po.gatunki.first { it.id == "joanna" }.robi)
        assertEquals(1790285884234L, po.gatunki.first { it.id == "joanna" }.robiOd)
        assertNull(po.gatunki.first { it.id == "kodeks" }.robi)
        assertEquals(listOf("joanna", "kodeks"), po.aktywnosc.map { it.kto })   // bez duplikatu Joanny
        assertEquals("ballada", po.aktywnosc.first().tresc)
    }
}
