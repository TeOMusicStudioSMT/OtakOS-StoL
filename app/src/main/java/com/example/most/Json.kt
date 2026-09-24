package com.example.most

/**
 * Minimalny parser JSON dla odpowiedzi mostu Katedry.
 *
 * Czysty Kotlin, bez Androida i bez bibliotek: ten sam kod działa w apce i w testach JVM.
 * Wynik: Map<String, Any?> / List<Any?> / String / Double / Boolean / null.
 */
object Json {
    class BladJson(wiadomosc: String) : IllegalArgumentException(wiadomosc)

    fun parsuj(tekst: String): Any? {
        val p = Parser(tekst)
        p.spacje()
        val w = p.wartosc()
        p.spacje()
        if (p.i != tekst.length) throw BladJson("Nadmiarowe znaki na pozycji ${p.i}")
        return w
    }

    private class Parser(val s: String) {
        var i = 0

        fun spacje() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun wartosc(): Any? {
            if (i >= s.length) throw BladJson("Nieoczekiwany koniec danych")
            return when (s[i]) {
                '{' -> obiekt()
                '[' -> tablica()
                '"' -> napis()
                't' -> slowo("true", true)
                'f' -> slowo("false", false)
                'n' -> slowo("null", null)
                else -> liczba()
            }
        }

        private fun slowo(w: String, v: Any?): Any? {
            if (!s.startsWith(w, i)) throw BladJson("Oczekiwano '$w' na pozycji $i")
            i += w.length
            return v
        }

        private fun obiekt(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++; spacje()
            if (s.getOrNull(i) == '}') { i++; return m }
            while (true) {
                spacje()
                if (s.getOrNull(i) != '"') throw BladJson("Oczekiwano klucza na pozycji $i")
                val k = napis()
                spacje()
                if (s.getOrNull(i) != ':') throw BladJson("Oczekiwano ':' na pozycji $i")
                i++; spacje()
                m[k] = wartosc()
                spacje()
                when (s.getOrNull(i)) {
                    ',' -> i++
                    '}' -> { i++; return m }
                    else -> throw BladJson("Oczekiwano ',' lub '}' na pozycji $i")
                }
            }
        }

        private fun tablica(): List<Any?> {
            val l = ArrayList<Any?>()
            i++; spacje()
            if (s.getOrNull(i) == ']') { i++; return l }
            while (true) {
                spacje()
                l.add(wartosc())
                spacje()
                when (s.getOrNull(i)) {
                    ',' -> i++
                    ']' -> { i++; return l }
                    else -> throw BladJson("Oczekiwano ',' lub ']' na pozycji $i")
                }
            }
        }

        private fun napis(): String {
            val sb = StringBuilder()
            i++
            while (true) {
                if (i >= s.length) throw BladJson("Niezamknięty napis")
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = s.getOrNull(i++) ?: throw BladJson("Urwana sekwencja ucieczki")
                        when (e) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw BladJson("Urwane \\u")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4
                            }
                            else -> throw BladJson("Nieznana ucieczka \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun liczba(): Double {
            val start = i
            if (s.getOrNull(i) == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
            return s.substring(start, i).toDoubleOrNull() ?: throw BladJson("Zła liczba na pozycji $start")
        }
    }
}

/** Wygodne odczyty z mapy JSON — brak pola albo zły typ daje null, nie wyjątek. */
internal fun Map<String, Any?>.napis(k: String): String? = this[k] as? String
internal fun Map<String, Any?>.liczba(k: String): Double? = this[k] as? Double
internal fun Map<String, Any?>.logika(k: String): Boolean? = this[k] as? Boolean
@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.obiekt(k: String): Map<String, Any?>? = this[k] as? Map<String, Any?>
@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.lista(k: String): List<Any?> = (this[k] as? List<Any?>) ?: emptyList()
