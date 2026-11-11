package com.gamaliel.finanzas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Barras agrupadas dibujadas con Canvas. Cada serie es un arreglo de valores
 * (uno por etiqueta). Admite valores negativos (pérdidas).
 */
class BarChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val d = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity
    private var etiquetas: List<String> = emptyList()
    private var series: List<DoubleArray> = emptyList()
    private var nombres: List<String> = emptyList()
    private var colores: IntArray = intArrayOf()

    private val pBarra = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pEje = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); strokeWidth = d
    }
    private val pCero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E"); strokeWidth = d
    }
    private val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575"); textSize = 10 * sp
    }
    private val pLeyenda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121"); textSize = 12 * sp
    }

    fun setDatos(etqs: List<String>, ser: List<DoubleArray>, noms: List<String>, cols: IntArray) {
        etiquetas = etqs
        series = ser
        nombres = noms
        colores = cols
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (etiquetas.isEmpty() || series.isEmpty()) return

        // Leyenda arriba
        var lx = 4 * d
        val ly = 14 * sp
        nombres.forEachIndexed { i, n ->
            pBarra.color = colores[i % colores.size]
            canvas.drawRect(lx, ly - 10 * d, lx + 10 * d, ly, pBarra)
            canvas.drawText(n, lx + 14 * d, ly, pLeyenda)
            lx += 14 * d + pLeyenda.measureText(n) + 18 * d
        }

        var maximo = 0.0
        var minimo = 0.0
        series.forEach { s -> s.forEach { maximo = max(maximo, it); minimo = min(minimo, it) } }
        if (maximo == 0.0 && minimo == 0.0) maximo = 1.0

        val izq = 38 * d
        val arriba = ly + 12 * d
        val abajo = height - 18 * d
        val ancho = width - izq - 4 * d
        val rango = maximo - minimo
        fun yDe(v: Double) = (abajo - (v - minimo) / rango * (abajo - arriba)).toFloat()

        // Rejilla con 4 divisiones
        pTexto.textAlign = Paint.Align.RIGHT
        for (k in 0..4) {
            val v = minimo + rango * k / 4
            val y = yDe(v)
            canvas.drawLine(izq, y, width.toFloat(), y, pEje)
            canvas.drawText(Formato.compacto(v), izq - 4 * d, y + 4 * d, pTexto)
        }
        val yCero = yDe(0.0)
        canvas.drawLine(izq, yCero, width.toFloat(), yCero, pCero)

        // Barras
        val grupo = ancho / etiquetas.size
        val anchoBarra = grupo * 0.8f / series.size
        pTexto.textAlign = Paint.Align.CENTER
        etiquetas.forEachIndexed { i, etq ->
            val x0 = izq + grupo * i + grupo * 0.1f
            series.forEachIndexed { s, valores ->
                val v = valores.getOrElse(i) { 0.0 }
                if (v != 0.0) {
                    pBarra.color = colores[s % colores.size]
                    val y = yDe(v)
                    val l = x0 + anchoBarra * s
                    canvas.drawRect(l, min(y, yCero), l + anchoBarra - d, max(y, yCero), pBarra)
                }
            }
            canvas.drawText(etq, izq + grupo * i + grupo / 2, height - 4 * d, pTexto)
        }
        pTexto.textAlign = Paint.Align.LEFT
    }
}
