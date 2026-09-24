package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StolApp
import com.example.ui.StolViewModel
import com.example.ui.katedra.KatedraViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val katedraViewModel: KatedraViewModel by viewModels()

  /** Przyszedł link parowania — po zbudowaniu UI przełącz na zakładkę Katedra. */
  private val pokazKatedre = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (savedInstanceState == null) obsluzLink(intent)
    setContent {
      MyApplicationTheme {
        val viewModel: StolViewModel = viewModel()
        LaunchedEffect(pokazKatedre.value) {
          if (pokazKatedre.value) {
            viewModel.selectTab(4)
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
