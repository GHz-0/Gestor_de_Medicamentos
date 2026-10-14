package com.gestor.medicamentos.domain

import com.gestor.medicamentos.data.Medication
import com.gestor.medicamentos.data.Mode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Motor de cálculo de tomas (MED-02, MED-07) respetando la duración del tratamiento. */
object Schedule {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Siguiente toma posterior a [after]; null si el tratamiento ya terminó. */
    fun next(m: Medication, after: Long): Long? {
        val n = compute(m, after) ?: return null
        return if (n > m.endMillis()) null else n
    }

    private fun compute(m: Medication, after: Long): Long? {
        if (m.mode == Mode.INTERVALO) {
            val step = m.intervalHours.coerceAtLeast(1) * 3_600_000L
            return if (after < m.startMillis) m.startMillis
            else m.startMillis + ((after - m.startMillis) / step + 1) * step
        }
        val start = Instant.ofEpochMilli(m.startMillis).atZone(zone)
        val firstDay = start.toLocalDate()
        val today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
        for (i in 0..800L) {
            val day = today.plusDays(i)
            if (day.isBefore(firstDay) || !isActiveDay(m, day, firstDay)) continue
            val t = day.atTime(start.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
            if (t > after) return t
        }
        return null
    }

    private fun isActiveDay(m: Medication, day: LocalDate, firstDay: LocalDate): Boolean = when (m.mode) {
        Mode.DIAS_SEMANA -> (m.weekdaysMask shr (day.dayOfWeek.value - 1)) and 1 == 1
        Mode.CICLO -> {
            val total = (m.cycleOn + m.cycleOff).coerceAtLeast(1)
            ChronoUnit.DAYS.between(firstDay, day).mod(total) < m.cycleOn
        }
        else -> true
    }

    /** Agenda de los próximos [days] días (MED-07). */
    fun upcoming(m: Medication, days: Int = 7): List<Long> {
        val now = System.currentTimeMillis()
        val limit = now + days * 86_400_000L
        val out = mutableListOf<Long>()
        var cursor = maxOf(m.startMillis - 1, now)
        while (out.size < 40) {
            val n = next(m, cursor) ?: break
            if (n > limit) break
            out += n; cursor = n
        }
        return out
    }
}
