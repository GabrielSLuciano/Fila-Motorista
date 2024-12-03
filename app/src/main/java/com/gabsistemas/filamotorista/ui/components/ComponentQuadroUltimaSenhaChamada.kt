package com.gabsistemas.filamotorista.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.gabsistemas.filamotorista.ui.theme.FilaMotoristaTheme

//layout do quadro de senha
@Composable
fun QuadroUltimaSenhaChamada(
    senha: String,
    nome: String,
    rota: String,
    placa: String,
    box: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-16).dp) // Move para cima
            .padding(vertical = 16.dp)
            .heightIn(min = 150.dp) // Altura mínima
            .padding(4.dp), // Adiciona espaçamento externo
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(2.dp, Color.Black), // Borda preta
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally // Centraliza o conteúdo no eixo horizontal
        ) {
            // Texto "Senha: 001" centralizado
            Text(
                text = "Senha: $senha",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 40.sp // Tamanho grande do número
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Nome, Código da Rota e Box alinhados horizontalmente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center // Centraliza os textos na linha
            ) {
                Text(
                    text = nome,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp) // Pequeno espaçamento à direita
                )
                Text(
                    text = rota,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp) // Espaçamento entre rota e box
                )
                Text(
                    text = "Box: $box", // Adiciona "Box:" antes do número
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Placa centralizada
            Text(
                text = placa,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuadroUltimaSenhaChamada() {
    FilaMotoristaTheme {
        QuadroUltimaSenhaChamada(
            senha = "001",
            nome = "João Silva",
            rota = "A-1",
            placa = "XYZ-1234",
            box = "00"
        )
    }
}


