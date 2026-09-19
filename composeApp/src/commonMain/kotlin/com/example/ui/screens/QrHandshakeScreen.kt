package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.network.IdentityNetworkClient
import com.example.security.identity.AesGcm
import com.example.security.identity.IdentityCurve25519
import com.example.security.identity.IdentityManager
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun QrHandshakeScreen(
    onHandshakeSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var handshakeToken by remember { mutableStateOf("Generating...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val ephemeralPriv = ByteArray(32).apply { Random.nextBytes(this) }
        val ephemeralPub = IdentityCurve25519.generatePublicKey(ephemeralPriv)
        val tokenBytes = ByteArray(32).apply { Random.nextBytes(this) }
        val tokenHex = tokenBytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        
        val pubKeyBase64 = Base64.encode(ephemeralPub)
        handshakeToken = "pmsg://invite?i=$tokenHex&fp=$pubKeyBase64"

        while (isActive) {
            delay(3000)
            try {
                val result = IdentityNetworkClient.pollHandshake(tokenHex)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        val peerPubKeyBase64 = data.first
                        val encryptedPayloadBase64 = data.second
                        
                        val peerPubKey = Base64.decode(peerPubKeyBase64)
                        val encryptedPayload = Base64.decode(encryptedPayloadBase64)
                        
                        val sharedSecret = IdentityCurve25519.computeSharedSecret(ephemeralPriv, peerPubKey)
                        val iv = encryptedPayload.sliceArray(0 until 12)
                        val ciphertext = encryptedPayload.sliceArray(12 until encryptedPayload.size)
                        
                        val decryptedWordsStr = AesGcm.decrypt(ciphertext, sharedSecret, iv).decodeToString()
                        val words = decryptedWordsStr.split(" ")
                        
                        val restoreResult = IdentityManager.restoreFromMnemonic(words)
                        if (restoreResult.isSuccess) {
                            onHandshakeSuccess()
                            break
                        } else {
                            errorMessage = "Handshake failed: Invalid identity payload."
                        }
                    }
                } else {
                    errorMessage = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0E17)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Link Device",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Scan this QR code from your authenticated mobile device to authorize this desktop session.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            val qrPainter = rememberQrCodePainter(handshakeToken) {
                shapes {
                    ball = QrBallShape.circle()
                    darkPixel = QrPixelShape.roundCorners()
                    frame = QrFrameShape.roundCorners(.25f)
                }
                colors {
                    dark = QrBrush.solid(Color.White)
                    light = QrBrush.solid(Color.Transparent)
                }
            }

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = qrPainter,
                    contentDescription = "Handshake QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}
