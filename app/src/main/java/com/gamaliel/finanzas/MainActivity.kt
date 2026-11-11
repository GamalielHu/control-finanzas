package com.gamaliel.finanzas

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var paneles: List<View>
    private var tabActual = 0

    // Movimientos
    private val mesVisto: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private val adaptador = MovimientoAdapter()

    // Colores
    private val cIngreso by lazy { ContextCompat.getColor(this, R.color.ingreso) }
    private val cGasto by lazy { ContextCompat.getColor(this, R.color.gasto) }
    private val cTexto by lazy { ContextCompat.getColor(this, R.color.texto) }
    private val cCerpi by lazy { ContextCompat.getColor(this, R.color.cerpi) }
    private val cCementerio by lazy { ContextCompat.getColor(this, R.color.cementerio) }
    private val cHeladeria by lazy { ContextCompat.getColor(this, R.color.heladeria) }
    private val cPersonal by lazy { ContextCompat.getColor(this, R.color.personal) }
    private val cPrimario by lazy { ContextCompat.getColor(this, R.color.primario) }

    private val prefs by lazy { getSharedPreferences("ajustes", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = DatabaseHelper.get(this)
        supportActionBar?.elevation = 0f

        paneles = listOf(
            findViewById(R.id.panelMovimientos),
            findViewById(R.id.panelResumen),
            findViewById(R.id.panelTemporadas)
        )

        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.addTab(tabs.newTab().setText(R.string.tab_movimientos))
        tabs.addTab(tabs.newTab().setText(R.string.tab_resumen))
        tabs.addTab(tabs.newTab().setText(R.string.tab_temporadas))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = mostrarPanel(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        prepararMovimientos()
        prepararResumen()
        prepararTemporadas()

        tabActual = savedInstanceState?.getInt("tab", 0) ?: 0
        tabs.getTabAt(tabActual)?.select()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", tabActual)
    }

    override fun onResume() {
        super.onResume()
        mostrarPanel(tabActual)
    }

    private fun mostrarPanel(i: Int) {
        tabActual = i
        paneles.forEachIndexed { k, v -> v.visibility = if (k == i) View.VISIBLE else View.GONE }
        when (i) {
            0 -> refrescarMovimientos()
            1 -> refrescarResumen()
            2 -> refrescarTemporadas()
        }
    }

    // ------------------------------------------------------------------ Movimientos

    private fun prepararMovimientos() {
        val lista = findViewById<ListView>(R.id.lista)
        lista.adapter = adaptador
        lista.setOnItemClickListener { _, _, pos, _ ->
            val m = adaptador.getItem(pos)
            val destino = if (m.tipo == DatabaseHelper.INGRESO) IngresoActivity::class.java else GastoActivity::class.java
            startActivity(Intent(this, destino).putExtra("id", m.id))
        }
        findViewById<Button>(R.id.btnMesAnterior).setOnClickListener {
            mesVisto.add(Calendar.MONTH, -1); refrescarMovimientos()
        }
        findViewById<Button>(R.id.btnMesSiguiente).setOnClickListener {
            mesVisto.add(Calendar.MONTH, 1); refrescarMovimientos()
        }
        findViewById<Button>(R.id.btnNuevoIngreso).setOnClickListener {
            startActivity(Intent(this, IngresoActivity::class.java))
        }
        findViewById<Button>(R.id.btnNuevoGasto).setOnClickListener {
            startActivity(Intent(this, GastoActivity::class.java))
        }
    }

    private fun refrescarMovimientos() {
        val (desde, hasta) = rangoMes(mesVisto)
        findViewById<TextView>(R.id.txtMes).text =
            "${Formato.MESES_LARGOS[mesVisto.get(Calendar.MONTH)]} ${mesVisto.get(Calendar.YEAR)}"
        val movs = db.listar(desde, hasta)
        adaptador.datos = movs
        adaptador.notifyDataSetChanged()
        findViewById<TextView>(R.id.txtVacio).visibility = if (movs.isEmpty()) View.VISIBLE else View.GONE

        val ing = movs.filter { it.tipo == DatabaseHelper.INGRESO }.sumOf { it.monto }
        val gas = movs.filter { it.tipo == DatabaseHelper.GASTO }.sumOf { it.monto }
        findViewById<TextView>(R.id.txtMesIngresos).text = "Ingresos\n${Formato.bs(ing)}"
        findViewById<TextView>(R.id.txtMesGastos).text = "Gastos\n${Formato.bs(gas)}"
        findViewById<TextView>(R.id.txtMesNeto).apply {
            text = "Neto\n${Formato.bs(ing - gas)}"
            setTextColor(if (ing - gas < 0) cGasto else cTexto)
        }
    }

    private inner class MovimientoAdapter : BaseAdapter() {
        var datos: List<Movimiento> = emptyList()
        override fun getCount() = datos.size
        override fun getItem(position: Int) = datos[position]
        override fun getItemId(position: Int) = datos[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_movimiento, parent, false)
            val m = datos[position]
            val titulo = v.findViewById<TextView>(R.id.txtTitulo)
            val detalle = v.findViewById<TextView>(R.id.txtDetalle)
            val monto = v.findViewById<TextView>(R.id.txtMonto)
            val marca = v.findViewById<View>(R.id.marca)
            val fecha = Formato.mostrar(m.fecha)
            if (m.tipo == DatabaseHelper.INGRESO) {
                titulo.text = "${m.lugar} · ${m.turno}"
                detalle.text = if (m.nota.isBlank()) fecha else "$fecha · ${m.nota}"
                monto.text = "+ ${Formato.bs(m.monto)}"
                monto.setTextColor(cIngreso)
                marca.setBackgroundColor(if (m.lugar == DatabaseHelper.CEMENTERIO) cCementerio else cCerpi)
            } else {
                val personal = m.ambito == DatabaseHelper.PERSONAL
                titulo.text = if (personal) "Gasto personal" else "Gasto heladería"
                detalle.text = "$fecha · ${m.nota}"
                monto.text = "− ${Formato.bs(m.monto)}"
                monto.setTextColor(cGasto)
                marca.setBackgroundColor(if (personal) cPersonal else cHeladeria)
            }
            return v
        }
    }

    // ------------------------------------------------------------------ Resumen

    private fun prepararResumen() {
        val grupo = findViewById<RadioGroup>(R.id.grupoPeriodo)
        grupo.check(prefs.getInt("periodo", R.id.perMes).let {
            if (it in listOf(R.id.perHoy, R.id.perSemana, R.id.perMes, R.id.perAnio, R.id.perTodo)) it else R.id.perMes
        })
        grupo.setOnCheckedChangeListener { _, id ->
            prefs.edit().putInt("periodo", id).apply(); refrescarResumen()
        }
        val sw = findViewById<SwitchMaterial>(R.id.swPersonales)
        sw.isChecked = prefs.getBoolean("restar_personales", true)
        sw.setOnCheckedChangeListener { _, activo ->
            prefs.edit().putBoolean("restar_personales", activo).apply(); refrescarResumen()
        }
    }

    private fun rangoResumen(): Pair<String, String> {
        val hoy = Calendar.getInstance()
        return when (findViewById<RadioGroup>(R.id.grupoPeriodo).checkedRadioButtonId) {
            R.id.perHoy -> Formato.iso(hoy) to Formato.iso(hoy)
            R.id.perSemana -> {
                val l = Formato.lunes(hoy)
                val dom = (l.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
                Formato.iso(l) to Formato.iso(dom)
            }
            R.id.perAnio -> "${hoy.get(Calendar.YEAR)}-01-01" to "${hoy.get(Calendar.YEAR)}-12-31"
            R.id.perTodo -> "0000-01-01" to "9999-12-31"
            else -> rangoMes(hoy)
        }
    }

    private fun refrescarResumen() {
        val (desde, hasta) = rangoResumen()
        findViewById<TextView>(R.id.txtRangoResumen).text =
            if (desde == "0000-01-01") "Todos los registros"
            else if (desde == hasta) Formato.mostrar(desde)
            else "Del ${Formato.mostrar(desde)} al ${Formato.mostrar(hasta)}"

        val I = DatabaseHelper.INGRESO
        val G = DatabaseHelper.GASTO
        val cerpi = db.suma(desde, hasta, I, lugar = DatabaseHelper.CERPI)
        val cementerio = db.suma(desde, hasta, I, lugar = DatabaseHelper.CEMENTERIO)
        val ingresos = cerpi + cementerio
        val gHeladeria = db.suma(desde, hasta, G, ambito = DatabaseHelper.HELADERIA)
        val gPersonal = db.suma(desde, hasta, G, ambito = DatabaseHelper.PERSONAL)
        val gananciaHeladeria = ingresos - gHeladeria
        val restar = findViewById<SwitchMaterial>(R.id.swPersonales).isChecked
        val real = gananciaHeladeria - (if (restar) gPersonal else 0.0)

        val cont = findViewById<LinearLayout>(R.id.contBalance)
        cont.removeAllViews()
        fila(cont, "Ingresos Cerpi", Formato.bs(cerpi), cCerpi)
        fila(cont, "Ingresos Cementerio", Formato.bs(cementerio), cCementerio)
        fila(cont, "Total ingresos", Formato.bs(ingresos), cIngreso, negrita = true)
        fila(cont, "Gastos de la heladería", "− " + Formato.bs(gHeladeria), cHeladeria)
        fila(cont, "Ganancia de la heladería", Formato.bs(gananciaHeladeria),
            if (gananciaHeladeria < 0) cGasto else cTexto, negrita = true)
        fila(cont, "Gastos personales", "− " + Formato.bs(gPersonal), cPersonal)

        findViewById<TextView>(R.id.txtGananciaReal).apply {
            text = Formato.bs(real)
            setTextColor(if (real < 0) cGasto else cIngreso)
        }
        findViewById<TextView>(R.id.txtGananciaNota).text =
            if (restar) "Ingresos − gastos heladería − gastos personales"
            else "Ingresos − gastos heladería (sin restar lo personal)"

        findViewById<PieChartView>(R.id.tortaGastos).setDatos(
            listOf("Heladería" to gHeladeria, "Personal" to gPersonal),
            intArrayOf(cHeladeria, cPersonal)
        )
        findViewById<PieChartView>(R.id.tortaLugares).setDatos(
            listOf("Cerpi" to cerpi, "Cementerio" to cementerio),
            intArrayOf(cCerpi, cCementerio)
        )

        val turnos = findViewById<LinearLayout>(R.id.contTurnos)
        turnos.removeAllViews()
        var mejor = "" to 0.0
        for (lugar in listOf(DatabaseHelper.CERPI, DatabaseHelper.CEMENTERIO)) {
            for (turno in listOf(DatabaseHelper.MANANA, DatabaseHelper.TARDE)) {
                val v = db.suma(desde, hasta, I, lugar = lugar, turno = turno)
                fila(turnos, "$lugar · $turno", Formato.bs(v),
                    if (lugar == DatabaseHelper.CERPI) cCerpi else cCementerio)
                if (v > mejor.second) mejor = "$lugar · $turno" to v
            }
        }
        val tm = db.suma(desde, hasta, I, turno = DatabaseHelper.MANANA)
        val tt = db.suma(desde, hasta, I, turno = DatabaseHelper.TARDE)
        fila(turnos, "Total Mañana", Formato.bs(tm), cTexto, negrita = true)
        fila(turnos, "Total Tarde", Formato.bs(tt), cTexto, negrita = true)
        if (mejor.second > 0) fila(turnos, "Mejor turno", mejor.first, cPrimario, negrita = true)
    }

    private fun fila(cont: LinearLayout, etiqueta: String, valor: String, color: Int, negrita: Boolean = false) {
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val estilo = if (negrita) Typeface.BOLD else Typeface.NORMAL
        f.addView(TextView(this).apply {
            text = etiqueta; textSize = 14f; setTextColor(cTexto); setTypeface(null, estilo)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        f.addView(TextView(this).apply {
            text = valor; textSize = 14f; setTextColor(color); setTypeface(null, estilo)
        })
        cont.addView(f)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ------------------------------------------------------------------ Temporadas

    private fun prepararTemporadas() {
        val grupo = findViewById<RadioGroup>(R.id.grupoTemporada)
        grupo.check(if (prefs.getBoolean("semanal", false)) R.id.temSemanal else R.id.temMensual)
        grupo.setOnCheckedChangeListener { _, id ->
            prefs.edit().putBoolean("semanal", id == R.id.temSemanal).apply(); refrescarTemporadas()
        }
    }

    private fun refrescarTemporadas() {
        val semanal = findViewById<RadioGroup>(R.id.grupoTemporada).checkedRadioButtonId == R.id.temSemanal
        val claves = ArrayList<String>()
        val etiquetas = ArrayList<String>()
        val nombresLargos = ArrayList<String>()
        val desde: String
        val hasta: String
        val agrupar: String

        if (semanal) {
            agrupar = DatabaseHelper.POR_SEMANA
            val c = Formato.lunes(Calendar.getInstance())
            hasta = Formato.iso((c.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) })
            c.add(Calendar.DAY_OF_MONTH, -7 * 11)
            desde = Formato.iso(c)
            repeat(12) {
                claves.add(Formato.iso(c))
                val d = c.get(Calendar.DAY_OF_MONTH)
                val m = c.get(Calendar.MONTH) + 1
                etiquetas.add(String.format(java.util.Locale.US, "%02d/%02d", d, m))
                nombresLargos.add("semana del ${Formato.mostrar(Formato.iso(c))}")
                c.add(Calendar.DAY_OF_MONTH, 7)
            }
        } else {
            agrupar = DatabaseHelper.POR_MES
            val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
            hasta = rangoMes(c).second
            c.add(Calendar.MONTH, -11)
            desde = Formato.iso(c)
            repeat(12) {
                val m = c.get(Calendar.MONTH)
                claves.add(Formato.iso(c).substring(0, 7))
                etiquetas.add(Formato.MESES[m])
                nombresLargos.add("${Formato.MESES_LARGOS[m]} ${c.get(Calendar.YEAR)}")
                c.add(Calendar.MONTH, 1)
            }
        }

        val ingresos = db.ingresosAgrupados(agrupar, desde, hasta)
        val gastos = db.gastosHeladeriaAgrupados(agrupar, desde, hasta)
        val sCerpi = DoubleArray(12) { ingresos[claves[it]]?.get(0) ?: 0.0 }
        val sCementerio = DoubleArray(12) { ingresos[claves[it]]?.get(1) ?: 0.0 }
        val sGanancia = DoubleArray(12) { sCerpi[it] + sCementerio[it] - (gastos[claves[it]] ?: 0.0) }

        findViewById<BarChartView>(R.id.barras).setDatos(
            etiquetas, listOf(sCerpi, sCementerio), listOf("Cerpi", "Cementerio"),
            intArrayOf(cCerpi, cCementerio)
        )
        findViewById<BarChartView>(R.id.barrasGanancia).setDatos(
            etiquetas, listOf(sGanancia), listOf("Ingresos − gastos heladería"),
            intArrayOf(cPrimario)
        )

        // Análisis de picos
        val unidad = if (semanal) "semana" else "mes"
        val sb = StringBuilder()
        fun pico(nombre: String, serie: DoubleArray) {
            val i = serie.indices.maxByOrNull { serie[it] } ?: return
            if (serie[i] <= 0) {
                sb.append("• $nombre: sin ventas registradas.\n"); return
            }
            val conDatos = serie.filter { it > 0 }
            val prom = conDatos.average()
            sb.append("• $nombre: mejor $unidad ${nombresLargos[i]} con ${Formato.bs(serie[i])}")
            sb.append(" (promedio ${Formato.bs(prom)} por $unidad con ventas).\n")
        }
        pico("Cerpi", sCerpi)
        pico("Cementerio", sCementerio)
        pico("Total heladería", DoubleArray(12) { sCerpi[it] + sCementerio[it] })

        val ultimo = sCerpi[11] + sCementerio[11]
        val anterior = sCerpi[10] + sCementerio[10]
        if (anterior > 0) {
            val cambio = (ultimo - anterior) / anterior * 100
            sb.append(String.format(java.util.Locale.US,
                "• Este %s va %s%.0f%% frente al anterior.\n", unidad, if (cambio >= 0) "+" else "", cambio))
        }

        val dias = db.ingresosPorDiaSemana(desde, hasta)
        val mejorDia = dias.indices.maxByOrNull { dias[it] } ?: 0
        if (dias[mejorDia] > 0) {
            sb.append("• Día que más se vende: ${Formato.DIAS[mejorDia]} (promedio ${Formato.bs(dias[mejorDia])}).\n")
        }
        val tm = db.suma(desde, hasta, DatabaseHelper.INGRESO, turno = DatabaseHelper.MANANA)
        val tt = db.suma(desde, hasta, DatabaseHelper.INGRESO, turno = DatabaseHelper.TARDE)
        if (tm + tt > 0) {
            sb.append("• Turno más fuerte: ${if (tm >= tt) "Mañana" else "Tarde"} ")
            sb.append("(Mañana ${Formato.bs(tm)} · Tarde ${Formato.bs(tt)}).")
        }
        findViewById<TextView>(R.id.txtAnalisis).text =
            if (sb.isEmpty()) "Registra ingresos para ver las temporadas." else sb.toString().trimEnd()
    }

    // ------------------------------------------------------------------ Utilidades

    private fun rangoMes(c: Calendar): Pair<String, String> {
        val ini = (c.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val fin = (c.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
        return Formato.iso(ini) to Formato.iso(fin)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.principal, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.exportar) {
            exportar(); return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** Comparte todos los registros como CSV (para respaldo o Excel). */
    private fun exportar() {
        val sb = StringBuilder("fecha;tipo;categoria;lugar;turno;monto_bs;nota\n")
        for (m in db.listarTodo().reversed()) {
            val nota = m.nota.replace(";", ",").replace("\n", " ")
            val cat = if (m.ambito == DatabaseHelper.PERSONAL) "Personal" else "Heladería"
            sb.append("${m.fecha};${m.tipo};$cat;${m.lugar ?: ""};${m.turno ?: ""};")
            sb.append(m.monto.toString().replace('.', ',')).append(';').append(nota).append('\n')
        }
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Control Finanzas - ${Formato.hoy()}")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(envio, "Exportar registros"))
    }
}
