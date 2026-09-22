package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.network.IdentityNetworkClient
import com.example.security.identity.AesGcm
import com.example.security.identity.IdentityCurve25519
import com.example.security.identity.IdentityManager
import com.example.ui.theme.RaixActionPrimary
import com.example.ui.theme.RaixBackground
import com.example.ui.theme.RaixError
import com.example.ui.theme.RaixTextPrimary
import com.example.ui.theme.RaixTextSecondary
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Tela de QR Handshake para Android.
 * Usa Google Code Scanner (ML Kit) para ler o QR gerado pelo desktop
 * e completar o pareamento criptografico.
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun QrScannerHandshakeScreen(
    onHandshakeSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scanState by remember { mutableStateOf("IDLE") } // IDLE, SCANNING, PROCESSING, SUCCESS, ERROR
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            errorMessage = "Permissao de camera negada. Necessaria para escanear QR."
        }
    }

    // Solicitar permissao de camera se ainda nao concedida
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun startScan() {
        scanState = "SCANNING"
        errorMessage = null

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(context, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null && rawValue.startsWith("pmsg://invite?")) {
                    scanState = "PROCESSING"
                    scope.launch {
                        processHandshake(rawValue, onHandshakeSuccess) { error ->
                            scanState = "ERROR"
                            errorMessage = error
                        }
                    }
                } else {
                    scanState = "ERROR"
                    errorMessage = "QR invalido. Escaneie o QR gerado pelo desktop Raix."
                }
            }
            .addOnCanceledListener {
                scanState = "IDLE"
            }
            .addOnFailureListener { e ->
                scanState = "ERROR"
                errorMessage = "Falha ao escanear: ${e.localizedMessage}"
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaixBackground)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val icon = when (scanState) {
                "SUCCESS" -> Icons.Default.CheckCircle
                "ERROR" -> Icons.Default.Warning
                else -> Icons.Default.QrCodeScanner
            }
            val iconTint = when (scanState) {
                "SUCCESS" -> RaixActionPrimary
                "ERROR" -> RaixError
                else -> RaixTextPrimary
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "QR HANDSHAKE",
                fontSize = 22.sp,
                color = RaixTextPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (scanState) {
                    "IDLE" -> "Escaneie o QR exibido no desktop para parear este dispositivo."
                    "SCANNING" -> "Posicione a camera sobre o QR do desktop..."
                    "PROCESSING" -> "Processando handshake criptografico..."
                    "SUCCESS" -> "Pareamento concluido com sucesso."
                    "ERROR" -> errorMessage ?: "Erro desconhecido."
                    else -> ""
                },
                fontSize = 14.sp,
                color = if (scanState == "ERROR") RaixError else RaixTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (scanState == "PROCESSING") {
                CircularProgressIndicator(
                    color = RaixActionPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            if (scanState == "IDLE" || scanState == "ERROR") {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            startScan()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RaixActionPrimary,
                        contentColor = RaixBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (scanState == "ERROR") "Tentar novamente" else "Escanear QR",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (scanState == "SUCCESS") {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    onHandshakeSuccess()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Voltar", color = RaixTextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private suspend fun processHandshake(
    qrData: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Extrair parametros do QR: pmsg://invite?i=TOKEN&fp=PUBKEY
        val uri = android.net.Uri.parse(qrData)
        val tokenHex = uri.getQueryParameter("i") ?: run {
            onError("QR sem token de convite.")
            return
        }
        val peerPubKeyBase64 = uri.getQueryParameter("fp") ?: run {
            onError("QR sem chave publica.")
            return
        }

        // Gerar par efemero
        val ephemeralPriv = ByteArray(32).apply { kotlin.random.Random.nextBytes(this) }
        val ephemeralPub = IdentityCurve25519.generatePublicKey(ephemeralPriv)

        // Derivar segredo compartilhado
        val peerPubKey = Base64.decode(peerPubKeyBase64)
        val sharedSecret = IdentityCurve25519.computeSharedSecret(ephemeralPriv, peerPubKey)

        // Criptografar identidade local para enviar ao desktop
        val localWords = IdentityManager.getMnemonicWords()
        if (localWords.isNullOrEmpty()) {
            onError("Identidade local nao encontrada.")
            return
        }
        val payload = localWords.joinToString(" ").encodeToByteArray()
        val iv = ByteArray(12).apply { kotlin.random.Random.nextBytes(this) }
        val ciphertext = AesGcm.encrypt(payload, sharedSecret, iv)
        val encryptedPayload = iv + ciphertext

        val ephemeralPubBase64 = Base64.encode(ephemeralPub)
        val encryptedPayloadBase64 = Base64.encode(encryptedPayload)

        // Enviar resposta ao servidor
        val result = IdentityNetworkClient.submitHandshake(tokenHex, ephemeralPubBase64, encryptedPayloadBase64, "anonymous_token")
        if (result.isSuccess) {
            onSuccess()
        } else {
            onError("Falha ao enviar resposta: ${result.exceptionOrNull()?.message}")
        }
    } catch (e: Exception) {
        onError("Erro no handshake: ${e.localizedMessage}")
    }
}
