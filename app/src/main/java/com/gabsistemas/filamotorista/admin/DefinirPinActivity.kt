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
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorSessao
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.firebase.firestore.FirebaseFirestore

class DefinirPinActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var gerenciadorSessao: GerenciadorSessao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gerenciadorSessao = GerenciadorSessao(this)

        setContent {
            DefinirPinScreen { pinDefinido ->
                definirPin(pinDefinido)
            }
        }
    }

    private fun definirPin(pin: String) {
        val pinData = mapOf("pin" to pin)
        db.collection("config").document("admin_pin").set(pinData)
            .addOnSuccessListener {
                // Salva o papel do usuário como administrador após definir o PIN com sucesso
                gerenciadorSessao.salvarPapelUsuario("administrador")

                Toast.makeText(this, "PIN definido com sucesso!", Toast.LENGTH_SHORT).show()

                // Redireciona para a tela de login de administrador
                startActivity(Intent(this, LoginAdminActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao definir PIN: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewDefinirPinScreen() {
    FilaMotoristaTheme {
        DefinirPinScreen(
            onPinSubmit = { pin ->
                // Simulação de definição do PIN
                println("PIN definido: $pin")
            }
        )
    }
}
@Composable
fun DefinirPinScreen(onPinSubmit: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Definir PIN de Administrador")
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Defina o PIN") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onPinSubmit(pin) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Definir PIN")
        }
    }
}
