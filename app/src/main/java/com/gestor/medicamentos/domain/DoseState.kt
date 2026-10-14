package com.gestor.medicamentos.domain

import com.gestor.medicamentos.data.Medication

/** Semáforo de la tarjeta: verde (lejos), amarillo (≤10 min), rojo (hora o vencida hasta marcarla). */
enum class DoseState {
    LEJOS, PRONTO, AHORA;

    companion object {
        fun of(m: Medication, now: Long): DoseState {
            val d = (m.nextDoseMillis ?: Long.MAX_VALUE) - now
            return when {
                d <= 0 -> AHORA
                d <= 10 * 60_000L -> PRONTO
                else -> LEJOS
            }
        }
    }
}
