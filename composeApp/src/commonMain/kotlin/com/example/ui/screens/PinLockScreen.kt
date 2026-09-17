package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinLockScreen(
    onUnlocked: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 4) {
            // For Desktop/iOS/WasmJs we stub to 1234 for testing Phase 3
            if (pinInput == "1234") {
                onUnlocked()
            } else {
                authError = "PIN Incorreto (Dica: 1234)"
                pinInput = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "RAIX", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "APLICATIVO BLOQUEADO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Digite o PIN de Segurança",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = authError ?: "",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = pinInput.length > index
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.5.dp, if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val keyRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "Del")
                )

                keyRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Box(modifier = Modifier.size(54.dp))
                                return@forEach
                            }
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (key == "Del") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.8.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable {
                                        when (key) {
                                            "Del" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                            else -> if (pinInput.length < 4) pinInput += key
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "Del" -> Icon(Icons.Default.Backspace, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    else -> Text(text = key, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
