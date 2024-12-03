package com.gabsistemas.filamotorista.motorista


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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAutenticacao
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorAnalise
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorListaMotorista
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorDadosUsuario
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gabsistemas.filamotorista.classesAuxiliares.GerenciadorLocalizacaoPermissao
import com.gabsistemas.filamotorista.ui.components.QuadroUltimaSenhaChamada
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*


class MotoristaActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var gerenciadorAnalise: GerenciadorAnalise
    private lateinit var gerenciadorDadosUsuario: GerenciadorDadosUsuario
    private lateinit var gerenciadorListaMotorista: GerenciadorListaMotorista
    private lateinit var gerenciadorLocalizacaoPermissao: GerenciadorLocalizacaoPermissao

    private var nomeMotorista by mutableStateOf("")
    private var placaVeiculo by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        //limpar o cache do firestore
        FirebaseFirestore.getInstance().clearPersistence()

        // Outras configurações e inicializações
        gerenciadorDadosUsuario = GerenciadorDadosUsuario(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        gerenciadorAnalise = GerenciadorAnalise(firebaseAnalytics)
        gerenciadorListaMotorista =
            GerenciadorListaMotorista(context = this, firestore = firestore)
        val gerenciadorAutenticacao = GerenciadorAutenticacao(context = this)
        gerenciadorLocalizacaoPermissao = GerenciadorLocalizacaoPermissao(
            context = this,
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        )

         // Configurar atualização da lista de motoristas
        configurarAtualizacaoListaMotoristas()

        // Buscar dados do motorista
        buscarDadosMotorista()
        // Define o conteúdo da tela usando Composable
        setContent {
            FilaMotoristaTheme {

                val motoristasState by gerenciadorListaMotorista.motoristas.collectAsState(emptyList())
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                var possuiSenha by remember { mutableStateOf(false) }
                var rota by remember { mutableStateOf("") }
                var errorMessage by remember { mutableStateOf("") }

                LaunchedEffect(motoristasState) {
                    if (userId != null) {
                        possuiSenha = motoristasState.any { it["userId"] == userId }
                    }
                }

                TelaMotorista(
                    gerenciadorListaMotorista = gerenciadorListaMotorista,
                    motoristas = motoristasState,
                    possuiSenha = possuiSenha,
                    nome = nomeMotorista,
                    placa = placaVeiculo,
                    rota = rota,
                    onRotaChange = { input ->
                        rota = input.replace(Regex("[^A-Z0-9-]"), "").uppercase()
                        errorMessage = if (!Regex("^[A-Z]-\\d{1,3}$").matches(rota)) {
                            "Formato inválido. Use o formato A-1, J-1000."
                        } else {
                            ""
                        }
                    },
                    errorMessage = errorMessage,
                    onGerarSenhaClick = { inputRota ->
                        gerarSenha(nomeMotorista, inputRota)
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
                    }
                )
            }
        }
    }

    private fun buscarDadosMotorista() {
        gerenciadorDadosUsuario.buscarDadosMotorista { nome, placa ->
            nomeMotorista = nome ?: "Desconhecido"
            placaVeiculo = placa ?: "Sem Placa"
        }
    }

    private fun validarRota(rota: String): Boolean {
        val regex = Regex("^[A-Z]-\\d{1,3}$")
        return regex.matches(rota)
    }

    private fun calcularDistanciaHaversine(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val raioTerraMetros = 6371e3 // Raio da Terra em metros
        val deltaLatitude = (latitude2 - latitude1).toRadians()
        val deltaLongitude = (longitude2 - longitude1).toRadians()
        val latitude1Radianos = latitude1.toRadians()
        val latitude2Radianos = latitude2.toRadians()

        Log.d("Haversine", "Delta Latitude: $deltaLatitude, Delta Longitude: $deltaLongitude")
        Log.d("Haversine", "Latitude 1: $latitude1Radianos, Latitude 2: $latitude2Radianos")

        val a = sin(deltaLatitude / 2).pow(2) +
                cos(latitude1Radianos) * cos(latitude2Radianos) *
                sin(deltaLongitude / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distancia = raioTerraMetros * c
        Log.d("Haversine", "Distância Calculada: $distancia metros")
        return distancia
    }

    // Extensão para converter graus em radianos
    private fun Double.toRadians(): Double = Math.toRadians(this)

    private fun redirecionarParaLogin() {
        val intent = Intent(this, LoginMotoristaActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun verificarDistanciaMotorista(
        motoristaLat: Double,
        motoristaLon: Double,
        callback: (Boolean) -> Unit
    ) {
        firestore.collection("configuracoesGlobais").document("estadoGlobal")
            .get()
            .addOnSuccessListener { document ->
                val referencia = document.get("localizacaoReferencia") as? Map<*, *>
                val hubLat = referencia?.get("latitude") as? Double ?: run {
                    Toast.makeText(
                        this,
                        "Localização de referência não definida.",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback(false)
                    return@addOnSuccessListener
                }
                val hubLon = referencia["longitude"] as? Double ?: run {
                    Toast.makeText(
                        this,
                        "Localização de referência não definida.",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback(false)
                    return@addOnSuccessListener
                }

                val distancia =
                    calcularDistanciaHaversine(motoristaLat, motoristaLon, hubLat, hubLon)
                callback(distancia <= 100)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao verificar localização de referência: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                callback(false)
            }
    }

    private fun gerarSenha(nome: String, rota: String) {
        if (!validarRota(rota)) {
            Toast.makeText(
                this,
                "Por favor, insira um código de rota no formato A-1, J-1000.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Verifica a localização e se o motorista está no hub
        gerenciadorLocalizacaoPermissao.acessarLocalizacao(
            onSucesso = { latitude, longitude ->
                verificarDistanciaMotorista(latitude, longitude) { dentroDoHub ->
                    if (dentroDoHub) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT)
                                .show()
                            return@verificarDistanciaMotorista
                        }

                        firestore.collection("filaMotorista")
                            .whereEqualTo("userId", userId)
                            .whereIn("status", listOf("aguardando", "chamado"))
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (querySnapshot.isEmpty) {
                                    // Inicia transação para gerar senha e inserir na lista
                                    firestore.runTransaction { transaction ->
                                        // Referência para o contador de senhas
                                        val controleRef =
                                            firestore.collection("controleSenha")
                                                .document("contador")
                                        val snapshot = transaction.get(controleRef)

                                        // Gera nova senha
                                        val ultimaSenha = snapshot.getLong("ultimaSenha") ?: 0
                                        val novaSenha =
                                            if (ultimaSenha < 1000) ultimaSenha + 1 else 1

                                        // Atualiza o contador de senhas
                                        transaction.update(controleRef, "ultimaSenha", novaSenha)

                                        // Dados do motorista
                                        val motoristaData = hashMapOf(
                                            "nome" to nome,
                                            "placa" to placaVeiculo,
                                            "rota" to rota,
                                            "senha" to novaSenha.toString(),
                                            "status" to "aguardando",
                                            "userId" to userId,
                                            "email" to FirebaseAuth.getInstance().currentUser?.email,
                                            "box" to "00"
                                        )

                                        // Insere o motorista na coleção filaMotorista
                                        transaction.set(
                                            firestore.collection("filaMotorista")
                                                .document(), // Cria um novo documento
                                            motoristaData
                                        )
                                    }.addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Senha gerada com sucesso! Você foi adicionado à lista.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Erro ao gerar senha: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Você já possui uma senha ativa na fila.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Erro ao verificar a lista de motoristas: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Você deve estar no Hub para gerar uma senha.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onFalha = {
                Toast.makeText(
                    this,
                    "Não foi possível acessar sua localização.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }


    private fun configurarAtualizacaoListaMotoristas() {
        firestore.collection("filaMotorista")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MotoristaActivity", "Erro ao ouvir atualizações: ", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val motoristasAtualizados = snapshot.documents.map { document ->
                        mapOf(
                            "nome" to document.getString("nome").orEmpty(),
                            "placa" to document.getString("placa").orEmpty(),
                            "rota" to document.getString("rota").orEmpty(),
                            "senha" to document.getString("senha").orEmpty(),
                            "status" to document.getString("status").orEmpty()
                        )
                    }
                    gerenciadorListaMotorista.atualizarMotoristas(motoristasAtualizados)
                }
            }
    }
}

//Layout da tela do Motorista
@Composable
fun TelaMotorista(
    gerenciadorListaMotorista: GerenciadorListaMotorista,
    motoristas: List<Map<String, Any>>,
    possuiSenha: Boolean,
    nome: String,
    placa: String,
    rota: String,
    onRotaChange: (String) -> Unit,
    errorMessage: String,
    onGerarSenhaClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    // Coleta o estado da última senha chamada em tempo real
    val ultimaSenha by gerenciadorListaMotorista.ultimaSenhaState.collectAsState()

    // Gerenciar altura dinâmica da lista
    val itemHeightDp = 56.dp
    val initialHeightDp = itemHeightDp * 4
    val minHeightDp = itemHeightDp * 3
    val maxHeightDp = 600.dp
    var alturaLista by remember { mutableStateOf(initialHeightDp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (!possuiSenha) Color.Black.copy(alpha = 0.6f) else Color.Transparent) // Fundo escurecido quando sem senha
            .padding(16.dp)
    ) {

        // Quadro Gerar Senha sempre visível no centro antes da geração de senha
        if (!possuiSenha) {
            QuadroGerarSenha(
                nome = nome,
                placa = placa,
                rota = rota,
                onRotaChange = onRotaChange,
                errorMessage = errorMessage,
                onGerarSenhaClick = onGerarSenhaClick,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
            )
        }

        // Exibir informações secundárias apenas após a geração da senha
        if (possuiSenha) {
            // Título
            Text(
                text = "Motorista",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Quadro da última senha chamada
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                QuadroUltimaSenhaChamada(
                    senha = ultimaSenha["senha"] ?: "N/A",
                    nome = ultimaSenha["nome"] ?: "N/A",
                    rota = ultimaSenha["rota"] ?: "N/A",
                    placa = ultimaSenha["placa"] ?: "N/A",
                    box = ultimaSenha["box"] ?: "N/A"

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
                                            (alturaLista - delta.dp).coerceIn(
                                                minHeightDp,
                                                maxHeightDp
                                            )
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
        }

            // Ícone de configurações aparece apenas após a geração de senha
        IconButton(
            onClick = { mostrarMenu = !mostrarMenu },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp)
        ) {
            Icon(
                imageVector = if (mostrarMenu) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = if (mostrarMenu) "Fechar Configurações" else "Abrir Configurações",
                tint = Color(0xFFEA772E) // laranja
            )
        }
    }

    // Mostrar Menu de Configurações
    if (mostrarMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            MenuConfiguracoes(
                onLogoutClick = onLogoutClick,
                onExcluirContaClick = {},
                onDismiss = { mostrarMenu = false },
            )
        }
    }
}


// layout do menu de configurações
@Composable
fun MenuConfiguracoes(
    onLogoutClick: () -> Unit,
    onExcluirContaClick: () -> Unit,
    onDismiss: () -> Unit,
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
fun QuadroGerarSenha(
    nome: String,
    placa: String,
    rota: String,
    onRotaChange: (String) -> Unit,
    errorMessage: String,
    onGerarSenhaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier // Use o Modifier passado
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Campo Nome
            TextField(
                value = nome,
                onValueChange = {},
                label = { Text("Nome") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Placa
            TextField(
                value = placa,
                onValueChange = {},
                label = { Text("Placa") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Código da Rota
            TextField(
                value = rota, // Exibe o estado atual do valor da rota
                onValueChange = { novoValor ->
                    onRotaChange(novoValor) // Atualiza o estado através do callback
                },
                label = { Text("Código da Rota (Ex: A-1, J-14)") },
                isError = errorMessage.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                )
            )


            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Gerar Senha
            Button(
                onClick = { onGerarSenhaClick(rota) },
                modifier = Modifier.fillMaxWidth(),
                enabled = errorMessage.isEmpty()
            ) {
                Text("Gerar Senha")
            }
        }
    }
}


//Visualização dos layouts


// menu de configurações
@Preview(showBackground = true)
@Composable
fun PreviewMenuConfiguracoes() {
    FilaMotoristaTheme {
        MenuConfiguracoes(
            onLogoutClick = { /* Simula logout */ },
            onExcluirContaClick = { /* Simula exclusão de conta */ },
            onDismiss = { /* Simula fechamento do menu */ }
        )
    }
}

// Quadro Gerar Senha
@Preview(showBackground = true)
@Composable
fun PreviewQuadroGerarSenha() {
    FilaMotoristaTheme {
        QuadroGerarSenha(
            nome = "João Silva",
            placa = "XYZ-1234",
            rota = "A-1",
            onRotaChange = { /* Atualiza o valor da rota */ },
            errorMessage = "",
            onGerarSenhaClick = { /* Simula geração de senha */ },
            modifier = Modifier.padding(16.dp)
        )
    }
}