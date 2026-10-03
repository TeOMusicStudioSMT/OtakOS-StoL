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

    /** QR z karty w Katedrze (StolCard.tsx → linkStronyParowaniaStol): strona parowania mostu, dane we fragmencie. */
    @Test fun czytaLinkStronyParowania() {
        val l = LinkParowania.zTekstu("https://ciche-lodzie-gra.trycloudflare.com/stol/paruj.html#k=065c105e6eab46998c0e651dbc85df66a82f1b2c3d4e5f60&kod=012345&do=1790509791260")
        assertEquals(LinkParowania("https://ciche-lodzie-gra.trycloudflare.com", "065c105e6eab46998c0e651dbc85df66a82f1b2c3d4e5f60", "012345"), l)
        // Katedra w sieci domowej (bez tunelu): http i port zostają.
        assertEquals("http://192.168.1.5:3001", LinkParowania.zTekstu("http://192.168.1.5:3001/stol/paruj.html#k=$klucz&kod=123456")?.adres)
        assertNull(LinkParowania.zTekstu("https://x.trycloudflare.com/stol/paruj.html#k=$klucz"))          // bez kodu
        assertNull(LinkParowania.zTekstu("https://x.trycloudflare.com/swiat/#k=$klucz&kod=123456"))        // nie strona parowania
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

class ProjektStadaTest {
    @Test fun jsonNapisUciekaZnakiSterujace() {
        val s = "Świat \"klocków\"\nz \\ ukośnikiem\t\u0001"
        assertEquals(s, Json.parsuj(jsonNapis(s)))
    }

    @Test fun nowyProjektSprawdzaToSamoCoMost() {
        assertEquals("Nadaj projektowi nazwę.", NowyProjekt(" ", "długa wizja projektu", listOf("a", "b")).brak())
        assertEquals("Opisz wizję choć jednym zdaniem.", NowyProjekt("X", "krótko", listOf("a", "b")).brak())
        assertEquals("Wspólny projekt potrzebuje co najmniej dwóch TeOgochi.", NowyProjekt("X", "długa wizja projektu", listOf("a", "a")).brak())
        assertNull(NowyProjekt("X", "długa wizja projektu", listOf("a", "b")).brak())
    }

    @Test fun nowyProjektToPoprawnyJson() {
        @Suppress("UNCHECKED_CAST")
        val m = Json.parsuj(NowyProjekt(" Iskra ", "Linia 1\nLinia \"2\"", listOf("joanna", "kupiec", "joanna"), samoZlecanie = false).doJson()) as Map<String, Any?>
        assertEquals("Iskra", m["nazwa"])
        assertEquals("Linia 1\nLinia \"2\"", m["wizja"])
        assertEquals(listOf("joanna", "kupiec"), m["uczestnicy"])
        assertEquals(false, m["samoZlecanie"])
        assertEquals(1.0, m["rundy"]); assertEquals(0.0, m["petla"])   // domyślnie: jedna runda, bez pętli
        @Suppress("UNCHECKED_CAST")
        val r = Json.parsuj(NowyProjekt("X", "długa wizja projektu", listOf("a", "b"), warsztat = Warsztat(8, 2)).doJson()) as Map<String, Any?>
        assertEquals(5.0, r["rundy"]); assertEquals(2.0, r["petla"])     // w granicach mostu
    }

    @Test fun czytaRundyProjektu() {
        @Suppress("UNCHECKED_CAST")
        val p = ProjektStada.zJson(Json.parsuj("""{"id":"f","nazwa":"F","wizja":"W","stan":"trwa","gotowe":1,"razem":4,"kroki":[],"zlecenia":[],"runda":2,"rundy":3,"oceny":[{"runda":1,"ocena":6}]}""") as Map<String, Any?>)
        assertEquals(Triple(2, 3, 6), Triple(p.runda, p.rundy, p.ocena))
    }

    /** Kształt jak services/ProjektStada.js → skrot (z polami zalozyl i zlecenia). */
    @Test fun czytaSkrotProjektu() {
        @Suppress("UNCHECKED_CAST")
        val m = Json.parsuj("""{"id":"iskra-ab12","nazwa":"Iskra","wizja":"W","stan":"gotowe","od":"2026-09-24T23:59:13.507Z","do":null,"zalozyl":"Pixel",
          "kroki":[{"agent":"joanna","imie":"Joanna","zadanie":"Muzyka: …","model":"qwen3.5:9b","stan":"gotowe","fala":2},
                   {"agent":"rezyser","imie":"Reżyser","zadanie":"Biblia","model":"gemma4:e2b","stan":"blad","fala":4}],
          "gotowe":1,"razem":2,
          "zlecenia":[{"id":"merch-1","modul":"merch","agent":"kupiec","imie":"Kupiec","opis":"Kubek","stan":"gotowe"},
                      {"id":"muzyka-3","modul":"muzyka","agent":"joanna","imie":"Joanna","opis":"synthwave","stan":"blad"}]}""") as Map<String, Any?>
        val p = ProjektStada.zJson(m)
        assertEquals("Pixel", p.zalozyl)
        assertEquals(listOf("gotowe", "blad"), p.kroki.map { it.stan })
        assertEquals(4, p.kroki[1].fala)
        assertEquals(1, p.zleceniaGotowe)
        assertEquals(2, p.zlecenia.size)
    }

    @Test fun brakiFormularzaNieIdaDoMostu() {
        val w = MostKlient("http://127.0.0.1:1", "k".repeat(48)).zalozProjekt("t", NowyProjekt("", "", emptyList()))
        assertEquals(MostKlient.Wynik.Blad("Nadaj projektowi nazwę."), w)
    }
}

class StolTest {
    /** Karta tak, jak oddaje ją most (services/Stol.js → lista: etap + projektSkrot). */
    private val json = """{"id":"k-mf3x-a1b2","tytul":"Forge Fashion","tresc":"Suweren: \"moda\"\n🔥 ISKRA: tak","wizja":"Mobilna gra RPG-fashion",
        "sugerowani":["Krawcowa","Paleta"],"zrodlo":"podcast-twin","zalozyl":null,"stan":"przyjeta","projekt":"p1","od":"2026-09-27T10:00:00.000Z",
        "decyzje":[{"co":"przyjeta","kto":"Pixel 8","kiedy":"2026-09-27T10:05:00.000Z"}],"etap":"do_akceptacji",
        "projektSkrot":{"id":"p1","stan":"gotowe","gotowe":6,"razem":6,"biblia":"BIBLIA Forge Fashion","zlecenia":[{"modul":"merch","opis":"Koszulka","stan":"czeka"}]}}"""

    @Suppress("UNCHECKED_CAST")
    @Test fun czytaKarteZMostu() {
        val k = KartaStolu.zJson(Json.parsuj(json) as Map<String, Any?>)
        assertEquals("Forge Fashion", k.tytul)
        assertEquals(listOf("Krawcowa", "Paleta"), k.sugerowani)
        assertNull(k.zalozyl)
        assertEquals(listOf(Decyzja("przyjeta", "Pixel 8", "2026-09-27T10:05:00.000Z")), k.decyzje)
        assertEquals("BIBLIA Forge Fashion", k.projekt?.biblia)
        assertEquals(listOf(ZlecenieKarty("merch", "Koszulka", "czeka")), k.projekt?.zlecenia)
        assertTrue(k.moznaRatyfikowac && k.czekaNaSuwerena && !k.moznaPrzyjac)
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun etapyDecydujaCoWolno() {
        fun k(etap: String) = KartaStolu.zJson(mapOf("id" to "x", "tytul" to "T", "etap" to etap))
        assertTrue(k("na_stole").moznaPrzyjac && k("na_stole").moznaOdrzucic && !k("na_stole").moznaRatyfikowac)
        assertTrue(k("utknela").moznaPrzyjac)
        assertTrue(!k("opracowuje").czekaNaSuwerena && !k("opracowuje").moznaOdrzucic)   // jak w moście: w trakcie nie odrzucisz
        assertTrue(!k("zratyfikowane").moznaOdrzucic && !k("zratyfikowane").czekaNaSuwerena)
        assertNull(k("na_stole").projekt)
    }

    @Test fun nowaKartaMowiCzegoBrakuje() {
        assertEquals("Nadaj propozycji tytuł.", NowaKarta(" ", "długa treść propozycji").brak())
        assertEquals("Opisz propozycję choć jednym zdaniem.", NowaKarta("Gra", "krótko").brak())
        assertNull(NowaKarta("Gra", "Zróbmy grę o klockach").brak())
        assertEquals("""{"tytul":"Gra \"K\"","tresc":"a\nb c d e f g"}""", NowaKarta(" Gra \"K\" ", "a\nb c d e f g ").doJson())
        val w = MostKlient("http://127.0.0.1:1", "k".repeat(48)).polozNaStol("t", NowaKarta("", ""))
        assertEquals(MostKlient.Wynik.Blad("Nadaj propozycji tytuł."), w)   // bez rundy przez tunel
    }
}

class RundyStoluTest {
    @Suppress("UNCHECKED_CAST")
    private fun k(json: String) = KartaStolu.zJson(Json.parsuj(json) as Map<String, Any?>)
    private fun karta(etap: String, projekt: String = "") =
        k("""{"id":"a","tytul":"Forge Fashion","etap":"$etap"${if (projekt.isNotEmpty()) ",\"projektSkrot\":$projekt" else ""}}""")

    @Test fun czytaRundyIOcenySedziego() {
        val p = karta("do_akceptacji", """{"id":"p","stan":"gotowe","gotowe":4,"razem":4,"biblia":"B","zlecenia":[],"runda":3,"rundy":3,"petla":1,
            "oceny":[{"runda":1,"ocena":6},{"runda":2,"ocena":null},{"runda":3,"ocena":8}],"braki":["dołóż Grade"]}""").projekt!!
        assertEquals(listOf(OcenaRundy(1, 6), OcenaRundy(2, null), OcenaRundy(3, 8)), p.oceny)
        assertEquals(8, p.ostatniaOcena)
        assertEquals(listOf("dołóż Grade"), p.braki)
        assertEquals(Triple(3, 3, 1), Triple(p.runda, p.rundy, p.petla))
        // Stary most (bez rund): jedna runda, bez ocen.
        val stary = karta("do_akceptacji", """{"id":"p","stan":"gotowe","gotowe":1,"razem":1,"biblia":"B","zlecenia":[]}""").projekt!!
        assertEquals(Triple(1, 1, 0), Triple(stary.runda, stary.rundy, stary.petla))
        assertNull(stary.ostatniaOcena)
        assertTrue(karta("do_akceptacji").moznaDoskonalic && !karta("opracowuje").moznaDoskonalic)
    }

    @Test fun warsztatWGranicachMostu() {
        assertEquals(Warsztat(5, 0), Warsztat(9, -2).wGranicach())
        assertEquals(Warsztat(1, 3), Warsztat(0, 7).wGranicach())
        val w = MostKlient("http://127.0.0.1:1", "k".repeat(48)).decyzja("t", "x", AkcjaStolu.DOSKONAL, warsztat = Warsztat(2, 1))
        assertTrue(w is MostKlient.Wynik.Blad)   // brak mostu → zdanie dla Suwerena, nie wyjątek
    }

    @Test fun zapowiedziTylkoPrzejscia() {
        val gotowa = karta("do_akceptacji", """{"id":"p","stan":"gotowe","gotowe":4,"razem":4,"biblia":"B","zlecenia":[],"runda":2,"rundy":3,"oceny":[{"runda":2,"ocena":9}]}""")
        assertEquals(emptyList<String>(), zapowiedziStolu(null, listOf(gotowa)))                 // pierwsze wczytanie milczy
        assertEquals(listOf("Stół: projekt Forge Fashion gotowy do ratyfikacji po 2 rundach, zgodność z wizją 9 na 10."),
            zapowiedziStolu(listOf(karta("opracowuje")), listOf(gotowa)))
        assertEquals(emptyList<String>(), zapowiedziStolu(listOf(gotowa), listOf(gotowa)))       // bez zmiany — cisza
        assertEquals(listOf("Stół: projekt Forge Fashion gotowy do ratyfikacji."),
            zapowiedziStolu(listOf(karta("opracowuje")), listOf(karta("do_akceptacji", """{"id":"p","stan":"gotowe","gotowe":1,"razem":1,"biblia":"B","zlecenia":[]}"""))))
        assertEquals(listOf("Stół: projekt Forge Fashion utknął. Możesz przyjąć go od nowa."), zapowiedziStolu(listOf(karta("opracowuje")), listOf(karta("utknela"))))
        val wToku = karta("zratyfikowane", """{"id":"p","stan":"gotowe","gotowe":1,"razem":1,"biblia":"B","zlecenia":[{"modul":"merch","opis":"K","stan":"gotowe"},{"modul":"muzyka","opis":"M","stan":"trwa"}]}""")
        val oddane = karta("zratyfikowane", """{"id":"p","stan":"gotowe","gotowe":1,"razem":1,"biblia":"B","zlecenia":[{"modul":"merch","opis":"K","stan":"gotowe"},{"modul":"muzyka","opis":"M","stan":"blad"}]}""")
        assertEquals(emptyList<String>(), zapowiedziStolu(listOf(karta("do_akceptacji")), listOf(wToku)))
        assertEquals(listOf("Moduły Katedry oddały zlecenia projektu Forge Fashion: 1 z 2 gotowe."), zapowiedziStolu(listOf(wToku), listOf(oddane)))
        assertEquals(emptyList<String>(), zapowiedziStolu(listOf(oddane), listOf(oddane)))
    }
}

class NocnaStoluTest {
    @Suppress("UNCHECKED_CAST")
    private fun k(json: String) = KartaStolu.zJson(Json.parsuj(json) as Map<String, Any?>)
    private val projekt = """{"id":"p","stan":"gotowe","gotowe":1,"razem":1,"biblia":"B","zlecenia":[]}"""

    @Test fun czytaNocnaKarty() {
        val karta = k("""{"id":"a","tytul":"F","etap":"zratyfikowane","projektSkrot":$projekt,
            "nocna":{"wlaczona":false,"zadania":[{"id":"nz-1","stan":"czeka","wykonane":1,"powtorzenia":3,"rundy":2,"blad":null}]}}""")
        assertEquals(NocnaKarty(false, listOf(NocneZadanie("nz-1", "czeka", 1, 3, 2, null))), karta.nocna)
        assertTrue(karta.moznaDoskonalic && karta.moznaNaNoc)                     // po ratyfikacji też
        assertNull(k("""{"id":"b","tytul":"F","etap":"na_stole"}""").nocna)      // stary most / bez projektu
        assertTrue(!k("""{"id":"b","tytul":"F","etap":"na_stole"}""").moznaNaNoc)
        assertTrue(k("""{"id":"c","tytul":"F","etap":"opracowuje","projektSkrot":$projekt}""").let { it.moznaNaNoc && !it.moznaDoskonalic })
    }

    @Test fun powtorzeniaWGranicach() {
        assertEquals(Warsztat(2, 1, 20), Warsztat(2, 1, 99).wGranicach())
        assertEquals(1, Warsztat(powtorzenia = 0).wGranicach().powtorzenia)
    }
}

/** Delegat w StoL-u: rozmówcy, odpowiedź, pamięć i wynik zamykania — tak, jak oddaje je most. */
class DelegatTest {
    @Suppress("UNCHECKED_CAST")
    private fun m(json: String) = Json.parsuj(json) as Map<String, Any?>

    @Test fun rozmowcyZMostu() {
        val lista = m("""{"success":true,"delegaci":[{"id":"kodeks","imie":"Kodeks","emoji":"🐙","dziedzina":"Kod","pelny":true,"narzedzia":["katedra.stan","projekty.stan"]},{"id":"","imie":"bez id"},{"id":"bilans","imie":"Bilans","pelny":false}]}""")
            .lista("delegaci").mapNotNull { (it as? Map<String, Any?>)?.let(Rozmowca::zJson) }
        assertEquals(listOf("kodeks", "bilans"), lista.map { it.id })
        assertTrue(lista[0].pelny && "projekty.stan" in lista[0].narzedzia)
        assertEquals("🥚", lista[1].emoji)
    }

    @Test fun odpowiedzDelegata() {
        val o = OdpowiedzDelegata.zJson(m("""{"success":true,"rozmowaId":"tel-abc-12ef","delegat":"kodeks","odpowiedz":"Projekt ma Biblię.","model":"qwen3.5:9b"}"""))
        assertEquals(OdpowiedzDelegata("tel-abc-12ef", "Projekt ma Biblię.", "qwen3.5:9b"), o)
        assertNull(OdpowiedzDelegata.zJson(m("""{"success":true}""")))
    }

    @Test fun pamiecZPidamiIOpisem() {
        val p = PamiecKatedry.zJson(m("""{"success":true,"totalGB":42.6,"freeGB":15,"procesy":[{"pid":4100,"name":"python.exe","mb":2425,"opis":"ComfyUI (obrazy, wideo, muzyka)","skrypt":"main.py","chroniony":false,"uwaga":"Zamknięcie przerwie render w toku."},{"pid":3000,"name":"Memory Compression","mb":1799,"opis":"skompresowana pamięć Windows","chroniony":true},{"name":"bez pid"}]}"""))
        assertEquals(15.0, p.wolneGB, 0.0)
        assertEquals(listOf(4100, 3000), p.procesy.map { it.pid })
        assertEquals("main.py", p.procesy[0].skrypt)
        assertTrue(p.procesy[1].chroniony)
        assertNull(p.blad)
    }

    @Test fun wynikZwolnieniaJednymZdaniem() {
        val w = WynikZwolnienia.zJson(m("""{"success":true,"zamkniete":[{"pid":5200,"name":"python.exe","opis":"pip"}],"odmowy":[{"pid":3000,"powod":"Memory Compression: chroniony"}]}"""))
        assertEquals("Zamknięto: python.exe #5200. #3000: Memory Compression: chroniony", w.zdanie)
        assertEquals("Nic nie zamknięto.", WynikZwolnienia(emptyList(), emptyList()).zdanie)
    }

    @Test fun pustaWypowiedzINicDoZamknieciaBezPytaniaMostu() {
        val k = MostKlient("http://127.0.0.1:9", "k")   // port 9: nikt nie słucha — gdyby klient pytał, byłby inny błąd
        assertEquals(MostKlient.Wynik.Blad("Pusta wypowiedź."), k.rozmawiaj("t", "kodeks", "  ", null))
        assertEquals(MostKlient.Wynik.Blad("Nie wskazano procesu."), k.zwolnij("t", emptyList()))
    }
}

/** TOST między Katedrami: dokładnie takie JSON-y oddaje most (services/TostSiec.js, /api/tost/siec/…). */
class TostTest {
    @Suppress("UNCHECKED_CAST")
    private fun j(s: String) = Json.parsuj(s) as Map<String, Any?>

    @Test fun kontaktyZMostu() {
        val k = KontaktyTost.zJson(j("""{"success":true,"ja":"teo-center","kontakty":[{"nick":"kael-elara","motto":"Echo","online":true,"nieprzeczytane":2,"czeka":1,"ostatnia":{"tekst":"Cześć","czas":"2026-10-02T23:00:00Z","kierunek":"przychodzaca"}},{"nick":"stara","online":false}],"rejestr":{"ok":true}}"""))
        assertEquals("teo-center", k.ja)
        assertEquals(2, k.kontakty.size)
        assertEquals(KontaktTost("kael-elara", "Echo", true, 2, 1, "Cześć"), k.kontakty[0])
        assertEquals(false, k.kontakty[1].online)
        assertNull(k.bladRejestru)
        assertEquals("rejestr HTTP 502", KontaktyTost.zJson(j("""{"ja":"","kontakty":[],"rejestr":{"blad":"rejestr HTTP 502"}}""")).bladRejestru)
        assertNull("pusty nick = brak wizytówki", KontaktyTost.zJson(j("""{"ja":"","kontakty":[]}""")).ja)
    }

    @Test fun wiadomosciIStany() {
        val czeka = WiadomoscTost.zJson(j("""{"id":"a","do":"kael-elara","kierunek":"wychodzaca","tekst":"test","czas":"t","stan":"czeka","blad":"odbiorca offline — czeka"}"""))!!
        assertEquals("⏳ czeka (odbiorca offline — czeka)", czeka.opisStanu)
        assertEquals("✓ dostarczona", czeka.copy(stan = "dostarczona", blad = null).opisStanu)
        val przych = WiadomoscTost.zJson(j("""{"id":"b","z":"kael-elara","kierunek":"przychodzaca","tekst":"hej","czas":"t"}"""))!!
        assertTrue(przych.przychodzaca)
        assertEquals("", przych.opisStanu)
        assertNull(WiadomoscTost.zJson(j("""{"tekst":"bez id"}""")))
    }

    @Test fun linkWizytowekZapamietujeMojaKatedre() {
        assertEquals("https://otakos.wtf/#katedry?moja=teo-center", linkWizytowek("teo-center"))
        assertEquals("https://otakos.wtf/#katedry", linkWizytowek(null))
    }

    @Test fun wysylkaSprawdzaZanimZapyta() {
        val k = MostKlient("http://127.0.0.1:9", "k".repeat(48), limitMs = 500)
        assertTrue(k.tostWyslij("t", "kael-elara", "   ") is MostKlient.Wynik.Blad)
        assertTrue(k.tostWyslij("t", "kael-elara", "x".repeat(4001)) is MostKlient.Wynik.Blad)
    }
}

/** Zatwierdzanie Katedr przez Stół: dokładnie taki JSON oddaje most (services/ZarzadcaRejestru.js, /api/rejestr/stan). */
class RejestrTest {
    @Suppress("UNCHECKED_CAST")
    private fun j(s: String) = Json.parsuj(s) as Map<String, Any?>

    @Test fun stanZarzadcy() {
        val s = StanRejestru.zJson(j("""{"success":true,"ja":"teo-mas","zarzadca":"teo-mas","jestZarzadca":true,"oczekujace":[{"nick":"kael-elara","klucz":"MCowBQ","kiedy":"2026-10-03T00:00:00.000Z","powod":"nowa Katedra"},{"nick":"bez-klucza"}],"zatwierdzone":[{"nick":"nowa-kat","klucz":"x","kiedy":"t"}],"odrzuconych":0,"ostatniaWysylka":null}"""))
        assertTrue(s.jestZarzadca)
        assertEquals(listOf(OczekujacaKatedra("kael-elara", "MCowBQ", "2026-10-03T00:00:00.000Z", "nowa Katedra")), s.oczekujace)
        assertEquals(listOf("nowa-kat"), s.zatwierdzone)
        val inna = StanRejestru.zJson(j("""{"ja":"ktos","zarzadca":"teo-mas","jestZarzadca":false,"oczekujace":[],"zatwierdzone":[]}"""))
        assertEquals("teo-mas", inna.zarzadca)
        assertTrue(!inna.jestZarzadca)
    }
}

class PublikacjeYouTubeTest {
    @Suppress("UNCHECKED_CAST")
    private fun j(s: String) = Json.parsuj(s) as Map<String, Any?>

    @Test fun listaZMostu() {
        val l = PublikacjaYouTube.lista(j("""{"success":true,"publikacje":[{"id":"yt_1","etap":"do_akceptacji","nazwa":"rozpad-materii — Rozpad Percepcji","tytul":"Rozpad Percepcji","opis":"Film.","tagi":["ambient","otakos"],"kanalNazwa":"TeO Univers Studio"},{"id":"yt_2","etap":"prywatna","nazwa":"x","url":"https://youtu.be/abcdefghijk","uwaga":"YouTube trzyma prywatnie"},{"etap":"bez-id"}]}"""))
        assertEquals(2, l.size)
        assertTrue(l[0].czekaNaSuwerena)
        assertEquals(listOf("ambient", "otakos"), l[0].tagi)
        assertEquals("TeO Univers Studio", l[0].kanalNazwa)
        assertNull(l[1].kanalNazwa)
        assertTrue(!l[1].czekaNaSuwerena)
        assertEquals("🔒 na YouTube, ale prywatny", l[1].opisEtapu)
        assertEquals("https://youtu.be/abcdefghijk", l[1].url)
    }
}
