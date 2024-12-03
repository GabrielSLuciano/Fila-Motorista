package com.gabsistemas.filamotorista.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAnalise
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAutenticacao
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorDadosUsuario
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorListaMotorista
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorLocalizacaoPermissao
import com.gabsistemas.filamotorista.ui.components.QuadroUltimaSenhaChamada
import com.gabsistemas.filamotorista.ui.theme.ButtonBlue
import com.gabsistemas.filamotorista.ui.theme.ButtonGreen
import com.gabsistemas.filamotorista.ui.theme.ButtonRed
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow


class AdminActivity : ComponentActivity() {

    private lateinit var gerenciadorAutenticacao: GerenciadorAutenticacao
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var gerenciadorAnalise: GerenciadorAnalise
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val motoristasState = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    private var localizacaoAtivada by mutableStateOf(false)
    private var isMasterAdmin by mutableStateOf(false)
    private lateinit var gerenciadorDadosUsuario: GerenciadorDadosUsuario
    private lateinit var gerenciadorListaMotorista: GerenciadorListaMotorista


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //limpar o cache do firestore
         FirebaseFirestore.getInstance().clearPersistence()
        // Verificar autenticação
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado. Redirecionando para login.", Toast.LENGTH_SHORT).show()
            redirecionarParaLogin()
            return
        }
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        gerenciadorAnalise = GerenciadorAnalise(firebaseAnalytics)
        gerenciadorListaMotorista = GerenciadorListaMotorista(context = this, firestore = firestore)
        gerenciadorDadosUsuario = GerenciadorDadosUsuario(this)
        gerenciadorAutenticacao = GerenciadorAutenticacao(context = this, firestore = firestore)
        // Verificar se o usuário é MasterAdmin

        verificarSeMasterAdmin { isMasterAdmin ->
            if (isMasterAdmin) {
                Toast.makeText(this, "Você é o MasterAdmin.", Toast.LENGTH_SHORT).show()
            }
        }
          // Observar última senha chamada
        gerenciadorListaMotorista.observarUltimaSenhaChamada()
         // Carregar o nome do administrador logado
        carregarNomeAdministrador()
        // Carregar estado global ao iniciar a Activity
        carregarEstadoGlobal()
        //observar, parar a lista de motoristas
        observarListaMotoristas()

        setContent {
            FilaMotoristaTheme {
                val motoristas by motoristasState.collectAsState()
                val ultimaSenha by gerenciadorListaMotorista.ultimaSenhaState.collectAsState()// Observa o estado da última senha

                // Verificar diretamente os dados recebidos
                LaunchedEffect(ultimaSenha) {
                    Log.d("Compose", "Dados do quadro: $ultimaSenha")
                }

                var mostrarDialogoExclusao by remember { mutableStateOf(false) }

                // Tela do administrador
                TelaAdmin(
                    motoristas = motoristas,
                    ultimaSenha = ultimaSenha,
                    isLocalizacaoAtivada = localizacaoAtivada,
                    onAtivarLocalizacao = { ativar ->
                        localizacaoAtivada = ativar
                        atualizarLocalizacaoAtivada(ativar)
                        Toast.makeText(
                            this,
                            if (ativar) "Localização de referência ativada" else "Localização de referência desativada",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDefinirLocalizacao = {
                        definirLocalizacaoReferencia()
                    },
                    onLogoutClick = {
                        gerenciadorAnalise.definirEstadoLogin(logado = false)
                        gerenciadorAutenticacao.deslogarUsuario()
                        Toast.makeText(
                            this,
                            "Logout realizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        redirecionarParaLogin()
                    },
                    onExcluirContaClick = {
                        mostrarDialogoExclusao = true
                    },
                    isMasterAdmin = isMasterAdmin,

                    onLimparClick = {
                        gerenciadorListaMotorista.limparListaMotoristas {
                            Toast.makeText(this, "Operação concluída!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExcluirMotorista = { senha ->
                        gerenciadorListaMotorista.excluirMotoristaPorSenha(senha) { _, mensagem ->
                            Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onChamarMotoristas = { quantidade ->
                        if (quantidade <= 0) {
                            Toast.makeText(this, "Quantidade inválida.", Toast.LENGTH_SHORT).show()
                            return@TelaAdmin
                        }

                        gerenciadorListaMotorista.chamarMotoristasPorQuantidade(quantidade) { sucesso, mensagem ->
                            Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
                            if (sucesso) {
                                // Atualização já gerenciada no estado de última senha
                                Log.d("AdminActivity", "Motoristas chamados com sucesso: quantidade=$quantidade")
                            }
                        }
                    }
                )

                // Confirmação para exclusão da conta
                if (mostrarDialogoExclusao) {
                    ConfirmacaoDialogo(
                        titulo = "Excluir Conta",
                        mensagem = "Deseja realmente excluir a conta?",
                        onConfirmar = {
                            mostrarDialogoExclusao = false
                            excluirContaAdministrador()
                        },
                        onCancelar = {
                            mostrarDialogoExclusao = false
                        }
                    )
                }
            }
        }
    }

    private fun carregarNomeAdministrador() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        gerenciadorDadosUsuario.obterNomeUsuario(userId) { nome ->
            if (nome != null) {
                gerenciadorDadosUsuario.salvarNomeUsuario(nome)
                Log.d("AdminActivity", "Nome do administrador carregado: $nome")
            } else {
                Toast.makeText(this, "Erro ao carregar nome do administrador.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observarListaMotoristas() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("Firestore", "Usuário não autenticado. Não é possível observar lista de motoristas.")
            return
        }
        firestore.collection("filaMotorista")
            .addSnapshotListener { snapshot, error ->
                if (isExcluindoConta) return@addSnapshotListener // Evita observação durante exclusão

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Erro ao carregar lista: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listaAtualizada = snapshot.documents.map { it.data ?: emptyMap() }
                    motoristasState.value = listaAtualizada
                }
            }
    }
    private fun atualizarLocalizacaoAtivada(ativada: Boolean) {
        val adminNome = gerenciadorDadosUsuario.nomeUsuario ?: "Administrador"

        val atualizacao = mapOf(
            "localizacaoAtivada" to ativada,
            "ultimoResponsavel" to adminNome,
            "ultimaAtualizacao" to System.currentTimeMillis()
        )

        firestore.collection("configuracoesGlobais").document("estadoGlobal")
            .set(atualizacao, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Localização de referência ${if (ativada) "ativada" else "desativada"}.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao atualizar estado: ${e.message}")
                Toast.makeText(this, "Erro ao alterar o estado.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarEstadoGlobal() {
        FirebaseFirestore.getInstance()
            .collection("configuracoesGlobais")
            .document("estadoGlobal")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    localizacaoAtivada = document.getBoolean("localizacaoAtivada") ?: false
                    val referencia = document.get("localizacaoReferencia") as? Map<*, *>
                    val latitude = referencia?.get("latitude") as? Double
                    val longitude = referencia?.get("longitude") as? Double

                    if (latitude != null && longitude != null) {
                        Log.d("Firestore", "Localização de referência carregada: $latitude, $longitude")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao carregar estado global: ${e.message}")
                Toast.makeText(this, "Erro ao carregar estado global.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun verificarSeMasterAdmin(onResultado: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onResultado(false) // Retorna false caso o usuário não esteja autenticado
            return
        }

        firestore.collection("administradores").document(userId)
            .get()
            .addOnSuccessListener { documento ->
                val isMasterAdmin = documento.getBoolean("isMasterAdmin") ?: false
                this.isMasterAdmin = isMasterAdmin // Atualiza o estado da classe
                onResultado(isMasterAdmin)
            }
            .addOnFailureListener {
                onResultado(false)
                Toast.makeText(
                    this,
                    "Erro ao verificar permissões. Tente novamente mais tarde.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun definirLocalizacaoReferencia() {
        verificarSeMasterAdmin { isMasterAdmin ->
            if (isMasterAdmin) {
                val gerenciador = GerenciadorLocalizacaoPermissao(
                    context = this,
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                )

                gerenciador.definirLocalizacaoReferencia(
                    onSucesso = { latitude, longitude ->
                        gerenciadorDadosUsuario.atualizarLocalizacaoReferencia(latitude, longitude) { sucesso ->
                            if (sucesso) {
                                Toast.makeText(
                                    this,
                                    "Localização de referência definida com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Erro ao salvar a localização de referência.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onFalha = { mensagemErro ->
                        Toast.makeText(this, mensagemErro, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(
                    this,
                    "Apenas o MasterAdmin pode configurar a localização de referência.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun excluirContaAdministrador() {
        isExcluindoConta = true // Evita observação contínua no Firestore
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // Primeiro, remover os dados do Firestore
        firestore.collection("administradores").document(user.uid)
            .delete()
            .addOnSuccessListener {
                // Após excluir os dados no Firestore, excluir a conta no Authentication
                user.delete()
                    .addOnSuccessListener {
                        // Limpar os dados de login salvos
                        limparDadosDeLogin()

                        // Mostrar mensagem clara de sucesso
                        Toast.makeText(
                            this,
                            "Conta excluída com sucesso. Você pode criar uma nova conta ou fazer login com outra conta.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirecionar para a tela de login
                        val intent = Intent(this, LoginAdminActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Erro ao excluir conta do Firebase Authentication: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao excluir dados do Firestore: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    private var isExcluindoConta = false

    private fun redirecionarParaLogin() {
        val intent = Intent(this, LoginAdminActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Limpar os dados salvos localmente no dispositivo
    private fun limparDadosDeLogin() {
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Log.d("AdminActivity", "Dados de login limpos.")
    }

}

// Confirmação da exclusão de conta
    @Composable
    fun ConfirmacaoDialogo(
        titulo: String,
        mensagem: String,
        onConfirmar: () -> Unit,
        onCancelar: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { onCancelar() },
            title = { Text(text = titulo) },
            text = { Text(text = mensagem) },
            confirmButton = {
                TextButton(onClick = onConfirmar) {
                    Text("Sim", color = Color.Red) // Botão de confirmação
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelar) {
                    Text("Não") // Botão de cancelar
                }
            }
        )
    }

    // Tela do administrador
    @Composable
    fun TelaAdmin(
        motoristas: List<Map<String, Any>>,
        isLocalizacaoAtivada: Boolean,
        ultimaSenha: Map<String, String>,
        onAtivarLocalizacao: (Boolean) -> Unit,
        onDefinirLocalizacao: () -> Unit,
        onLogoutClick: () -> Unit,
        onExcluirContaClick: () -> Unit,
        isMasterAdmin: Boolean,
        onLimparClick: () -> Unit,
        onExcluirMotorista: (String) -> Unit,
        onChamarMotoristas: (Int) -> Unit
    ) {

        var mostrarDialogoChamar by remember { mutableStateOf(false) }
        var mostrarDialogoExcluir by remember { mutableStateOf(false) }
        var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }
        var senhaParaExcluir by remember { mutableStateOf("") }
        var mensagemExclusao by remember { mutableStateOf("") }
        var mostrarConfirmacaoLimpar by remember { mutableStateOf(false) }
        var erroSenhaVazia by remember { mutableStateOf(false) }

        // Estado para mostrar ou ocultar o menu
        var mostrarMenu by remember { mutableStateOf(false) }

        // Altura inicial para mostrar 4 a 5 itens da lista
        val itemHeightDp = 56.dp // Altura média estimada por item
        val initialHeightDp = itemHeightDp * 4 // Altura inicial para 4 itens
        val minHeightDp = itemHeightDp * 3 // Altura mínima (3 itens)
        val maxHeightDp = 500.dp // Altura máxima

        // Gerenciar altura dinâmica da lista
        var alturaLista by remember { mutableStateOf(initialHeightDp) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Título
            Text(
                text = "Administrador",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Quadro da última senha chamada
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp) // Espaçamento abaixo do título
            ) {
                QuadroUltimaSenhaChamada(
                    senha = ultimaSenha["senha"] ?: "N/A",
                    nome = ultimaSenha["nome"] ?: "N/A",
                    rota = ultimaSenha["rota"] ?: "N/A",
                    placa = ultimaSenha["placa"] ?: "N/A",
                    box = ultimaSenha["box"] ?: "N/A"
                )

                // Barra de botões
                BarraDeBotoes(
                    onChamarClick = { mostrarDialogoChamar = true },
                    onExcluirClick = { mostrarDialogoExcluir = true },
                    onLimparClick = { mostrarConfirmacaoLimpar = true }
                )

                if (mostrarDialogoChamar) {
                    ChamarMotoristasDialog(
                        onDismiss = { mostrarDialogoChamar = false },
                        onConfirm = { quantidade ->
                            onChamarMotoristas(quantidade)
                            mostrarDialogoChamar = false
                        }
                    )
                }
            }
            // Excluir motorista
            if (mostrarDialogoExcluir) {
                ExcluirMotoristaDialog(
                    onDismiss = { mostrarDialogoExcluir = false },
                    onConfirm = { senha ->
                        if (senha.isBlank()) {
                            erroSenhaVazia = true // Ativa o estado de erro
                        } else {
                            erroSenhaVazia = false // Reseta o estado de erro
                            mostrarDialogoExcluir = false
                            senhaParaExcluir = senha
                            mostrarConfirmacaoExclusao = true // Abre o diálogo de confirmação
                        }
                    },
                    erroSenhaVazia = erroSenhaVazia // Passa o estado de erro
                )
            }

// Diálogo de confirmação de exclusão
            if (mostrarConfirmacaoExclusao) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmacaoExclusao = false },
                    title = { Text("Confirmação") },
                    text = {
                        Text("Você tem certeza que deseja excluir a senha \"$senhaParaExcluir\"?")
                    },
                    confirmButton = {
                        Button(onClick = {
                            mostrarConfirmacaoExclusao = false
                            onExcluirMotorista(senhaParaExcluir)
                            mensagemExclusao = "Motorista com a senha $senhaParaExcluir foi excluído com sucesso."
                        }) {
                            Text("Sim")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { mostrarConfirmacaoExclusao = false }) {
                            Text("Não")
                        }
                    }
                )
            }

             // Exibição de mensagens de exclusão
            if (mensagemExclusao.isNotEmpty()) {
                Text(
                    text = mensagemExclusao,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                )
            }
               // Diálogo de confirmação para limpar lista
            if (mostrarConfirmacaoLimpar) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmacaoLimpar = false },
                    title = { Text("Confirmação") },
                    text = {
                        Text("Você tem certeza que deseja limpar toda a lista de motoristas?")
                    },
                    confirmButton = {
                        Button(onClick = {
                            mostrarConfirmacaoLimpar = false
                            onLimparClick() // Executa a limpeza
                        }) {
                            Text("Sim")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { mostrarConfirmacaoLimpar = false }) {
                            Text("Não")
                        }
                    }
                )
            }


            // Ícone de configurações
            IconButton(
                onClick = { mostrarMenu = !mostrarMenu }, // Alterna o estado do menu
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 30.dp)
            ) {
                Icon(
                    imageVector = if (mostrarMenu) Icons.Default.Close else Icons.Default.Settings, // Mostra ícone de fechar se o menu estiver aberto
                    contentDescription = if (mostrarMenu) "Fechar Configurações" else "Abrir Configurações",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Lista de motoristas com cabeçalho arrastável
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Cabeçalho da lista com funcionalidade de arrastar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        alturaLista =
                                            (alturaLista - delta.dp).coerceIn(minHeightDp, maxHeightDp)
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Nome", Modifier.weight(1f))
                                Text("Senha", Modifier.weight(1f))
                                Text("Status", Modifier.weight(1f))
                            }
                        }

                        // Conteúdo da lista que expande ou contrai com base na altura ajustada
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(alturaLista)
                                .padding(8.dp)
                        ) {
                            items(motoristas) { motorista ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = motorista["nome"].toString(),
                                            Modifier.weight(1f)
                                        )
                                        Text(
                                            text = motorista["senha"].toString(),
                                            Modifier.weight(1f)
                                        )
                                        Text(
                                            text = motorista["status"].toString(),
                                            Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // Mostrar Menu de Configurações
            if (mostrarMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White) // Fundo branco cobrindo  a tela
                ) {
                    MenuConfiguracoes(
                        isLocalizacaoAtivada = isLocalizacaoAtivada,
                        onAtivarLocalizacao = onAtivarLocalizacao,
                        onDefinirLocalizacao = onDefinirLocalizacao,
                        onLogoutClick = onLogoutClick,
                        onExcluirContaClick = onExcluirContaClick,
                        onDismiss = { mostrarMenu = false },
                        isMasterAdmin = isMasterAdmin
                    )
                }
            }
        }
    }

//Diálogo e tratamento de exceção para exclusão de senha
@Composable
fun ExcluirMotoristaDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    erroSenhaVazia: Boolean
) {
    var senhaParaExcluir by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Excluir Motorista") },
        text = {
            Column {
                if (erroSenhaVazia) {
                    Text(
                        text = "Você deve inserir uma senha válida.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = senhaParaExcluir,
                    onValueChange = { senhaParaExcluir = it },
                    label = { Text("Senha") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(senhaParaExcluir) }) {
                Text("Excluir")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}

//conjunto de botões "Excluir" "Limpar" "Chamar"
    @Composable
    fun BarraDeBotoes(
        onChamarClick: () -> Unit,
        onExcluirClick: () -> Unit,
        onLimparClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp) // Ajuste no espaçamento vertical
                .offset(y = (-16).dp)
        ) {
            // Linha com Excluir e Limpar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onExcluirClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonRed), // Vermelho
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Excluir")
                }

                Button(
                    onClick = onLimparClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue), // Azul
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Limpar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão Chamar abaixo
            Button(
                onClick = onChamarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp), // Define altura consistente
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen), // Verde
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Chamar",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // layout do menu de configurações
    @Composable
    fun MenuConfiguracoes(
        isLocalizacaoAtivada: Boolean,
        onAtivarLocalizacao: (Boolean) -> Unit,
        onDefinirLocalizacao: () -> Unit,
        onLogoutClick: () -> Unit,
        onExcluirContaClick: () -> Unit,
        onDismiss: () -> Unit,
        isMasterAdmin: Boolean
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onDismiss() } // Fecha o menu ao clicar fora
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) {}, // Evita o fechamento ao clicar no menu
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Ativar/Desativar Localização de Referência
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ativar Localização de Referência", Modifier.weight(1f))
                    Switch(
                        checked = isLocalizacaoAtivada,
                        onCheckedChange = { onAtivarLocalizacao(it) }
                    )
                }

                // Botão para Definir Localização Atual
                Button(
                    onClick = onDefinirLocalizacao,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isMasterAdmin, // Apenas MasterAdmin pode definir
                ) {
                    Text("Definir Localização Atual")
                }

                // Botão de Logout
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 25.dp),
                ) {
                    Text("Sair da conta")
                }

                Button(
                    onClick = onExcluirContaClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp), // Espaçamento superior ajustado
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir Conta")
                }

                Spacer(modifier = Modifier.height(16.dp)) // Espaçamento entre os itens
                TextButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .padding(top = 50.dp)
                ) {
                    Text(
                        text = "Fechar Menu",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp, // Aumenta o tamanho da fonte
                            fontWeight = FontWeight.Bold // Destaca o texto
                        )
                    )
                }
            }
        }
    }

@Composable
fun ChamarMotoristasDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantidadeMotoristas by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Chamar Motoristas") },
        text = {
            Column {
                Text("Digite a quantidade de motoristas para chamar (1 a 20):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantidadeMotoristas,
                    onValueChange = { quantidadeMotoristas = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantidade") },
                    placeholder = { Text("Ex.: 5") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val quantidade = quantidadeMotoristas.toIntOrNull() ?: 0
                if (quantidade in 1..20) {
                    onConfirm(quantidade)
                } else {
                    toastMessage = "Digite uma quantidade válida (1 a 20)."
                }
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}

//visualização do layout configurações

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun PreviewModalConfiguracoes() {
        FilaMotoristaTheme {
            MenuConfiguracoes(
                isLocalizacaoAtivada = true,
                onAtivarLocalizacao = { ativado -> println("Localização ativada: $ativado") },
                onDefinirLocalizacao = { println("Definir localização atual") },
                onLogoutClick = { println("Logout realizado") },
                onExcluirContaClick = { println("Conta excluída") },
                onDismiss = { },
                isMasterAdmin = true

            )
        }
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTelaAdmin() {
    FilaMotoristaTheme {
        TelaAdmin(
            motoristas = listOf(
                mapOf(
                    "nome" to "João Silva",
                    "senha" to "001",
                    "status" to "Aguardando",
                    "userId" to "1"
                ),
                mapOf(
                    "nome" to "Maria Oliveira",
                    "senha" to "002",
                    "status" to "Carregando",
                    "userId" to "2"
                ),
                mapOf(
                    "nome" to "Carlos Santos",
                    "senha" to "003",
                    "status" to "Chamado",
                    "userId" to "3"
                )
            ),
            isLocalizacaoAtivada = true,
            ultimaSenha = mapOf(
                "senha" to "003",
                "nome" to "Carlos Santos",
                "rota" to "B12",
                "placa" to "XYZ-1234",
                "box" to "Box 1"
            ),
            onAtivarLocalizacao = { ativar ->
                println("Localização ativada: $ativar")
            },
            onDefinirLocalizacao = {
                println("Definir localização atual")
            },
            onLogoutClick = {
                println("Logout realizado")
            },
            onExcluirContaClick = {
                println("Conta excluída")
            },
            isMasterAdmin = true,
            onLimparClick = {
                println("Lista de motoristas limpa!")
            },
            onExcluirMotorista = { senha ->
                println("Motorista com senha $senha excluído!")
            },
            onChamarMotoristas = { quantidade ->
                println("Chamando $quantidade motoristas")
            }
        )
    }
}





