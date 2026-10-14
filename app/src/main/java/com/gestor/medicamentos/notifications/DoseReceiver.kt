package com.gestor.medicamentos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gestor.medicamentos.data.MedRepository
import com.gestor.medicamentos.data.Medication
import com.gestor.medicamentos.data.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** MED-02: alarma + botones Tomada / Pospuesta / Omitida. Repite el aviso cada 10 min hasta resolverse. */
class DoseReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        val offset = intent.getLongExtra(EXTRA_OFFSET, 0L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedRepository(ctx.applicationContext)
                when (intent.action) {
                    ACTION_ALARM -> alert(ctx, repo, id, offset)
                    ACTION_TAKEN -> repo.take(id)
                    ACTION_SKIP -> repo.skip(id)
                    ACTION_SNOOZE -> repo.snooze(id)
                }
            } finally { pending.finish() }
        }
    }

    private suspend fun alert(ctx: Context, repo: MedRepository, id: Long, offset: Long) {
        val m = repo.get(id)?.takeIf { it.status == Status.ACTIVE } ?: return
        show(ctx, m, offset)
        if (offset == 0L) {
            AlarmScheduler.schedule(ctx, id, System.currentTimeMillis() + REPEAT_MIN * 60_000L)
        }
    }

    private fun show(ctx: Context, m: Medication, offset: Long) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Recordatorios de medicamentos", NotificationManager.IMPORTANCE_HIGH))
        fun act(a: String, label: String, n: Int) = NotificationCompat.Action(
            0, label, PendingIntent.getBroadcast(
                ctx, m.id.toInt() * 100 + n,
                Intent(ctx, DoseReceiver::class.java).setAction(a).putExtra(EXTRA_ID, m.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        val open = PendingIntent.getActivity(
            ctx, 0, ctx.packageManager.getLaunchIntentForPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (offset == 0L) "Hora de tomar: ${m.name}" else "Recordatorio: ${m.name}"
        val mins = (offset / 60_000L).toInt()
        val text = if (offset == 0L) "Dosis: ${m.dose}" else "Faltan $mins min para tomar tu dosis (${m.dose})"

        nm.notify(
            m.id.toInt() * 10 + (offset / 60_000L).toInt(),
            NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(act(ACTION_TAKEN, "Tomada", 1))
                .addAction(act(ACTION_SNOOZE, "Pospuesta", 2))
                .addAction(act(ACTION_SKIP, "Omitida", 3))
                .build()
        )
    }

    companion object {
        const val EXTRA_ID = "medId"
        const val EXTRA_OFFSET = "offset"
        const val ACTION_ALARM = "gestor.ALARM"
        const val ACTION_TAKEN = "gestor.TAKEN"
        const val ACTION_SKIP = "gestor.SKIP"
        const val ACTION_SNOOZE = "gestor.SNOOZE"
        private const val CHANNEL = "dosis"
        private const val REPEAT_MIN = 10
    }
}
