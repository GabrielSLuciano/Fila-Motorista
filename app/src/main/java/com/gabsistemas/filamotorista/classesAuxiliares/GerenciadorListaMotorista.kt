package com.gabsistemas.filamotorista.classesAuxiliares

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GerenciadorListaMotorista(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val _ultimaSenhaState = MutableStateFlow(
        mapOf(
            "senha" to "N/A",
            "nome" to "N/A",
            "rota" to "N/A",
            "placa" to "N/A",
            "box" to "N/A"
        )
    )
    val ultimaSenhaState: StateFlow<Map<String, String>> get() = _ultimaSenhaState

    private val _motoristas = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val motoristas: StateFlow<List<Map<String, Any>>> get() = _motoristas

    fun atualizarMotoristas(listaAtualizada: List<Map<String, Any>>) {
        _motoristas.value = listaAtualizada
    }

    init {
        // Observa mudanças na coleção motoristas
        firestore.collection("filaMotorista")
            .orderBy("senha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    _motoristas.value = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                } else {
                    Log.e("Firestore", "Erro ao observar motoristas: ${error.message}")
                }
            }

        // Inicia a observação automática da última senha chamada
        observarUltimaSenhaChamada()
    }

    fun observarUltimaSenhaChamada() {

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("Auth", "Usuário não autenticado. Impedindo acesso ao Firestore.")
            return
        } else {
            Log.d("Auth", "Usuário autenticado com UID: ${user.uid}")
        }

        val ultimaSenhaDocRef = firestore.collection("filaMotorista")
            .document("ultimaSenha")

        ultimaSenhaDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Erro ao observar última senha: ${error.message}")
                return@addSnapshotListener
            }

                if (snapshot != null && snapshot.exists()) {
                    val ultimaSenhaData = mapOf(
                        "senha" to (snapshot.getString("senha") ?: "N/A"),
                        "nome" to (snapshot.getString("nome") ?: "N/A"),
                        "rota" to (snapshot.getString("rota") ?: "N/A"),
                        "placa" to (snapshot.getString("placa") ?: "N/A"),
                        "box" to (snapshot.getString("box") ?: "00")
                    )
                    _ultimaSenhaState.value = ultimaSenhaData
                    Log.d("Firestore", "Última senha recebida: $ultimaSenhaData")
                } else {
                    Log.w("Firestore", "Documento de última senha não encontrado ou vazio.")
                }
            }
    }


    fun chamarMotoristasPorQuantidade(
        quantidade: Int,
        callback: (Boolean, String) -> Unit
    ) {
        firestore.collection("filaMotorista")
            .whereEqualTo("status", "aguardando")
            .orderBy("senha", Query.Direction.ASCENDING) // Ordena pela senha
            .limit(quantidade.toLong()) // Limita o número de motoristas
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback(false, "Nenhum motorista com status 'aguardando' foi encontrado.")
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                var ultimaSenhaChamada: Map<String, Any>? = null

                querySnapshot.documents.forEachIndexed { index, document ->
                    val data = mapOf("status" to "chamado")
                    batch.update(document.reference, data)

                    // Atualiza o dado da última senha chamada com o primeiro motorista chamado
                    if (index == 0) {
                        ultimaSenhaChamada = document.data?.toMutableMap()?.apply {
                            this["status"] = "chamado"
                        }
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        // Atualiza o documento `ultimaSenha` com os dados do primeiro motorista chamado
                        ultimaSenhaChamada?.let {
                            firestore.collection("controleSenha")
                                .document("ultimaSenha")
                                .set(it, SetOptions.merge())
                                .addOnSuccessListener {
                                    callback(true, "Motoristas chamados com sucesso e última senha atualizada!")
                                }
                                .addOnFailureListener { e ->
                                    callback(false, "Motoristas chamados, mas erro ao atualizar última senha: ${e.message}")
                                }
                        } ?: callback(false, "Erro ao atualizar a última senha chamada.")
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Erro ao chamar motoristas: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                callback(false, "Erro ao acessar a coleção motoristas: ${e.message}")
            }
    }


    fun limparListaMotoristas(onConcluido: () -> Unit) {
        firestore.collection("filaMotorista")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach { document ->
                        batch.delete(document.reference)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            // Redefinir o contador de senhas explicitamente para 0
                            firestore.collection("controleSenha")
                                .document("contador")
                                .set(mapOf("ultimaSenha" to 0), SetOptions.merge()) // Define 0 no campo ultimaSenha
                                .addOnSuccessListener {
                                    // Limpar os dados do quadro de última senha chamada
                                    firestore.collection("controleSenha")
                                        .document("ultimaSenha")
                                        .delete() // Remove o documento ultimaSenha
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Lista limpa, contador de senhas reiniciado e quadro de última senha limpo.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onConcluido()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Firestore", "Erro ao limpar o quadro de última senha: ${e.message}")
                                            Toast.makeText(
                                                context,
                                                "Erro ao limpar o quadro de última senha: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Erro ao redefinir o contador: ${e.message}")
                                    Toast.makeText(
                                        context,
                                        "Erro ao redefinir o contador: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Erro ao limpar a lista: ${e.message}")
                            Toast.makeText(
                                context,
                                "Erro ao limpar a lista: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        context,
                        "A lista já está vazia.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onConcluido()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao acessar a lista de motoristas: ${e.message}")
                Toast.makeText(
                    context,
                    "Erro ao acessar a lista de motoristas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }




    fun excluirMotoristaPorSenha(senha: String, callback: (Boolean, String) -> Unit) {
        firestore.collection("filaMotorista")
            .whereEqualTo("senha", senha)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach { document ->
                        batch.delete(document.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            callback(true, "Motorista com a senha $senha foi excluído com sucesso.")
                        }
                        .addOnFailureListener { e ->
                            callback(false, "Erro ao excluir motorista: ${e.message}")
                        }
                } else {
                    callback(false, "Nenhum motorista encontrado com a senha $senha.")
                }
            }
            .addOnFailureListener { e ->
                callback(false, "Erro ao acessar a coleção motoristas: ${e.message}")
            }
    }
}
