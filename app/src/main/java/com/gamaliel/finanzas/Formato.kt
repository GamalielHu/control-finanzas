package com.gamaliel.finanzas

import android.app.DatePickerDialog
import android.content.Context
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Formato {
    private val simbolos = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val dinero = DecimalFormat("#,##0.00", simbolos)
    private val corto = DecimalFormat("#,##0", simbolos)

    val MESES = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val MESES_LARGOS = arrayOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val DIAS = arrayOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

    fun bs(v: Double): String = "Bs " + dinero.format(v)

    /** Para ejes de gráficos: 1.250 -> "1,3k". */
    fun compacto(v: Double): String = when {
        v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000).replace('.', ',')
        v >= 1_000 -> String.format(Locale.US, "%.1fk", v / 1_000).replace('.', ',')
        else -> corto.format(v)
    }

    /** Acepta "12,50", "12.50" o "1.200,50". */
    fun leerMonto(texto: String): Double? {
        var t = texto.trim().replace(" ", "")
        if (t.isEmpty()) return null
        if (t.contains(',') && t.contains('.')) t = t.replace(".", "")
        return t.replace(',', '.').toDoubleOrNull()
    }

    private fun isoFmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun iso(c: Calendar): String = isoFmt().format(c.time)

    fun hoy(): String = iso(Calendar.getInstance())

    fun aCalendar(iso: String): Calendar = Calendar.getInstance().apply {
        time = isoFmt().parse(iso) ?: time
    }

    /** "2026-11-10" -> "Mar 10/11/2026". */
    fun mostrar(iso: String): String {
        val c = aCalendar(iso)
        val dia = DIAS[c.get(Calendar.DAY_OF_WEEK) - 1].substring(0, 3)
        return String.format(
            Locale.US, "%s %02d/%02d/%d", dia,
            c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)
        )
    }

    /** Lunes de la semana de [c] (a las 00:00). */
    fun lunes(c: Calendar): Calendar = (c.clone() as Calendar).apply {
        val dow = get(Calendar.DAY_OF_WEEK)               // 1=domingo
        val atras = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        add(Calendar.DAY_OF_MONTH, -atras)
    }

    fun elegirFecha(ctx: Context, actual: String, alElegir: (String) -> Unit) {
        val c = aCalendar(actual)
        DatePickerDialog(ctx, { _, y, m, d ->
            val n = Calendar.getInstance().apply { set(y, m, d) }
            alElegir(iso(n))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
}
