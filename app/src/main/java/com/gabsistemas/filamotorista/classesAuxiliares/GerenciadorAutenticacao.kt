package com.gabsistemas.filamotorista.classesAuxiliares

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore

class GerenciadorAutenticacao(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context
) {

    // Verifica se o usuário está logado
    fun verificarUsuarioLogado(): Boolean {
        return auth.currentUser != null
    }

    // Função de logout
    fun deslogarUsuario() {
        auth.signOut()
    }

    // Realiza login do usuário com email e senha
    fun loginUsuario(
        email: String,
        senha: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("Erro no login: ${task.exception?.message}")
                }
            }
    }

    // Verificar se o usuário é MasterAdmin
    fun verificarSeMasterAdmin(onResultado: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("GerenciadorAutenticacao", "Usuário não autenticado.")
            onResultado(false)
            return
        }

        firestore.collection("administradores").document(userId)
            .get()
            .addOnSuccessListener { documento ->
                val isMasterAdmin = documento.getBoolean("isMasterAdmin") ?: false
                Log.d("GerenciadorAutenticacao", "isMasterAdmin: $isMasterAdmin")
                onResultado(isMasterAdmin)
            }
            .addOnFailureListener { e ->
                Log.e("GerenciadorAutenticacao", "Erro ao verificar MasterAdmin: ${e.message}")
                onResultado(false)
            }
    }



    // Tratamento de erros do Firebase Authentication
    fun tratarErroFirebase(exception: Exception?): String {
        val errorCode = (exception as? FirebaseAuthException)?.errorCode
        return when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "O e-mail já está em uso. Por favor, faça login ou use outro e-mail."
            "ERROR_WEAK_PASSWORD" -> "A senha é muito fraca. Escolha uma senha mais segura."
            else -> exception?.localizedMessage ?: "Erro desconhecido ao criar conta."
        }
    }
}
