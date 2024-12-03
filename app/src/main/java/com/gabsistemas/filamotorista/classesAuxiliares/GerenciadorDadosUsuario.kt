package com.gabsistemas.filamotorista.classesAuxiliares

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class GerenciadorDadosUsuario(private val context: Context) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Obtém o nome do usuário armazenado localmente (SharedPreferences)
    val nomeUsuario: String?
        get() = sharedPreferences.getString("nomeUsuario", null)

    // Salva o nome do usuário localmente
    fun salvarNomeUsuario(nome: String) {
        sharedPreferences.edit().putString("nomeUsuario", nome).apply()
    }

    // Função para buscar dados do motorista no Firestore
    fun buscarDadosMotorista(callback: (nome: String?, placa: String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            callback(null, null)
            return
        }

        firestore.collection("motoristas")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome")
                    val placa = document.getString("placa")
                    salvarNomeUsuario(nome ?: "Motorista") // Salva o nome localmente
                    callback(nome, placa)
                } else {
                    Toast.makeText(context, "Dados do motorista não encontrados.", Toast.LENGTH_SHORT).show()
                    callback(null, null)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao buscar dados do motorista: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(null, null)
            }
    }

    // Obtém o nome do usuário administrador no Firestore
    fun obterNomeUsuario(userId: String, callback: (String?) -> Unit) {
        firestore.collection("administradores")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val nomeUsuario = document.getString("nome")
                if (nomeUsuario != null) {
                    salvarNomeUsuario(nomeUsuario) // Atualiza o nome localmente
                }
                callback(nomeUsuario)
            }
            .addOnFailureListener {
                callback(null)
            }
    }


    // Busca e salva dados de localização de referência no Firestore
    fun atualizarLocalizacaoReferencia(
        latitude: Double,
        longitude: Double,
        callback: (Boolean) -> Unit
    ) {
        val localizacao = mapOf(
            "localizacaoReferencia" to mapOf(
                "latitude" to latitude,
                "longitude" to longitude
            ),
            "ultimaAtualizacao" to System.currentTimeMillis(),
            "ultimoResponsavel" to (nomeUsuario ?: "Administrador")
        )

        firestore.collection("configuracoesGlobais").document("estadoGlobal")
            .set(localizacao, SetOptions.merge())
            .addOnSuccessListener {
                callback(true)
                Toast.makeText(context, "Localização atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                callback(false)
                Toast.makeText(context, "Erro ao atualizar localização: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



}
