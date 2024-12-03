package com.gabsistemas.filamotorista.classesAuxiliares

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.gabsistemas.filamotorista.MainActivity
import com.gabsistemas.filamotorista.admin.LoginAdminActivity
import com.gabsistemas.filamotorista.motorista.LoginMotoristaActivity

class GerenciadorNavegacao {

    companion object {

        /**
         * Redireciona para uma nova tela.
         *
         * @param contexto Context da atividade atual.
         * @param destino Classe da atividade de destino.
         * @param limparStack Boolean indicando se deve limpar o back stack.
         * @param finalizarAtividadeAtual Boolean indicando se deve finalizar a atividade atual.
         */
       fun navegarPara(
            contexto: Context,
            destino: Class<out Activity>,
            limparStack: Boolean = false,
            finalizarAtividadeAtual: Boolean = false
        ) {
            val intent = Intent(contexto, destino)
            if (limparStack) {
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            contexto.startActivity(intent)

            // Finaliza a atividade atual, se aplicável
            if (finalizarAtividadeAtual && contexto is Activity) {
                contexto.finish()
            }
        }

        /**
         * Navega para a MainActivity.
         *
         * @param contexto Context da atividade atual.
         */
        fun irParaMainActivity(contexto: Context) {
            navegarPara(contexto, MainActivity::class.java, limparStack = true, finalizarAtividadeAtual = true)
        }

        /**
         * Navega para a LoginMotoristaActivity.
         *
         * @param contexto Context da atividade atual.
         */
        fun irParaLoginMotorista(contexto: Context) {
            navegarPara(contexto, LoginMotoristaActivity::class.java, limparStack = true, finalizarAtividadeAtual = true)
        }

        /**
         * Navega para a LoginAdminActivity.
         *
         * @param contexto Context da atividade atual.
         */
        fun irParaLoginAdmin(contexto: Context) {
            navegarPara(contexto, LoginAdminActivity::class.java, limparStack = true, finalizarAtividadeAtual = true)
        }

        /**
         * Volta para a tela anterior.
         *
         * @param atividade Atividade atual.
         */
        fun voltarParaTelaAnterior(atividade: Activity) {
            atividade.finish()
        }
    }
}
