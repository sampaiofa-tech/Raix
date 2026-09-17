package com.example.ui.screens

import androidx.compose.runtime.Composable

@Composable
actual fun AppLockGate(
    onUnlocked: () -> Unit
) {
    BiometricLockScreen(
        onVerifyPin = { true }, // Stub for now, in a real app this verifies via secure prefs
        biometricEnabled = true,
        autoLockTimeoutMinutes = 0,
        onDuressTriggered = {},
        onUnlocked = onUnlocked
    )
}
