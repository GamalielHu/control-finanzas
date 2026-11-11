package com.gamaliel.finanzas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.min

/** Gráfico de torta (dona) dibujado con Canvas, con leyenda a la derecha. */
class PieChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val densidad = resources.displayMetrics.density
    private val escala = resources.displayMetrics.scaledDensity
    private var datos: List<Pair<String, Double>> = emptyList()
    private var colores: IntArray = intArrayOf()

    private val pRelleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121"); textSize = 14 * escala
    }
    private val pSuave = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575"); textSize = 12 * escala
    }
    private val pCentro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val rect = RectF()

    fun setDatos(valores: List<Pair<String, Double>>, cols: IntArray) {
        datos = valores
        colores = cols
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = datos.sumOf { it.second }
        if (total <= 0.0) {
            pSuave.textAlign = Paint.Align.CENTER
            canvas.drawText("Sin datos en este periodo", width / 2f, height / 2f, pSuave)
            pSuave.textAlign = Paint.Align.LEFT
            return
        }
        val diam = min(height.toFloat() - 8 * densidad, width * 0.45f)
        val izq = 4 * densidad
        val arriba = (height - diam) / 2f
        rect.set(izq, arriba, izq + diam, arriba + diam)

        var inicio = -90f
        datos.forEachIndexed { i, (_, v) ->
            val barrido = (v / total * 360.0).toFloat()
            pRelleno.color = colores[i % colores.size]
            canvas.drawArc(rect, inicio, barrido, true, pRelleno)
            inicio += barrido
        }
        canvas.drawCircle(rect.centerX(), rect.centerY(), diam * 0.28f, pCentro)

        // Leyenda
        val x = izq + diam + 16 * densidad
        val alto = 46 * densidad
        var y = (height - alto * datos.size) / 2f + 16 * escala
        datos.forEachIndexed { i, (nombre, v) ->
            pRelleno.color = colores[i % colores.size]
            canvas.drawRect(x, y - 12 * densidad, x + 12 * densidad, y, pRelleno)
            val pct = String.format(Locale.US, "%.0f%%", v / total * 100)
            canvas.drawText("$nombre  $pct", x + 18 * densidad, y, pTexto)
            canvas.drawText(Formato.bs(v), x + 18 * densidad, y + 18 * escala, pSuave)
            y += alto
        }
    }
}
