package com.gamaliel.finanzas

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Un ingreso (siempre de la heladería) o un gasto (heladería o personal). */
data class Movimiento(
    val id: Long = 0,
    val fecha: String,          // yyyy-MM-dd
    val tipo: String,           // INGRESO / GASTO
    val ambito: String,         // HELADERIA / PERSONAL
    val lugar: String?,         // Cerpi / Cementerio (solo ingresos)
    val turno: String?,         // Mañana / Tarde (solo ingresos)
    val monto: Double,
    val nota: String
)

class DatabaseHelper private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx, "finanzas.db", null, 1) {

    companion object {
        const val INGRESO = "INGRESO"
        const val GASTO = "GASTO"
        const val HELADERIA = "HELADERIA"
        const val PERSONAL = "PERSONAL"
        const val CERPI = "Cerpi"
        const val CEMENTERIO = "Cementerio"
        const val MANANA = "Mañana"
        const val TARDE = "Tarde"

        /** Agrupaciones permitidas para las temporadas (texto fijo, nunca del usuario). */
        const val POR_MES = "substr(fecha,1,7)"
        const val POR_SEMANA = "date(fecha,'-6 days','weekday 1')"

        @Volatile
        private var instancia: DatabaseHelper? = null

        fun get(ctx: Context): DatabaseHelper =
            instancia ?: synchronized(this) {
                instancia ?: DatabaseHelper(ctx.applicationContext).also { instancia = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE movimientos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                tipo TEXT NOT NULL,
                ambito TEXT NOT NULL,
                lugar TEXT,
                turno TEXT,
                monto REAL NOT NULL,
                nota TEXT NOT NULL DEFAULT '')"""
        )
        db.execSQL("CREATE INDEX idx_mov_fecha ON movimientos(fecha)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión 1: sin migraciones todavía.
    }

    fun guardar(m: Movimiento): Long {
        val v = ContentValues().apply {
            put("fecha", m.fecha)
            put("tipo", m.tipo)
            put("ambito", m.ambito)
            put("lugar", m.lugar)
            put("turno", m.turno)
            put("monto", m.monto)
            put("nota", m.nota)
        }
        return if (m.id == 0L) {
            writableDatabase.insert("movimientos", null, v)
        } else {
            writableDatabase.update("movimientos", v, "id=?", arrayOf(m.id.toString()))
            m.id
        }
    }

    fun borrar(id: Long) {
        writableDatabase.delete("movimientos", "id=?", arrayOf(id.toString()))
    }

    fun obtener(id: Long): Movimiento? =
        readableDatabase.rawQuery("SELECT * FROM movimientos WHERE id=?", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) leer(c) else null }

    /** Movimientos entre dos fechas (incluidas), los más nuevos primero. */
    fun listar(desde: String, hasta: String): List<Movimiento> {
        val lista = ArrayList<Movimiento>()
        readableDatabase.rawQuery(
            "SELECT * FROM movimientos WHERE fecha BETWEEN ? AND ? ORDER BY fecha DESC, id DESC",
            arrayOf(desde, hasta)
        ).use { c -> while (c.moveToNext()) lista.add(leer(c)) }
        return lista
    }

    fun listarTodo(): List<Movimiento> = listar("0000-01-01", "9999-12-31")

    /** Suma de montos con filtros opcionales (null = sin filtro). */
    fun suma(
        desde: String, hasta: String, tipo: String,
        ambito: String? = null, lugar: String? = null, turno: String? = null
    ): Double {
        val sql = StringBuilder("SELECT COALESCE(SUM(monto),0) FROM movimientos WHERE fecha BETWEEN ? AND ? AND tipo=?")
        val args = arrayListOf(desde, hasta, tipo)
        if (ambito != null) { sql.append(" AND ambito=?"); args.add(ambito) }
        if (lugar != null) { sql.append(" AND lugar=?"); args.add(lugar) }
        if (turno != null) { sql.append(" AND turno=?"); args.add(turno) }
        return readableDatabase.rawQuery(sql.toString(), args.toTypedArray())
            .use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    }

    /**
     * Ingresos agrupados por periodo ([POR_MES] o [POR_SEMANA]).
     * Devuelve clave -> [Cerpi, Cementerio].
     */
    fun ingresosAgrupados(agrupar: String, desde: String, hasta: String): Map<String, DoubleArray> {
        require(agrupar == POR_MES || agrupar == POR_SEMANA)
        val mapa = HashMap<String, DoubleArray>()
        readableDatabase.rawQuery(
            "SELECT $agrupar AS clave, lugar, SUM(monto) FROM movimientos " +
                "WHERE tipo='$INGRESO' AND fecha BETWEEN ? AND ? GROUP BY clave, lugar",
            arrayOf(desde, hasta)
        ).use { c ->
            while (c.moveToNext()) {
                val arr = mapa.getOrPut(c.getString(0)) { DoubleArray(2) }
                val i = if (c.getString(1) == CEMENTERIO) 1 else 0
                arr[i] += c.getDouble(2)
            }
        }
        return mapa
    }

    /** Gastos de la heladería agrupados por periodo: clave -> total. */
    fun gastosHeladeriaAgrupados(agrupar: String, desde: String, hasta: String): Map<String, Double> {
        require(agrupar == POR_MES || agrupar == POR_SEMANA)
        val mapa = HashMap<String, Double>()
        readableDatabase.rawQuery(
            "SELECT $agrupar AS clave, SUM(monto) FROM movimientos " +
                "WHERE tipo='$GASTO' AND ambito='$HELADERIA' AND fecha BETWEEN ? AND ? GROUP BY clave",
            arrayOf(desde, hasta)
        ).use { c -> while (c.moveToNext()) mapa[c.getString(0)] = c.getDouble(1) }
        return mapa
    }

    /** Promedio de ingresos por día de la semana (0=domingo … 6=sábado). */
    fun ingresosPorDiaSemana(desde: String, hasta: String): DoubleArray {
        val tot = DoubleArray(7)
        readableDatabase.rawQuery(
            "SELECT CAST(strftime('%w',fecha) AS INTEGER), SUM(monto), COUNT(DISTINCT fecha) " +
                "FROM movimientos WHERE tipo='$INGRESO' AND fecha BETWEEN ? AND ? GROUP BY 1",
            arrayOf(desde, hasta)
        ).use { c ->
            while (c.moveToNext()) {
                val dias = c.getInt(2)
                if (dias > 0) tot[c.getInt(0)] = c.getDouble(1) / dias
            }
        }
        return tot
    }

    private fun leer(c: Cursor) = Movimiento(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        fecha = c.getString(c.getColumnIndexOrThrow("fecha")),
        tipo = c.getString(c.getColumnIndexOrThrow("tipo")),
        ambito = c.getString(c.getColumnIndexOrThrow("ambito")),
        lugar = c.getString(c.getColumnIndexOrThrow("lugar")),
        turno = c.getString(c.getColumnIndexOrThrow("turno")),
        monto = c.getDouble(c.getColumnIndexOrThrow("monto")),
        nota = c.getString(c.getColumnIndexOrThrow("nota")) ?: ""
    )
}
