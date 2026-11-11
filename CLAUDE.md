# CLAUDE.md — Control Finanzas

App Android en Kotlin (vistas XML, sin Compose, sin Room: SQLite nativo con
`SQLiteOpenHelper`), paquete `com.gamaliel.finanzas`. Textos en español, moneda Bs.

## Qué hace
- `InicioActivity` (launcher): elegir **Trabajo** (heladería) o **Personal**, e interruptor
  **Vincular** (pref `vincular`, objeto `Modo`). MainActivity recibe el modo por extra y filtra todo por `ambito`.
- Vinculado: Resumen/Temporadas muestran el total heladería + personal en ambos modos; separado, cada uno lo suyo.
- Ingresos diarios de la heladería: lugar **Cerpi / Cementerio**, turno **Mañana / Tarde**.
- Gastos e ingresos personales (sin lugar ni turno). Gastos de la heladería afectan a ambos lugares.
- Resumen: ganancia/saldo, tortas Canvas (a dónde va el dinero, Cerpi vs Cementerio), ingresos por turno.
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
