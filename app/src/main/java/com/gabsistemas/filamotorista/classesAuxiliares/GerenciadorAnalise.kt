package com.gabsistemas.filamotorista.classesAuxiliares

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

class GerenciadorAnalise(private val firebaseAnalytics: FirebaseAnalytics) {

    // Função para definir o estado de login
    fun definirEstadoLogin(logado: Boolean) {
        firebaseAnalytics.setUserProperty("estado_login", if (logado) "logado" else "deslogado")
    }

    fun registrarEvento(evento: String, itemId: String, descricao: String) {
        if (evento.isNotBlank() && itemId.isNotBlank() && descricao.isNotBlank()) {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, itemId)
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, descricao)
            }
            firebaseAnalytics.logEvent(evento, bundle)
        } else {
            Log.w("GerenciadorAnalise", "Tentativa de registrar evento com parâmetros vazios ou nulos.")
        }
    }

}
