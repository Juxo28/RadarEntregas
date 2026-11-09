package com.julian.radarentregas

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import java.time.LocalDate

object Notificaciones {
    // Si cambias el sonido o la vibración, sube la versión: Android no deja editar un canal ya creado.
    const val CANAL_URGENTE = "urgente_v1"
    const val CANAL_NORMAL = "recordatorio_v1"

    fun crearCanales(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)

        val sonidoAlarma = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val urgente = NotificationChannel(
            CANAL_URGENTE, "Entregas urgentes (faltan 3 días)", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Suena como alarma, se repite hasta que la detengas y aparece a pantalla completa."
            setSound(
                sonidoAlarma,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // usa el volumen de alarma
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 900, 400, 900, 400, 900)
            enableLights(true)
            lightColor = 0xFFD12C22.toInt()
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val normal = NotificationChannel(
            CANAL_NORMAL, "Recordatorios (faltan 7 días)", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Aviso con sonido una semana antes del cierre."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(listOf(urgente, normal))
    }

    /** Muestra el aviso de las entregas pendientes que cierran en [fecha]. */
    fun mostrar(ctx: Context, fecha: LocalDate, tipo: Int, prueba: Boolean = false) {
        val pendientes = Repo.cargar(ctx)
            .filter { !it.hecha && it.fecha == fecha }
            .sortedByDescending { it.puntos ?: 0 }
        if (pendientes.isEmpty()) return

        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        crearCanales(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val urgente = tipo == Alarmas.TIPO_URGENTE
        val id = Alarmas.codigo(fecha, tipo)
        val dias = Fechas.diasHasta(fecha)
        val cuantos = if (pendientes.size == 1) "1 entrega cierra" else "${pendientes.size} entregas cierran"
        val faltan = when {
            dias <= 0 -> "cierra HOY"
            dias == 1 -> "falta 1 día"
            else -> "faltan $dias días"
        }
        val titulo = if (urgente) "🚨 URGENTE: $faltan" else "📅 Recordatorio: $faltan"
        val texto = "$cuantos el ${Fechas.larga(fecha)}"
        val lista = pendientes.joinToString("\n") { "• ${it.curso}: ${it.nombre}" }

        val abrirApp = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val b = Notification.Builder(ctx, if (urgente) CANAL_URGENTE else CANAL_NORMAL)
            .setSmallIcon(R.drawable.ic_stat_radar)
            .setColor(if (urgente) 0xFFD12C22.toInt() else 0xFF2C47D6.toInt())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(Notification.BigTextStyle().bigText("$texto\n\n$lista"))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(if (urgente) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)

        var pantallaCompleta: Intent? = null
        if (urgente) {
            pantallaCompleta = Intent(ctx, AlarmaActivity::class.java)
                .putExtra(Alarmas.EXTRA_FECHA, fecha.toString())
                .putExtra(Alarmas.EXTRA_NOTIF, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(
                ctx, id, pantallaCompleta,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            b.setFullScreenIntent(pi, true)
            b.setContentIntent(pi)

            val detener = PendingIntent.getBroadcast(
                ctx, id + 1_000_000,
                Intent(ctx, AlarmReceiver::class.java)
                    .setAction(AlarmReceiver.ACCION_DETENER)
                    .putExtra(Alarmas.EXTRA_NOTIF, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            b.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(ctx, R.drawable.ic_stat_radar), "Detener alarma", detener
                ).build()
            )
        }

        val notificacion = b.build()
        if (urgente) {
            // FLAG_INSISTENT: el sonido se repite hasta que la persona toque la notificación o la detenga.
            notificacion.flags = notificacion.flags or Notification.FLAG_INSISTENT
        }
        nm.notify(id, notificacion)

        // En la prueba desde la app, abre también la pantalla de alarma directamente.
        if (prueba && pantallaCompleta != null) ctx.startActivity(pantallaCompleta)
    }

    fun cancelar(ctx: Context, id: Int) {
        ctx.getSystemService(NotificationManager::class.java).cancel(id)
    }
}
