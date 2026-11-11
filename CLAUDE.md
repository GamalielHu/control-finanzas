# CLAUDE.md — Control Finanzas

App Android en Kotlin (vistas XML, sin Compose, sin Room: SQLite nativo con
`SQLiteOpenHelper`), paquete `com.gamaliel.finanzas`. Textos en español, moneda Bs.

## Qué hace
- Ingresos diarios de la heladería: lugar **Cerpi / Cementerio**, turno **Mañana / Tarde**.
- Gastos: **Heladería** (afecta a ambos lugares) o **Personal**.
- Resumen: ganancia de la heladería y "ganancia real" (interruptor para restar o no lo personal),
  tortas Canvas (Heladería vs Personal, Cerpi vs Cementerio), ingresos por turno.
- Temporadas: barras Canvas mensual (12 meses) / semanal (12 semanas) y picos de venta.
- Menú: exportar CSV por compartir.

## Estructura (`app/src/main/java/com/gamaliel/finanzas`)
- `DatabaseHelper`: tabla única `movimientos` (fecha ISO, tipo INGRESO/GASTO, ambito HELADERIA/PERSONAL, lugar, turno, monto, nota).
- `MainActivity`: pestañas Movimientos / Resumen / Temporadas (paneles en `activity_main.xml`).
- `IngresoActivity`, `GastoActivity`: alta/edición/eliminación (extra `id`).
- `PieChartView`, `BarChartView`: gráficos dibujados a mano. `Formato`: Bs, fechas.

## Compilar
`.github/workflows/android.yml` (ubuntu, JDK 17, AGP 8.9.1, Kotlin 2.0.21, Gradle 8.14.3): en cada push
publica Release `v1.N` con `ControlFinanzas.apk`. Firma fija `app/firma.jks` (clave `finanzas`)
para actualizar sin desinstalar. Si falla, los errores quedan en la rama `registro`.
