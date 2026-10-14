package com.gestor.medicamentos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.gestor.medicamentos.data.Medication
import com.gestor.medicamentos.domain.DoseState
import com.gestor.medicamentos.ui.fmt
import com.gestor.medicamentos.ui.fmtRemaining
import com.gestor.medicamentos.ui.theme.*

/**
 * Tarjeta autoajustable: color según cercanía de la toma, reloj analógico, casilla de "tomada" a la derecha,
 * menú Editar/Eliminar que se despliega en el punto donde se toca.
 */
@Composable
fun MedCard(
    m: Medication, now: Long, modifier: Modifier,
    onTake: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onRefill: () -> Unit,
    onNotifyCaregiver: (() -> Unit)?
) {
    val state = DoseState.of(m, now)
    val isDark = isSystemInDarkTheme()
    val (bg, border) = when (state) {
        DoseState.LEJOS -> if (isDark) androidx.compose.ui.graphics.Color(0xFF1B3326) to androidx.compose.ui.graphics.Color(0xFF4CAF50) else OkBg to OkBorder
        DoseState.PRONTO -> if (isDark) androidx.compose.ui.graphics.Color(0xFF38311B) to androidx.compose.ui.graphics.Color(0xFFFFC107) else SoonBg to SoonBorder
        DoseState.AHORA -> if (isDark) androidx.compose.ui.graphics.Color(0xFF3B1E1E) to androidx.compose.ui.graphics.Color(0xFFEF5350) else RedSoft to Red
    }
    var menu by remember { mutableStateOf(false) }
    var tap by remember { mutableStateOf(Offset.Zero) }
    var height by remember { mutableIntStateOf(0) }
    var askTake by remember { mutableStateOf(false) }
    var askEdit by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val diff = (m.nextDoseMillis ?: now) - now

    Box(modifier.onSizeChanged { height = it.height }.pointerInput(Unit) { detectTapGestures { tap = it; menu = true } }) {
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(1.5.dp, border)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                AnalogClock(now, border)
                Spacer(Modifier.width(10.dp))
                if (m.photoPath != null) {
                    ImageBox(m.photoPath, Modifier.size(54.dp), RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(buildString {
                        append("Dosis: ${m.dose}")
                        if (m.formula.isNotBlank()) append(" · ${m.formula}")
                        if (m.gramsPerPill.isNotBlank()) append(" · ${m.gramsPerPill} g/pastilla")
                    }, style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (diff > 0) "Próxima en ${fmtRemaining(diff)}" else "⏰ ¡Es hora! (hace ${fmtRemaining(-diff)})",
                        fontWeight = FontWeight.SemiBold, color = if (state == DoseState.AHORA) Red else MaterialTheme.colorScheme.onSurface
                    )
                    Text(fmt(m.nextDoseMillis ?: now), style = MaterialTheme.typography.bodySmall)
                    m.stock?.let {
                        if (m.lowStock()) Text(
                            "⚠ Quedan $it unidades · Recargar", color = Red, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onRefill() }.padding(vertical = 4.dp)
                        ) else Text("Unidades: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state == DoseState.AHORA && onNotifyCaregiver != null)
                        TextButton(onNotifyCaregiver, contentPadding = PaddingValues(0.dp)) { Text("📩 Avisar a mi cuidador") }
                }
                Checkbox(checked = false, onCheckedChange = { askTake = true })
            }
        }
        DropdownMenu(
            expanded = menu, onDismissRequest = { menu = false },
            offset = with(density) { DpOffset(tap.x.toDp(), (tap.y - height).toDp()) }
        ) {
            DropdownMenuItem({ Text("✏️  Editar") }, { menu = false; askEdit = true })
            DropdownMenuItem({ Text("🗑  Eliminar", color = Red) }, { menu = false; askDelete = true })
        }
    }

    if (askTake) AlertDialog(
        onDismissRequest = { askTake = false },
        title = { Text("¿Tomaste tu dosis?") },
        text = { Text("${m.name} — ${m.dose}") },
        confirmButton = { TextButton({ askTake = false; onTake() }) { Text("Sí, la tomé") } },
        dismissButton = { TextButton({ askTake = false }) { Text("Aún no") } }
    )
    if (askEdit) AlertDialog(
        onDismissRequest = { askEdit = false },
        title = { Text("Editar medicamento") },
        text = { Text("¿Quieres editar ${m.name}?") },
        confirmButton = { TextButton({ askEdit = false; onEdit() }) { Text("Sí") } },
        dismissButton = { TextButton({ askEdit = false }) { Text("No") } }
    )
    if (askDelete) AlertDialog(
        onDismissRequest = { askDelete = false },
        title = { Text("¿Eliminar la dosis de ${m.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Se guardará en el historial y podrás restaurarla. Mantén presionado el botón 4 segundos para confirmar.")
                HoldToConfirmButton("Mantén para eliminar") { askDelete = false; onDelete() }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton({ askDelete = false }) { Text("Cancelar") } }
    )
}
