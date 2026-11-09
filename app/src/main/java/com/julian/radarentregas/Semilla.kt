package com.julian.radarentregas

import java.time.LocalDate

/** Entregas cargadas la primera vez que se abre la app (tomadas de UNAgeNda). */
object Semilla {
    fun tareas(): List<Tarea> {
        val n15 = LocalDate.of(2026, 11, 15)
        val n22 = LocalDate.of(2026, 11, 22)
        return listOf(
            Tarea("bio-reto4", "Biología", "201101", "Reto 4 - Un camino hacia los ODS", n15, 75, "Individual"),
            Tarea("pl-reto3", "Programación Lineal", "100404", "Reto 3 - Decisiones inteligentes mediante analítica de datos", n22, 175, "Individual"),
            Tarea("ed-tarea4", "Ecuaciones Diferenciales", "100412", "Tarea 4 - Aplicación de la Transformada de Laplace a través del pensamiento computacional", n22, 100, "Individual"),
            Tarea("fe-tarea3", "Física Electrónica", "100414", "Tarea 3 - Optoelectrónica y fotónica aplicada", n22, 70, "Individual"),
            Tarea("fe-tarea4", "Física Electrónica", "100414", "Tarea 4 - Componente práctico - Práctica de Laboratorio", n22, 140, "Laboratorio"),
            Tarea("bio-reto2", "Biología", "201101", "Reto 2 - Componente práctico - Práctica de laboratorio", n22, 200, "Laboratorio"),
            Tarea("cc-tarea4", "Contabilidad y Costos", "212018", "Tarea 4 - Presentación de informe con la solución de los problemas costeo de productos", n22, 115, "Individual"),
            Tarea("pst-tarea3", "La Persona como Ser Trascendente", "80004", "Tarea 3 - Articulación del sujeto histórico, político y trascendente", n22, 175, "Individual"),
            Tarea("td-task4", "Teoría de las Decisiones", "212066", "Task 4 - Decision models under Markovian processes", n22, 110, "Individual"),
            Tarea("ss-fase3", "Prestación del Servicio Social Unadista", "700004", "Fase 3 - Acción y Evaluación: Servicio Social", n22, 200, "Individual"),
            Tarea("bd-fase4", "Bases de Datos", "202016896", "Fase 4 - Gestión de bases de datos NoSQL", n22, 120, "Colaborativa"),
            Tarea("pi2-fase4", "Proyecto de Ingeniería II", "202337121", "Fase 4 - Componente Práctico - Práctica Simulada", n22, 150, "Colaborativa"),
            Tarea("is-tarea4", "Inclusión Social", "40004", "Tarea 4 - Propuesta de inclusión social", n22, 115, "Colaborativa"),
            Tarea("fgi-esc4", "Fundamentos y Generalidades de Investigación", "150001", "Escenario 4 - Características del proceso investigativo", n22, 125, "Individual"),
            Tarea("ia2-task4", "Inglés A2", "900002", "Task 4 - Language explorations", n22, 85, "Individual"),
            Tarea("ia2-task5", "Inglés A2", "900002", "Task 5 - Food and Culture Exploration", n22, 90, "Individual"),
            Tarea("pa-fase4", "Pensamiento Administrativo", "112002", "Fase 4 - Aplicación enfoques de la administración", n22, 80, "Colaborativa"),
            Tarea("ec-tarea4", "Ética y Ciudadanía", "40002", "Tarea 4 - Análisis de espacios de formación ética y ciudadana", n22, 120, "Individual"),
            Tarea("ccom-tarea4", "Competencias Comunicativas", "40003", "Tarea 4 - El texto expositivo", n22, 115, "Individual"),
            Tarea("ia1-task4", "Inglés A1", "900001", "Task 4 - Second Practice Zone", n22, 100, "Individual"),
        )
    }
}
