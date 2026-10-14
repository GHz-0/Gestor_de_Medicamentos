package com.gestor.medicamentos.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.gestor.medicamentos.domain.Schedule
import com.gestor.medicamentos.notifications.AlarmScheduler

/** Única puerta de entrada a los datos y a la lógica de tomas (la usan la UI y las notificaciones). */
class MedRepository(private val ctx: Context) {
    private val db = AppDatabase.get(ctx)
    private val nm get() = ctx.getSystemService(NotificationManager::class.java)

    val profiles = db.profile().observe()
    val meds = db.meds().observeAll()
    val logs = db.logs().all()

    suspend fun saveProfile(p: Profile) = db.profile().upsert(p)
    suspend fun get(id: Long) = db.meds().get(id)

    /** MED-01 / MED-05: alta o edición y reprogramación de alarmas. */
    suspend fun save(m: Medication) {
        val now = System.currentTimeMillis()
        val next = Schedule.next(m, maxOf(m.startMillis - 1, now))
        val toSave = m.copy(status = if (next == null) Status.FINISHED else Status.ACTIVE, nextDoseMillis = next, deletedAt = null)
        val id = if (m.id == 0L) db.meds().insert(toSave) else { db.meds().update(toSave); m.id }
        if (toSave.lowStock()) {
            notifyLowStock(toSave)
        }
        if (next == null) AlarmScheduler.cancel(ctx, id) else AlarmScheduler.schedule(ctx, id, next)
    }

    /** Eliminación suave: se guarda en el historial y se puede restaurar. */
    suspend fun delete(id: Long) {
        val m = get(id) ?: return
        AlarmScheduler.cancel(ctx, id); nm.cancel(id.toInt())
        db.meds().update(m.copy(status = Status.DELETED, deletedAt = System.currentTimeMillis()))
    }

    suspend fun restore(id: Long) { get(id)?.let { save(it.copy(status = Status.ACTIVE)) } }

    /** MED-04: recarga de inventario. */
    suspend fun refill(id: Long, n: Int) {
        get(id)?.let {
            val updated = it.copy(stock = (it.stock ?: 0) + n)
            db.meds().update(updated)
        }
    }

    suspend fun take(id: Long) = resolve(id, DoseStatus.TOMADA)
    suspend fun skip(id: Long) = resolve(id, DoseStatus.OMITIDA)

    suspend fun snooze(id: Long) {
        val m = get(id) ?: return
        log(m, DoseStatus.POSPUESTA); nm.cancel(id.toInt())
        AlarmScheduler.schedule(ctx, id, System.currentTimeMillis() + 10 * 60_000L)
    }

    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        db.meds().active().forEach { m -> m.nextDoseMillis?.let { AlarmScheduler.schedule(ctx, m.id, maxOf(it, now + 5_000)) } }
    }

    /** Registra la toma/omisión, descuenta stock y avanza a la siguiente toma (MED-02, MED-03, MED-04). */
    private suspend fun resolve(id: Long, status: DoseStatus) {
        val m = get(id) ?: return
        val now = System.currentTimeMillis()
        log(m, status)
        var next = Schedule.next(m, m.nextDoseMillis ?: now)
        if (next != null && next <= now) next = Schedule.next(m, now)
        val newStock = if (status == DoseStatus.TOMADA) m.stock?.let { maxOf(it - 1, 0) } else m.stock
        val updatedMed = m.copy(stock = newStock, nextDoseMillis = next, status = if (next == null) Status.FINISHED else Status.ACTIVE)
        db.meds().update(updatedMed)
        nm.cancel(id.toInt())

        if (status == DoseStatus.TOMADA && updatedMed.lowStock()) {
            notifyLowStock(updatedMed)
        }

        if (next == null) AlarmScheduler.cancel(ctx, id) else AlarmScheduler.schedule(ctx, id, next)
    }

    private fun notifyLowStock(m: Medication) {
        val stockVal = m.stock ?: return
        val minVal = m.minStock ?: Medication.LOW_STOCK
        nm.createNotificationChannel(NotificationChannel("stock_channel", "Avisos de inventario", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(
            ctx, 0, ctx.packageManager.getLaunchIntentForPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            m.id.toInt() * 1000 + 99,
            NotificationCompat.Builder(ctx, "stock_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠ Quedan pocas unidades: ${m.name}")
                .setContentText("Te quedan $stockVal unidades (Mínimo aviso: $minVal). ¡Es hora de comprar más!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }

    private suspend fun log(m: Medication, s: DoseStatus) =
        db.logs().insert(DoseLog(medId = m.id, medName = m.name, dose = m.dose, status = s, timeMillis = System.currentTimeMillis()))
}
