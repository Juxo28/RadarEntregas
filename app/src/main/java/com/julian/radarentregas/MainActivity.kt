@file:OptIn(ExperimentalMaterial3Api::class)

package com.julian.radarentregas

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    /** Cambia cada vez que vuelves a la app, para refrescar la cuenta regresiva y los permisos. */
    private val reanudado = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notificaciones.crearCanales(this)
        Alarmas.reprogramar(this)
        setContent { RadarTheme { PantallaPrincipal(reanudado.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        reanudado.intValue++
    }
}

enum class Vista(val etiqueta: String) {
    PENDIENTES("Pendientes"), ENTREGADAS("Entregadas"), TODAS("Todas")
}

@Composable
fun PantallaPrincipal(reanudado: Int) {
    val ctx = LocalContext.current
    var tareas by remember { mutableStateOf(Repo.cargar(ctx)) }
    var vista by rememberSaveable { mutableStateOf(Vista.PENDIENTES) }
    var editando by remember { mutableStateOf<Tarea?>(null) }
    var creando by remember { mutableStateOf(false) }
    val hoy = remember(reanudado) { Fechas.hoy() }

    fun guardar(nueva: List<Tarea>) {
        tareas = nueva
        Repo.guardar(ctx, nueva)
        Alarmas.reprogramar(ctx, nueva)
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val visibles = tareas
        .filter {
            when (vista) {
                Vista.PENDIENTES -> !it.hecha
                Vista.ENTREGADAS -> it.hecha
                Vista.TODAS -> true
            }
        }
        .sortedWith(compareBy<Tarea>({ it.fecha }, { it.curso }))
    val grupos = visibles.groupBy { it.fecha }.toList()
        .let { if (vista == Vista.ENTREGADAS) it.reversed() else it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Radar de Entregas", fontWeight = FontWeight.Bold)
                        Text(
                            "Hoy · " + Fechas.larga(hoy),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = { TextButton(onClick = { probarAlarma(ctx, tareas) }) { Text("Probar alarma") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creando = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva entrega") },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp,
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "permisos") { AvisosPermisos(reanudado) }
            item(key = "resumen") { Resumen(tareas) }
            item(key = "filtros") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Vista.entries.forEach { v ->
                        FilterChip(selected = vista == v, onClick = { vista = v }, label = { Text(v.etiqueta) })
                    }
                }
            }
            if (grupos.isEmpty()) {
                item(key = "vacio") {
                    Text(
                        when {
                            vista == Vista.ENTREGADAS -> "Todavía no has marcado entregas como hechas."
                            tareas.isEmpty() -> "Aún no hay entregas. Toca “Nueva entrega” para agregar la primera."
                            else -> "No tienes entregas pendientes. 🎉"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            grupos.forEach { (fecha, lista) ->
                item(key = "f$fecha") { CabeceraFecha(fecha, lista) }
                items(lista, key = { it.id }) { t ->
                    FilaTarea(
                        t,
                        onMarcar = { guardar(tareas.map { x -> if (x.id == t.id) x.copy(hecha = !x.hecha) else x }) },
                        onEditar = { editando = t },
                    )
                }
            }
        }
    }

    if (creando || editando != null) {
        val actual = editando
        DialogoTarea(
            inicial = actual,
            cursos = tareas.map { it.curso }.distinct().sorted(),
            onGuardar = { t ->
                guardar(if (actual != null) tareas.map { x -> if (x.id == t.id) t else x } else tareas + t)
                creando = false
                editando = null
            },
            onEliminar = if (actual != null) {
                {
                    guardar(tareas.filter { x -> x.id != actual.id })
                    editando = null
                }
            } else null,
            onCerrar = {
                creando = false
                editando = null
            },
        )
    }
}

fun probarAlarma(ctx: Context, tareas: List<Tarea>) {
    val nm = ctx.getSystemService(NotificationManager::class.java)
    if (!nm.areNotificationsEnabled()) {
        Toast.makeText(ctx, "Activa las notificaciones de la app para oír la alarma.", Toast.LENGTH_LONG).show()
        return
    }
    val fecha = tareas.filter { !it.hecha }.minByOrNull { it.fecha }?.fecha
    if (fecha == null) {
        Toast.makeText(ctx, "No hay entregas pendientes para probar la alarma.", Toast.LENGTH_LONG).show()
        return
    }
    Notificaciones.mostrar(ctx, fecha, Alarmas.TIPO_URGENTE, prueba = true)
}

/** Tarjetas que aparecen solo si falta un permiso necesario para que suene la alarma. */
@Composable
fun AvisosPermisos(reanudado: Int) {
    val ctx = LocalContext.current
    val faltan = remember(reanudado) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val paquete = Uri.parse("package:" + ctx.packageName)
        buildList {
            if (!nm.areNotificationsEnabled()) {
                add(
                    "Activa las notificaciones para recibir las alertas." to
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                add(
                    "Permite las alarmas exactas para que el aviso suene a la hora justa." to
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, paquete)
                )
            }
            if (Build.VERSION.SDK_INT >= 34 && !nm.canUseFullScreenIntent()) {
                add(
                    "Permite la alerta a pantalla completa para que las urgentes se vean aunque el celular esté bloqueado." to
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, paquete)
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        faltan.forEach { (texto, intent) ->
            Card(colors = CardDefaults.cardColors(containerColor = Colores.Urgente.copy(alpha = 0.12f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(texto, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        try {
                            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("Abrir ajustes") }
                }
            }
        }
    }
}

@Composable
fun Resumen(tareas: List<Tarea>) {
    val pendientes = tareas.filter { !it.hecha }
    val urgentes = pendientes.count { Fechas.diasHasta(it.fecha) in 0..3 }
    val semana = pendientes.count { Fechas.diasHasta(it.fecha) in 4..7 }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dato("Urgentes", urgentes, Colores.Urgente, Modifier.weight(1f))
        Dato("Semana", semana, Colores.Semana, Modifier.weight(1f))
        Dato("Pendientes", pendientes.size, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        Dato("Hechas", tareas.size - pendientes.size, Colores.Hecha, Modifier.weight(1f))
    }
}

@Composable
fun Dato(etiqueta: String, valor: Int, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                valor.toString(), color = color, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            )
            Text(
                etiqueta, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            )
        }
    }
}

@Composable
fun CabeceraFecha(fecha: LocalDate, lista: List<Tarea>) {
    val d = Fechas.diasHasta(fecha)
    val nivel = nivelPara(d, lista.all { it.hecha })
    val n = abs(d)
    val unidad = when {
        d == 0 -> "cierra hoy"
        d < 0 -> if (n == 1) "día vencida" else "días vencida"
        else -> if (n == 1) "día" else "días"
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            n.toString(), color = nivel.color, fontSize = 40.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        )
        Column(Modifier.weight(1f)) {
            Text(
                unidad.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp,
            )
            Text(Fechas.larga(fecha), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Surface(shape = RoundedCornerShape(50), color = nivel.color.copy(alpha = 0.14f)) {
            Text(
                nivel.etiqueta.uppercase(), color = nivel.color,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun FilaTarea(t: Tarea, onMarcar: () -> Unit, onEditar: () -> Unit) {
    Card(
        onClick = onEditar,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = t.hecha,
                onCheckedChange = { onMarcar() },
                colors = CheckboxDefaults.colors(checkedColor = Colores.Hecha),
            )
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                val curso = if (t.codigo.isNotBlank()) "${t.curso} · ${t.codigo}" else t.curso
                Text(
                    curso.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp,
                )
                Text(
                    t.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (t.hecha) TextDecoration.LineThrough else null,
                    color = if (t.hecha) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                val meta = listOfNotNull(t.puntos?.let { "$it pts" }, t.tipo.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DialogoTarea(
    inicial: Tarea?,
    cursos: List<String>,
    onGuardar: (Tarea) -> Unit,
    onEliminar: (() -> Unit)?,
    onCerrar: () -> Unit,
) {
    var curso by remember { mutableStateOf(inicial?.curso ?: "") }
    var codigo by remember { mutableStateOf(inicial?.codigo ?: "") }
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var puntos by remember { mutableStateOf(inicial?.puntos?.toString() ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: "Individual") }
    var fecha by remember { mutableStateOf(inicial?.fecha) }
    var verFecha by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nueva entrega" else "Editar entrega") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = curso, onValueChange = { curso = it },
                    label = { Text("Curso") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                val sugerencias = cursos.filter { curso.isNotBlank() && it.contains(curso, ignoreCase = true) && it != curso }.take(4)
                if (sugerencias.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sugerencias.forEach { s -> SuggestionChip(onClick = { curso = s }, label = { Text(s) }) }
                    }
                }
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Actividad") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = codigo, onValueChange = { codigo = it.filter(Char::isDigit) },
                    label = { Text("Código del curso") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { verFecha = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(fecha?.let { "Cierra: " + Fechas.larga(it) } ?: "Elegir fecha de cierre")
                }
                OutlinedTextField(
                    value = puntos, onValueChange = { puntos = it.filter(Char::isDigit) },
                    label = { Text("Puntos") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Individual", "Colaborativa", "Laboratorio").forEach { op ->
                        FilterChip(selected = tipo == op, onClick = { tipo = op }, label = { Text(op) })
                    }
                }
                aviso?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (onEliminar != null) {
                    if (!confirmarBorrado) {
                        TextButton(onClick = { confirmarBorrado = true }) {
                            Text("Eliminar entrega", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("¿Eliminarla?", Modifier.weight(1f))
                            TextButton(onClick = onEliminar) { Text("Sí, eliminar", color = MaterialTheme.colorScheme.error) }
                            TextButton(onClick = { confirmarBorrado = false }) { Text("No") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val f = fecha
                if (curso.isBlank() || nombre.isBlank() || f == null) {
                    aviso = "Completa el curso, la actividad y la fecha de cierre."
                } else {
                    val base = inicial ?: Tarea(curso = "", nombre = "", fecha = f)
                    onGuardar(
                        base.copy(
                            curso = curso.trim(), codigo = codigo.trim(), nombre = nombre.trim(),
                            fecha = f, puntos = puntos.toIntOrNull(), tipo = tipo,
                        )
                    )
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )

    if (verFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: Fechas.hoy()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { verFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    verFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { verFecha = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}
