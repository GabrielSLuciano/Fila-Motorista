package com.gabsistemas.filamotorista.motorista

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gabsistemas.filamotorista.classesAuxiliares.ValidadorFormulario
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorNavegacao
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore

class CadastroMotoristaActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var validarFormulario: ValidadorFormulario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        validarFormulario = ValidadorFormulario(this)

        setContent {
            FilaMotoristaTheme {
                CadastroMotoristaScreen(
                    onVoltarClick = {
                        finish()
                    },
                    onCadastrarClick = { nome: String, email: String, placa: String, senha: String, confirmarSenha: String ->
                        if (validarFormulario.validarFormulario(
                                nome = nome,
                                email = email,
                                senha = senha,
                                confirmarSenha = confirmarSenha,
                                placa = placa
                            )
                        ) {
                            criarContaFirebase(nome, email, placa, senha)
                        } else {
                            Toast.makeText(
                                this,
                                "Erro ao validar o formulário.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    // Exibe uma mensagem para o usuário
    private fun mostrarMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    // Salva o papel do usuário como "motorista" em SharedPreferences
    private fun salvarPapelUsuario() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_role", "motorista")
            apply()
        }
    }

    // Cria uma nova conta no Firebase Authentication e salva o usuário no Firestore
    private fun criarContaFirebase(nome: String, email: String, placa: String, senha: String) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val motorista = hashMapOf(
                        "nome" to nome,
                        "email" to email,
                        "placa" to placa
                    )

                    user?.let {
                        db.collection("motoristas").document(it.uid)
                            .set(motorista)
                            .addOnSuccessListener {
                                salvarNomeENoSharedPreferences(nome)
                                salvarPapelUsuario()
                                mostrarMensagem("Conta criada sucesso!")

                                // Redireciona para MotoristaActivity após o cadastro
                                GerenciadorNavegacao.irParaLoginMotorista(this)
                            }
                            .addOnFailureListener { e ->
                                mostrarMensagem("Erro ao salvar os dados: ${e.message}")
                            }
                    }
                } else {
                    tratarErroFirebase(task.exception)
                }
            }
    }

    // Salva o nome do motorista em SharedPreferences
    private fun salvarNomeENoSharedPreferences(nome: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("nome_motorista", nome)
            apply()
        }
    }

    // Trata erros do Firebase Authentication e exibe mensagens apropriadas
    private fun tratarErroFirebase(exception: Exception?) {
        val errorCode = (exception as? FirebaseAuthException)?.errorCode
        when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> {
                mostrarMensagem("O e-mail já está em uso. Por favor, faça login ou use outro e-mail.")
            }

            "ERROR_WEAK_PASSWORD" -> {
                mostrarMensagem("A senha é muito fraca. Escolha uma senha mais segura.")
            }

            else -> {
                mostrarMensagem("Erro ao criar conta: ${exception?.message}")
            }
        }
    }

    // visualização do layout
    @Preview(showBackground = true)
    @Composable
    fun PreviewCadastroMotoristaScreen() {
        FilaMotoristaTheme {
            CadastroMotoristaScreen(
                onVoltarClick = { /* Simulação do botão Voltar */ },
                onCadastrarClick = { nome, email, placa, senha, confirmarSenha ->
                    // Simulação de cadastro
                    println("Cadastrar: Nome: $nome, Email: $email, Placa: $placa, Senha: $senha, Confirmar Senha: $confirmarSenha")
                }
            )
        }
    }

    // Tela de cadastro para coletar informações do motorista
    @Composable
    fun CadastroMotoristaScreen(
        onVoltarClick: () -> Unit,
        onCadastrarClick: (String, String, String, String, String) -> Unit
    ) {
        var nome by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var placa by remember { mutableStateOf("") }
        var senha by remember { mutableStateOf("") }
        var confirmarSenha by remember { mutableStateOf("") }

        var senhaVisivel by remember { mutableStateOf(false) }
        var confirmarSenhaVisivel by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Título "Fila Motorista" centralizado no topo
            Text(
                text = "Fila Motorista",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Subtítulo "Motorista" acima do primeiro campo
                Text(
                    text = "Cadastro Motorista",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 8.dp) // Espaçamento abaixo do subtítulo
                )

                // Campo Nome
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Placa
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters, // Define letras maiúsculas
                        keyboardType = KeyboardType.Text
                    )
                )

                            Spacer(modifier = Modifier.height(16.dp))

                // Campo Senha
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                            Icon(
                                imageVector = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Confirmar Senha
                OutlinedTextField(
                    value = confirmarSenha,
                    onValueChange = { confirmarSenha = it },
                    label = { Text("Confirmar Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) {
                            Icon(
                                imageVector = if (confirmarSenhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmarSenhaVisivel) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botão de Cadastrar
                Button(
                    onClick = { onCadastrarClick(nome, email, placa, senha, confirmarSenha) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cadastrar")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Voltar
                TextButton(
                    onClick = onVoltarClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voltar")
                }
            }
        }
    }
}
