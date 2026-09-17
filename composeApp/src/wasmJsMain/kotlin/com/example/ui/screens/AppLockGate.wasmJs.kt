package com.example.ui.screens

import androidx.compose.runtime.Composable

@Composable
actual fun AppLockGate(
    onUnlocked: () -> Unit
) {
    PinLockScreen(
        onUnlocked = onUnlocked
    )
}
