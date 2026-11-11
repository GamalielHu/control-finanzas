package com.gamaliel.finanzas

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

/** Registrar o editar un ingreso diario de la heladería (Cerpi / Cementerio, Mañana / Tarde). */
class IngresoActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var id = 0L
    private var fecha = Formato.hoy()

    private lateinit var btnFecha: Button
    private lateinit var grupoLugar: RadioGroup
    private lateinit var grupoTurno: RadioGroup
    private lateinit var edtMonto: EditText
    private lateinit var edtNota: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingreso)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = DatabaseHelper.get(this)

        btnFecha = findViewById(R.id.btnFecha)
        grupoLugar = findViewById(R.id.grupoLugar)
        grupoTurno = findViewById(R.id.grupoTurno)
        edtMonto = findViewById(R.id.edtMonto)
        edtNota = findViewById(R.id.edtNota)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        id = intent.getLongExtra("id", 0L)
        val existente = if (id != 0L) db.obtener(id) else null
        if (existente != null) {
            title = "Editar ingreso"
            fecha = existente.fecha
            grupoLugar.check(if (existente.lugar == DatabaseHelper.CEMENTERIO) R.id.rbCementerio else R.id.rbCerpi)
            grupoTurno.check(if (existente.turno == DatabaseHelper.TARDE) R.id.rbTarde else R.id.rbManana)
            edtMonto.setText(existente.monto.toString().replace('.', ','))
            edtNota.setText(existente.nota)
            btnEliminar.visibility = android.view.View.VISIBLE
        } else {
            // Comodidad: recordar el último lugar y sugerir el turno según la hora.
            val pref = getSharedPreferences("ajustes", MODE_PRIVATE)
            when (pref.getString("ultimo_lugar", null)) {
                DatabaseHelper.CERPI -> grupoLugar.check(R.id.rbCerpi)
                DatabaseHelper.CEMENTERIO -> grupoLugar.check(R.id.rbCementerio)
            }
            val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            grupoTurno.check(if (hora < 14) R.id.rbManana else R.id.rbTarde)
        }
        mostrarFecha()

        btnFecha.setOnClickListener {
            Formato.elegirFecha(this, fecha) { fecha = it; mostrarFecha() }
        }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardar() }
        btnEliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("¿Eliminar este ingreso?")
                .setPositiveButton("Eliminar") { _, _ -> db.borrar(id); finish() }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarFecha() {
        btnFecha.text = Formato.mostrar(fecha)
    }

    private fun guardar() {
        val lugar = when (grupoLugar.checkedRadioButtonId) {
            R.id.rbCerpi -> DatabaseHelper.CERPI
            R.id.rbCementerio -> DatabaseHelper.CEMENTERIO
            else -> null
        }
        val turno = when (grupoTurno.checkedRadioButtonId) {
            R.id.rbManana -> DatabaseHelper.MANANA
            R.id.rbTarde -> DatabaseHelper.TARDE
            else -> null
        }
        val monto = Formato.leerMonto(edtMonto.text.toString())
        when {
            lugar == null -> aviso("Elige el lugar: Cerpi o Cementerio")
            turno == null -> aviso("Elige el turno: Mañana o Tarde")
            monto == null || monto <= 0 -> { edtMonto.error = "Escribe un monto válido"; edtMonto.requestFocus() }
            else -> {
                db.guardar(
                    Movimiento(
                        id = id, fecha = fecha, tipo = DatabaseHelper.INGRESO,
                        ambito = DatabaseHelper.HELADERIA, lugar = lugar, turno = turno,
                        monto = monto, nota = edtNota.text.toString().trim()
                    )
                )
                getSharedPreferences("ajustes", MODE_PRIVATE).edit()
                    .putString("ultimo_lugar", lugar).apply()
                Toast.makeText(this, "Ingreso guardado: ${Formato.bs(monto)}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun aviso(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
