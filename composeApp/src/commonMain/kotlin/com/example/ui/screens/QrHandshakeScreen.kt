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
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
@Composable
fun QrHandshakeScreen(
    onHandshakeSuccess: () -> Unit,
    onBack: () -> Unit
) {
    // A temporary ephemeral handshake token
    var handshakeToken by remember { mutableStateOf("Generating...") }

    LaunchedEffect(Unit) {
        // In a real scenario, this would generate an ephemeral key pair and register the 
        // public key on the server to wait for the mobile device to scan and complete handshake.
        handshakeToken = "raix-handshake-v1:ephemeral-${kotlin.random.Random.nextInt(1000, 9999)}"
    }

    Scaffold(
        containerColor = Color(0xFF0A0E17) // Dark background for Raix
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

            // Display QR Code using qrose
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

            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}
