package com.julian.radarentregas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Pantalla roja a pantalla completa, como un despertador. El sonido sigue hasta tocar "Entendido". */
class AlarmaActivity : ComponentActivity() {
    private var notifId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val fecha = intent.getStringExtra(Alarmas.EXTRA_FECHA)?.let { LocalDate.parse(it) }
        if (fecha == null) {
            finish()
            return
        }
        notifId = intent.getIntExtra(Alarmas.EXTRA_NOTIF, 0)
        val pendientes = Repo.cargar(this)
            .filter { !it.hecha && it.fecha == fecha }
            .sortedByDescending { it.puntos ?: 0 }

        setContent {
            PantallaAlarma(
                fecha = fecha,
                pendientes = pendientes,
                onListo = { finish() },
                onAbrir = {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                },
            )
        }
    }

    override fun onDestroy() {
        // Al cerrar esta pantalla se quita la notificación y se detiene el sonido.
        if (isFinishing && notifId != 0) Notificaciones.cancelar(this, notifId)
        super.onDestroy()
    }
}

private val Rojo = Color(0xFF7A120C)

@Composable
fun PantallaAlarma(fecha: LocalDate, pendientes: List<Tarea>, onListo: () -> Unit, onAbrir: () -> Unit) {
    val d = Fechas.diasHasta(fecha)
    Surface(Modifier.fillMaxSize(), color = Rojo) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("🚨 URGENTE", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    d <= 0 -> "Cierra HOY"
                    d == 1 -> "Falta 1 día"
                    else -> "Faltan $d días"
                },
                color = Color.White, fontSize = 46.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold,
            )
            val cuantos = if (pendientes.size == 1) "1 entrega cierra" else "${pendientes.size} entregas cierran"
            Text("$cuantos el ${Fechas.larga(fecha)}", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
            pendientes.forEach { t ->
                Text(
                    "• ${t.curso}: ${t.nombre}" + (t.puntos?.let { " ($it pts)" } ?: ""),
                    color = Color.White, fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onListo,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Rojo),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) { Text("Entendido, detener alarma", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            OutlinedButton(
                onClick = onAbrir,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Abrir Radar de Entregas") }
        }
    }
}
