package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RaixBackground
import com.example.ui.theme.RaixSurface
import com.example.ui.theme.RaixSurfaceElevated
import com.example.ui.theme.RaixTextPrimary
import com.example.ui.theme.RaixTextSecondary
import com.example.ui.theme.RaixActionPrimary
import com.example.ui.theme.RaixBorder
import com.example.util.security.SecurePrefsHelper

/**
 * Tela de criacao de senha mestra (PIN de 4 digitos) para o onboarding Android.
 * Espelha o fluxo do desktop: digitar PIN -> confirmar -> salvar via SecurePrefsHelper.
 * Apos criar o PIN, o usuario pode opcionalmente ativar biometria nas Configuracoes.
 */
@Composable
fun MasterPasswordSetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 4) {
            if (firstPin == null) {
                firstPin = pinInput
                pinInput = ""
            } else {
                if (firstPin == pinInput) {
                    SecurePrefsHelper.setPin(context, pinInput)
                    SecurePrefsHelper.setAutoLockEnabled(context, true)
                    onSetupComplete()
                } else {
                    error = "Os PINs nao coincidem. Tente novamente."
                    firstPin = null
                    pinInput = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaixBackground)
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
                imageVector = Icons.Default.LockPerson,
                contentDescription = null,
                tint = RaixActionPrimary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "CRIAR SENHA MESTRA",
                fontSize = 20.sp,
                color = RaixTextPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Proteja seu aplicativo com um PIN de 4 digitos",
                fontSize = 12.sp,
                color = RaixTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(RaixSurface)
                    .border(1.dp, RaixBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = if (firstPin == null) "Digite um PIN de 4 digitos" else "Confirme seu PIN",
                    fontWeight = FontWeight.Bold,
                    color = RaixTextPrimary,
                    fontSize = 15.sp
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error ?: "",
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
                                .background(if (isFilled) RaixActionPrimary else RaixSurfaceElevated)
                                .border(
                                    1.5.dp,
                                    if (isFilled) RaixActionPrimary else RaixBorder,
                                    CircleShape
                                )
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
                                    .background(if (key == "Del") RaixSurfaceElevated else RaixSurface)
                                    .border(0.8.dp, RaixBorder, CircleShape)
                                    .clickable {
                                        error = null
                                        when (key) {
                                            "Del" -> if (pinInput.isNotEmpty()) pinInput =
                                                pinInput.dropLast(1)

                                            else -> if (pinInput.length < 4) pinInput += key
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "Del" -> Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Apagar",
                                        tint = RaixTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    else -> Text(
                                        text = key,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaixTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Voce podera ativar biometria nas Configuracoes",
                fontSize = 11.sp,
                color = RaixTextSecondary
            )
        }
    }
}
