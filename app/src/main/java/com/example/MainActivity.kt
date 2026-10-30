package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StolApp
import com.example.ui.katedra.KatedraViewModel
import com.example.ui.powiadomienia.WiesciKatedry
import com.example.ui.stol.StolViewModel
import com.example.ui.tlumacz.TlumaczStoL
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val katedraViewModel: KatedraViewModel by viewModels()

  /** Przyszedł link parowania — po zbudowaniu UI przełącz na zakładkę Katedra. */
  private val pokazKatedre = mutableStateOf(false)

  /** Android 13+: zgoda na powiadomienia — bez niej wieści z Katedry tylko się liczą, nie wyskakują. */
  private val zgodaNaPowiadomienia = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    TlumaczStoL.start(this)   // 🌍 język StoL-a i słownik z pamięci telefonu, zanim narysuje się pierwszy ekran
    if (savedInstanceState == null) obsluzLink(intent)
    WiesciKatedry.uruchom(this)   // 📯 co 15 min w tle: nowe wieści kanału Mistrza → powiadomienia
    if (Build.VERSION.SDK_INT >= 33 && !WiesciKatedry.mozna(this)) zgodaNaPowiadomienia.launch(Manifest.permission.POST_NOTIFICATIONS)
    setContent {
      MyApplicationTheme {
        val viewModel: StolViewModel = viewModel()
        LaunchedEffect(pokazKatedre.value) {
          if (pokazKatedre.value) {
            viewModel.wybierz(4)
            pokazKatedre.value = false
          }
        }
        StolApp(
          viewModel = viewModel,
          katedraViewModel = katedraViewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    obsluzLink(intent)
  }

  /** Skan QR z Katedry otwiera `otakos-stol://paruj?…` — parujemy i pokazujemy zakładkę Katedra. */
  private fun obsluzLink(intent: Intent?) {
    val link = intent?.dataString ?: return
    if (!link.startsWith("otakos-stol://paruj")) return
    katedraViewModel.sparuj(link)
    pokazKatedre.value = true
  }
}
