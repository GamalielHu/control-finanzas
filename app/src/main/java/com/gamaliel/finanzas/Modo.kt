package com.gamaliel.finanzas

import android.content.Context

/** Trabajo (heladería) o Personal, y la vinculación opcional de ambos. */
object Modo {
    const val EXTRA = "modo"
    const val TRABAJO = DatabaseHelper.HELADERIA
    const val PERSONAL = DatabaseHelper.PERSONAL

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    fun vinculado(ctx: Context) = prefs(ctx).getBoolean("vincular", false)

    fun setVinculado(ctx: Context, activo: Boolean) {
        prefs(ctx).edit().putBoolean("vincular", activo).apply()
    }

    fun explicacion(activo: Boolean) =
        if (activo) "Vinculados: la ganancia de la heladería se suma a tu saldo personal, " +
            "y la ganancia real del trabajo descuenta tus gastos personales."
        else "Separados: Trabajo y Personal llevan sus cuentas por su lado."
}
