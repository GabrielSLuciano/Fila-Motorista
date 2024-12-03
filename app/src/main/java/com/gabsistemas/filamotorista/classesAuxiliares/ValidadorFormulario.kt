package com.gabsistemas.filamotorista.classesAuxiliares

import android.content.Context
import android.widget.Toast

class ValidadorFormulario(private val context: Context) {

    /**
     * Função para validar os campos do formulário de cadastro.
     * Cada parâmetro é opcional, permitindo flexibilidade para atender diferentes formulários.
     *
     * @param nome Nome do usuário.
     * @param email Email do usuário.
     * @param senha Senha do usuário.
     * @param confirmarSenha Confirmação da senha.
     * @param placa Placa do veículo.
     * @return `true` se o formulário for válido, `false` caso contrário.
     */
    fun validarFormulario(
        nome: String? = null,
        email: String? = null,
        senha: String? = null,
        confirmarSenha: String? = null,
        placa: String? = null
    ): Boolean {
        val senhaRegex = Regex("^(?=.*[A-Z])(?=.*[!@#\$%^&*()_+=-]).{6,}\$")
        val placaRegex = Regex("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}\$") // Formato ABC1D23

        return when {
            nome != null && nome.isEmpty() -> {
                Toast.makeText(context, "Por favor, insira seu nome.", Toast.LENGTH_SHORT).show()
                false
            }
            email != null && email.isEmpty() -> {
                Toast.makeText(context, "Por favor, insira um email.", Toast.LENGTH_SHORT).show()
                false
            }
            senha != null && senha.isEmpty() -> {
                Toast.makeText(context, "Por favor, insira uma senha.", Toast.LENGTH_SHORT).show()
                false
            }
            senha != null && !senha.matches(senhaRegex) -> {
                Toast.makeText(context, "A senha deve ter pelo menos 6 caracteres, uma letra maiúscula e um caractere especial.", Toast.LENGTH_SHORT).show()
                false
            }
            senha != null && confirmarSenha != null && senha != confirmarSenha -> {
                Toast.makeText(context, "As senhas devem ser iguais.", Toast.LENGTH_SHORT).show()
                false
            }
            placa != null && placa.isEmpty() -> {
                Toast.makeText(context, "Por favor, insira a placa do veículo.", Toast.LENGTH_SHORT).show()
                false
            }
            placa != null && !placa.matches(placaRegex) -> {
                Toast.makeText(context, "Formato da placa inválido. Exemplo: ABC1D23", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}
