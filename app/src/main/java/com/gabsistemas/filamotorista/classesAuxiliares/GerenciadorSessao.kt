package com.gabsistemas.filamotorista.classesAuxiliares


import android.content.Context


class GerenciadorSessao(private val contexto: Context) {

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val USER_ROLE_KEY = "user_role"
    }

    private val sharedPreferences by lazy {
        contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun redefinirSessao() {
        sharedPreferences.edit().remove(USER_ROLE_KEY).apply()
    }

    // Função para obter o papel do usuário (motorista ou administrador)
    fun obterPapelUsuario(): String? {
        val sharedPreferences = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(USER_ROLE_KEY, null)
    }

    // Função para salvar o papel do usuário
    fun salvarPapelUsuario(papel: String) {
        val sharedPreferences = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(USER_ROLE_KEY, papel)
            apply()
        }
    }

    // Função para verificar se o papel do usuário existe
    fun existePapelUsuario(): Boolean {
        return obterPapelUsuario() != null
    }
}



