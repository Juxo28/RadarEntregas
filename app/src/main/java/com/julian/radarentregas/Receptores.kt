package com.julian.radarentregas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

/** Se despierta cuando suena una alarma programada (o cuando tocas "Detener alarma"). */
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACCION_ALARMA = "com.julian.radarentregas.ALARMA"
        const val ACCION_DETENER = "com.julian.radarentregas.DETENER"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            ACCION_DETENER -> Notificaciones.cancelar(ctx, intent.getIntExtra(Alarmas.EXTRA_NOTIF, 0))
            else -> {
                val fecha = intent.getStringExtra(Alarmas.EXTRA_FECHA) ?: return
                val tipo = intent.getIntExtra(Alarmas.EXTRA_TIPO, Alarmas.TIPO_NORMAL)
                Notificaciones.mostrar(ctx, LocalDate.parse(fecha), tipo)
            }
        }
    }
}

/** Android borra las alarmas al reiniciar o cambiar la hora: aquí se vuelven a programar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Notificaciones.crearCanales(ctx)
        Alarmas.reprogramar(ctx)
    }
}
