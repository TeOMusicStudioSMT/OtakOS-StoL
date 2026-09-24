package com.example.ui.katedra

import android.content.Context

/**
 * Zapamiętane połączenie z Katedrą: adres tunelu, klucz Straży i token urządzenia.
 * Trzymane lokalnie w SharedPreferences telefonu — nigdzie dalej nie jedzie.
 */
data class Polaczenie(val adres: String, val klucz: String, val token: String)

class PolaczenieStore(context: Context) {
    private val prefs = context.getSharedPreferences("stol_katedra", Context.MODE_PRIVATE)

    fun wczytaj(): Polaczenie? {
        val a = prefs.getString("adres", null) ?: return null
        val k = prefs.getString("klucz", null) ?: return null
        val t = prefs.getString("token", null) ?: return null
        return Polaczenie(a, k, t)
    }

    fun zapisz(p: Polaczenie) {
        prefs.edit().putString("adres", p.adres).putString("klucz", p.klucz).putString("token", p.token).apply()
    }

    fun zapomnij() { prefs.edit().clear().apply() }
}
