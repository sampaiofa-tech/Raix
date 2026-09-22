package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PushDiagnostics

/**
 * Painel de diagnostico do pipeline de notificacao push.
 * Exibe estado em tempo real na tela, sem depender de adb/logcat.
 * Projetado para ser inserido temporariamente em qualquer tela.
 */
@Composable
fun PushDiagnosticsPanel(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val diag by PushDiagnostics.state.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0D1B2A),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DIAGNOSTICO PUSH",
                    color = Color(0xFF00FFC2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "[X]",
                    color = Color(0xFFFF8080),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            DiagRow("Token FCM", if (diag.fcmTokenExists) "SIM (${diag.fcmTokenPrefix})" else "NAO", diag.fcmTokenExists)
            DiagRow("Registrado no servidor", if (diag.tokenRegisteredOnServer) "SIM (${diag.tokenRegistrationTime})" else "NAO", diag.tokenRegisteredOnServer)
            DiagRow("Ultimo push recebido", if (diag.lastPushReceivedTime.isNotBlank()) "${diag.lastPushReceivedTime} de ${diag.lastPushSenderId.take(8)}" else "(nenhum)", diag.lastPushReceivedTime.isNotBlank())
            DiagRow("Worker rodou", if (diag.workerLastRunTime.isNotBlank()) "${diag.workerLastRunTime} (${diag.workerMsgCount} msgs)" else "(nenhum)", diag.workerLastRunTime.isNotBlank())
            DiagRow("Notificacao disparada", if (diag.lastNotificationTime.isNotBlank()) "${diag.lastNotificationTime} via ${diag.lastNotificationSource}" else "(nenhuma)", diag.lastNotificationTime.isNotBlank())
            DiagRow("Polling ativo", if (diag.pollingActive) "SIM (${diag.pollingLastCycle})" else "NAO", diag.pollingActive)
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (ok) Color(0xFF00E676) else Color(0xFFFF5252))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            color = Color(0xFF90A4AE),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
