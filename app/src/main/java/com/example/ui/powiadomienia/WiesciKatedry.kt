package com.example.ui.powiadomienia

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.most.MostKlient
import com.example.most.Wiesc
import com.example.most.doPowiadomienia
import com.example.ui.katedra.PolaczenieStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 📯 Powiadomienia z Katedry (Suweren 2026-10-30: „powiadomienia na mój telefon, jak ktoś wchodzi
 * na stronę i skąd — tak jak miał Wix”).
 *
 * WorkManager co 15 minut (najkrócej, na ile Android pozwala w tle) pyta most o nowe wieści
 * kanału Mistrza i pokazuje je jako powiadomienia. Każdy rodzaj ma własny kanał Androida —
 * wyłącza się go w ustawieniach powiadomień StoL-a. Katedra śpi albo telefon niesparowany =
 * robotnik po cichu kończy, następna próba za 15 min. Nic nie idzie dalej niż do mostu Suwerena.
 */
object WiesciKatedry {
    private const val PRACA = "wiesci-katedry"
    private const val PREFS = "stol_wiesci"

    fun uruchom(context: Context) {
        utworzKanaly(context)
        val praca = PeriodicWorkRequestBuilder<Robotnik>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PRACA, ExistingPeriodicWorkPolicy.KEEP, praca)
    }

    private fun kanal(grupa: String) = "wiesci-$grupa"

    private fun utworzKanaly(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        Wiesc.RODZAJE.forEach { (grupa, nazwa) ->
            nm.createNotificationChannel(NotificationChannel(kanal(grupa), nazwa, NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun mozna(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Jedno sprawdzenie: pyta most, pokazuje nowe, zapamiętuje numer. Zwraca, ile pokazał. */
    suspend fun sprawdz(context: Context): Int = withContext(Dispatchers.IO) {
        val p = PolaczenieStore(context).wczytaj() ?: return@withContext 0
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val widziane = prefs.getInt("widziane", -1)
        val w = MostKlient(p.adres, p.klucz).wiesci(p.token, widziane)
        if (w !is MostKlient.Wynik.Ok) return@withContext 0
        val (wiesci, ostatni) = w.wartosc
        val d = doPowiadomienia(wiesci, ostatni, widziane, wylaczone = emptySet())
        prefs.edit().putInt("widziane", d.noweWidziane).apply()
        if (!mozna(context)) return@withContext 0
        d.pokaz.forEach { pokaz(context, it) }
        if (d.pominiete > 0) pokazZbiorcze(context, d.pominiete)
        d.pokaz.size
    }

    private fun otworzStoL(context: Context) = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @Suppress("MissingPermission")   // sprawdzone w mozna()
    private fun pokaz(context: Context, w: Wiesc) {
        val n = NotificationCompat.Builder(context, kanal(Wiesc.grupa(w.rodzaj)))
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(w.skad)
            .setContentText(w.tresc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(w.tresc))
            .setContentIntent(otworzStoL(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1000 + w.nr, n)
    }

    @Suppress("MissingPermission")
    private fun pokazZbiorcze(context: Context, ile: Int) {
        val n = NotificationCompat.Builder(context, kanal("pole"))
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Katedra")
            .setContentText("…i jeszcze $ile wcześniejszych wieści — Orbita w Hubie ma wszystkie.")
            .setContentIntent(otworzStoL(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(999, n)
    }

    class Robotnik(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            runCatching { sprawdz(applicationContext) }
            return Result.success()   // błąd sieci = cisza do następnego razu, bez lawiny ponowień
        }
    }
}
