package com.gestor.medicamentos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Mode { INTERVALO, DIAS_SEMANA, CICLO }          // MED-07
enum class Status { ACTIVE, FINISHED, DELETED }              // activo / terminado / eliminado (restaurable)
enum class DoseStatus { TOMADA, OMITIDA, POSPUESTA }         // MED-03

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val photoPath: String? = null,
    val birthDate: String = "",
    val weightKg: String = "",
    val heightCm: String = "",
    val bloodType: String = "",
    val insuranceNo: String = "",
    val allergies: String = "",
    val caregiverName: String = "",
    val caregiverPhone: String = ""
)

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Obligatorios
    val name: String,
    val dose: String,
    val startMillis: Long,
    val mode: Mode = Mode.INTERVALO,
    val intervalHours: Int = 8,
    val weekdaysMask: Int = 0,            // bit0 = lunes ... bit6 = domingo
    val cycleOn: Int = 5,
    val cycleOff: Int = 2,
    val durationDays: Int = 7,
    // Opcionales
    val formula: String = "",
    val gramsPerPill: String = "",
    val stock: Int? = null,
    val minStock: Int? = 5,               // Mínimo de unidades para advertencia/recarga
    val photoPath: String? = null,
    // Estado interno
    val status: Status = Status.ACTIVE,
    val nextDoseMillis: Long? = null,     // toma pendiente actual (si ya pasó = vencida)
    val deletedAt: Long? = null
) {
    fun endMillis(): Long = startMillis + durationDays * 86_400_000L
    fun lowStock(): Boolean = stock != null && stock <= (minStock ?: LOW_STOCK)
    companion object { const val LOW_STOCK = 5 }
}

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medId: Long,
    val medName: String,
    val dose: String,
    val status: DoseStatus,
    val timeMillis: Long
)
