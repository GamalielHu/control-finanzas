package com.gamaliel.finanzas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

/** Pantalla de entrada: elegir Trabajo (heladería) o Personal, y si se vinculan. */
class InicioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)
        supportActionBar?.hide()

        val sw = findViewById<SwitchMaterial>(R.id.swVincular)
        val txt = findViewById<TextView>(R.id.txtVincular)
        sw.isChecked = Modo.vinculado(this)
        txt.text = Modo.explicacion(sw.isChecked)
        sw.setOnCheckedChangeListener { _, activo ->
            Modo.setVinculado(this, activo)
            txt.text = Modo.explicacion(activo)
        }

        findViewById<Button>(R.id.btnTrabajo).setOnClickListener { abrir(Modo.TRABAJO) }
        findViewById<Button>(R.id.btnPersonal).setOnClickListener { abrir(Modo.PERSONAL) }
    }

    private fun abrir(modo: String) {
        startActivity(Intent(this, MainActivity::class.java).putExtra(Modo.EXTRA, modo))
    }
}
