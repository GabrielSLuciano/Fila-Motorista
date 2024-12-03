package com.gabsistemas.filamotorista.admin

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.gabsistemas.filamotorista.MainActivity
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAnalise
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorSessao
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class LoginAdminActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var executor: Executor
    private var usarDigitalNasProximasVezes = false
    private lateinit var gerenciadorAnalise: GerenciadorAnalise
    private lateinit var gerenciadorSessao: GerenciadorSessao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("admin_prefs", MODE_PRIVATE)
        gerenciadorAnalise = GerenciadorAnalise(FirebaseAnalytics.getInstance(this))
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = criarBiometricPrompt()
        gerenciadorSessao = GerenciadorSessao(this)

        // Carrega a configuração salva para o uso da biometria
        usarDigitalNasProximasVezes = sharedPreferences.getBoolean("usarDigital", false)

        // Recuperar email, senha e a preferência de salvar login
        val emailSalvo = sharedPreferences.getString("email", "") ?: ""
        val senhaSalva = sharedPreferences.getString("senha", "") ?: ""
        val salvarLogin = sharedPreferences.getBoolean("salvarLogin", false)

        setContent {
            FilaMotoristaTheme {
                TelaLoginAdmin(
                    aoClicarLogin = { email, senha, salvarLogin -> loginAdmin(email, senha, salvarLogin) },
                    onRecuperarSenha = { email -> recuperarSenha(email) },
                    usarDigitalNasProximasVezes = usarDigitalNasProximasVezes,
                    aoMudarPreferenciaDigital = { usarDigital ->
                        usarDigitalNasProximasVezes = usarDigital
                        sharedPreferences.edit().putBoolean("usarDigital", usarDigital).apply()
                        if (usarDigital) {
                            autenticarPorDigital()
                        }
                    },
                    emailInicial = if (salvarLogin) emailSalvo else "",
                    senhaInicial = if (salvarLogin) senhaSalva else "",
                    salvarLoginInicial = salvarLogin,
                    onRedefinirUsuario = {
                        try {
                            // Remove o papel do usuário
                            gerenciadorSessao.redefinirSessao()

                            // Redefine para MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                            Log.d("Navigation", "Redirecionando para MainActivity")
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erro ao redefinir usuário: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                )
            }
        }

        // Autentica por biometria se a opção estiver ativada
        if (usarDigitalNasProximasVezes) {
            autenticarPorDigital()
        }
    }

    private fun loginAdmin(email: String, senha: String, salvarLogin: Boolean) {
        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha.isEmpty()) {
            Toast.makeText(this, "Por favor, insira sua senha.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    gerenciadorAnalise.definirEstadoLogin(logado = true) // Registra o evento de login bem-sucedido
                    Log.d("AnalyticsTest", "Estado de login enviado para o Firebase: logado = true")
                    if (salvarLogin) {
                        sharedPreferences.edit()
                            .putString("email", email)
                            .putString("senha", senha)
                            .putBoolean("salvarLogin", true) // Salva a preferência de login
                            .apply()
                    } else {
                        // Se não salvar, limpa os dados
                        sharedPreferences.edit()
                            .remove("email")
                            .remove("senha")
                            .putBoolean("salvarLogin", false)
                            .apply()
                    }
                    Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Erro no login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun recuperarSenha(email: String) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu email.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Email de recuperação enviado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erro ao enviar email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun autenticarPorDigital() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticação Biométrica")
                .setSubtitle("Use sua digital para acessar o app")
                .setNegativeButtonText("Cancelar")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun criarBiometricPrompt(): BiometricPrompt {
        return BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val emailSalvo = sharedPreferences.getString("email", "")
                val senhaSalva = sharedPreferences.getString("senha", "")
                if (!emailSalvo.isNullOrEmpty() && !senhaSalva.isNullOrEmpty()) {
                    loginAdmin(emailSalvo, senhaSalva, salvarLogin = true)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@LoginAdminActivity, "Erro de autenticação: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@LoginAdminActivity, "Autenticação falhou.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTelaLoginAdmin() {
    FilaMotoristaTheme {
        TelaLoginAdmin(
            aoClicarLogin = { email, senha, salvarLogin ->
                println("Login com email: $email, senha: $senha, salvar login: $salvarLogin")
            },
            onRecuperarSenha = { email ->
                println("Recuperar senha para o email: $email")
            },
            usarDigitalNasProximasVezes = false,
            aoMudarPreferenciaDigital = { habilitarDigital ->
                println("Habilitar digital: $habilitarDigital")
            },
            onRedefinirUsuario = { println("Usuário redefinido") },
            emailInicial = "exemplo@email.com",
            senhaInicial = "senhaAdmin",
            salvarLoginInicial = false
        )
    }
}

@Composable
fun TelaLoginAdmin(
    aoClicarLogin: (String, String, Boolean) -> Unit,
    onRecuperarSenha: (String) -> Unit,
    usarDigitalNasProximasVezes: Boolean,
    aoMudarPreferenciaDigital: (Boolean) -> Unit,
    emailInicial: String,
    senhaInicial: String,
    salvarLoginInicial: Boolean,
    onRedefinirUsuario: () -> Unit,
) {
    var email by remember { mutableStateOf(emailInicial) }
    var senha by remember { mutableStateOf(senhaInicial) }
    var salvarLogin by remember { mutableStateOf(salvarLoginInicial) }
    var habilitarDigital by remember { mutableStateOf(usarDigitalNasProximasVezes) }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
    // Ícone de menu para redefinir o usuário
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = { expanded = true }
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize()
        ) {
            DropdownMenuItem(
                text = { Text("Redefinir usuário") },
                onClick = {
                    expanded = false
                    onRedefinirUsuario()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Título "Fila Motorista" centralizado
            Text(
                text = "Fila Motorista",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 50.dp)
            )

            // Subtítulo "Administrador"
            Text(
                text = "Login Administrador",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Start)
                    .padding(bottom = 5.dp)
            )

            // Campo de Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Senha com botão "Esqueceu?"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    modifier = Modifier.weight(1f),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = { onRecuperarSenha(email) }) {
                    Text(text = "Esqueceu?", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botões para Salvar Login e Habilitar Digital
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = salvarLogin,
                    onCheckedChange = { salvarLogin = it }
                )
                Text(text = "Salvar login")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = habilitarDigital,
                    onCheckedChange = {
                        habilitarDigital = it
                        aoMudarPreferenciaDigital(it)
                    }
                )
                Text(text = "Habilitar digital")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de Login
            Button(
                onClick = { aoClicarLogin(email, senha, salvarLogin) },
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Entrar")
            }
        }
    }
}



