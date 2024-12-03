package com.gabsistemas.filamotorista.admin

import android.content.Intent
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

class CadastroAdminActivity : ComponentActivity() {

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
                CadastroAdminScreen(
                    onVoltarClick = { irParaVerificaPin() }, // Redireciona para VerificarPinActivity
                    onCadastrarClick = { nome, email, senha, confirmarSenha ->
                        if (validarFormulario.validarFormulario(
                                nome = nome,
                                email = email,
                                senha = senha,
                                confirmarSenha = confirmarSenha
                            )
                        ) {
                            criarContaFirebase(nome, email, senha)
                        } else {
                            Toast.makeText(
                                this,
                                "Erro ao validar o formulário.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onJaTenhoCadastroClick = {
                        GerenciadorNavegacao.irParaLoginAdmin(this) // Navega para a tela de login do administrador
                    }
                )
            }
        }
    }

    /**
     * Redireciona para VerificaPinActivity.
     */
       private fun irParaVerificaPin() {
        val intent = Intent(this, VerificarPinActivity::class.java)
        startActivity(intent)
        finish()
        }

        private fun mostrarMensagem(mensagem: String) {
            Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
        }

        private fun salvarPapelUsuario() {
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("user_role", "administrador")
                apply()
            }
        }

        private fun criarContaFirebase(nome: String, email: String, senha: String) {
            auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val administrador = hashMapOf(
                            "nome" to nome,
                            "email" to email
                        )

                        user?.let {
                            db.collection("administradores").document(it.uid)
                                .set(administrador)
                                .addOnSuccessListener {
                                    salvarPapelUsuario()
                                    mostrarMensagem("Conta criada com sucesso!")

                                    // Redireciona para LoginAdminActivity após o cadastro
                                    GerenciadorNavegacao.irParaLoginAdmin(this)
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
    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun PreviewCadastroAdminScreen() {
        FilaMotoristaTheme {
            CadastroAdminScreen(
                onVoltarClick = { println("Voltar clicado") },
                onCadastrarClick = { nome, email, senha, confirmarSenha ->
                    println("Cadastrar clicado: Nome: $nome, Email: $email, Senha: $senha, Confirmar Senha: $confirmarSenha")
                },
                onJaTenhoCadastroClick = { println("Já tenho cadastro clicado") }
            )
        }
    }


        @Composable

        fun CadastroAdminScreen(
            onVoltarClick: () -> Unit,
            onCadastrarClick: (String, String, String, String) -> Unit,
            onJaTenhoCadastroClick: () -> Unit // Adicionamos este parâmetro
        ) {
            var nome by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var senha by remember { mutableStateOf("") }
            var confirmarSenha by remember { mutableStateOf("") }

            var senhaVisivel by remember { mutableStateOf(false) }
            var confirmarSenhaVisivel by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
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
                    Text(
                        text = "Cadastro Administrador",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                    OutlinedTextField(
                        value = confirmarSenha,
                        onValueChange = { confirmarSenha = it },
                        label = { Text("Confirmar Senha") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                confirmarSenhaVisivel = !confirmarSenhaVisivel
                            }) {
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

                    Button(
                        onClick = { onCadastrarClick(nome, email, senha, confirmarSenha) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cadastrar")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onVoltarClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Voltar")
                    }

                    // Novo botão "Já tenho Cadastro"
                    TextButton(
                        onClick = onJaTenhoCadastroClick, // Acionará a função correspondente
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Já tenho Cadastro")
                    }
                }
            }
        }
    }


