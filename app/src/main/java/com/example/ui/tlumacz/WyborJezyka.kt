package com.example.ui.tlumacz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.most.JEZYKI_KATEDRY

/**
 * 🌍 Wybór języka StoL-a (zakładka Katedra). Pod spodem prawda o Tłumaczu: ile napisów czeka na model,
 * błąd mostu — żeby nie udawać, że wszystko już przetłumaczone.
 */
@Composable
fun WyborJezyka(modifier: Modifier = Modifier) {
    var otwarty by remember { mutableStateOf(false) }
    val jezyk = TlumaczStoL.jezyk
    Column(modifier) {
        Box {
            OutlinedButton(onClick = { otwarty = true }, modifier = Modifier.testTag("btn_jezyk")) {
                Text("🌍 " + (JEZYKI_KATEDRY.firstOrNull { it.first == jezyk }?.second ?: jezyk))
            }
            DropdownMenu(expanded = otwarty, onDismissRequest = { otwarty = false }) {
                JEZYKI_KATEDRY.forEach { (kod, nazwa) ->
                    DropdownMenuItem(text = { Text(nazwa) }, onClick = { otwarty = false; TlumaczStoL.ustaw(kod) })
                }
            }
        }
        val blad = TlumaczStoL.blad
        when {
            jezyk == "pl" -> {}
            blad != null -> Text("⚠ $blad", fontSize = 11.sp, color = Color(0xFFDC2626))
            TlumaczStoL.czeka > 0 -> Text(tl("🌍 tłumaczę…") + " (${TlumaczStoL.czeka})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
