package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.security.MasterPasswordManager

@Composable
actual fun AppLockGate(
    onUnlocked: () -> Unit
) {
    if (!MasterPasswordManager.isMasterPasswordSet()) {
        MasterPasswordSetupScreen(onSetupComplete = onUnlocked)
    } else {
        PinLockScreen(
            onVerifyPin = { MasterPasswordManager.verifyMasterPassword(it) },
            onUnlocked = onUnlocked
        )
    }
}
