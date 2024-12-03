package com.gabsistemas.filamotorista.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gabsistemas.filamotorista.MainActivity
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorSessao
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.firebase.firestore.FirebaseFirestore

class VerificarPinActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var gerenciadorSessao: GerenciadorSessao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Limpar a cache do Firebase para evitar erros de persistência
        FirebaseFirestore.getInstance().clearPersistence()
        gerenciadorSessao = GerenciadorSessao(this)

        setContent {
            VerificarPinScreen(
                onPinSubmit = { pinInserido -> verificarPin(pinInserido) },
                onBackPress = { voltarParaMainActivity() }
            )
        }
    }

    /**
     * Função para verificar se o PIN inserido pelo usuário corresponde ao PIN salvo no Firestore.
     * Caso o PIN seja válido, o papel do usuário é salvo como "administrador" e ele é redirecionado
     * para a tela de Cadastro de Administrador.
     *
     * @param "pinInserido" O PIN digitado pelo usuário.
     */

    private fun verificarPin(pinInserido: String) {
        if (pinInserido.isEmpty()) {
            Toast.makeText(this, "Digite o PIN", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Config").document("khAct2ladcWvNNNdcGwt").get()
            .addOnSuccessListener { document ->
                val pinSalvo = document.getString("PIN")

                if (pinSalvo == pinInserido) {
                    gerenciadorSessao.salvarPapelUsuario("administrador")
                    Toast.makeText(this, "PIN correto! Redirecionando...", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, CadastroAdminActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "PIN incorreto.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar o PIN: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
        // Função para redirecionar à MainActivity
       private fun voltarParaMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewVerificarPinScreen() {
    FilaMotoristaTheme {
        VerificarPinScreen(
            onPinSubmit = { pin ->
                println("PIN enviado: $pin")
            },
            onBackPress = {
                println("Botão Voltar pressionado")
            }
        )
    }
}

@Composable
fun VerificarPinScreen(
    onPinSubmit: (String) -> Unit,
    onBackPress: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título centralizado no topo
        Text(
            text = "Verificar Autorização",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Campo PIN
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("Digite o PIN") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Verificar PIN
            Button(
                onClick = { onPinSubmit(pin) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Verificar PIN")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Voltar
            TextButton(
                onClick = onBackPress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voltar")
            }
        }
    }
}



