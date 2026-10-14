package com.gestor.medicamentos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Alertas de medicamentos: Notifica a 1 hora, 30 min, 15 min y 5 min antes de la hora límite, además de la toma exacta. */
object AlarmScheduler {
    private val OFFSETS = listOf(
        0L to 0,            // Hora exacta de la toma
        60 * 60_000L to 1, // 1 hora antes
        30 * 60_000L to 2, // 30 min antes
        15 * 60_000L to 3, // 15 min antes
        5 * 60_000L to 4   // 5 min antes
    )

    fun schedule(ctx: Context, medId: Long, at: Long) {
        val now = System.currentTimeMillis()
        val am = ctx.getSystemService(AlarmManager::class.java)

        OFFSETS.forEach { (offset, subId) ->
            val alarmTime = at - offset
            if (alarmTime > now) {
                val pi = pending(ctx, medId, subId, offset)
                if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
                else
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
            }
        }
    }

    fun cancel(ctx: Context, medId: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        (0..4).forEach { subId ->
            am.cancel(pending(ctx, medId, subId, 0L))
        }
    }

    private fun pending(ctx: Context, medId: Long, subId: Int, offset: Long) = PendingIntent.getBroadcast(
        ctx, medId.toInt() * 100 + subId,
        Intent(ctx, DoseReceiver::class.java)
            .setAction(DoseReceiver.ACTION_ALARM)
            .putExtra(DoseReceiver.EXTRA_ID, medId)
            .putExtra(DoseReceiver.EXTRA_OFFSET, offset),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
