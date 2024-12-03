package com.gabsistemas.filamotorista

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gabsistemas.filamotorista.admin.VerificarPinActivity
import com.gabsistemas.filamotorista.admin.LoginAdminActivity
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAnalise
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorLocalizacaoPermissao
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorSessao
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.android.gms.location.LocationServices
import com.gabsistemas.filamotorista.motorista.LoginMotoristaActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var gerenciadorAnalise: GerenciadorAnalise
    private lateinit var gerenciadorLocalizacaoPermissao: GerenciadorLocalizacaoPermissao
    private lateinit var gerenciadorSessao: GerenciadorSessao
    private lateinit var db: FirebaseFirestore

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                gerenciadorLocalizacaoPermissao.verificarLocalizacaoAtiva {
                    Toast.makeText(this, "Localização necessária para o app", Toast.LENGTH_LONG).show()
                }
            } else {
                mostrarPermissaoNegada()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializações
        db = FirebaseFirestore.getInstance()
        gerenciadorAnalise = GerenciadorAnalise(FirebaseAnalytics.getInstance(this))
        gerenciadorSessao = GerenciadorSessao(this)
        gerenciadorLocalizacaoPermissao = GerenciadorLocalizacaoPermissao(this,

            LocationServices.getFusedLocationProviderClient(this)
        )

        // Verifica o papel do usuário e redireciona
        if (gerenciadorSessao.existePapelUsuario()) {
            redirecionarParaTelaDeUsuario()
        } else {
            setContent {
                FilaMotoristaTheme {
                    ExibirTelaInicial()
                }
            }
        }

        // Verificar permissões de localização
        gerenciadorLocalizacaoPermissao.verificarPermissaoLocalizacao(
            requestPermissionLauncher,
            onConcedida = {
                gerenciadorLocalizacaoPermissao.verificarLocalizacaoAtiva {
                    // Ação adicional se necessário
                }
            },
            onNegada = {
                gerenciadorLocalizacaoPermissao.mostrarPermissaoNegada()
            }
        )
    }

    // Redireciona para a tela conforme o papel do usuário
    private fun redirecionarParaTelaDeUsuario() {
        val papel = gerenciadorSessao.obterPapelUsuario()

        if (papel.isNullOrEmpty()) {
            Toast.makeText(this, "Papel do usuário não definido.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = when (papel.lowercase()) {
            "motorista" -> Intent(this, LoginMotoristaActivity::class.java)
            "administrador" -> Intent(this, LoginAdminActivity::class.java)
            else -> {
                Toast.makeText(this, "Papel do usuário inválido: $papel", Toast.LENGTH_SHORT).show()
                null
            }
        }

        intent?.let {
            startActivity(it)
            finish()
        }
    }

    // Mostra um alerta informando que a permissão de localização foi negada
    private fun mostrarPermissaoNegada() {
        AlertDialog.Builder(this)
            .setTitle("Permissão de Localização Necessária")
            .setMessage("Você desativou a permissão de localização. Para utilizar o app, ative a localização nas configurações.")
            .setPositiveButton("Configurações") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun PreviewTelaInicial() {
        FilaMotoristaTheme {
            TelaInicial(
                onMotoristaSelected = { println("Simulação: Motorista selecionado") },
                onAdminSelected = { println("Simulação: Administrador selecionado") }
            )
        }
    }

    // Exibe a tela inicial para seleção de tipo de usuário
    @Composable
    fun ExibirTelaInicial() {
        TelaInicial(
            onMotoristaSelected = {
                gerenciadorSessao.salvarPapelUsuario("motorista")
                gerenciadorAnalise.registrarEvento(
                    evento = "selecao_usuario",
                    itemId = "motorista",
                    descricao = "Usuário selecionou o papel de motorista"
                )
                redirecionarParaLogin()
            },
            onAdminSelected = {
                verificarPinAdministrador()
            }
        )
    }
   //redireciona para a tela de login
    private fun redirecionarParaLogin() {
        val intent = Intent(this, LoginMotoristaActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Verifica se o PIN de administrador já existe e direciona para a tela correta
    private fun verificarPinAdministrador() {
        val intent = Intent(this, VerificarPinActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Composable TelaInicial para mostrar as opções "Sou Motorista" e "Sou Administrador"
    @Composable
    fun TelaInicial(
        onMotoristaSelected: () -> Unit,
        onAdminSelected: () -> Unit,
        exibirMensagem: ((String) -> Unit)? = null // Função opcional para exibir mensagens
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                exibirMensagem?.invoke("Bem-vindo, Administrador")
                onAdminSelected()
            }) {
                Text(text = "Sou Administrador")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                exibirMensagem?.invoke("Bem-vindo, Motorista")
                onMotoristaSelected()
            }) {
                Text(text = "Sou Motorista")
            }
        }
    }

}
