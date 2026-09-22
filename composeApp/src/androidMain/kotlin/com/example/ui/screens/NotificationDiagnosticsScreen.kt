package com.example.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.RaixActionPrimary
import com.example.ui.theme.RaixBackground
import com.example.ui.theme.RaixBorder
import com.example.ui.theme.RaixError
import com.example.ui.theme.RaixSurface
import com.example.ui.theme.RaixTextPrimary
import com.example.ui.theme.RaixTextSecondary
import com.example.util.NotificationHelper

/**
 * Tela de autodiagnostico de notificacoes.
 * Mostra permissao, canais, servico FCM e botao de teste.
 *
 * Android-only (androidMain).
 */
@Composable
fun NotificationDiagnosticsScreen(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit = {},
    onTestNotification: () -> Unit = {}
) {
    val context = LocalContext.current

    // Permissao POST_NOTIFICATIONS
    val hasPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    // Canal FCM
    val channelInfo = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = nm.getNotificationChannel(NotificationHelper.CHANNEL_NEW_CONVERSATIONS_ID)
            if (channel != null) {
                ChannelDiagInfo(
                    exists = true,
                    name = channel.name?.toString() ?: "(sem nome)",
                    id = channel.id,
                    importance = when (channel.importance) {
                        NotificationManager.IMPORTANCE_HIGH -> "Alta"
                        NotificationManager.IMPORTANCE_DEFAULT -> "Padrao"
                        NotificationManager.IMPORTANCE_LOW -> "Baixa"
                        NotificationManager.IMPORTANCE_MIN -> "Minima"
                        NotificationManager.IMPORTANCE_NONE -> "Desativada"
                        else -> "Desconhecida"
                    }
                )
            } else {
                ChannelDiagInfo(exists = false, name = "", id = NotificationHelper.CHANNEL_NEW_CONVERSATIONS_ID, importance = "")
            }
        } else {
            ChannelDiagInfo(exists = true, name = "Pre-Oreo (sem canais)", id = "N/A", importance = "N/A")
        }
    }

    // Servico FCM
    val fcmServiceRegistered = remember {
        try {
            val pm = context.packageManager
            val services = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SERVICES
            ).services
            services?.any { it.name.contains("FcmService") || it.name.contains("FirebaseMessaging") } ?: false
        } catch (_: Exception) {
            false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RaixBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaixSurface)
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = RaixTextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = RaixActionPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Diagnostico de Notificacoes",
                    color = RaixTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Card: Permissao
            DiagnosticCard(
                title = "Permissao POST_NOTIFICATIONS",
                status = hasPermission,
                statusText = if (hasPermission) "Concedida" else "Negada",
                detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    "Android 13+ requer permissao explicita"
                else
                    "Pre-Android 13: verificado via NotificationManager",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (!hasPermission) {
                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("request_permission_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaixActionPrimary)
                ) {
                    Text("Solicitar Permissao", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Card: Canal FCM
            DiagnosticCard(
                title = "Canal FCM",
                status = channelInfo.exists,
                statusText = if (channelInfo.exists) "Registrado" else "Nao encontrado",
                detail = if (channelInfo.exists)
                    "Nome: ${channelInfo.name}\nID: ${channelInfo.id}\nImportancia: ${channelInfo.importance}"
                else
                    "Canal esperado: ${channelInfo.id}\nO canal nao foi criado no sistema. Reinicie o app.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Card: Servico FCM
            DiagnosticCard(
                title = "Servico FCM (FcmService)",
                status = fcmServiceRegistered,
                statusText = if (fcmServiceRegistered) "Registrado no Manifest" else "Nao encontrado",
                detail = if (fcmServiceRegistered)
                    "O servico de mensagens Firebase esta declarado e ativo."
                else
                    "Nenhum servico FCM encontrado no PackageInfo. Verifique o AndroidManifest.xml.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Botao de teste
            Button(
                onClick = onTestNotification,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .testTag("test_notification_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RaixActionPrimary,
                    contentColor = RaixBackground
                ),
                enabled = hasPermission && channelInfo.exists
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Enviar Notificacao de Teste",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            if (!hasPermission || !channelInfo.exists) {
                Text(
                    text = "Botao desabilitado: corrija os itens acima primeiro.",
                    color = RaixError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Card de diagnostico individual com indicador de status.
 */
@Composable
private fun DiagnosticCard(
    title: String,
    status: Boolean,
    statusText: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RaixSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = RaixTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (status) RaixActionPrimary else RaixError,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = if (status) RaixActionPrimary else RaixError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = RaixBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = detail,
                color = RaixTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Dados de diagnostico do canal de notificacao.
 */
private data class ChannelDiagInfo(
    val exists: Boolean,
    val name: String,
    val id: String,
    val importance: String
)
