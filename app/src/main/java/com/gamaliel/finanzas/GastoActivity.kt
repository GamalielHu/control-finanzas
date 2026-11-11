package com.gamaliel.finanzas

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Registrar o editar un gasto: de la Heladería (afecta a Cerpi y Cementerio) o Personal. */
class GastoActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var id = 0L
    private var fecha = Formato.hoy()

    private lateinit var btnFecha: Button
    private lateinit var grupoCategoria: RadioGroup
    private lateinit var edtMonto: EditText
    private lateinit var edtDescripcion: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gasto)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = DatabaseHelper.get(this)

        btnFecha = findViewById(R.id.btnFecha)
        grupoCategoria = findViewById(R.id.grupoCategoria)
        edtMonto = findViewById(R.id.edtMonto)
        edtDescripcion = findViewById(R.id.edtDescripcion)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        id = intent.getLongExtra("id", 0L)
        val existente = if (id != 0L) db.obtener(id) else null
        // La categoría la define el modo elegido al entrar (Trabajo o Personal).
        val modo = existente?.ambito ?: intent.getStringExtra(Modo.EXTRA)
        if (modo != null) {
            grupoCategoria.check(if (modo == Modo.PERSONAL) R.id.rbPersonal else R.id.rbHeladeria)
            grupoCategoria.visibility = View.GONE
            findViewById<View>(R.id.lblCategoria).visibility = View.GONE
            val personal = modo == Modo.PERSONAL
            title = (if (existente != null) "Editar gasto " else "Gasto ") +
                (if (personal) "personal" else "de la heladería")
            if (personal) edtDescripcion.hint = "Ej.: almuerzo, pasaje, recarga"
            else edtDescripcion.hint = "Ej.: leche, conos, azúcar, luz"
        }
        if (existente != null) {
            fecha = existente.fecha
            grupoCategoria.check(
                if (existente.ambito == DatabaseHelper.PERSONAL) R.id.rbPersonal else R.id.rbHeladeria
            )
            edtMonto.setText(existente.monto.toString().replace('.', ','))
            edtDescripcion.setText(existente.nota)
            btnEliminar.visibility = View.VISIBLE
        }
        mostrarFecha()

        btnFecha.setOnClickListener {
            Formato.elegirFecha(this, fecha) { fecha = it; mostrarFecha() }
        }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardar() }
        btnEliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("¿Eliminar este gasto?")
                .setPositiveButton("Eliminar") { _, _ -> db.borrar(id); finish() }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarFecha() {
        btnFecha.text = Formato.mostrar(fecha)
    }

    private fun guardar() {
        val ambito = when (grupoCategoria.checkedRadioButtonId) {
            R.id.rbHeladeria -> DatabaseHelper.HELADERIA
            R.id.rbPersonal -> DatabaseHelper.PERSONAL
            else -> null
        }
        val monto = Formato.leerMonto(edtMonto.text.toString())
        val desc = edtDescripcion.text.toString().trim()
        when {
            ambito == null -> Toast.makeText(this, "Elige la categoría: Heladería o Personal", Toast.LENGTH_SHORT).show()
            monto == null || monto <= 0 -> { edtMonto.error = "Escribe un monto válido"; edtMonto.requestFocus() }
            desc.isEmpty() -> { edtDescripcion.error = "Escribe en qué se gastó"; edtDescripcion.requestFocus() }
            else -> {
                db.guardar(
                    Movimiento(
                        id = id, fecha = fecha, tipo = DatabaseHelper.GASTO, ambito = ambito,
                        lugar = null, turno = null, monto = monto, nota = desc
                    )
                )
                Toast.makeText(this, "Gasto guardado: ${Formato.bs(monto)}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
