package com.gestor.medicamentos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestor.medicamentos.data.Status
import com.gestor.medicamentos.ui.MedViewModel
import com.gestor.medicamentos.ui.components.ScreenScaffold
import com.gestor.medicamentos.ui.fmt
import com.gestor.medicamentos.ui.theme.statusColor

/** MED-03: historial de tomas con filtros, y tratamientos anteriores (finalizados / eliminados con opción de restaurar). */
@Composable
fun HistoryScreen(
    vm: MedViewModel,
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    ScreenScaffold("Historial", onBack, isDark = isDark, onToggleTheme = onToggleTheme) { mod ->
        Column(mod) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }) { Text("Tomas", Modifier.padding(14.dp)) }
                Tab(tab == 1, { tab = 1 }) { Text("Tratamientos anteriores", Modifier.padding(14.dp)) }
            }
            if (tab == 0) LogsTab(vm) else PastTab(vm)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogsTab(vm: MedViewModel) {
    val logs by vm.logs.collectAsState()
    val meds by vm.allMeds.collectAsState()
    val medFilter by vm.medFilter.collectAsState()
    val days by vm.daysFilter.collectAsState()

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Todo", 7 to "7 días", 30 to "30 días").forEach { (d, l) ->
                FilterChip(days == d, { vm.daysFilter.value = d }, label = { Text(l) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(medFilter == null, { vm.medFilter.value = null }, label = { Text("Todos") })
            meds.forEach { FilterChip(medFilter == it.id, { vm.medFilter.value = it.id }, label = { Text(it.name) }) }
        }
        if (logs.isEmpty()) Text("Sin registros para este filtro.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs, key = { it.id }) { l ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(l.medName, fontWeight = FontWeight.Bold)
                            Text("${l.dose} · ${fmt(l.timeMillis)}", style = MaterialTheme.typography.bodySmall)
                        }
                        AssistChip({}, label = { Text(l.status.name.lowercase().replaceFirstChar(Char::titlecase), color = statusColor(l.status)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PastTab(vm: MedViewModel) {
    val past by vm.past.collectAsState()
    if (past.isEmpty()) { Text("Aún no hay tratamientos anteriores.", Modifier.padding(16.dp)); return }
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(past, key = { it.id }) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.Bold)
                        Text("${m.dose} · ${m.durationDays} días", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (m.status == Status.DELETED) "Eliminado ${m.deletedAt?.let(::fmt) ?: ""}" else "Tratamiento finalizado",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (m.status == Status.DELETED) Button({ vm.restore(m.id) }) { Text("Restaurar") }
                }
            }
        }
    }
}
