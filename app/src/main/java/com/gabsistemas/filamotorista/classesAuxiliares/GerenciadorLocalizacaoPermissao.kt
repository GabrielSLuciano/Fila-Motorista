package com.gabsistemas.filamotorista.classesAuxiliares

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import androidx.activity.ComponentActivity
import android.provider.Settings
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class GerenciadorLocalizacaoPermissao(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {


//caso a localização esteja desativada
    fun mostrarPermissaoNegada() {
        AlertDialog.Builder(context)
            .setTitle("Permissão de Localização Necessária")
            .setMessage("Você desativou a permissão de localização. Para utilizar o app, ative a localização nas configurações.")
            .setPositiveButton("Configurações") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) // Removido o qualificador redundante
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(context, "Permissão negada, o app pode não funcionar corretamente.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    //Verifica se a permissão de localização já foi concedida
    fun verificarPermissaoLocalizacao(
        requestPermissionLauncher: ActivityResultLauncher<String>,
        onConcedida: () -> Unit,
        onNegada: () -> Unit
    ) {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onConcedida()
        } else {
            // Verifica se devemos mostrar uma explicação para o usuário
            if (shouldShowRequestPermissionRationale(context)) {
                mostrarDialogoExplicacaoPermissao {
                    // Após o usuário entender, solicita a permissão
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                // Solicita diretamente a permissão sem explicação
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            onNegada() // Notifica que a permissão foi negada
        }
    }

    // Função para verificar se a permissão foi negada anteriormente e mostrar a explicação
    private fun shouldShowRequestPermissionRationale(context: Context): Boolean {
        return (context as? ComponentActivity)?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            ?: false
    }

    // Mostra um diálogo explicativo para o usuário sobre a necessidade da permissão de localização
    private fun mostrarDialogoExplicacaoPermissao(onPermitir: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Permissão de Localização Necessária")
            .setMessage("Este aplicativo requer acesso à sua localização para funcionar corretamente.")
            .setPositiveButton("Permitir") { dialog, _ ->
                onPermitir()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                Toast.makeText(context, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }


    fun acessarLocalizacao(
        onSucesso: (latitude: Double, longitude: Double) -> Unit,
        onFalha: () -> Unit
    ) {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        onSucesso(it.latitude, it.longitude)
                    } ?: run {
                        onFalha()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Localização", "Erro de segurança ao acessar a localização: ${e.message}")
            onFalha()
        }
    }

    // Mostra um diálogo solicitando que o usuário ative a localização nas configurações do dispositivo
    fun verificarLocalizacaoAtiva(onFalha: () -> Unit) {
        acessarLocalizacao(
            onSucesso = { lat, lon ->
                // A localização foi obtida com sucesso, mas não exibe mensagem na inicialização
                Log.d("VerificarLocalizacao", "Localização obtida: Latitude: $lat, Longitude: $lon")
            },
            onFalha = {
                // Exibe um diálogo solicitando que o usuário ative a localização
                AlertDialog.Builder(context)
                    .setTitle("Ativar Localização")
                    .setMessage("Para o funcionamento adequado do app, ative a localização nas configurações.")
                    .setPositiveButton("Configurações") { _, _ ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(
                            context,
                            "O app pode não funcionar corretamente sem a localização.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .show()
                onFalha()
            }
        )
    }

    fun definirLocalizacaoReferencia(
        onSucesso: (Double, Double) -> Unit, // Retorna latitude e longitude ao sucesso
        onFalha: (String) -> Unit // Mensagem de erro ao falhar
    ) {
        acessarLocalizacao(
            onSucesso = { latitude, longitude ->
                val localizacaoReferencia = mapOf(
                    "localizacaoReferencia" to mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude
                    ),
                    "ultimaAtualizacao" to System.currentTimeMillis(),
                    "ultimoResponsavel" to (FirebaseAuth.getInstance().currentUser?.uid ?: "Desconhecido")
                )

                FirebaseFirestore.getInstance()
                    .collection("configuracoesGlobais")
                    .document("estadoGlobal") // Certifique-se de que o documento existe
                    .set(localizacaoReferencia, SetOptions.merge()) // Mesclar para preservar outros dados
                    .addOnSuccessListener {
                        onSucesso(latitude, longitude) // Retorna os valores atualizados
                    }
                    .addOnFailureListener { e ->
                        onFalha("Erro ao salvar a localização: ${e.message}")
                    }
            },
            onFalha = {
                onFalha("Não foi possível acessar a localização atual.")
            }
        )
    }


}
