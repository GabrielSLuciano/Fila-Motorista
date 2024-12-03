package com.gabsistemas.filamotorista.ui.theme


import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.gabsistemas.filamotorista.admin.BarraDeBotoes


val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val ButtonRed = Color(0xFFD92B2B) // Vermelho
val ButtonBlue = Color(0xFF0C39B8) // Azul
val ButtonGreen = Color(0xFF289526) // Verde



@Preview(showBackground = true)
@Composable
fun PreviewBarraDeBotoes() {
    FilaMotoristaTheme {
        BarraDeBotoes(
            onChamarClick = { println("Botão Chamar clicado") },
            onExcluirClick = { println("Botão Excluir clicado") },
            onLimparClick = { println("Botão Limpar clicado") }
        )
    }
}
