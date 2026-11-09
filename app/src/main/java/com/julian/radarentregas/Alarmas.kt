package com.julian.radarentregas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

/**
 * Programa las alarmas del sistema.
 * Una alarma por FECHA de cierre (no por tarea), para que 19 entregas del mismo día
 * produzcan un solo aviso con la lista completa.
 *  - 7 días antes a las 7:00 a. m. -> recordatorio con sonido
 *  - 3 días antes a las 7:00 a. m. -> alarma URGENTE (sonido de alarma repetido + pantalla completa)
 */
object Alarmas {
    const val HORA = 7
    const val MINUTO = 0
    const val TIPO_NORMAL = 0
    const val TIPO_URGENTE = 1
    const val EXTRA_FECHA = "fecha"
    const val EXTRA_TIPO = "tipo"
    const val EXTRA_NOTIF = "notifId"
    private const val KEY_PROGRAMADAS = "programadas"

    /** Días de anticipación de cada aviso. */
    private val AVISOS = listOf(7L to TIPO_NORMAL, 3L to TIPO_URGENTE)

    fun codigo(fecha: LocalDate, tipo: Int): Int = (fecha.toEpochDay() * 2 + tipo).toInt()

    /** Cancela todo lo programado y vuelve a programar según las entregas pendientes. */
    fun reprogramar(ctx: Context, tareas: List<Tarea> = Repo.cargar(ctx)) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val prefs = ctx.getSharedPreferences(Repo.PREFS, Context.MODE_PRIVATE)

        prefs.getStringSet(KEY_PROGRAMADAS, emptySet())?.forEach { s ->
            val partes = s.split("|")
            if (partes.size == 2) {
                am.cancel(intentAlarma(ctx, LocalDate.parse(partes[0]), partes[1].toInt()))
            }
        }

        val nuevas = mutableSetOf<String>()
        val ahora = System.currentTimeMillis()
        tareas.filter { !it.hecha }.map { it.fecha }.distinct().forEach { fecha ->
            for ((dias, tipo) in AVISOS) {
                val cuando = fecha.minusDays(dias).atTime(HORA, MINUTO)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (cuando <= ahora) continue
                programarUna(ctx, am, cuando, intentAlarma(ctx, fecha, tipo))
                nuevas += "$fecha|$tipo"
            }
        }
        prefs.edit().putStringSet(KEY_PROGRAMADAS, nuevas).apply()
    }

    private fun intentAlarma(ctx: Context, fecha: LocalDate, tipo: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACCION_ALARMA)
            .putExtra(EXTRA_FECHA, fecha.toString())
            .putExtra(EXTRA_TIPO, tipo)
        return PendingIntent.getBroadcast(
            ctx, codigo(fecha, tipo), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun programarUna(ctx: Context, am: AlarmManager, cuando: Long, op: PendingIntent) {
        val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (puedeExacta) {
                // setAlarmClock es lo más fiable: Android no la retrasa por ahorro de batería.
                val abrir = PendingIntent.getActivity(
                    ctx, 0, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(cuando, abrir), op)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, op)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, op)
        }
    }
}
