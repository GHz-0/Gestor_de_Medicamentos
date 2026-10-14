package com.gestor.medicamentos.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val DateFmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale("es", "MX"))

fun fmt(ms: Long): String = DateFmt.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun fmtRemaining(ms: Long): String {
    val s = ms / 1000
    val d = s / 86_400
    return (if (d > 0) "${d}d " else "") + "%02d:%02d:%02d".format((s % 86_400) / 3600, (s % 3600) / 60, s % 60)
}

fun timeGreeting(now: Long): String {
    val h = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Buenos días"; h < 19 -> "Buenas tardes"; else -> "Buenas noches" }
}

fun randomHealthPhrase(): String {
    val phrases = listOf(
        "💊 Recuerda tomar tus medicamentos a la hora exacta.",
        "⏰ Tu salud es lo primero: mantén tus tomas al día.",
        "📦 Revisa el stock de tus medicinas para que no te falten.",
        "✅ Cada dosis tomada a tiempo ayuda a tu recuperación.",
        "📅 La constancia en tu tratamiento asegura mejores resultados.",
        "🔔 Tus recordatorios te ayudan a no olvidar ninguna toma.",
        "💙 Cuidar de tu tratamiento es cuidar de tu bienestar.",
        "💊 La puntualidad con tus medicinas es clave para tu salud.",
        "🩺 Sigue las indicaciones médicas con ayuda de tu gestor.",
        "📝 Registra tus medicamentos para llevar un control completo.",
        "🛡️ Un tratamiento ordenado protege tu salud cada día.",
        "🕒 Cumplir con tus horarios fortalece tu bienestar diario."
    )
    return phrases.random()
}

fun greeting(now: Long): String = timeGreeting(now)
