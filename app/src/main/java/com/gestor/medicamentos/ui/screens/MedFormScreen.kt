package com.gestor.medicamentos.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestor.medicamentos.data.Medication
import com.gestor.medicamentos.data.Mode
import com.gestor.medicamentos.domain.Schedule
import com.gestor.medicamentos.ui.components.PhotoPicker
import com.gestor.medicamentos.ui.components.ScreenScaffold
import com.gestor.medicamentos.ui.fmt
import java.util.Calendar

/** Registro/edición. Obligatorios: nombre, dosis, inicio, frecuencia y duración. Opcionales: fórmula, gramos, unidades, foto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedFormScreen(
    initial: Medication?,
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSave: (Medication) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var dose by remember { mutableStateOf(initial?.dose ?: "") }
    var start by remember { mutableLongStateOf(initial?.startMillis ?: (System.currentTimeMillis() + 5 * 60_000L)) }
    var mode by remember { mutableStateOf(initial?.mode ?: Mode.INTERVALO) }
    var interval by remember { mutableStateOf((initial?.intervalHours ?: 8).toString()) }
    var mask by remember { mutableIntStateOf(initial?.weekdaysMask ?: 0) }
    var on by remember { mutableStateOf((initial?.cycleOn ?: 5).toString()) }
    var off by remember { mutableStateOf((initial?.cycleOff ?: 2).toString()) }
    var duration by remember { mutableStateOf(initial?.durationDays?.toString() ?: "") }
    var formula by remember { mutableStateOf(initial?.formula ?: "") }
    var grams by remember { mutableStateOf(initial?.gramsPerPill ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var minStock by remember { mutableStateOf(initial?.minStock?.toString() ?: "5") }
    var photo by remember { mutableStateOf(initial?.photoPath) }
    var tried by remember { mutableStateOf(false) }

    val nameErr = name.isBlank()
    val doseErr = dose.isBlank()
    val durErr = (duration.toIntOrNull() ?: 0) < 1
    val freqErr = when (mode) {
        Mode.INTERVALO -> (interval.toIntOrNull() ?: 0) < 1
        Mode.DIAS_SEMANA -> mask == 0
        Mode.CICLO -> (on.toIntOrNull() ?: 0) < 1
    }
    val valid = !(nameErr || doseErr || durErr || freqErr)

    fun build() = (initial ?: Medication(name = "", dose = "", startMillis = start)).copy(
        name = name.trim(), dose = dose.trim(), startMillis = start, mode = mode,
        intervalHours = interval.toIntOrNull() ?: 8, weekdaysMask = mask,
        cycleOn = on.toIntOrNull() ?: 5, cycleOff = off.toIntOrNull() ?: 2,
        durationDays = duration.toIntOrNull() ?: 1,
        formula = formula.trim(), gramsPerPill = grams.trim(),
        stock = stock.toIntOrNull(), minStock = minStock.toIntOrNull() ?: 5,
        photoPath = photo
    )

    fun pickDateTime() {
        val c = Calendar.getInstance().apply { timeInMillis = start }
        DatePickerDialog(ctx, { _, y, mo, d ->
            TimePickerDialog(ctx, { _, h, mi ->
                start = Calendar.getInstance().apply { set(y, mo, d, h, mi, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    var optionalExpanded by remember { mutableStateOf(false) }

    ScreenScaffold(
        if (initial == null) "Registrar nueva dosis" else "Editar medicamento",
        onBack,
        isDark = isDark,
        onToggleTheme = onToggleTheme,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (valid) onSave(build()) else tried = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                icon = { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) },
                text = { Text("Guardar", fontWeight = FontWeight.Bold) }
            )
        }
    ) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos obligatorios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                name, { name = it }, label = { Text("Nombre del medicamento *") }, singleLine = true,
                isError = tried && nameErr, supportingText = { if (tried && nameErr) Text("Obligatorio") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                dose, { dose = it }, label = { Text("Dosis * (ej. 1 tableta, 5 ml)") }, singleLine = true,
                isError = tried && doseErr, supportingText = { if (tried && doseErr) Text("Obligatorio") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(::pickDateTime, Modifier.fillMaxWidth()) { Text("🕒 Inicia: ${fmt(start)} *") }

            Text("¿Cada cuánto? *", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mode.entries.forEach {
                    FilterChip(mode == it, { mode = it }, label = {
                        Text(when (it) { Mode.INTERVALO -> "Cada X horas"; Mode.DIAS_SEMANA -> "Días de la semana"; Mode.CICLO -> "Ciclo" })
                    })
                }
            }
            when (mode) {
                Mode.INTERVALO -> OutlinedTextField(
                    interval, { interval = it.filter(Char::isDigit) }, label = { Text("Cada cuántas horas *") }, singleLine = true,
                    isError = tried && freqErr
                )
                Mode.DIAS_SEMANA -> Column {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, d ->
                            FilterChip((mask shr i) and 1 == 1, { mask = mask xor (1 shl i) }, label = { Text(d) })
                        }
                    }
                    if (tried && freqErr) Text("Selecciona al menos un día", color = MaterialTheme.colorScheme.error)
                }
                Mode.CICLO -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(on, { on = it.filter(Char::isDigit) }, label = { Text("Días con toma") }, singleLine = true,
                        isError = tried && freqErr, modifier = Modifier.weight(1f))
                    OutlinedTextField(off, { off = it.filter(Char::isDigit) }, label = { Text("Días de descanso") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                duration, { duration = it.filter(Char::isDigit) }, label = { Text("Duración del tratamiento (días) *") }, singleLine = true,
                isError = tried && durErr, supportingText = { if (tried && durErr) Text("Obligatorio") }, modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    stock, { stock = it.filter(Char::isDigit) },
                    label = { Text("Unidades disponibles") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    minStock, { minStock = it.filter(Char::isDigit) },
                    label = { Text("Mínimo aviso recarga") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Datos del medicamento en menú desplegable
            Card(
                Modifier.fillMaxWidth().clickable { optionalExpanded = !optionalExpanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Datos del medicamento (fórmula, gramos)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(if (optionalExpanded) "▴" else "▾")
                }
            }
            if (optionalExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(formula, { formula = it }, label = { Text("Nombre de la fórmula (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(grams, { grams = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Gramos por pastilla (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            // Foto del medicamento visible justo debajo del menú desplegable y centrado
            Spacer(Modifier.height(4.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Foto del medicamento (opcional)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                PhotoPicker(photo, { photo = it }, Modifier.size(120.dp), RoundedCornerShape(16.dp))
            }

            if (valid) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("Agenda de los próximos 7 días", fontWeight = FontWeight.Bold)
                    val list = Schedule.upcoming(build())
                    if (list.isEmpty()) Text("Sin tomas programadas") else list.take(12).forEach { Text("• ${fmt(it)}") }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
