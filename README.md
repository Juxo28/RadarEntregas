# Radar de Entregas (Android)

App Android para no olvidar las entregas de la UNAD.

- **7 días antes, 7:00 a. m.:** recordatorio con sonido y vibración.
- **3 días antes, 7:00 a. m.:** ALARMA URGENTE. Suena con el tono de alarma (volumen de alarma), **se repite hasta que la detengas**, vibra fuerte y aparece **a pantalla completa** aunque el celular esté bloqueado.
- Las entregas del mismo día se agrupan en un solo aviso con la lista completa.
- Las entregas marcadas como hechas no suenan.
- Funciona sin internet. Los datos quedan guardados en el celular.
- Las alarmas se vuelven a programar solas si reinicias el celular.
- Trae precargadas tus 20 entregas del 15 y 22 de noviembre de 2026.

## Cómo instalarla (Android Studio)

1. Instala **Android Studio** (gratis): https://developer.android.com/studio
2. Abre Android Studio → **Open** → elige la carpeta `RadarEntregas` (la que contiene `settings.gradle.kts`).
3. Espera a que termine el "Gradle sync" (la primera vez descarga lo necesario; tarda unos minutos).
   Si te ofrece actualizar el plugin de Android (AGP), puedes aceptar.
4. En el celular activa las **Opciones de desarrollador** (Ajustes → Acerca del teléfono → toca 7 veces "Número de compilación") y luego **Depuración USB**.
5. Conecta el celular por USB, acepta el aviso en el celular y pulsa ▶ **Run** en Android Studio.

¿Prefieres un archivo APK para pasarlo al celular?
Menú **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. El archivo queda en
`app/build/outputs/apk/debug/app-debug.apk`. Cópialo al celular, ábrelo y permite "instalar apps desconocidas".

## Al abrirla la primera vez

1. Acepta el permiso de **notificaciones**.
2. Si aparece una tarjeta roja pidiendo un permiso (alarmas exactas o pantalla completa), toca **Abrir ajustes** y actívalo.
3. Toca **Probar alarma** (arriba a la derecha) para oír cómo suena una alerta urgente. Detenla con **Entendido, detener alarma**.

### Para que suene siempre

- Sube el **volumen de alarma** del celular.
- En Xiaomi, Samsung, Huawei, Oppo, etc.: Ajustes → Batería → busca *Radar de Entregas* → **Sin restricciones**. Si el fabricante tiene "Inicio automático", actívalo.

## Estructura del código

| Archivo | Qué hace |
|---|---|
| `MainActivity.kt` | Pantalla principal (lista, resumen, crear/editar entregas) |
| `AlarmaActivity.kt` | Pantalla roja de alarma a pantalla completa |
| `Alarmas.kt` | Programa las alarmas del sistema (7 y 3 días antes) |
| `Notificaciones.kt` | Canales de notificación, sonido de alarma y repetición |
| `Receptores.kt` | Recibe la alarma y reprograma al reiniciar el celular |
| `Tarea.kt` | Modelo de datos, guardado local y fechas |
| `Semilla.kt` | Entregas precargadas |

Para cambiar la hora del aviso, edita `HORA` y `MINUTO` en `Alarmas.kt`.

## Compilar sin Android Studio (GitHub)

El proyecto incluye `.github/workflows/compilar-apk.yml`: si lo subes a un repositorio de GitHub,
GitHub compila el APK solo. Lo descargas desde la pestaña **Actions** → la última ejecución → **Artifacts**.
