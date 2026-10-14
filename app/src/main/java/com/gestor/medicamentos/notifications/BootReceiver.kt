package com.gestor.medicamentos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gestor.medicamentos.data.MedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reprograma las alarmas después de reiniciar el teléfono. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { MedRepository(ctx.applicationContext).rescheduleAll() } finally { pending.finish() }
        }
    }
}
