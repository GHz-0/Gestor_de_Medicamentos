package com.gestor.medicamentos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor.medicamentos.data.DoseStatus
import com.gestor.medicamentos.data.Medication
import com.gestor.medicamentos.data.Profile
import com.gestor.medicamentos.data.Status
import com.gestor.medicamentos.domain.Schedule
import com.gestor.medicamentos.ui.fmt
import com.gestor.medicamentos.ui.fmtRemaining
import com.gestor.medicamentos.ui.greeting
import com.gestor.medicamentos.ui.randomHealthPhrase
import com.gestor.medicamentos.ui.timeGreeting
import com.gestor.medicamentos.ui.MedViewModel
import com.gestor.medicamentos.ui.components.ImageBox
import com.gestor.medicamentos.ui.components.MedCard
import com.gestor.medicamentos.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private fun getDosesForDay(m: Medication, dayStartMillis: Long, dayEndMillis: Long): List<Long> {
    if (m.status != Status.ACTIVE) return emptyList()
    val endTreatment = m.endMillis()
    if (dayStartMillis > endTreatment) return emptyList()

    val list = mutableListOf<Long>()
    if (m.mode == com.gestor.medicamentos.data.Mode.INTERVALO) {
        val step = m.intervalHours.coerceAtLeast(1) * 3_600_000L
        var t = m.startMillis
        if (t < dayStartMillis) {
            val count = (dayStartMillis - t) / step
            t += count * step
            if (t < dayStartMillis) t += step
        }
        while (t <= dayEndMillis && t <= endTreatment) {
            list.add(t)
            t += step
        }
    } else {
        var cursor = maxOf(m.startMillis - 1, dayStartMillis - 1)
        while (true) {
            val nextTime = Schedule.next(m, cursor) ?: break
            if (nextTime > dayEndMillis) break
            if (nextTime >= dayStartMillis) {
                list.add(nextTime)
            }
            cursor = nextTime
        }
    }
    return list
}

@Composable
fun HomeScreen(
    vm: MedViewModel, profile: Profile,
    isDark: Boolean = false, onToggleTheme: () -> Unit = {},
    onOpenProfile: () -> Unit, onAdd: () -> Unit, onEdit: (Medication) -> Unit, onHistory: () -> Unit
) {
    val ctx = LocalContext.current
    val meds by vm.active.collectAsState()
    val logs by vm.logs.collectAsState()
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(1000); value = System.currentTimeMillis() } }
    var activePanel by rememberSaveable { mutableStateOf("stats") }
    var statPage by remember { mutableIntStateOf(0) }
    var refillFor by remember { mutableStateOf<Medication?>(null) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }

    val next = meds.firstOrNull { it.nextDoseMillis != null }
    val low = meds.filter { it.lowStock() }
    val phrase = remember { randomHealthPhrase() }

    // Carrusel automático de estadísticas que cambia cada 10 segundos
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            statPage = (statPage + 1) % 3
        }
    }

    val startOfDay = remember(now) {
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val takenToday = remember(logs, startOfDay) {
        logs.count { it.timeMillis >= startOfDay && it.status == DoseStatus.TOMADA }
    }
    val todayDateStr = remember(now) {
        val fmtStr = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "MX"))
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(fmtStr)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }
    }

    fun notifyCaregiver(m: Medication) {
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${profile.caregiverPhone}"))
            .putExtra("sms_body", "${profile.name} aún no toma ${m.name} (${m.dose}) programado para ${fmt(m.nextDoseMillis ?: now)}.")
        runCatching { ctx.startActivity(i) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Perfil Compacto (sin la notificación dentro)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Header row: Fecha + Botón de Tema
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "📅 $todayDateStr",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                onClick = onToggleTheme,
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    if (isDark) "☀️ Claro" else "🌙 Oscuro",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Compact Profile Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImageBox(
                                profile.photoPath,
                                Modifier
                                    .size(60.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable(onClick = onOpenProfile),
                                CircleShape, profile.name.take(1).uppercase()
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "¡${timeGreeting(now)}, ${profile.name}!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    phrase,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Card 2: Agenda diaria de tomas (Ubicada inmediatamente debajo del Perfil)
                val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm", Locale("es", "MX")) }
                val daysList = remember(now, meds) {
                    (0..6).map { offset ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = now
                            add(Calendar.DAY_OF_YEAR, offset)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val dayStart = cal.timeInMillis
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        val dayEnd = cal.timeInMillis

                        val dayLabel = when (offset) {
                            0 -> "Hoy"
                            1 -> "Mañana"
                            else -> {
                                val dayOfWeekFmt = DateTimeFormatter.ofPattern("EEE", Locale("es", "MX"))
                                Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).format(dayOfWeekFmt)
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }
                            }
                        }
                        val dayNum = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_MONTH)

                        val scheduled = meds.flatMap { m ->
                            getDosesForDay(m, dayStart, dayEnd).map { time -> m to time }
                        }.sortedBy { it.second }

                        Triple(dayLabel, "$dayNum", scheduled)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "📅  Agenda diaria de tomas",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Tira Horizontal de Selección de Días del Calendario
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysList.forEachIndexed { index, (label, num, scheduled) ->
                                val isSelected = index == selectedDayOffset
                                Surface(
                                    onClick = { selectedDayOffset = index },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            num,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (scheduled.isNotEmpty()) {
                                            Spacer(Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Turquoise, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Lista de Tomas Programadas para el Día Seleccionado
                        val activeDayScheduled = daysList.getOrNull(selectedDayOffset)?.third ?: emptyList()
                        if (activeDayScheduled.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🌱 No hay medicamentos programados para este día",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activeDayScheduled.forEach { (m, doseTime) ->
                                    val formattedTime = Instant.ofEpochMilli(doseTime).atZone(ZoneId.systemDefault()).format(timeFmt)
                                    val isTaken = logs.any {
                                        it.medId == m.id &&
                                        it.status == DoseStatus.TOMADA &&
                                        kotlin.math.abs(it.timeMillis - doseTime) < 3_600_000L
                                    }

                                    if (isTaken) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            border = BorderStroke(1.dp, Color(0xFF2E7D32))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFC8E6C9)
                                                    ) {
                                                        Text(
                                                            formattedTime,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1B5E20)
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            m.name,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1B5E20)
                                                        )
                                                        Text(
                                                            "Dosis: ${m.dose}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFC8E6C9)
                                                ) {
                                                    Text(
                                                        "✓ Tomada",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1B5E20)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Dosis pendiente: se permite marcar con checkbox si es la más reciente o hasta 30 min antes
                                        val diff = doseTime - now
                                        val isClosest = (m.id == next?.id)
                                        val canTake = selectedDayOffset == 0 && (isClosest || diff <= 30 * 60_000L)
                                        val nextBg = if (diff <= 0) (if (isDark) Color(0xFF3B1E1E) else RedSoft) else MaterialTheme.colorScheme.primaryContainer
                                        val nextText = if (diff <= 0) (if (isDark) Color(0xFFEF5350) else Red) else MaterialTheme.colorScheme.onPrimaryContainer

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = nextBg),
                                            border = BorderStroke(1.dp, nextText.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = nextText.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                formattedTime,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = nextText
                                                            )
                                                        }
                                                        Text(
                                                            if (isClosest) {
                                                                if (diff > 0) "⏰ En ${fmtRemaining(diff)}" else "⏰ ¡Es hora!"
                                                            } else {
                                                                if (diff > 0) "⏰ En ${fmtRemaining(diff)}" else "⏰ ¡Pendiente!"
                                                            },
                                                            fontWeight = FontWeight.Bold,
                                                            color = nextText,
                                                            style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        "${m.name} (${m.dose})",
                                                        fontWeight = FontWeight.Bold,
                                                        color = nextText,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                if (canTake) {
                                                    Checkbox(
                                                        checked = false,
                                                        onCheckedChange = { vm.take(m.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 3: Paneles Acordeón Mutuos (Estadísticas arriba, Medicamentos abajo)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Advertencia de pocas unidades (si aplica)
                        if (low.isNotEmpty()) {
                            val lowBg = if (isDark) Color(0xFF3B1E1E) else RedSoft
                            val lowText = if (isDark) Color(0xFFEF5350) else Red
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = lowBg),
                                border = BorderStroke(1.dp, lowText.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "⚠ Quedan pocas unidades: " + low.joinToString { "${it.name} (${it.stock})" },
                                    Modifier.padding(10.dp), color = lowText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Panel 1: Estadísticas del día (empieza expandido)
                        val isStatsExpanded = activePanel == "stats"
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { activePanel = if (isStatsExpanded) "" else "stats" },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("📊  Estadísticas del día", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(if (isStatsExpanded) "▴" else "▾", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 18.sp)
                            }
                        }

                        if (isStatsExpanded) {
                            // Carrusel de Estadísticas que rota cada 10 segundos
                            val currentStatIdx = statPage % 3
                            val (statVal, statLabel, statColor) = when (currentStatIdx) {
                                0 -> Triple("${meds.size}", "Medicamentos activos en tratamiento", MaterialTheme.colorScheme.primary)
                                1 -> Triple("$takenToday", "Tomas completadas hoy con éxito", Color(0xFF2E7D32))
                                else -> Triple("${meds.count { it.nextDoseMillis != null }}", "Tomas pendientes por realizar hoy", CyanDark)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { statPage = (statPage + 1) % 3 },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        statVal,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = statColor
                                    )
                                    Text(
                                        statLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0, 1, 2).forEach { idx ->
                                            Box(
                                                modifier = Modifier
                                                    .size(if (idx == currentStatIdx) 8.dp else 5.dp)
                                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (idx == currentStatIdx) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Panel 2: Medicamentos Activos (desplegable)
                        val isMedsExpanded = activePanel == "meds"
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { activePanel = if (isMedsExpanded) "" else "meds" },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💊  Medicamentos activos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    if (meds.isNotEmpty()) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                "${meds.size}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                                Text(if (isMedsExpanded) "▴" else "▾", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 18.sp)
                            }
                        }

                        if (isMedsExpanded) {
                            if (meds.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🌱 No hay dosis pendientes por tomar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    meds.forEach { m ->
                                        MedCard(
                                            m, now, Modifier.fillMaxWidth(),
                                            onTake = { vm.take(m.id) },
                                            onEdit = { onEdit(m) },
                                            onDelete = { vm.delete(m.id) },
                                            onRefill = { refillFor = m },
                                            onNotifyCaregiver = if (profile.caregiverPhone.isNotBlank()) ({ notifyCaregiver(m) }) else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 4: Botones rectangulares compactos pegados a la parte inferior
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onAdd,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("➕  Registrar nueva dosis", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = onHistory,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("🕘  Historial de tomas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    refillFor?.let { m ->
        var qty by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { refillFor = null },
            title = { Text("Recargar ${m.name}") },
            text = { OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text("Unidades compradas") }, singleLine = true) },
            confirmButton = { TextButton({ qty.toIntOrNull()?.let { vm.refill(m.id, it) }; refillFor = null }) { Text("Agregar") } },
            dismissButton = { TextButton({ refillFor = null }) { Text("Cancelar") } }
        )
    }
}
