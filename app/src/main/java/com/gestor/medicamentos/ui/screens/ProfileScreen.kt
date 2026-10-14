package com.gestor.medicamentos.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.gestor.medicamentos.data.Profile
import com.gestor.medicamentos.ui.components.PhotoPicker
import com.gestor.medicamentos.ui.components.ScreenScaffold
import com.gestor.medicamentos.ui.theme.Turquoise
import java.util.Calendar

/** Perfil del paciente: nombre obligatorio; foto y datos de salud opcionales; contacto de cuidador (MED-06 local). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    p: Profile,
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSave: (Profile) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var name by remember(p.name) { mutableStateOf(p.name) }
    var photo by remember(p.photoPath) { mutableStateOf(p.photoPath) }
    var birth by remember(p.birthDate) { mutableStateOf(p.birthDate) }
    var weight by remember(p.weightKg) { mutableStateOf(p.weightKg) }
    var height by remember(p.heightCm) { mutableStateOf(p.heightCm) }
    var blood by remember(p.bloodType) { mutableStateOf(p.bloodType) }
    var insurance by remember(p.insuranceNo) { mutableStateOf(p.insuranceNo) }
    var allergies by remember(p.allergies) { mutableStateOf(p.allergies) }
    var cName by remember(p.caregiverName) { mutableStateOf(p.caregiverName) }
    var cPhone by remember(p.caregiverPhone) { mutableStateOf(p.caregiverPhone) }
    var caregiverExpanded by remember { mutableStateOf(false) }

    val hasChanges = name != p.name || photo != p.photoPath || birth != p.birthDate ||
        weight != p.weightKg || height != p.heightCm || blood != p.bloodType ||
        insurance != p.insuranceNo || allergies != p.allergies ||
        cName != p.caregiverName || cPhone != p.caregiverPhone

    ScreenScaffold(
        "Mi perfil",
        onBack,
        isDark = isDark,
        onToggleTheme = onToggleTheme,
        floatingActionButton = {
            if (hasChanges && name.isNotBlank()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onSave(p.copy(
                            name = name.trim(), photoPath = photo, birthDate = birth, weightKg = weight, heightCm = height,
                            bloodType = blood, insuranceNo = insurance.trim(), allergies = allergies.trim(),
                            caregiverName = cName.trim(), caregiverPhone = cPhone
                        ))
                        Toast.makeText(ctx, "Perfil guardado", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    icon = { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) },
                    text = { Text("Guardar", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { mod ->
        Column(
            mod.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoPicker(photo, { photo = it }, Modifier.size(130.dp).border(4.dp, Turquoise, CircleShape), placeholder = name.take(1).uppercase())
            Text("Toca la foto para cambiarla", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true, isError = name.isBlank(), modifier = Modifier.fillMaxWidth())

            OutlinedButton({
                val c = Calendar.getInstance()
                DatePickerDialog(ctx, { _, y, m, d -> birth = "%02d/%02d/%04d".format(d, m + 1, y) },
                    c.get(Calendar.YEAR) - 30, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }, Modifier.fillMaxWidth()) { Text(if (birth.isBlank()) "🎂 Fecha de nacimiento (opcional)" else "🎂 Nacimiento: $birth") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Peso (kg)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(height, { height = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Altura (cm)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text("Tipo de sangre (opcional)", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").forEach {
                    FilterChip(blood == it, { blood = if (blood == it) "" else it }, label = { Text(it) })
                }
            }
            OutlinedTextField(insurance, { insurance = it }, label = { Text("Número de seguro (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(allergies, { allergies = it }, label = { Text("Alergias / notas médicas (opcional)") }, modifier = Modifier.fillMaxWidth())

            Card(
                Modifier.fillMaxWidth().clickable { caregiverExpanded = !caregiverExpanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text("Cuidador o familiar (opcional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(if (caregiverExpanded) "▴" else "▾")
                }
            }
            if (caregiverExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(cName, { cName = it }, label = { Text("Nombre del cuidador (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cPhone, { cPhone = it.filter { c -> c.isDigit() || c == '+' } }, label = { Text("Teléfono del cuidador (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
