package com.julian.radarentregas

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/** Una entrega de la UNAD. */
data class Tarea(
    val id: String = UUID.randomUUID().toString(),
    val curso: String,
    val codigo: String = "",
    val nombre: String,
    val fecha: LocalDate,
    val puntos: Int? = null,
    val tipo: String = "Individual",
    val hecha: Boolean = false,
)

/** Guarda las entregas en el propio celular (no necesita internet). */
object Repo {
    const val PREFS = "radar"
    private const val KEY = "tareas"

    fun cargar(ctx: Context): List<Tarea> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null)
        if (raw == null) {
            // Primera vez: carga las entregas del semestre.
            val semilla = Semilla.tareas()
            guardar(ctx, semilla)
            return semilla
        }
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { desdeJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardar(ctx: Context, lista: List<Tarea>) {
        val arr = JSONArray()
        lista.forEach { arr.put(aJson(it)) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).commit()
    }

    private fun aJson(t: Tarea) = JSONObject().apply {
        put("id", t.id)
        put("curso", t.curso)
        put("codigo", t.codigo)
        put("nombre", t.nombre)
        put("fecha", t.fecha.toString())
        if (t.puntos != null) put("puntos", t.puntos)
        put("tipo", t.tipo)
        put("hecha", t.hecha)
    }

    private fun desdeJson(o: JSONObject) = Tarea(
        id = o.getString("id"),
        curso = o.optString("curso"),
        codigo = o.optString("codigo"),
        nombre = o.optString("nombre"),
        fecha = LocalDate.parse(o.getString("fecha")),
        puntos = if (o.has("puntos")) o.getInt("puntos") else null,
        tipo = o.optString("tipo", "Individual"),
        hecha = o.optBoolean("hecha", false),
    )
}

object Fechas {
    private val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO"))

    fun hoy(): LocalDate = LocalDate.now()

    /** Días calendario que faltan hasta [fecha] (negativo si ya pasó). */
    fun diasHasta(fecha: LocalDate): Int = ChronoUnit.DAYS.between(hoy(), fecha).toInt()

    fun larga(fecha: LocalDate): String =
        fecha.format(formato).replaceFirstChar { it.uppercase() }
}

object Colores {
    val Urgente = Color(0xFFD12C22)
    val Semana = Color(0xFFB86E00)
    val Vencida = Color(0xFF7B3A96)
    val Hecha = Color(0xFF1E7A46)
    val Tranquila = Color(0xFF5B6779)
}

enum class Nivel(val etiqueta: String, val color: Color) {
    URGENTE("Urgente", Colores.Urgente),
    SEMANA("Esta semana", Colores.Semana),
    TRANQUILA("Con tiempo", Colores.Tranquila),
    VENCIDA("Vencida", Colores.Vencida),
    HECHA("Entregada", Colores.Hecha),
}

fun nivelPara(dias: Int, todasHechas: Boolean): Nivel = when {
    todasHechas -> Nivel.HECHA
    dias < 0 -> Nivel.VENCIDA
    dias <= 3 -> Nivel.URGENTE
    dias <= 7 -> Nivel.SEMANA
    else -> Nivel.TRANQUILA
}
