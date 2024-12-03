package com.gabsistemas.filamotorista.motorista

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAnalise
import com.gabsistemas.filamotorista.MainActivity
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorSessao


class LoginMotoristaActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var executor: Executor
    private var usarDigitalNasProximasVezes = false
    private lateinit var gerenciadorAnalise: GerenciadorAnalise
    private lateinit var gerenciadorSessao: GerenciadorSessao

    override fun onPause() {
        super.onPause()
        // Adicione log para monitorar o estado da atividade
        Log.d("LoginMotoristaActivity", "LoginMotoristaActivity pausada.")
    }

    override fun onStop() {
        super.onStop()
        // Monitora quando a atividade é completamente parada
        Log.d("LoginMotoristaActivity", "LoginMotoristaActivity parada.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        gerenciadorAnalise = GerenciadorAnalise(FirebaseAnalytics.getInstance(this))
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = criarBiometricPrompt()
        gerenciadorSessao = GerenciadorSessao(this)

        // Carrega a configuração salva para o uso da biometria
        usarDigitalNasProximasVezes = sharedPreferences.getBoolean("usarDigital", false)

        // Recuperar email, senha e a preferência de salvar login
        val emailSalvo = sharedPreferences.getString("email", "") ?: ""
        val senhaSalva = sharedPreferences.getString("senha", "") ?: ""
        val salvarLogin = sharedPreferences.getBoolean("salvarLogin", false) // Verifica se o login deve ser salvo

        setContent {
            FilaMotoristaTheme {
                TelaLogin(
                    aoClicarLogin = { email, senha, salvarLogin -> loginUsuario(email, senha, salvarLogin) },
                    aoClicarCadastrar = {
                        val intent = Intent(this, CadastroMotoristaActivity::class.java)
                        startActivity(intent)
                    },
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
                    },
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
                    onRecuperarSenha = { email -> recuperarSenha(email) }
                )
            }
        }


        // Autentica por biometria se a opção estiver ativada
        if (usarDigitalNasProximasVezes) {
            autenticarPorDigital()
        }
    }

    private fun loginUsuario(email: String, senha: String, salvarLogin: Boolean) {
        // Verifica se os campos de email e senha estão preenchidos
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu email.", Toast.LENGTH_SHORT).show()
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
                    val intent = Intent(this, MotoristaActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Erro no login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
                    loginUsuario(emailSalvo, senhaSalva, salvarLogin = true)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@LoginMotoristaActivity, "Erro de autenticação: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@LoginMotoristaActivity, "Autenticação falhou.", Toast.LENGTH_SHORT).show()
            }
          }
        )
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
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTelaLogin() {
    FilaMotoristaTheme {
        TelaLogin(
            aoClicarLogin = { email, senha, salvarLogin -> println("Login com email: $email, senha: $senha, salvar login: $salvarLogin") },
            aoClicarCadastrar = { println("Navegar para a tela de cadastro") },
            onRedefinirUsuario = { println("Usuário redefinido") },
            usarDigitalNasProximasVezes = false,
            aoMudarPreferenciaDigital = { habilitarDigital -> println("Habilitar digital: $habilitarDigital") },
            emailInicial = "exemplo@email.com",
            senhaInicial = "senha123",
            salvarLoginInicial = false,
            onRecuperarSenha = { email -> println("Recuperar senha para o email: $email") }
        )
    }
}

@Composable
fun TelaLogin(
    aoClicarLogin: (String, String, Boolean) -> Unit,
    aoClicarCadastrar: () -> Unit,
    onRedefinirUsuario: () -> Unit,
    usarDigitalNasProximasVezes: Boolean,
    aoMudarPreferenciaDigital: (Boolean) -> Unit,
    emailInicial: String,
    senhaInicial: String,
    salvarLoginInicial: Boolean,
    onRecuperarSenha: (String) -> Unit
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
                        expanded = false // Atualiza o estado antes de executar a ação
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
            // Título
            Text(
                text = "Fila Motorista",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 50.dp)
            )

            // Subtítulo "Motorista"
            Text(
                text = "Login Motorista",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Mesma largura do campo de email
                    .align(Alignment.Start) // Alinha ao início do espaço disponível
                    .padding(bottom = 5.dp) // Espaçamento abaixo do subtítulo
            )

            // Campo de Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(0.9f), // Largura do campo
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Senha com botão "Esqueceu?" ao lado
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

            Button(
                onClick = { aoClicarLogin(email, senha, salvarLogin) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = "Entrar")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = aoClicarCadastrar) {
                Text(text = "Não tem conta? Cadastre-se aqui")
            }
        }
    }
}


